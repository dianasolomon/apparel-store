package com.dianastore.repository;

import com.dianastore.entities.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {

    // Custom query methods-
}
