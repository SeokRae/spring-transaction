package com.example.transaction.application.service;

import com.example.transaction.AbstractIntegrationTest;
import com.example.transaction.application.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("[PaymentService] 테스트")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PaymentServiceTest extends AbstractIntegrationTest {

  @Autowired
  private PaymentService paymentService;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private OrderPessimisticLockService orderService;

  @DisplayName("[결제] 생성 테스트")
  @Test
  void createPaymentTest() {
    // Given: 상품 및 주문 생성
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    Order order = orderService.createOrder(product.getProductId(), 5);

    Long orderId = order.getOrderId();
    BigDecimal paymentAmount = BigDecimal.valueOf(500.00); // 총 주문 금액
    String paymentMethod = "card";

    // When: 결제 생성
    Payment payment = paymentService.createPayment(orderId, paymentAmount, paymentMethod);

    // Then: 생성된 결제 정보 확인
    assertThat(payment)
      .isNotNull()
      .satisfies(p -> {
        assertThat(p.getOrderId()).isEqualTo(orderId);
        assertThat(p.getPaymentAmount()).isEqualByComparingTo(paymentAmount);
        assertThat(p.getPaymentMethod()).isEqualTo(paymentMethod);
        assertThat(p.getPaymentStatus()).isEqualTo(PaymentStatus.PENDING);
      });

    // DB에 저장된 결제 정보 확인
    assertThat(paymentRepository.findById(payment.getPaymentId()))
      .isPresent()
      .satisfies(p -> {
        assertThat(p.get().getPaymentId()).isEqualTo(payment.getPaymentId());
        assertThat(p.get().getOrderId()).isEqualTo(orderId);
      });
  }

  @DisplayName("[결제] 완료 테스트")
  @Test
  void completePaymentTest() {
    // Given: 상품 및 주문 생성
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    Order order = orderService.createOrder(product.getProductId(), 5);

    Long orderId = order.getOrderId();
    BigDecimal paymentAmount = BigDecimal.valueOf(500.00); // 총 주문 금액
    String paymentMethod = "card";
    Payment payment = paymentService.createPayment(orderId, paymentAmount, paymentMethod);

    // When: 결제 완료 처리
    paymentService.completePayment(payment.getPaymentId());

    // Then: 결제 상태가 완료로 변경되었는지 확인
    assertThat(paymentRepository.findById(payment.getPaymentId()))
      .isPresent()
      .satisfies(p -> {
        assertThat(p.get().getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);
      });
  }

  @DisplayName("[결제] 실패 처리 테스트")
  @Test
  void failPaymentTest() {
    // Given: 상품 및 주문 생성
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    Order order = orderService.createOrder(product.getProductId(), 5);

    Long orderId = order.getOrderId();
    BigDecimal paymentAmount = BigDecimal.valueOf(500.00); // 총 주문 금액
    String paymentMethod = "card";
    Payment payment = paymentService.createPayment(orderId, paymentAmount, paymentMethod);

    // When: 결제 실패 처리
    paymentService.failPayment(payment.getPaymentId());

    // Then: 결제 상태가 실패로 변경되었는지 확인
    assertThat(paymentRepository.findById(payment.getPaymentId()))
      .isPresent()
      .satisfies(p -> {
        assertThat(p.get().getPaymentStatus()).isEqualTo(PaymentStatus.FAILED);
      });
  }

  @DisplayName("[결제] 조회 테스트")
  @Test
  void getPaymentByIdTest() {
    // Given: 상품 및 주문 생성
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    Order order = orderService.createOrder(product.getProductId(), 5);

    Long orderId = order.getOrderId();
    BigDecimal paymentAmount = BigDecimal.valueOf(500.00); // 총 주문 금액
    String paymentMethod = "card";
    Payment payment = paymentService.createPayment(orderId, paymentAmount, paymentMethod);

    // When: 결제 정보 조회
    Payment foundPayment = paymentService.getPaymentById(payment.getPaymentId());

    // Then: 조회된 결제 정보 확인
    assertThat(foundPayment)
      .isNotNull()
      .satisfies(p -> {
        assertThat(p.getOrderId()).isEqualTo(orderId);
        assertThat(p.getPaymentAmount()).isEqualByComparingTo(paymentAmount);
        assertThat(p.getPaymentMethod()).isEqualTo(paymentMethod);
      });
  }
}