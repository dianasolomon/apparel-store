package com.dianastore.jobs;

import com.dianastore.jobs.entities.Price;
import com.dianastore.jobs.repository.PriceRepository;
import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PriceImporter {
    private static final Logger log = LoggerFactory.getLogger(PriceImporter.class);
    private final PriceRepository priceRepo;

    @Value("${feed.location:./feeds}")
    private String feedLocation;

    private static final int BATCH_SIZE = 500;

    public PriceImporter(PriceRepository priceRepo) {
        this.priceRepo = priceRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        log.info("CSV import at startup for prices from {}", feedLocation);
        runPriceImport();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledRun() {
        runPriceImport();
    }

    @Transactional
    public void runPriceImport() {
        log.info("Starting price CSV import");

        long feedId = Instant.now().toEpochMilli();
        Timestamp runTimestamp = new Timestamp(System.currentTimeMillis());

        // input file
        Path sourcePath = Path.of(feedLocation, "price_feed_5000.csv");
        // processed dir
        Path processedDir = Path.of(feedLocation, "processed");

        try {
            Files.createDirectories(processedDir);

            if (!Files.exists(sourcePath)) {
                log.warn("No price feed file at {}. Skipping import.", sourcePath);
                return;
            }

            List<Price> incoming = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            // --- STEP 1: Read CSV ---
            try (CSVReader r = new CSVReader(new FileReader(sourcePath.toFile()))) {
                r.readNext(); // skip header
                String[] row;
                int rowNum = 1;

                while ((row = r.readNext()) != null) {
                    rowNum++;
                    try {
                        String sku = safe(row[0]);
                        String country = safe(row[1]);
                        if (sku == null || country == null) continue;

                        String key = country + "|" + sku;
                        if (!seen.add(key)) continue;

                        Price p = new Price();
                        p.setSku(sku);
                        p.setCountryCode(country);
                        p.setMsrp(CsvUtils.parseDouble(row[2]));
                        p.setDiscount(CsvUtils.parseDouble(row[3]));
                        p.setSellingPrice(CsvUtils.parseDouble(row[4]));
                        p.setCreationTimestamp(CsvUtils.parseSqlTimestamp(row[5]) != null
                                ? CsvUtils.parseSqlTimestamp(row[5])
                                : runTimestamp);
                        p.setModifiedTimestamp(CsvUtils.parseSqlTimestamp(row[6]) != null
                                ? CsvUtils.parseSqlTimestamp(row[6])
                                : runTimestamp);
                        p.setFeedId(feedId);
                        p.setMarkedForDelete(false);

                        incoming.add(p);
                    } catch (Exception ex) {
                        log.warn("Skipping row {} due to data error: {}", rowNum, ex.getMessage());
                    }
                }
            }

            // --- STEP 2: Save to DB ---
            Map<String, List<Price>> byCountry = incoming.stream()
                    .collect(Collectors.groupingBy(Price::getCountryCode));
            List<Price> toSave = new ArrayList<>();

            for (Map.Entry<String, List<Price>> entry : byCountry.entrySet()) {
                String country = entry.getKey();
                List<Price> bucket = entry.getValue();
                List<String> skus = bucket.stream().map(Price::getSku).distinct().toList();
                List<Price> existing = priceRepo.findAllByCountryCodeAndSkuIn(country, skus);

                Map<String, Price> existingMap = existing.stream()
                        .collect(Collectors.toMap(p -> p.getCountryCode() + "|" + p.getSku(), p -> p));

                for (Price inc : bucket) {
                    String key = inc.getCountryCode() + "|" + inc.getSku();
                    if (existingMap.containsKey(key)) {
                        Price db = existingMap.get(key);
                        db.setMsrp(inc.getMsrp());
                        db.setDiscount(inc.getDiscount());
                        db.setSellingPrice(inc.getSellingPrice());
                        db.setModifiedTimestamp(inc.getModifiedTimestamp() != null
                                ? inc.getModifiedTimestamp()
                                : db.getModifiedTimestamp() != null
                                ? db.getModifiedTimestamp()
                                : runTimestamp);
                        db.setFeedId(feedId);
                        db.setMarkedForDelete(false);
                        toSave.add(db);
                    } else {
                        toSave.add(inc);
                    }

                    if (toSave.size() >= BATCH_SIZE) {
                        priceRepo.saveAll(toSave);
                        toSave.clear();
                    }
                }
            }

            if (!toSave.isEmpty()) priceRepo.saveAll(toSave);
            priceRepo.markNotInFeedIdAsMarkedForDelete(feedId);

            // --- STEP 3: Move file to processed folder ---
            try {
                Path target = processedDir.resolve(
                        sourcePath.getFileName().toString().replace(".csv", "_" + feedId + ".csv"));
                Files.move(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("CSV {} moved to processed folder", sourcePath.getFileName());
            } catch (Exception ex) {
                log.error("Failed moving CSV {} to processed folder: {}", sourcePath.getFileName(), ex.getMessage(), ex);
            }

        } catch (Exception ex) {
            log.error("Price import failed: {}", ex.getMessage(), ex);
        }
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
