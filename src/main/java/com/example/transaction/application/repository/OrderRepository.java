package com.example.transaction.application.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
  Optional<Order> findByIdWithLock(@Param("orderId") Long orderId);

  /**
   * EXISTS를 사용한 효율적인 쿼리
   */
  @Query("SELECT CASE WHEN EXISTS (SELECT 1 FROM Order o WHERE o.productId = :productId AND o.orderStatus = :orderStatus) THEN true ELSE false END")
  boolean existsByProductIdAndOrderStatus(@Param("productId") Long productId, @Param("orderStatus") OrderStatus orderStatus);
}