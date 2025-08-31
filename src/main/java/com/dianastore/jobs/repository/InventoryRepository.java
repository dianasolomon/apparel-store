package com.dianastore.jobs.repository;

import com.dianastore.jobs.entities.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findAllByCountryCodeAndSkuIn(String countryCode, List<String> skus);

    @Modifying
    @Transactional
    @Query("UPDATE Inventory i SET i.markedForDelete = true WHERE i.feedId <> :feedId")
    int markNotInFeedIdAsMarkedForDelete(long feedId);
}
