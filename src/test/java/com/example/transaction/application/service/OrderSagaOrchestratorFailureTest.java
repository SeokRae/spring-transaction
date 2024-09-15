package com.example.transaction.application.service;

import com.example.transaction.AbstractIntegrationTest;
import com.example.transaction.application.repository.PaymentRepository;
import com.example.transaction.application.repository.Product;
import com.example.transaction.application.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderSagaOrchestratorFailureTest extends AbstractIntegrationTest {

  @Autowired
  private OrderSagaOrchestrator orderSagaOrchestrator;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @BeforeEach
  void setUp() {
    // 테스트에 사용할 상품 생성
    Product product = Product.createProduct("테스트 상품", 10, BigDecimal.valueOf(100.00), "테스트 상품입니다.");
    productRepository.save(product);
  }

  @DisplayName("[주문 오케스트레이션] 주문 생성 실패 테스트")
  @Test
  void testOrderCreationFailure() {
    // Given: 상품 준비
    Product product = productRepository.findAll().get(0);
    int invalidQuantity = 20; // 재고보다 많은 양을 주문하여 실패를 유도
    BigDecimal paymentAmount = BigDecimal.valueOf(500.00);
    String paymentMethod = "CREDIT_CARD";

    // When & Then: 주문 생성 실패를 확인
    assertThrows(RuntimeException.class, () -> {
      orderSagaOrchestrator.handleOrderSaga(product.getProductId(), invalidQuantity, paymentAmount, paymentMethod);
    });

    // 주문 생성 실패 후 재고 복원 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(10);  // 재고가 변동 없이 유지되어야 함
  }

  @DisplayName("[주문 오케스트레이션] 결제 생성 실패 테스트")
  @Test
  void testPaymentCreationFailure() {
    // Given: 상품 및 유효한 주문 준비
    Product product = productRepository.findAll().get(0);
    int quantity = 5;
    BigDecimal invalidPaymentAmount = BigDecimal.valueOf(-500.00); // 결제 금액을 음수로 하여 실패 유도
    String paymentMethod = "CREDIT_CARD";

    // When & Then: 결제 생성 실패 확인
    assertThrows(RuntimeException.class, () -> {
      orderSagaOrchestrator.handleOrderSaga(product.getProductId(), quantity, invalidPaymentAmount, paymentMethod);
    });

    // 결제 실패 후 재고 복원 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(10);  // 재고가 복원되었는지 확인

    // 결제 정보가 저장되지 않아야 함
    assertThat(paymentRepository.findAll()).isEmpty();
  }
}