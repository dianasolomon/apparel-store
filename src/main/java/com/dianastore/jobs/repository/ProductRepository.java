package com.dianastore.jobs.repository;

import com.dianastore.jobs.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO products
            (country_code, sku, short_description, long_description, image_url, product_name,
             category, size, colour, style, notes, out_of_stock, feed_id, creation_timestamp, modified_timestamp)
            VALUES (:countryCode, :sku, :shortDescription, :longDescription, :imageUrl, :productName,
                    :category, :size, :colour, :style, :notes, FALSE, :feedId, :now, :now)
            ON DUPLICATE KEY UPDATE
            short_description = IF(VALUES(short_description) <> short_description, VALUES(short_description), short_description),
            long_description  = IF(VALUES(long_description)  <> long_description,  VALUES(long_description),  long_description),
            image_url         = IF(VALUES(image_url)         <> image_url,         VALUES(image_url),         image_url),
            product_name      = IF(VALUES(product_name)      <> product_name,      VALUES(product_name),      product_name),
            category          = IF(VALUES(category)          <> category,          VALUES(category),          category),
            size              = IF(VALUES(size)              <> size,              VALUES(size),              size),
            colour            = IF(VALUES(colour)            <> colour,            VALUES(colour),            colour),
            style             = IF(VALUES(style)             <> style,             VALUES(style),             style),
            notes             = IF(VALUES(notes)             <> notes,             VALUES(notes),             notes),
            out_of_stock   = FALSE,
            modified_timestamp = IF(
                (VALUES(short_description) <> short_description) OR
                (VALUES(long_description)  <> long_description)  OR
                (VALUES(image_url)         <> image_url)         OR
                (VALUES(product_name)      <> product_name)      OR
                (VALUES(category)          <> category)          OR
                (VALUES(size)              <> size)              OR
                (VALUES(colour)            <> colour)            OR
                (VALUES(style)             <> style)             OR
                (VALUES(notes)             <> notes)             OR
                (out_of_stock = TRUE),
                :now, modified_timestamp),
            feed_id = :feedId
            """, nativeQuery = true)
    void insertOrUpdateNative(
            @Param("countryCode") String countryCode,
            @Param("sku") String sku,
            @Param("shortDescription") String shortDescription,
            @Param("longDescription") String longDescription,
            @Param("imageUrl") String imageUrl,
            @Param("productName") String productName,
            @Param("category") String category,
            @Param("size") String size,
            @Param("colour") String colour,
            @Param("style") String style,
            @Param("notes") String notes,
            @Param("feedId") Long feedId,
            @Param("now") Timestamp now
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE products SET out_of_stock = TRUE WHERE feed_id <> :feedId", nativeQuery = true)
    void markMissingAsOutOfStock(@Param("feedId") Long feedId);
}
