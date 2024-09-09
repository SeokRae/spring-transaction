package com.example.transaction.application.service;

import com.example.transaction.AbstractIntegrationTest;
import com.example.transaction.application.repository.*;
import com.example.transaction.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("[OrderService] 테스트")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrderPessimisticLockServiceTest extends AbstractIntegrationTest {

  @Autowired
  private OrderPessimisticLockService orderPessimisticLockService;
  @Autowired
  private OrderRepository orderRepository;
  @Autowired
  private ProductRepository productRepository;

  @DisplayName("[주문] 생성 테스트")
  @Test
  void createOrderTest() {
    // Given
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    // When: 주문 생성
    Order order = orderPessimisticLockService.createOrder(product.getProductId(), 5);

    assertThat(order)
      .isNotNull()
      .satisfies(o -> {
        assertThat(o.getProductId()).isEqualTo(product.getProductId());
        assertThat(o.getQuantity()).isEqualTo(5);
        assertThat(o.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(500.00));
      });

    // Then: 주문 생성 후 재고가 감소되었는지 확인
    assertThat(productRepository.findById(product.getProductId()))
      .isPresent()
      .satisfies(p -> {
        assertThat(p.get().getStockQuantity()).isEqualTo(5);
      });
  }

  @DisplayName("[주문] 취소 테스트")
  @Test
  void cancelOrderTest() {
    // Given
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    Order order = orderPessimisticLockService.createOrder(product.getProductId(), 5);

    // When: 주문 취소
    Order canceledOrder = orderPessimisticLockService.cancelOrder(order.getOrderId());

    // Then: 주문 상태가 취소되고, 재고가 복원되었는지 확인
    assertThat(canceledOrder.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);

    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(10); // 재고 복원 확인
  }

  @DisplayName("[주문] 재고 부족으로 인한 주문 실패 테스트")
  @Test
  void createOrderWithInsufficientStockTest() {
    // Given
    Product product = Product.createProduct(
      "Test Product", 2, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    // When & Then: 재고 부족으로 예외 발생
    assertThatThrownBy(() -> orderPessimisticLockService.createOrder(product.getProductId(), 5))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("재고가 부족합니다.");

    // 재고가 감소하지 않았는지 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(2); // 재고 변화 없음
  }

  @Test
  @DisplayName("[주문] 잘못된 상품 ID로 주문 시 예외 발생 테스트")
  void createOrderWithInvalidProductIdTest() {
    // Given: 존재하지 않는 상품 ID
    Long invalidProductId = 999L;

    // When & Then: 잘못된 상품 ID로 주문 시 ResourceNotFoundException 발생
    assertThatThrownBy(() -> orderPessimisticLockService.createOrder(invalidProductId, 1))
      .isInstanceOf(ResourceNotFoundException.class)
      .hasMessageContaining("상품을 찾을 수 없습니다.");
  }

  @Test
  @DisplayName("[주문] 결제된(PAID) 또는 배송된(SHIPPED) 주문을 취소할 수 없다는 테스트")
  void cancelPaidOrShippedOrderTest() {
    // Given: 상품 생성 및 주문을 결제(PAID) 또는 배송(SHIPPED) 상태로 설정
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    // 주문 생성 후 상태를 PAID로 변경
    Order order = orderPessimisticLockService.createOrder(product.getProductId(), 5);
    orderPessimisticLockService.payForOrder(order.getOrderId());  // 주문 상태를 PAID로 변경

    // When & Then: PAID 상태의 주문을 취소 시도 시 예외 발생
    assertThatThrownBy(() -> orderPessimisticLockService.cancelOrder(order.getOrderId()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("결제 대기 중인 주문만 취소할 수 있습니다.");

    // 주문 상태를 SHIPPED로 변경
    orderPessimisticLockService.shipOrder(order.getOrderId());  // 주문 상태를 SHIPPED로 변경

    // When & Then: SHIPPED 상태의 주문을 취소 시도 시 예외 발생
    assertThatThrownBy(() -> orderPessimisticLockService.cancelOrder(order.getOrderId()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("결제 대기 중인 주문만 취소할 수 있습니다.");
  }

  @Test
  @DisplayName("주문 생성 시 총 금액 계산 테스트")
  void createOrderTotalAmountTest() {
    // Given: 상품을 생성
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(150.00), "Test Description"
    );
    productRepository.save(product);

    // When: 3개의 상품을 주문
    Order order = orderPessimisticLockService.createOrder(product.getProductId(), 3);

    // Then: 총 금액이 450.00으로 정확하게 계산되는지 확인
    assertThat(order.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(450.00));
  }

  @Test
  @DisplayName("[주문] 주문 내역 조회 테스트")
  void getOrderByIdTest() {
    // Given: 상품을 생성하고 주문
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    Order order = orderPessimisticLockService.createOrder(product.getProductId(), 2);
    orderRepository.save(order);

    // When: 주문 ID로 조회
    Order foundOrder = orderPessimisticLockService.getOrderById(order.getOrderId());

    // Then: 주문 정보가 정확하게 반환되는지 확인
    assertThat(foundOrder).isNotNull();
    assertThat(foundOrder.getProductId()).isEqualTo(product.getProductId());
    assertThat(foundOrder.getQuantity()).isEqualTo(2);
  }
}