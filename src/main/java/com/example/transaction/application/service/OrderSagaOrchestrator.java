package com.example.transaction.application.service;

import com.example.transaction.application.repository.Order;
import com.example.transaction.application.repository.Payment;
import com.example.transaction.application.repository.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

  private final OrderPessimisticLockService orderService;
  private final PaymentService paymentService;
  private final ProductPessimisticLockService productService;
  private final ShipmentService shipmentService;
  private final CompensationService compensationService;

  @Transactional
  public void handleOrderSaga(Long productId, int quantity, BigDecimal paymentAmount, String paymentMethod) {
    log.info("[OrderSagaOrchestrator] 주문 처리 시작: productId={}, quantity={}, paymentAmount={}, paymentMethod={}",
      productId, quantity, paymentAmount, paymentMethod);
    Order order;
    Payment payment = null;

    try {
      // Step 1: 주문 생성 및 재고 감소, 응답 확인
      order = handleOrderCreation(productId, quantity);
      if (order == null) {
        log.warn("[OrderSagaOrchestrator] 주문 생성 실패");
        throw new RuntimeException("Order creation failed");
      }

      // Step 2: 결제 요청 생성, 응답 확인
      payment = handlePaymentCreation(order.getOrderId(), paymentAmount, paymentMethod);
      if (payment == null || payment.getStatus() != PaymentStatus.PENDING) {
        log.warn("[OrderSagaOrchestrator] 결제 생성 실패 또는 결제가 대기 중이 아님");
        throw new RuntimeException("Payment creation failed or payment is not pending");
      }

      // Step 3: 결제 완료 처리, 응답 확인
      if (!handlePaymentCompletion(order, payment)) {
        log.warn("[OrderSagaOrchestrator] 결제 완료 처리 실패");
        throw new RuntimeException("Payment completion failed");
      }

      // Step 4: 배송 처리, 응답 확인
      if (!handleShipment(order)) {
        log.warn("[OrderSagaOrchestrator] 배송 처리 실패");
        throw new RuntimeException("Shipment failed");
      }

      // Step 5: 배송 완료 후 후속 작업
      if (!handleShipmentComplement(order)) {
        log.warn("[OrderSagaOrchestrator] 배송 완료 후 후속 작업 실패");
        throw new RuntimeException("Shipment complement failed");
      }

    } catch (Exception e) {
      log.error("[OrderSagaOrchestrator] 주문 처리 실패: productId={}, quantity={}, paymentAmount={}, paymentMethod={}",
        productId, quantity, paymentAmount, paymentMethod, e);
      // 실패 시 보상 트랜잭션 처리 (CompensationService 호출)
      compensationService.handleCompensation(productId, quantity, payment);
      throw new RuntimeException("Order saga failed", e);
    }
  }

  private Order handleOrderCreation(Long productId, int quantity) {
    try {
      return orderService.createOrder(productId, quantity);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create order", e);
    }
  }

  private Payment handlePaymentCreation(Long orderId, BigDecimal paymentAmount, String paymentMethod) {
    try {
      return paymentService.createPayment(orderId, paymentAmount, paymentMethod);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create payment", e);
    }
  }

  private boolean handlePaymentCompletion(Order order, Payment payment) {
    try {
      paymentService.completePayment(payment.getPaymentId());
      orderService.payForOrder(order.getOrderId());
      return true;
    } catch (Exception e) {
      // 결제 실패 시 처리
      return false;
    }
  }

  private boolean handleShipment(Order order) {
    try {
      shipmentService.createShipment(order.getOrderId());
      return true;
    } catch (Exception e) {
      // 배송 실패 시 처리
      return false;
    }
  }

  private boolean handleShipmentComplement(Order order) {
    try {
      // 배송 완료 후 추가 작업
      shipmentService.shipShipment(order.getOrderId());
      return true;
    } catch (Exception e) {
      // 후속 작업 실패 시 처리
      return false;
    }
  }
}