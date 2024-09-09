package com.example.transaction.application.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
  /**
   * 특정 주문에 대해 이미 완료된 결제가 있는지 확인하는 메서드
   *
   * @param orderId       주문 ID
   * @param paymentStatus 결제 상태 (주로 COMPLETED)
   * @return 완료된 결제가 존재하는지 여부
   */
  @Query("SELECT CASE WHEN EXISTS (SELECT 1 FROM Payment p WHERE p.orderId = :orderId AND p.paymentStatus = :paymentStatus) THEN true ELSE false END")
  boolean existsByOrderIdAndPaymentStatus(@Param("orderId") Long orderId, @Param("paymentStatus") PaymentStatus paymentStatus);
}
