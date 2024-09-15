package com.example.transaction.application.service;

import com.example.transaction.AbstractIntegrationTest;
import com.example.transaction.application.repository.*;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrderSagaOrchestratorTest extends AbstractIntegrationTest {

  @Autowired
  private OrderSagaOrchestrator orderSagaOrchestrator;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private PaymentRepository paymentRepository;

  @Autowired
  private ShipmentRepository shipmentRepository;

  @BeforeEach
  void setUp() {
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);
  }

  @DisplayName("[주문 오케스트레이션] 성공 테스트")
  @Test
  void testSuccessfulOrderSaga() {
    // 성공 시나리오 테스트
    Product product = productRepository.findAll().get(0);
    orderSagaOrchestrator.handleOrderSaga(product.getProductId(), 2, BigDecimal.valueOf(200.00), "card");

    // 주문, 결제, 배송 확인
    Order order = orderRepository.findAll().get(0);
    Payment payment = paymentRepository.findAll().get(0);
    Shipment shipment = shipmentRepository.findAll().get(0);

    assertThat(order)
      .isNotNull()
      .satisfies(o -> assertThat(o.getStatus()).isEqualTo(OrderStatus.PAID));
    assertThat(payment)
      .isNotNull()
      .satisfies(p -> assertThat(p.getStatus()).isEqualTo(PaymentStatus.COMPLETED));
    assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
    assertThat(productRepository.findById(product.getProductId())
      .get().getStockQuantity()).isEqualTo(8);
  }

  @DisplayName("[주문 오케스트레이션] 결제 실패 테스트")
  @Test
  void testCompensationOnPaymentFailure() {
    // 결제 실패 시나리오 테스트
    Product product = productRepository.findAll().get(0);

    assertThrows(RuntimeException.class, () -> {
      orderSagaOrchestrator.handleOrderSaga(product.getProductId(), 20, BigDecimal.valueOf(2000.00), "card");
    });

    // 재고 복원 확인
    assertThat(productRepository.findById(product.getProductId()).get().getStockQuantity()).isEqualTo(10);

    // 주문 및 결제가 생성되지 않거나 취소 상태인지 확인
    assertThat(orderRepository.findAll()).isEmpty();
    assertThat(paymentRepository.findAll()).isEmpty();
  }
}