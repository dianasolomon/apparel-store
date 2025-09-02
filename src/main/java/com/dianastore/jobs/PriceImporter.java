package com.dianastore.jobs;

import com.dianastore.repository.PriceRepository;
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
public class PriceImporter {

    private static final Logger log = LoggerFactory.getLogger(PriceImporter.class);
    private final PriceRepository priceRepo;

    @Value("${feed.location:./feeds}")
    private String feedLocation;

    public PriceImporter(PriceRepository priceRepo) {
        this.priceRepo = priceRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        runPriceImport();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledRun() {
        runPriceImport();
    }

    @Transactional
    public void runPriceImport() {
        long feedId = Instant.now().toEpochMilli();
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Path sourcePath = Path.of(feedLocation, "price_feed_5000.csv");

        try (
                Reader fileReader = Files.newBufferedReader(sourcePath);
                CSVReader csvReader = new CSVReader(fileReader)
        ) {
            csvReader.readNext(); // skip header
            String[] row;
            while ((row = csvReader.readNext()) != null) {
                String sku = row[0].trim();
                String countryCode = row[1].trim();
                double msrp = Double.parseDouble(row[2]);
                double discount = Double.parseDouble(row[3]);
                double sellingPrice = Double.parseDouble(row[4]);

                priceRepo.insertOrUpdateNative(
                        sku, countryCode, msrp, discount, sellingPrice, feedId, now
                );
            }

            priceRepo.markNotInFeedIdAsMarkedForDelete(feedId);

        } catch (Exception e) {
            log.error("Price import failed: {}", e.getMessage(), e);
        }

        try {
            Path processedDir = Path.of(feedLocation, "processed");
            Files.createDirectories(processedDir);
            Path target = processedDir.resolve("price_feed_" + feedId + ".csv");
            Files.move(sourcePath, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            log.error("Failed to move processed file: {}", ex.getMessage(), ex);
        }

        log.info("Price import completed. FeedId={}", feedId);
    }
}
