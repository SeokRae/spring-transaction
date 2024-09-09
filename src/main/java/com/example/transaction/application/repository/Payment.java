package com.example.transaction.application.repository;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Builder
@AllArgsConstructor
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long paymentId;

  /**
   * 결제 요청은 주문으로 인해 발생하여 주문과 결제는 1:1 관계로 정의하지만,
   * order 객체 대신 orderId 필드로 매핑합니다.
   */
  @Column(name = "order_id", nullable = false)
  private Long orderId;

  private LocalDateTime paymentDate;
  /**
   * 결제 금액은 주문의 최종 결제 금액과 동일하게 설정합니다.
   */
  private BigDecimal paymentAmount;

  /**
   * 현재 결제 상태를 나타냅니다.
   * 결제 상태는 다음 중 하나일 수 있습니다.
   * - PENDING: 결제가 시작되었지만 아직 완료되지 않았습니다.
   * - COMPLETED: 결제가 성공적으로 완료되었습니다.
   * - FAILED: 결제 시도가 실패했습니다.
   */
  @Enumerated(EnumType.STRING)
  private PaymentStatus paymentStatus;

  /**
   * 결제 수단을 나타냅니다.
   * 결제방법은 신용카드, 간편결제, 계좌이체 등 다양한 형태가 가능합니다.
   */
  private String paymentMethod;

  /**
   * 결제 생성 메서드
   *
   * @param orderId       주문 ID
   * @param paymentAmount 결제 금액
   * @param paymentMethod 결제 방법
   * @return 생성된 Payment 객체
   */
  public static Payment createPayment(Long orderId, BigDecimal paymentAmount, String paymentMethod) {
    if (orderId == null) {
      throw new IllegalArgumentException("주문 ID가 필요합니다.");
    }
    if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
    }
    return Payment.builder()
      .orderId(orderId)
      .paymentAmount(paymentAmount)
      .paymentMethod(paymentMethod)
      .paymentStatus(PaymentStatus.PENDING)
      .paymentDate(LocalDateTime.now())
      .build();
  }

  /**
   * 결제 완료 처리
   */
  public void completePayment() {
    if (this.paymentStatus == PaymentStatus.COMPLETED) {
      throw new IllegalStateException("이미 완료된 결제입니다.");
    }
    this.paymentStatus = PaymentStatus.COMPLETED;
  }

  /**
   * 결제 실패 처리
   */
  public void failPayment() {
    if (this.paymentStatus == PaymentStatus.FAILED) {
      throw new IllegalStateException("이미 실패한 결제입니다.");
    }
    this.paymentStatus = PaymentStatus.FAILED;
  }
}
