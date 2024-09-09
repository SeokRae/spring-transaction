package com.example.transaction.application.service;

import com.example.transaction.application.repository.Payment;
import com.example.transaction.application.repository.PaymentRepository;
import com.example.transaction.application.repository.PaymentStatus;
import com.example.transaction.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

  private final PaymentRepository paymentRepository;

  /**
   * 결제 요청 생성
   *
   * @param orderId       주문 ID
   * @param paymentAmount 결제 금액
   * @param paymentMethod 결제 방법
   * @return 생성된 Payment 객체
   */
  @Transactional
  public Payment createPayment(Long orderId, BigDecimal paymentAmount, String paymentMethod) {
    // 중복 결제 방지
    boolean paymentExists = paymentRepository.existsByOrderIdAndPaymentStatus(orderId, PaymentStatus.COMPLETED);

    if (paymentExists) {
      throw new IllegalStateException("이미 완료된 결제가 존재합니다.");
    }

    // 결제 요청 생성
    Payment payment = Payment.createPayment(orderId, paymentAmount, paymentMethod);

    // 결제 정보 저장
    return paymentRepository.save(payment);
  }
  
  /**
   * 결제 완료 처리
   *
   * @param paymentId 결제 ID
   * @return 완료된 Payment 객체
   */
  @Transactional
  public Payment completePayment(Long paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
      .orElseThrow(() -> new ResourceNotFoundException("결제 정보를 찾을 수 없습니다. 결제 ID: " + paymentId));

    payment.completePayment();

    return paymentRepository.save(payment);
  }

  /**
   * 결제 실패 처리
   *
   * @param paymentId 결제 ID
   * @return 실패된 Payment 객체
   */
  @Transactional
  public Payment failPayment(Long paymentId) {
    Payment payment = paymentRepository.findById(paymentId)
      .orElseThrow(() -> new ResourceNotFoundException("결제 정보를 찾을 수 없습니다. 결제 ID: " + paymentId));

    payment.failPayment();
    return paymentRepository.save(payment);
  }

  /**
   * 결제 정보 조회
   *
   * @param paymentId 결제 ID
   * @return Payment 객체
   */
  @Transactional(readOnly = true)
  public Payment getPaymentById(Long paymentId) {
    return paymentRepository.findById(paymentId)
      .orElseThrow(() -> new ResourceNotFoundException("결제 정보를 찾을 수 없습니다. 결제 ID: " + paymentId));
  }
}
