package com.example.transaction.application.service;

import com.example.transaction.AbstractIntegrationTest;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[ProductService] Pessimistic Concurrency 테스트")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class PessimisticLockConcurrencyTest extends AbstractIntegrationTest {

  @Autowired
  private ProductPessimisticLockService productService;

  @Autowired
  private ProductRepository productRepository;

  @Test
  @DisplayName("동시성 테스트 - 재고 감소")
  void testConcurrentDecreaseStock() throws InterruptedException {
    // Given: 상품을 생성하고 재고를 100으로 설정
    Product product = Product.createProduct("Concurrent Test Product", 100, new BigDecimal("100.00"), "Test Description");
    productRepository.save(product);

    int threadCount = 10; // 10개의 스레드가 동시에 실행
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // When: 각 스레드가 동시에 10개의 재고를 감소
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          productService.decreaseStock(product.getProductId(), 10); // 각 스레드가 10개의 재고를 감소
        } finally {
          latch.countDown();
        }
      });
    }

    // 모든 스레드가 작업을 마칠 때까지 대기
    latch.await();

    // Then: 재고가 0으로 감소되었는지 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(0); // 100 - (10 * 10) = 0
  }

  @Test
  @DisplayName("동시성 테스트 - 재고 부족 시 예외 처리")
  void testConcurrentDecreaseStockInsufficientStock() throws InterruptedException {
    // Given: 상품을 생성하고 재고를 50으로 설정
    Product product = Product.createProduct("Insufficient Stock Test Product", 50, new BigDecimal("100.00"), "Test Description");
    productRepository.save(product);

    int threadCount = 10; // 10개의 스레드가 동시에 실행
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // When: 각 스레드가 동시에 10개의 재고를 감소
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          productService.decreaseStock(product.getProductId(), 10); // 각 스레드가 10개의 재고를 감소
        } catch (IllegalArgumentException e) {
          // 재고 부족으로 인한 예외 처리
          assertThat(e.getMessage()).isEqualTo("재고가 부족합니다.");
        } finally {
          latch.countDown();
        }
      });
    }

    // 모든 스레드가 작업을 마칠 때까지 대기
    latch.await();

    // Then: 재고가 0 이상으로 남았는지 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isGreaterThanOrEqualTo(0);
  }

  @Test
  @DisplayName("동시성 테스트 - 재고 증가")
  void testConcurrentIncreaseStock() throws InterruptedException {
    // Given: 상품을 생성하고 재고를 0으로 설정
    Product product = Product.createProduct("Concurrent Test Product", 0, new BigDecimal("100.00"), "Test Description");
    productRepository.save(product);

    int threadCount = 10; // 10개의 스레드가 동시에 실행
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // When: 각 스레드가 동시에 10개의 재고를 증가
    for (int i = 0; i < threadCount; i++) {
      executorService.submit(() -> {
        try {
          productService.increaseStock(product.getProductId(), 10); // 각 스레드가 10개의 재고를 증가
        } finally {
          latch.countDown();
        }
      });
    }

    // 모든 스레드가 작업을 마칠 때까지 대기
    latch.await();

    // Then: 재고가 100으로 증가했는지 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(100); // 0 + (10 * 10) = 100
  }

}