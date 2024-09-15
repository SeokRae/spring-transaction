package com.example.transaction.application.service;

import com.example.transaction.application.repository.Payment;
import com.example.transaction.application.repository.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompensationService {

  private final PaymentService paymentService;
  private final ProductPessimisticLockService productService;

  /**
   * 보상 트랜잭션 처리
   *
   * @param productId 상품 ID
   * @param quantity  주문 수량
   * @param payment   결제 정보
   */
  public void handleCompensation(Long productId, int quantity, Payment payment) {
    try {
      log.info("[CompensationService] 보상 트랜잭션 처리 시작: productId={}, quantity={}", productId, quantity);
      // Step 1: 결제 취소
      if (payment != null && payment.getStatus() != PaymentStatus.CANCELLED) {
        log.warn("[CompensationService] 결제 취소: paymentId={}", payment.getPaymentId());
        paymentService.cancelPayment(payment.getPaymentId());
      }

      // Step 2: 재고 복원
      productService.increaseStock(productId, quantity);

    } catch (Exception e) {
      log.error("[CompensationService] 보상 트랜잭션 처리 실패", e);
      throw new RuntimeException("Failed to handle compensation", e);
    }
  }
}