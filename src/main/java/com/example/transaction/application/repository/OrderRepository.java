package com.example.transaction.application.repository;

import com.example.transaction.application.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
}