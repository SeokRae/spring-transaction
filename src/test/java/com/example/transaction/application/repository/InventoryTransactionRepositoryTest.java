package com.example.transaction.application.repository;

import com.example.transaction.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("[InventoryTransaction] 테이블 관련 테스트")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class InventoryTransactionRepositoryTest extends AbstractIntegrationTest {

  @Autowired
  private InventoryTransactionRepository inventoryTransactionRepository;

  @Autowired
  private ProductRepository productRepository;

  @DisplayName("[재고 트랜잭션] 저장 및 조회 테스트")
  @Test
  void testSaveAndFindInventoryTransaction() {
    // Given
    Product product = new Product();
    product.setProductName("Test Product");
    product.setStockQuantity(100);
    product.setPrice(new BigDecimal("1000.00"));

    productRepository.save(product);
    entityManager.clear();

    InventoryTransaction inventoryTransaction = new InventoryTransaction();
    inventoryTransaction.setProduct(product);
    inventoryTransaction.setTransactionType(TransactionType.INCREASE);
    inventoryTransaction.setQuantity(100);

    // When
    InventoryTransaction savedInventoryTransaction = inventoryTransactionRepository.save(inventoryTransaction);
    entityManager.clear();

    // Then
    assertThat(inventoryTransactionRepository.findById(savedInventoryTransaction.getTransactionId()))
      .satisfies(inventoryTransaction1 -> {
        assertThat(inventoryTransaction1.get().getProduct()).isNotNull();
        assertThat(inventoryTransaction1.get().getTransactionType()).isEqualTo(TransactionType.INCREASE);
        assertThat(inventoryTransaction1.get().getQuantity()).isEqualTo(100);
      });
  }
}