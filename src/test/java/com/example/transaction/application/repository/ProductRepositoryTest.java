package com.example.transaction.application.repository;

import com.example.transaction.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[products] 테이블 관련 테스트")
@DataJpaTest
/* 내장 DB 자동으로 사용하려는 것을 방지*/
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class ProductRepositoryTest extends AbstractIntegrationTest {

  @Autowired
  private ProductRepository productRepository;

  @DisplayName("[상품] 저장 및 조회 테스트")
  @Test
  public void testSaveAndFindProduct() {
    // Given
    Product product = new Product();
    product.setProductName("Test Product");
    product.setProductDescription("Description of Test Product");
    product.setStockQuantity(100);
    product.setPrice(new BigDecimal("1000.00"));

    // When
    Product savedProduct = productRepository.save(product);
    entityManager.clear();
    // Then
    Optional<Product> foundProduct = productRepository.findById(savedProduct.getProductId());
    assertThat(foundProduct)
      .satisfies(product1 -> {
        assertThat(product1.get().getProductName()).isEqualTo("Test Product");
        assertThat(product1.get().getProductDescription()).isEqualTo("Description of Test Product");
        assertThat(product1.get().getStockQuantity()).isEqualTo(100);
        assertThat(product1.get().getPrice()).isEqualTo(new BigDecimal("1000.00"));
      });
  }

  @DisplayName("[상품] 삭제 테스트")
  @Test
  public void testDeleteProduct() {
    // Given
    Product product = new Product();
    product.setProductName("Product to Delete");
    product.setStockQuantity(50);
    product.setPrice(new BigDecimal("900.00"));
    productRepository.save(product);

    entityManager.clear();
    // When
    productRepository.delete(product);
    productRepository.flush();

    // Then
    Optional<Product> foundProduct = productRepository.findById(product.getProductId());
    assertThat(foundProduct).isNotPresent();
  }

  @DisplayName("[상품] 재고 수정 테스트")
  @Test
  public void testUpdateProductStock() {
    // Given
    Product product = new Product();
    product.setProductName("Product to Update");
    product.setStockQuantity(20);
    product.setPrice(new BigDecimal("500.00"));
    productRepository.save(product);
    entityManager.clear();

    // When
    product.setStockQuantity(10);
    productRepository.save(product);
    entityManager.flush();
    entityManager.clear();

    // Then
    Optional<Product> updatedProduct = productRepository.findById(product.getProductId());
    assertThat(updatedProduct)
      .isPresent()
      .satisfies(p -> {
        assertThat(p.get().getProductName()).isEqualTo("Product to Update");
        assertThat(p.get().getStockQuantity()).isEqualTo(10);
        assertThat(p.get().getPrice()).isEqualTo(new BigDecimal("500.00"));
      });
  }

  @DisplayName("[상품] 전체 조회 테스트")
  @Test
  public void testFindAllProducts() {
    // Given
    Product product1 = new Product();
    product1.setProductName("Product 1");
    product1.setStockQuantity(30);
    product1.setPrice(new BigDecimal("300.00"));

    Product product2 = new Product();
    product2.setProductName("Product 2");
    product2.setStockQuantity(40);
    product2.setPrice(new BigDecimal("400.00"));

    productRepository.save(product1);
    productRepository.save(product2);

    // When
    List<Product> products = productRepository.findAll();

    // Then
    assertThat(products)
      .isNotNull()
      .hasSize(2)
      .satisfies(products1 -> {
        assertThat(products1.get(0).getProductName()).isEqualTo("Product 1");
        assertThat(products1.get(0).getStockQuantity()).isEqualTo(30);
        assertThat(products1.get(0).getPrice()).isEqualTo(new BigDecimal("300.00"));

        assertThat(products1.get(1).getProductName()).isEqualTo("Product 2");
        assertThat(products1.get(1).getStockQuantity()).isEqualTo(40);
        assertThat(products1.get(1).getPrice()).isEqualTo(new BigDecimal("400.00"));
      });
  }
}