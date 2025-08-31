package com.dianastore.jobs.repository;

import com.dianastore.jobs.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findAllByCountryCodeAndSkuIn(String countryCode, List<String> skus);

    @Modifying
    @Transactional
    @Query("UPDATE Product p SET p.markedForDelete = true WHERE p.feedId <> :feedId")
    int markNotInFeedIdAsMarkedForDelete(long feedId);
}
