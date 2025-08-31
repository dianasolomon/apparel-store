package com.dianastore.jobs.repository;

import com.dianastore.jobs.entities.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface PriceRepository extends JpaRepository<Price, Long> {
    List<Price> findAllByCountryCodeAndSkuIn(String countryCode, List<String> skus);

    @Modifying
    @Transactional
    @Query("UPDATE Price p SET p.markedForDelete = true WHERE p.feedId <> :feedId")
    int markNotInFeedIdAsMarkedForDelete(long feedId);
}
