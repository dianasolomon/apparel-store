package com.dianastore.repository;

import com.dianastore.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @Modifying
    @Transactional
    @Query(value = """
    INSERT INTO products
    (country_code, sku, short_description, medium_description, long_description, image_url, product_name,
     category, size, colour, style, notes, out_of_stock, feed_id, creation_timestamp, modified_timestamp)
    VALUES (:countryCode, :sku, :shortDescription, :mediumDescription, :longDescription, :imageUrl, :productName,
            :category, :size, :colour, :style, :notes, FALSE, :feedId, :now, :now)
    ON DUPLICATE KEY UPDATE
    short_description = COALESCE(VALUES(short_description), short_description),
    medium_description = COALESCE(VALUES(medium_description), medium_description),
    long_description  = COALESCE(VALUES(long_description), long_description),
    image_url         = COALESCE(VALUES(image_url), image_url),
    product_name      = COALESCE(VALUES(product_name), product_name),
    category          = COALESCE(VALUES(category), category),
    size              = COALESCE(VALUES(size), size),
    colour            = COALESCE(VALUES(colour), colour),
    style             = COALESCE(VALUES(style), style),
    notes             = COALESCE(VALUES(notes), notes),
    out_of_stock = FALSE,
    modified_timestamp = IF(
        (VALUES(short_description) IS NOT NULL AND VALUES(short_description) <> short_description) OR
        (VALUES(medium_description) IS NOT NULL AND VALUES(medium_description) <> medium_description) OR
        (VALUES(long_description)  IS NOT NULL AND VALUES(long_description)  <> long_description)  OR
        (VALUES(image_url)         IS NOT NULL AND VALUES(image_url)         <> image_url)         OR
        (VALUES(product_name)      IS NOT NULL AND VALUES(product_name)      <> product_name)      OR
        (VALUES(category)          IS NOT NULL AND VALUES(category)          <> category)          OR
        (VALUES(size)              IS NOT NULL AND VALUES(size)              <> size)              OR
        (VALUES(colour)            IS NOT NULL AND VALUES(colour)            <> colour)            OR
        (VALUES(style)             IS NOT NULL AND VALUES(style)             <> style)             OR
        (VALUES(notes)             IS NOT NULL AND VALUES(notes)             <> notes)             OR
        (out_of_stock = TRUE),
        :now, modified_timestamp),
    feed_id = :feedId
    """, nativeQuery = true)

    void insertOrUpdateNative(
            @Param("countryCode") String countryCode,
            @Param("sku") String sku,
            @Param("shortDescription") String shortDescription,
            @Param("mediumDescription") String mediumDescription,
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

    Page<Product> findByCountryCodeAndCategoryContainingIgnoreCase(String countryCode, String category, Pageable pageable);
    @Query("SELECT p FROM Product p " +
            "WHERE (:region IS NULL OR p.countryCode = :region) " +
            "AND (:category IS NULL OR p.category LIKE %:category%) " +
            "AND (:size IS NULL OR p.size = :size) " +
            "AND (:colour IS NULL OR p.colour LIKE %:colour%)")
    List<Product> findByFilters(
            @Param("region") String region,
            @Param("category") String category,
            @Param("size") String size,
            @Param("colour") String colour,
            Pageable pageable
    );

}
