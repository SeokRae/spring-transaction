package com.example.transaction.application.service;

import com.example.transaction.application.repository.Order;
import com.example.transaction.application.repository.OrderRepository;
import com.example.transaction.application.repository.OrderStatus;
import com.example.transaction.application.repository.Product;
import com.example.transaction.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderPessimisticLockService {

  private final OrderRepository orderRepository;
  private final ProductPessimisticLockService productService;

  /**
   * 주문 생성 (재고 감소)
   */
  @Transactional
  public Order createOrder(Long productId, int quantity) {
    // 상품 조회 및 재고 감소 (비관적 락 사용)
    Product product = productService.findProductWithLock(productId);
    // 중복된 주문이 이미 처리된 경우 방지
    boolean pendingOrderExists = orderRepository.existsByProductIdAndOrderStatus(productId, OrderStatus.PENDING);
    if (pendingOrderExists) {
      throw new IllegalStateException("이미 동일한 상품에 대한 주문이 처리 중입니다.");
    }

    // 재고 감소 처리 (Product 객체에서 수행)
    product.decreaseStock(quantity);

    // 주문 생성
    Order order = Order.createOrder(product, quantity);

    // 주문 저장
    return orderRepository.save(order);
  }

  /**
   * 주문 결제 (상태를 PAID로 변경)
   */
  @Transactional
  public Order payForOrder(Long orderId) {
    // 주문 조회
    Order order = orderRepository.findByIdWithLock(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. 주문 ID: " + orderId));
    // 이미 결제된 주문은 중복 결제 방지
    if (order.getOrderStatus() == OrderStatus.PAID) {
      throw new IllegalStateException("이미 결제된 주문입니다.");
    }
    // 결제 처리
    order.payForOrder();
    // 결제 처리 후 변경된 주문 저장
    return orderRepository.save(order);
  }

  /**
   * 주문 배송 (상태를 SHIPPED로 변경)
   */
  @Transactional
  public Order shipOrder(Long orderId) {
    // 주문 조회
    Order order = orderRepository.findByIdWithLock(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. 주문 ID: " + orderId));

    // 이미 배송된 주문은 중복 배송 방지
    if (order.getOrderStatus() == OrderStatus.SHIPPED) {
      throw new IllegalStateException("이미 배송된 주문입니다.");
    }

    // 배송 처리
    order.shipOrder();
    // 배송 처리 후 변경된 주문 저장
    return orderRepository.save(order);
  }

  /**
   * 주문 취소 (재고 복원)
   */
  @Transactional
  public Order cancelOrder(Long orderId) {
    // 주문 조회
    Order order = orderRepository.findByIdWithLock(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. 주문 ID: " + orderId));

    // 이미 취소된 주문은 중복 취소 방지
    if (order.getOrderStatus() == OrderStatus.CANCELLED) {
      throw new IllegalStateException("이미 취소된 주문입니다.");
    }

    // 상품 조회 (비관적 락 사용)
    Product product = productService.findProductWithLock(order.getProductId());

    // 주문 취소 및 재고 복원
    order.cancelOrder(product);

    // 변경된 주문 저장
    return orderRepository.save(order);
  }

  /**
   * 주문 조회 (단일 주문)
   */
  @Transactional(readOnly = true)
  public Order getOrderById(Long orderId) {
    return orderRepository.findById(orderId)
      .orElseThrow(() -> new ResourceNotFoundException("주문을 찾을 수 없습니다. 주문 ID: " + orderId));
  }
}
