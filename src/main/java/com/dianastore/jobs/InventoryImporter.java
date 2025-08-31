package com.dianastore.jobs;

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

@Component
public class InventoryImporter {

    private static final Logger log = LoggerFactory.getLogger(InventoryImporter.class);
    private final InventoryRepository inventoryRepo;

    @Value("${feed.location:./feeds}")
    private String feedLocation;

    public InventoryImporter(InventoryRepository inventoryRepo) {
        this.inventoryRepo = inventoryRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        runInventoryImport();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledRun() {
        runInventoryImport();
    }

    @Transactional
    public void runInventoryImport() {
        long feedId = Instant.now().toEpochMilli();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Path sourcePath = Path.of(feedLocation, "inventory_feed_5000.csv");

        try (
                Reader fileReader = Files.newBufferedReader(sourcePath);
                CSVReader csvReader = new CSVReader(fileReader)
        ) {
            csvReader.readNext(); // skip header
            String[] row;
            while ((row = csvReader.readNext()) != null) {
                String countryCode = row[0].trim();
                String sku = row[1].trim();
                int stock = Integer.parseInt(row[2]);
                int isDisabled = Integer.parseInt(row[3]);

                inventoryRepo.insertOrUpdateNative(
                        sku, countryCode, stock, isDisabled, feedId, now
                );
            }

            inventoryRepo.markNotInFeedIdAsMarkedForDelete(feedId);

        } catch (Exception e) {
            log.error("Inventory import failed: {}", e.getMessage(), e);
        }

        try {
            Path processedDir = Path.of(feedLocation, "processed");
            Files.createDirectories(processedDir);
            Path target = processedDir.resolve("inventory_feed_" + feedId + ".csv");
            Files.move(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            log.error("Failed to move processed file: {}", ex.getMessage(), ex);
        }

        log.info("Inventory import completed. FeedId={}", feedId);
    }
}
