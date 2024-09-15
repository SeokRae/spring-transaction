package com.example.transaction.application.repository;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Setter
@Getter
@Entity
@ToString(of = {"productId", "stockQuantity", "price"})
@EqualsAndHashCode(of = "productId")
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long productId;

  @Column(nullable = false)
  private String productName;

  @Column(nullable = false)
  private int stockQuantity;

  @Column(nullable = false)
  private BigDecimal price;

  private String productDescription;

  // 팩토리 메서드를 통한 객체 생성
  public static Product createProduct(String productName, int stockQuantity, BigDecimal price, String productDescription) {
    if (stockQuantity < 0) {
      throw new IllegalArgumentException("재고는 0보다 커야 합니다.");
    }
    if (price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("가격은 0보다 커야 합니다.");
    }

    Product product = new Product();
    product.productName = productName;
    product.stockQuantity = stockQuantity;
    product.price = price;
    product.productDescription = productDescription;
    return product;
  }


  /**
   * 상품의 재고 감소
   */
  public void decreaseStock(int quantity) {
    if (this.stockQuantity < quantity) {
      throw new IllegalArgumentException("재고가 부족합니다.");
    }
    this.stockQuantity -= quantity;
  }

  /**
   * 상품의 재고 증가
   * - 재고가 0보다 작아지는 경우를 방지하기 위해 추가 유효성 검사를 할 수 있음
   */
  public void increaseStock(int quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("재고는 음수로 증가할 수 없습니다.");
    }
    this.stockQuantity += quantity;
  }
}