package com.dianastore.jobs;

import com.dianastore.jobs.entities.Inventory;
import com.dianastore.jobs.repository.InventoryRepository;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class InventoryImporter {
    private static final Logger log = LoggerFactory.getLogger(InventoryImporter.class);
    private final InventoryRepository inventoryRepo;

    @Value("${feed.location:./feeds}")
    private String feedLocation;

    private static final int BATCH_SIZE = 500;

    public InventoryImporter(InventoryRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        log.info("CSV import at startup for inventories from {}", feedLocation);
        runInventoryImport();
    }

    // ✅ Runs daily at 01:00
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledRun() {
        log.info("Scheduled CSV import triggered");
        runInventoryImport();
    }

    @Transactional
    public void runInventoryImport() {
        log.info("Starting inventory CSV import");
        long feedId = Instant.now().toEpochMilli();
        Path sourcePath = Path.of(feedLocation, "inventory_feed_5000.csv");

        // ✅ One timestamp for the entire run
        Timestamp runTimestamp = new Timestamp(System.currentTimeMillis());

        List<Inventory> incoming = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        try (
                Reader fileReader = Files.newBufferedReader(sourcePath); // ✅ avoids locking issues
                CSVReader r = new CSVReader(fileReader)
        ) {
            r.readNext(); // skip header
            String[] row;
            int rowNum = 1;
            while ((row = r.readNext()) != null) {
                rowNum++;
                try {
                    String country = safe(row[0]);
                    String sku = safe(row[1]);
                    if (country == null || sku == null) {
                        log.warn("Skipping row {} missing keys", rowNum);
                        continue;
                    }
                    String key = country + "|" + sku;
                    if (!seen.add(key)) {
                        log.info("Skipping duplicate row {} -> {}", rowNum, key);
                        continue;
                    }

                    Integer stock = CsvUtils.parseInteger(row[2]);
                    Boolean disabled = CsvUtils.parseBoolean(row[3]);
                    Timestamp created = CsvUtils.parseSqlTimestamp(row[4]);
                    Timestamp modified = CsvUtils.parseSqlTimestamp(row[5]);

                    Inventory i = new Inventory();
                    i.setCountryCode(country);
                    i.setSku(sku);
                    i.setStock(stock == null ? 0 : stock);
                    i.setIsDisabled(disabled == null ? false : disabled);

                    // ✅ fallback to runTimestamp if missing
                    i.setCreationTimestamp(created != null ? created : runTimestamp);
                    i.setModifiedTimestamp(modified != null ? modified : runTimestamp);

                    i.setFeedId(feedId);
                    i.setMarkedForDelete(false);

                    incoming.add(i);
                } catch (Exception ex) {
                    log.warn("Skipping row {} due to error: {}", rowNum, ex.getMessage());
                }
            }

            // group by country
            Map<String, List<Inventory>> byCountry = incoming.stream()
                    .collect(Collectors.groupingBy(Inventory::getCountryCode));
            List<Inventory> toSave = new ArrayList<>();

            for (Map.Entry<String, List<Inventory>> e : byCountry.entrySet()) {
                String country = e.getKey();
                List<Inventory> bucket = e.getValue();
                List<String> skus = bucket.stream().map(Inventory::getSku).distinct().collect(Collectors.toList());

                List<Inventory> existing = inventoryRepo.findAllByCountryCodeAndSkuIn(country, skus);
                Map<String, Inventory> existingMap = existing.stream()
                        .collect(Collectors.toMap(i -> i.getCountryCode() + "|" + i.getSku(), i -> i));

                for (Inventory inc : bucket) {
                    String key = inc.getCountryCode() + "|" + inc.getSku();
                    if (existingMap.containsKey(key)) {
                        Inventory db = existingMap.get(key);
                        db.setStock(inc.getStock());
                        db.setIsDisabled(inc.getIsDisabled());

                        // ✅ prefer incoming modified, else keep db, else fallback
                        db.setModifiedTimestamp(
                                inc.getModifiedTimestamp() != null ? inc.getModifiedTimestamp()
                                        : (db.getModifiedTimestamp() != null ? db.getModifiedTimestamp() : runTimestamp)
                        );

                        // ✅ keep db.created if exists
                        db.setCreationTimestamp(
                                db.getCreationTimestamp() != null ? db.getCreationTimestamp()
                                        : (inc.getCreationTimestamp() != null ? inc.getCreationTimestamp() : runTimestamp)
                        );

                        db.setFeedId(feedId);
                        db.setMarkedForDelete(false);
                        toSave.add(db);
                    } else {
                        inc.setFeedId(feedId);
                        inc.setMarkedForDelete(false);
                        toSave.add(inc);
                    }

                    if (toSave.size() >= BATCH_SIZE) {
                        inventoryRepo.saveAll(toSave);
                        toSave.clear();
                    }
                }
            }

            if (!toSave.isEmpty()) inventoryRepo.saveAll(toSave);

            // mark missing as deleted
            inventoryRepo.markNotInFeedIdAsMarkedForDelete(feedId);

        } catch (java.nio.file.NoSuchFileException nsf) {
            log.warn("No inventory file at {}. Skipping import.", sourcePath);
            return; // ✅ nothing to archive
        } catch (Exception ex) {
            log.error("Inventory import failed: {}", ex.getMessage(), ex);
            return; // ✅ avoid moving broken files
        }

        // ✅ move file after reader is closed
        try {
            Path processedDir = Path.of(feedLocation, "processed");
            Files.createDirectories(processedDir);
            Path target = processedDir.resolve(
                    sourcePath.getFileName().toString().replace(".csv", "_" + feedId + ".csv"));
            Files.move(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Inventory import completed successfully. FeedId={}", feedId);
        } catch (Exception moveEx) {
            log.error("Failed to move processed file {}: {}", sourcePath, moveEx.getMessage(), moveEx);
        }
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
