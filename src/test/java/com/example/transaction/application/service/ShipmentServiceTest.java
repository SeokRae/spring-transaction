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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@DisplayName("[ShipmentService] 테스트")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ShipmentServiceTest extends AbstractIntegrationTest {

  @Autowired
  private ShipmentService shipmentService;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProductRepository productRepository;

  private Order order;

  @BeforeEach
  void setUp() {
    // Given: 상품 생성
    Product product = Product.createProduct("Test Product", 10, BigDecimal.valueOf(100.00), "Test Description");
    productRepository.save(product);

    // Given: 주문 생성
    order = Order.createOrder(product, 5);  // 5개의 상품 주문
    order.payForOrder();// 주문 상태를 PAID로 설정
    orderRepository.save(order);
  }

  @DisplayName("[배송] 생성 테스트")
  @Test
  void createShipmentTest() {
    // Given: 주문 ID와 추적 번호
    Long orderId = order.getOrderId();

    // When: 배송 생성
    Shipment shipment = shipmentService.createShipment(orderId);

    // Then: 배송 정보 검증
    assertThat(shipment)
      .isNotNull()
      .satisfies(s -> {
        assertThat(s.getShipmentId()).isNotNull();
        assertThat(s.getOrderId()).isEqualTo(orderId);
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.PENDING);  // 초기 상태는 PENDING
      });
  }

  @DisplayName("[배송] 조회 테스트")
  @Test
  void getShipmentByIdTest() {
    // Given: 배송 생성
    Shipment shipment = shipmentService.createShipment(order.getOrderId());

    // When: 배송 ID로 조회
    Shipment foundShipment = shipmentService.getShipmentById(shipment.getShipmentId());

    // Then: 조회된 배송 정보 검증
    assertThat(foundShipment)
      .isNotNull()
      .satisfies(s -> {
        assertThat(s.getShipmentId()).isEqualTo(shipment.getShipmentId());
        assertThat(s.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(s.getStatus()).isEqualTo(ShipmentStatus.PENDING);
      });
  }

  @DisplayName("[배송] 취소 테스트")
  @Test
  void cancelShipmentTest() {
    // Given: 배송 생성
    Shipment shipment = shipmentService.createShipment(order.getOrderId());

    // When: 배송 취소
    shipmentService.cancelShipment(shipment.getShipmentId());

    // Then: 취소된 배송 정보 검증
    Shipment cancelledShipment = shipmentService.getShipmentById(shipment.getShipmentId());
    assertThat(cancelledShipment.getStatus()).isEqualTo(ShipmentStatus.CANCELLED);
  }

  @DisplayName("[배송] 취소 실패 테스트 - 이미 배송 완료된 경우")
  @Test
  void cancelShipmentFailTest() {
    // Given: 배송 생성 후 완료 처리
    Shipment shipment = shipmentService.createShipment(order.getOrderId());

    shipmentService.shipShipment(shipment.getShipmentId());

    shipmentService.completeShipment(shipment.getShipmentId());

    // Then: 이미 배송 완료된 배송을 취소할 수 없음
    assertThatThrownBy(() -> shipmentService.cancelShipment(shipment.getShipmentId()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("이미 완료된 배송은 취소할 수 없습니다.");
  }

  @DisplayName("[배송] 완료 처리 테스트")
  @Test
  void completeShipmentTest() {
    // Given: 배송 생성
    Shipment shipment = shipmentService.createShipment(order.getOrderId());

    // When: 배송 시작 처리
    shipmentService.shipShipment(shipment.getShipmentId());
    // When: 배송 완료 처리
    shipmentService.completeShipment(shipment.getShipmentId());

    // Then: 배송 완료 정보 검증
    Shipment completedShipment = shipmentService.getShipmentById(shipment.getShipmentId());
    assertThat(completedShipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
  }

  @DisplayName("[배송] 완료 실패 테스트 - 배송되지 않은 상태에서 완료 처리 불가")
  @Test
  void completeShipmentFailTest() {
    // Given: 배송 생성
    Shipment shipment = shipmentService.createShipment(order.getOrderId());

    // Then: PENDING 상태에서는 완료 처리 불가
    assertThatThrownBy(() -> shipmentService.completeShipment(shipment.getShipmentId()))
      .isInstanceOf(IllegalStateException.class)
      .hasMessageContaining("배송이 완료되기 전에는 배송 완료를 처리할 수 없습니다.");
  }

}