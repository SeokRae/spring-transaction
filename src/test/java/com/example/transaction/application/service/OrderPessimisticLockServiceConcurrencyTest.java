package com.example.transaction.application.service;

import com.example.transaction.AbstractIntegrationTest;
import com.example.transaction.application.repository.Order;
import com.example.transaction.application.repository.OrderStatus;
import com.example.transaction.application.repository.Product;
import com.example.transaction.application.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DisplayName("[OrderService] 동시성 처리 통합 테스트")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class OrderPessimisticLockServiceConcurrencyTest extends AbstractIntegrationTest {

  @Autowired
  private OrderPessimisticLockService orderPessimisticLockService;

  @Autowired
  private ProductRepository productRepository;

  @Test
  @DisplayName("동시성 테스트 - 여러 스레드에서 동시에 주문 요청으로 재고 감소 시도 및 중복 주문 방지")
  void concurrentOrderTestWithDuplicatePrevention() throws InterruptedException {
    // Given: 재고가 100인 상품을 생성
    Product product = Product.createProduct(
      "Concurrent Test Product", 100, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    int threadCount = 10;  // 10개의 스레드가 동시에 실행
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);  // 스레드 동기화를 위한 CountDownLatch

    // 성공한 주문 카운트를 저장할 변수
    AtomicInteger successfulOrderCount = new AtomicInteger(0);
    AtomicInteger failedOrderCount = new AtomicInteger(0);

    // When: 각 스레드가 동시에 10개씩 재고 감소 시도
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          orderPessimisticLockService.createOrder(product.getProductId(), 10);  // 각 스레드가 10개씩 주문 시도
          successfulOrderCount.incrementAndGet();  // 성공한 주문 카운트 증가
        } catch (Exception e) {
          failedOrderCount.incrementAndGet();  // 실패한 주문 카운트 증가
        } finally {
          latch.countDown();  // 스레드가 종료될 때 latch 감소
        }
      });
    }

    // 모든 스레드가 작업을 완료할 때까지 대기
    latch.await();

    // Then: 재고가 정확하게 감소했는지 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(90);  // 성공한 주문 하나만 10 감소 (100 - 10 = 90)

    // 성공한 주문 수는 1번, 실패한 주문 수는 나머지 9번이어야 함
    assertThat(successfulOrderCount.get()).isEqualTo(1);  // 한 번만 주문 성공
    assertThat(failedOrderCount.get()).isEqualTo(9);  // 나머지 9번은 실패
  }

  @Test
  @DisplayName("동시성 테스트 - 주문 결제 중복 방지")
  void concurrentPayForOrderTest() throws InterruptedException {
    // Given: 재고가 50인 상품과 주문을 생성
    Product product = Product.createProduct(
      "Concurrent Pay Test Product", 50, BigDecimal.valueOf(100.00), "Test Description"
    );
    productRepository.save(product);

    Order order = orderPessimisticLockService.createOrder(product.getProductId(), 5);  // 주문 생성

    int threadCount = 10;  // 10개의 스레드가 동시에 실행
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);

    // When: 각 스레드가 동시에 주문 결제를 시도
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          orderPessimisticLockService.payForOrder(order.getOrderId());  // 결제 시도
          successCount.incrementAndGet();  // 성공 시 카운트 증가
        } catch (Exception e) {
          failureCount.incrementAndGet();  // 실패 시 카운트 증가
        } finally {
          latch.countDown();
        }
      });
    }

    // 모든 스레드가 작업을 완료할 때까지 대기
    latch.await();

    // Then: 하나의 결제만 성공하고 나머지는 실패했는지 확인
    assertThat(successCount.get()).isEqualTo(1);  // 한 번만 결제 성공
    assertThat(failureCount.get()).isEqualTo(threadCount - 1);  // 나머지 결제 실패

    // 결제 완료 후 주문 상태 확인
    Order updatedOrder = orderPessimisticLockService.getOrderById(order.getOrderId());
    assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.PAID);  // 주문 상태가 PAID로 변경됨
  }
}