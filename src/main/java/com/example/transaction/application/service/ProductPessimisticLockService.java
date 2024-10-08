package com.example.transaction.application.service;

import com.example.transaction.application.repository.Product;
import com.example.transaction.application.repository.ProductRepository;
import com.example.transaction.exception.ResourceNotFoundException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductPessimisticLockService {

  private final ProductRepository productRepository;

  /**
   * 재고 감소 (비관적 락 사용)
   */
  @Transactional
  public Product decreaseStock(Long productId, int quantity) {
    Product product = productRepository.findByIdWithLock(productId)
      .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다.: " + productId)); // 비관락 사용
    if (product.getStockQuantity() < quantity) {
      throw new IllegalArgumentException("재고가 부족합니다.");
    }
    product.decreaseStock(quantity);
    Product save = productRepository.save(product);
    log.info("[ProductPessimisticLockService] 재고 감소: {}", save);
    return save;  // 재고 변경 후 저장
  }

  /**
   * 재고 증가 (비관적 락 사용)
   */
  @Transactional
  public void increaseStock(Long productId, int quantity) {
    Product product = productRepository.findByIdWithLock(productId)
      .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다.: " + productId)); // 비관락 사용
    product.increaseStock(quantity);
    Product save = productRepository.save(product);// 재고 복원 후 저장
    log.info("[ProductPessimisticLockService] 재고 증가: {}", save);
  }


  /**
   * 비관적 락을 사용한 상품 조회
   */
  @Transactional
  public Product findProductWithLock(Long productId) {
    try {
      Product product = productRepository.findByIdWithLock(productId)
        .orElseThrow(() -> new ResourceNotFoundException("상품을 찾을 수 없습니다."));
      log.info("[ProductPessimisticLockService] 상품 조회: {}", product);
      return product;
    } catch (PessimisticLockException | LockTimeoutException e) {
      // 비관적 락 예외 처리
      throw new IllegalStateException("다른 프로세스에서 이미 상품을 사용 중입니다. 다시 시도해 주세요.", e);
    }
  }
}