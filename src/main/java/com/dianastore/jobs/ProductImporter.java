package com.dianastore.jobs;

import com.dianastore.repository.ProductRepository;
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
public class ProductImporter {

    private static final Logger log = LoggerFactory.getLogger(ProductImporter.class);
    private final ProductRepository productRepo;

    @Value("${feed.location:./feeds}")
    private String feedLocation;

    public ProductImporter(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        runProductImport();
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledRun() {
        runProductImport();
    }
    @Transactional
    public void runProductImport() {
        long feedId = Instant.now().toEpochMilli();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Path sourcePath = Path.of(feedLocation, "product_feed_5000.csv");

        try (Reader fileReader = Files.newBufferedReader(sourcePath);
             CSVReader csvReader = new CSVReader(fileReader)) {

            csvReader.readNext(); // skip header
            String[] row;
            int rowNum = 1;

            while ((row = csvReader.readNext()) != null) {
                rowNum++;
                try {
                    String countryCode = safe(row.length > 0 ? row[0] : null);
                    String sku = safe(row.length > 1 ? row[1] : null);
                    String shortDescription = safe(row.length > 2 ? row[2] : null);
                    String longDescription = safe(row.length > 3 ? row[3] : null);
                    String imageUrl = safe(row.length > 4 ? row[4] : null);
                    String productName = safe(row.length > 5 ? row[5] : null);
                    String category = safe(row.length > 6 ? row[6] : null);
                    String size = safe(row.length > 7 ? row[7] : null);
                    String colour = safe(row.length > 8 ? row[8] : null);
                    String style = safe(row.length > 9 ? row[9] : null);
                    String notes = safe(row.length > 10 ? row[10] : null);

                    if (countryCode == null || sku == null) {
                        log.warn("Skipping row {}: missing countryCode or sku", rowNum);
                        continue;
                    }

                    productRepo.insertOrUpdateNative(
                            countryCode, sku,
                            shortDescription, longDescription, imageUrl, productName,
                            category, size, colour, style, notes,
                            feedId, now
                    );

                } catch (Exception ex) {
                    log.warn("Skipping row {} due to error: {}", rowNum, ex.getMessage());
                }
            }

            // Missing SKUs → mark as out of stock
            productRepo.markMissingAsOutOfStock(feedId);

        } catch (java.nio.file.NoSuchFileException nsf) {
            log.warn("No product file at {}. Skipping import.", sourcePath);
        } catch (Exception ex) {
            log.error("Product import failed: {}", ex.getMessage(), ex);
        }

        try {
            Path processedDir = Path.of(feedLocation, "processed");
            Files.createDirectories(processedDir);
            Path target = processedDir.resolve("product_feed_" + feedId + ".csv");
            Files.move(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception moveEx) {
            log.error("Failed to move processed file: {}", moveEx.getMessage(), moveEx);
        }

        log.info("Product import completed. FeedId={}", feedId);
    }


    private static String safe(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
