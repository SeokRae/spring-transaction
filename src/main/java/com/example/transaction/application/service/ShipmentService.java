package com.example.transaction.application.service;

import com.example.transaction.application.repository.Shipment;
import com.example.transaction.application.repository.ShipmentRepository;
import com.example.transaction.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipmentService {

  private final ShipmentRepository shipmentRepository;

  /**
   * 배송 생성
   * 외부에서 주문을 조회한 후 orderId와 trackingNumber를 전달받아 처리
   */
  @Transactional
  public Shipment createShipment(Long orderId, String trackingNumber) {
    // 배송 생성 (Order는 외부에서 조회하여 전달받음)
    Shipment shipment = Shipment.createShipment(orderId, trackingNumber);

    // 배송 저장
    return shipmentRepository.save(shipment);
  }

  /**
   * 배송 시작
   */
  @Transactional
  public Shipment shipShipment(Long shipmentId, String trackingNumber) {
    // 배송 조회
    Shipment shipment = shipmentRepository.findById(shipmentId)
      .orElseThrow(() -> new ResourceNotFoundException("배송 정보를 찾을 수 없습니다. 배송 ID: " + shipmentId));

    // 도메인 로직 호출
    shipment.ship(trackingNumber);

    // 변경된 상태 저장
    return shipmentRepository.save(shipment);
  }

  /**
   * 배송 취소
   */
  @Transactional
  public void cancelShipment(Long shipmentId) {
    // 배송 조회
    Shipment shipment = shipmentRepository.findById(shipmentId)
      .orElseThrow(() -> new ResourceNotFoundException("배송 정보를 찾을 수 없습니다. 배송 ID: " + shipmentId));

    // 도메인 로직 호출
    shipment.cancelShipment();

    // 변경된 상태 저장
    shipmentRepository.save(shipment);
  }

  /**
   * 배송 완료 처리
   */
  @Transactional
  public void completeShipment(Long shipmentId) {
    // 배송 조회
    Shipment shipment = shipmentRepository.findById(shipmentId)
      .orElseThrow(() -> new ResourceNotFoundException("배송 정보를 찾을 수 없습니다. 배송 ID: " + shipmentId));

    // 도메인 로직 호출
    shipment.completeShipment();

    // 변경된 상태 저장
    shipmentRepository.save(shipment);
  }

  /**
   * 배송 조회
   */
  @Transactional(readOnly = true)
  public Shipment getShipmentById(Long shipmentId) {
    return shipmentRepository.findById(shipmentId)
      .orElseThrow(() -> new ResourceNotFoundException("배송 정보를 찾을 수 없습니다. 배송 ID: " + shipmentId));
  }
}