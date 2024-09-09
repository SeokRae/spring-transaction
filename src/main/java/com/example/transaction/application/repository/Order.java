package com.example.transaction.application.repository;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "orders")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long orderId;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  private LocalDateTime orderDate;

  @Enumerated(EnumType.STRING)
  private OrderStatus orderStatus;

  private int quantity;  // 주문 수량

  private BigDecimal totalAmount;  // 최종 결제 금액

  /**
   * 주문 생성 메서드: 재고 감소는 서비스나 Product에서 수행
   */
  public static Order createOrder(Product product, int quantity) {
    // 주문 생성 (재고 감소는 Product에서 수행)
    return Order.builder()
      .productId(product.getProductId())
      .quantity(quantity)
      .orderDate(LocalDateTime.now())
      .orderStatus(OrderStatus.PENDING)
      .totalAmount(product.getPrice().multiply(BigDecimal.valueOf(quantity)))  // 금액 계산
      .build();
  }

  // 주문 결제 처리 (상태를 PAID로 변경)
  public void payForOrder() {
    if (this.orderStatus != OrderStatus.PENDING) {
      throw new IllegalStateException("결제는 대기 상태의 주문만 가능합니다.");
    }
    this.orderStatus = OrderStatus.PAID;
  }

  // 주문 배송 처리 (상태를 SHIPPED로 변경)
  public void shipOrder() {
    if (this.orderStatus != OrderStatus.PAID) {
      throw new IllegalStateException("배송은 결제 완료 상태의 주문만 가능합니다.");
    }
    this.orderStatus = OrderStatus.SHIPPED;
  }

  // 주문 취소 처리 (상태를 CANCELLED로 변경)
  public void cancelOrder(Product product) {
    if (this.orderStatus != OrderStatus.PENDING) {
      throw new IllegalStateException("결제 대기 중인 주문만 취소할 수 있습니다.");
    }
    // 주문 취소 시 상품 재고 복원
    product.increaseStock(this.quantity);
    this.orderStatus = OrderStatus.CANCELLED;
  }

}