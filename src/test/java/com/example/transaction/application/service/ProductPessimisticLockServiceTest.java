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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("[ProductService] Pessimistic Lock 테스트")
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ProductPessimisticLockServiceTest extends AbstractIntegrationTest {

  @Autowired
  private ProductPessimisticLockService productService;

  @Autowired
  private ProductRepository productRepository;

  @Test
  @DisplayName("재고 감소 테스트")
  void testDecreaseStock() {
    // Given: 상품을 생성하고 저장
    Product product = Product.createProduct("Test Product", 100, new BigDecimal("100.00"), "Test Description");
    productRepository.save(product);

    // When: 재고를 10개 감소
    productService.decreaseStock(product.getProductId(), 10);

    // Then: 재고가 90개로 감소되었는지 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(90);
  }

  @Test
  @DisplayName("재고 증가 테스트")
  void testIncreaseStock() {
    // Given: 상품을 생성하고 저장
    Product product = Product.createProduct("Test Product", 50, new BigDecimal("100.00"), "Test Description");
    productRepository.save(product);

    // When: 재고를 10개 증가
    productService.increaseStock(product.getProductId(), 10);

    // Then: 재고가 60개로 증가되었는지 확인
    Product updatedProduct = productRepository.findById(product.getProductId()).orElseThrow();
    assertThat(updatedProduct.getStockQuantity()).isEqualTo(60);
  }

  @Test
  @DisplayName("재고 부족 예외 발생 테스트")
  void testDecreaseStock_InsufficientStock() {
    // Given: 재고가 5인 상품을 생성
    Product product = Product.createProduct("Limited Stock Product", 5, new BigDecimal("100.00"), "Test Description");
    productRepository.save(product);

    // When & Then: 재고보다 많은 양을 감소시키려 할 때 예외 발생
    assertThatThrownBy(() -> productService.decreaseStock(product.getProductId(), 10))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("재고가 부족합니다.");
  }
}