package com.example.transaction.application.service;

import com.example.transaction.application.repository.Payment;
import com.example.transaction.application.repository.PaymentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@DisplayName("[서비스] 보상 트랜잭션 서비스")
@ExtendWith(MockitoExtension.class)
class CompensationServiceTest {

  @Mock
  private PaymentService paymentService;

  @Mock
  private ProductPessimisticLockService productService;

  @InjectMocks
  private CompensationService compensationService;

  @DisplayName("[보상] 결제 실패 시 보상 트랜잭션 테스트")
  @Test
  void testCompensationOnPaymentFailure() {
    // Given
    Long productId = 1L;
    int quantity = 5;
    Payment payment = Payment.builder()
      .paymentId(1L)
      .status(PaymentStatus.PENDING)
      .build();

    // 결제 취소에 대한 반환값을 실제로 설정 (Payment 객체 반환)
    when(paymentService.cancelPayment(payment.getPaymentId())).thenReturn(payment);
    doNothing().when(productService).increaseStock(productId, quantity);

    // When: 결제 취소가 정상적으로 호출되는 시나리오
    compensationService.handleCompensation(productId, quantity, payment);

    // Then: 결제 취소와 재고 복원이 호출되었는지 확인
    verify(paymentService, times(1)).cancelPayment(payment.getPaymentId());
    verify(productService, times(1)).increaseStock(productId, quantity);
  }

  @DisplayName("[보상] 결제 없이 보상 트랜잭션 테스트")
  @Test
  void testCompensationWithoutPayment() {
    // Given: 결제 객체가 null인 경우 (결제 실패 시)
    Long productId = 1L;
    int quantity = 5;

    // When: 결제 없이 보상 트랜잭션 실행
    compensationService.handleCompensation(productId, quantity, null);

    // Then: 결제 취소는 호출되지 않고 재고 복원만 호출되는지 확인
    verify(paymentService, never()).cancelPayment(anyLong());
    verify(productService, times(1)).increaseStock(productId, quantity);
  }

  @DisplayName("[보상] 재고 복원 실패 시 보상 트랜잭션 테스트")
  @Test
  void testCompensationFailureOnStockRestore() {
    // Given: 결제 객체와 재고 복원이 실패하는 시나리오
    Long productId = 1L;
    int quantity = 5;
    Payment payment = Payment.builder()
      .paymentId(1L)
      .status(PaymentStatus.PENDING)
      .build();

    // 재고 복원 시 예외 발생을 유도
    doThrow(new RuntimeException("Failed to restore stock")).when(productService).increaseStock(productId, quantity);

    // When & Then: 예외가 발생하고 보상 트랜잭션 실패를 확인
    assertThrows(RuntimeException.class, () -> {
      compensationService.handleCompensation(productId, quantity, payment);
    });

    // 결제 취소는 호출되었으나 재고 복원에서 실패함을 확인
    verify(paymentService, times(1)).cancelPayment(payment.getPaymentId());
    verify(productService, times(1)).increaseStock(productId, quantity);
  }

  @DisplayName("[보상] 이미 취소된 결제에 대한 보상 트랜잭션 테스트")
  @Test
  void testCompensationAlreadyCancelledPayment() {
    // Given: 이미 취소된 결제
    Long productId = 1L;
    int quantity = 5;
    Payment payment = Payment.builder()
      .paymentId(1L)
      .status(PaymentStatus.CANCELLED)
      .build();

    // When: 보상 트랜잭션 실행
    compensationService.handleCompensation(productId, quantity, payment);

    // Then: 이미 취소된 결제는 취소되지 않고 재고 복원만 호출됨
    verify(paymentService, never()).cancelPayment(payment.getPaymentId());
    verify(productService, times(1)).increaseStock(productId, quantity);
  }
}