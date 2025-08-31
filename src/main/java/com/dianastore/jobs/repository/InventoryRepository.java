package com.dianastore.jobs.repository;

import com.dianastore.jobs.entities.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO inventory
            (sku, country_code, stock, is_disabled, creation_timestamp, modified_timestamp, feed_id, marked_for_delete)
            VALUES (:sku, :countryCode, :stock, :isDisabled, :now, :now, :feedId, FALSE)
            ON DUPLICATE KEY UPDATE
            stock = IF(stock <> VALUES(stock) OR is_disabled <> VALUES(is_disabled), VALUES(stock), stock),
            is_disabled = IF(stock <> VALUES(stock) OR is_disabled <> VALUES(is_disabled), VALUES(is_disabled), is_disabled),
            modified_timestamp = IF(stock <> VALUES(stock) OR is_disabled <> VALUES(is_disabled), :now, modified_timestamp),
            feed_id = :feedId,
            marked_for_delete = FALSE
            """, nativeQuery = true)
    void insertOrUpdateNative(
            @Param("sku") String sku,
            @Param("countryCode") String countryCode,
            @Param("stock") int stock,
            @Param("isDisabled") int isDisabled,
            @Param("feedId") Long feedId,
            @Param("now") Timestamp now
    );

    @Modifying
    @Transactional
    @Query(value = "UPDATE inventory SET marked_for_delete = TRUE WHERE feed_id <> :feedId", nativeQuery = true)
    void markNotInFeedIdAsMarkedForDelete(@Param("feedId") Long feedId);
}
