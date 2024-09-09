package com.example.transaction.application.repository;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
@Builder
@Table(name = "shipments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Shipment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long shipmentId;

  @Column(name = "order_id", nullable = false)
  private Long orderId;  // Order 객체 대신 orderId 필드 사용

  private LocalDateTime shipmentDate;

  @Enumerated(EnumType.STRING)
  private ShipmentStatus shipmentStatus;

  private String trackingNumber;

  /**
   * 배송을 생성하는 팩토리 메서드
   * 주문 정보를 바탕으로 배송을 생성하고, 배송 상태와 추적 번호를 설정합니다.
   */
  public static Shipment createShipment(Long orderId, String trackingNumber) {
    return Shipment.builder()
      .orderId(orderId)
      .shipmentStatus(ShipmentStatus.PENDING) // 배송 생성 시 PENDING 상태로 시작
      .trackingNumber(trackingNumber)
      .shipmentDate(LocalDateTime.now())
      .build();
  }

  /**
   * 배송을 시작하는 메서드
   * 배송을 SHIPPED 상태로 변경하고 배송일자를 기록합니다.
   */
  public void ship(String trackingNumber) {
    if (this.shipmentStatus != ShipmentStatus.PENDING) {
      throw new IllegalStateException("배송은 PENDING 상태일 때만 시작할 수 있습니다.");
    }
    this.shipmentStatus = ShipmentStatus.SHIPPED;
    this.shipmentDate = LocalDateTime.now();
    this.trackingNumber = trackingNumber;
  }

  /**
   * 배송 취소 메서드
   * 배송을 취소 상태로 변경합니다. (이미 배송이 완료된 경우에는 취소 불가)
   */
  public void cancelShipment() {
    if (this.shipmentStatus == ShipmentStatus.DELIVERED) {
      throw new IllegalStateException("이미 완료된 배송은 취소할 수 없습니다.");
    }
    this.shipmentStatus = ShipmentStatus.CANCELLED;
  }

  /**
   * 배송이 완료되었음을 표시하는 메서드
   * 배송 상태를 DELIVERED로 변경하고 완료일자를 기록합니다.
   */
  public void completeShipment() {
    if (this.shipmentStatus != ShipmentStatus.SHIPPED) {
      throw new IllegalStateException("배송이 완료되기 전에는 배송 완료를 처리할 수 없습니다.");
    }
    this.shipmentStatus = ShipmentStatus.DELIVERED;
  }
}