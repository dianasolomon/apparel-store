package com.dianastore.jobs;

import com.dianastore.jobs.entities.Product;
import com.dianastore.jobs.repository.ProductRepository;
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
public class ProductImporter {
    private static final Logger log = LoggerFactory.getLogger(ProductImporter.class);
    private final ProductRepository productRepo;

    @Value("${feed.location:./feeds}")
    private String feedLocation;

    private static final int BATCH_SIZE = 500;

    public ProductImporter(ProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStartup() {
        log.info("CSV import at startup for products from {}", feedLocation);
        runProductImport();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void scheduledRun() {
        runProductImport();
    }

    @Transactional
    public void runProductImport() {
        log.info("Starting product CSV import");
        long feedId = Instant.now().toEpochMilli();
        Timestamp runTimestamp = new Timestamp(System.currentTimeMillis());

        // ✅ Input file is always product_feed_5000.csv
        Path sourcePath = Path.of(feedLocation, "product_feed_5000.csv");
        // ✅ Processed folder
        Path processedDir = Path.of(feedLocation, "processed");

        try {
            Files.createDirectories(processedDir);

            if (!Files.exists(sourcePath)) {
                log.warn("No product feed file at {}. Skipping import.", sourcePath);
                return;
            }

            List<Product> incoming = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            // --- STEP 1: Read CSV ---
            try (CSVReader r = new CSVReader(new FileReader(sourcePath.toFile()))) {
                r.readNext(); // skip header
                String[] row;
                int rowNum = 1;

                while ((row = r.readNext()) != null) {
                    rowNum++;
                    try {
                        String country = safe(row[0]);
                        String sku = safe(row[1]);

                        if (country == null || sku == null) continue;

                        String compositeKey = country + "|" + sku;
                        if (!seen.add(compositeKey)) continue;

                        Product p = new Product();
                        p.setCountryCode(country);
                        p.setSku(sku);
                        p.setShortDescription(safeNullable(row[2]));
                        p.setMediumDescription(safeNullable(row[3]));
                        p.setLongDescription(safeNullable(row[4]));
                        p.setImageUrl(safeNullable(row[5]));
                        p.setProductName(safeNullable(row[6]));
                        p.setCategory(safeNullable(row[7]));
                        p.setSize(safeNullable(row[8]));
                        p.setColour(safeNullable(row[9]));
                        p.setStyle(safeNullable(row[10]));
                        p.setNotes(safeNullable(row[11]));

                        Timestamp created = CsvUtils.parseSqlTimestamp(row.length > 12 ? row[12] : null);
                        Timestamp modified = CsvUtils.parseSqlTimestamp(row.length > 13 ? row[13] : null);
                        Boolean outOfStockBool = (row.length > 14) ? CsvUtils.parseBoolean(row[14]) : null;
                        boolean outOfStock = (outOfStockBool != null) ? outOfStockBool : false;

                        p.setCreationTimestamp(created != null ? created : runTimestamp);
                        p.setModifiedTimestamp(modified != null ? modified : runTimestamp);
                        p.setOutOfStock(outOfStock);
                        p.setFeedId(feedId);
                        p.setMarkedForDelete(false);

                        incoming.add(p);
                    } catch (Exception ex) {
                        log.warn("Skipping row {} due to data error: {}", rowNum, ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.error("Failed reading CSV {}: {}", sourcePath.getFileName(), ex.getMessage());
                return;
            }

            // --- STEP 2: Save to DB ---
            try {
                Map<String, List<Product>> byCountry = incoming.stream()
                        .collect(Collectors.groupingBy(Product::getCountryCode));
                List<Product> toSave = new ArrayList<>();

                for (Map.Entry<String, List<Product>> entry : byCountry.entrySet()) {
                    String country = entry.getKey();
                    List<Product> bucket = entry.getValue();
                    List<String> skus = bucket.stream().map(Product::getSku).distinct().toList();
                    List<Product> existing = productRepo.findAllByCountryCodeAndSkuIn(country, skus);

                    Map<String, Product> existingMap = existing.stream()
                            .collect(Collectors.toMap(p -> p.getCountryCode() + "|" + p.getSku(), p -> p));

                    for (Product inc : bucket) {
                        String key = inc.getCountryCode() + "|" + inc.getSku();
                        if (existingMap.containsKey(key)) {
                            Product db = existingMap.get(key);
                            db.setShortDescription(inc.getShortDescription());
                            db.setMediumDescription(inc.getMediumDescription());
                            db.setLongDescription(inc.getLongDescription());
                            db.setImageUrl(inc.getImageUrl());
                            db.setProductName(inc.getProductName());
                            db.setCategory(inc.getCategory());
                            db.setSize(inc.getSize());
                            db.setColour(inc.getColour());
                            db.setStyle(inc.getStyle());
                            db.setNotes(inc.getNotes());
                            db.setOutOfStock(inc.isOutOfStock());

                            db.setModifiedTimestamp(
                                    inc.getModifiedTimestamp() != null
                                            ? inc.getModifiedTimestamp()
                                            : (db.getModifiedTimestamp() != null ? db.getModifiedTimestamp() : runTimestamp)
                            );
                            db.setCreationTimestamp(
                                    db.getCreationTimestamp() != null
                                            ? db.getCreationTimestamp()
                                            : (inc.getCreationTimestamp() != null ? inc.getCreationTimestamp() : runTimestamp)
                            );
                            db.setFeedId(feedId);
                            db.setMarkedForDelete(false);
                            toSave.add(db);
                        } else {
                            toSave.add(inc);
                        }

                        if (toSave.size() >= BATCH_SIZE) {
                            productRepo.saveAll(toSave);
                            toSave.clear();
                        }
                    }
                }

                if (!toSave.isEmpty()) productRepo.saveAll(toSave);
                productRepo.markNotInFeedIdAsMarkedForDelete(feedId);
            } catch (Exception ex) {
                log.error("Failed saving CSV {}: {}", sourcePath.getFileName(), ex.getMessage(), ex);
            }

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
            log.error("Product import failed: {}", ex.getMessage(), ex);
        }
    }

    private static String safe(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String safeNullable(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
