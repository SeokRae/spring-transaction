package com.example.transaction.application.repository;

public enum ShipmentStatus {
  PENDING,    // 배송 준비 중
  SHIPPED,    // 배송 시작
  DELIVERED,  // 배송 완료
  CANCELLED   // 배송 취소
}