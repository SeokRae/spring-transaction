package com.example.transaction.application.repository;

import com.example.transaction.application.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Stock findByProductId(Long productId);
}