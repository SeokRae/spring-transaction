package com.example.transaction.application.repository;

import com.example.transaction.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("[ShipmentRepository] 테스트")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ShipmentRepositoryTest extends AbstractIntegrationTest {

  @Autowired
  private ShipmentRepository shipmentRepository;

  @Autowired
  private OrderRepository orderRepository;

  @Autowired
  private ProductRepository productRepository;

  private Order order;

  @BeforeEach
  void setUp() {
    // Given: 상품 생성
    Product product = Product.createProduct(
      "Test Product", 10, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    // Given: 주문 생성
    order = Order.createOrder(product, 5);  // 예시로 5개의 상품을 주문
    order.setOrderStatus(OrderStatus.PAID);  // 주문 상태를 PAID로 설정
    orderRepository.save(order);
  }

  @DisplayName("[배송] 생성 및 저장 테스트")
  @Test
  void createAndSaveShipmentTest() {
    // Given: 배송 객체 생성
    Shipment shipment = new Shipment();
    shipment.setOrderId(order.getOrderId());
    shipment.setShipmentDate(LocalDateTime.now());
    shipment.setShipmentStatus(ShipmentStatus.SHIPPED);
    shipment.setTrackingNumber("TRACK12345");

    // When: 배송 저장
    Shipment savedShipment = shipmentRepository.save(shipment);

    // Then: 저장된 배송 정보 검증
    assertThat(savedShipment)
      .isNotNull()
      .satisfies(s -> {
        assertThat(s.getShipmentId()).isNotNull();
        assertThat(s.getOrderId()).isEqualTo(order.getOrderId());
        assertThat(s.getShipmentDate()).isNotNull();
        assertThat(s.getShipmentStatus()).isEqualTo(ShipmentStatus.SHIPPED);
        assertThat(s.getTrackingNumber()).isEqualTo("TRACK12345");
      });
  }

  @DisplayName("[배송] 조회 테스트")
  @Test
  void findShipmentByIdTest() {
    // Given: 배송 객체 저장
    Shipment shipment = new Shipment();
    shipment.setOrderId(order.getOrderId());
    shipment.setShipmentDate(LocalDateTime.now());
    shipment.setShipmentStatus(ShipmentStatus.SHIPPED);
    shipment.setTrackingNumber("TRACK12345");
    shipmentRepository.save(shipment);

    // When: 배송 ID로 조회
    Optional<Shipment> foundShipment = shipmentRepository.findById(shipment.getShipmentId());

    // Then: 조회된 배송 정보 검증
    assertThat(foundShipment)
      .isPresent()
      .satisfies(s -> {
        assertThat(s.get().getShipmentId()).isEqualTo(shipment.getShipmentId());
        assertThat(s.get().getOrderId()).isEqualTo(order.getOrderId());
        assertThat(s.get().getShipmentStatus()).isEqualTo(ShipmentStatus.SHIPPED);
        assertThat(s.get().getTrackingNumber()).isEqualTo("TRACK12345");
      });
  }

  @DisplayName("[배송] 삭제 테스트")
  @Test
  void deleteShipmentTest() {
    // Given: 배송 객체 저장
    Shipment shipment = new Shipment();
    shipment.setOrderId(order.getOrderId());
    shipment.setShipmentDate(LocalDateTime.now());
    shipment.setShipmentStatus(ShipmentStatus.SHIPPED);
    shipment.setTrackingNumber("TRACK12345");
    shipmentRepository.save(shipment);

    // When: 배송 삭제
    shipmentRepository.delete(shipment);

    // Then: 삭제된 배송이 존재하지 않는지 확인
    Optional<Shipment> foundShipment = shipmentRepository.findById(shipment.getShipmentId());
    assertThat(foundShipment).isEmpty();
  }
}