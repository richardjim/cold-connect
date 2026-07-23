package com.coldconnect.repository;

import com.coldconnect.entity.CustomerType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomerTypeRepository extends JpaRepository<CustomerType, Long> {
    Optional<CustomerType> findByName(String name);
    List<CustomerType> findByActiveTrue();
}