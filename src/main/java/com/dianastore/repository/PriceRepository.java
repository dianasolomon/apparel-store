package com.dianastore.repository;

import com.dianastore.entities.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Repository
public interface PriceRepository extends JpaRepository<Price, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO prices
            (sku, country_code, msrp, discount, selling_price, creation_ts, modified_ts, feed_id, marked_for_delete)
            VALUES (:sku, :countryCode, :msrp, :discount, :sellingPrice, :now, :now, :feedId, FALSE)
            ON DUPLICATE KEY UPDATE
            msrp = IF(msrp <> VALUES(msrp) OR discount <> VALUES(discount) OR selling_price <> VALUES(selling_price),
                      VALUES(msrp), msrp),
            discount = IF(msrp <> VALUES(msrp) OR discount <> VALUES(discount) OR selling_price <> VALUES(selling_price),
                          VALUES(discount), discount),
            selling_price = IF(msrp <> VALUES(msrp) OR discount <> VALUES(discount) OR selling_price <> VALUES(selling_price),
                               VALUES(selling_price), selling_price),
            modified_ts = IF(msrp <> VALUES(msrp) OR discount <> VALUES(discount) OR selling_price <> VALUES(selling_price),
                                    :now, modified_ts),
            feed_id = :feedId,
            marked_for_delete = FALSE
            """, nativeQuery = true)
    void insertOrUpdateNative(
            @Param("sku") String sku,
            @Param("countryCode") String countryCode,
            @Param("msrp") double msrp,
            @Param("discount") double discount,
            @Param("sellingPrice") double sellingPrice,
            @Param("feedId") Long feedId,
            @Param("now") Timestamp now
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE prices SET marked_for_delete = TRUE WHERE feed_id <> :feedId", nativeQuery = true)
    void markNotInFeedIdAsMarkedForDelete(@Param("feedId") Long feedId);
}
