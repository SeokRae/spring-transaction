package com.example.transaction.application.repository;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Table(name = "inventory_transactions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long transactionId;

  @ManyToOne
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Enumerated(EnumType.STRING)
  private TransactionType transactionType;

  private int quantity;
  private LocalDateTime transactionDate;

  // Getters and Setters
}