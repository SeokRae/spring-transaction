package com.example.transaction.core.listner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static java.sql.Connection.*;

@Slf4j
public class TransactionMonitoringListener implements TransactionSynchronization {

  private final String className;
  private final String methodName;
  private int stepCounter = 0;

  public TransactionMonitoringListener(String className, String methodName) {
    this.className = className;
    this.methodName = methodName;
  }

  @Override
  public void beforeCommit(boolean readOnly) {
    logTransactionInfo("트랜잭션이 곧 커밋됩니다 (읽기 전용: " + readOnly + ")");
  }

  @Override
  public void beforeCompletion() {
    logTransactionInfo("트랜잭션이 곧 완료됩니다");
  }

  @Override
  public void afterCommit() {
    logTransactionInfo("트랜잭션이 커밋되었습니다");
  }

  @Override
  public void afterCompletion(int status) {
    String statusMessage = (status == STATUS_COMMITTED) ? "커밋" : "롤백";
    logTransactionInfo("트랜잭션이 " + statusMessage + " 상태로 완료되었습니다");
    stepCounter = 0;  // 트랜잭션 종료 시 카운터 초기화
  }

  @Override
  public void suspend() {
    logTransactionInfo("트랜잭션이 일시 중단되었습니다");
  }

  @Override
  public void resume() {
    logTransactionInfo("트랜잭션이 다시 시작되었습니다");
  }

  private void logTransactionInfo(String message) {
    boolean isTransactionActive = TransactionSynchronizationManager.isActualTransactionActive();
    boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();
    boolean isSynchronizationActive = TransactionSynchronizationManager.isSynchronizationActive();

    String isolationLevelName = mapIsolationLevelToName(isolationLevel);

    log.info("[단계 {}] {}.{} - {} | (트랜잭션 활성화 여부: {}, 읽기 전용 여부: {}, 격리 수준: {}, 동기화 활성화 여부: {})",
      ++stepCounter, className, methodName, message, isTransactionActive, isReadOnly, isolationLevelName, isSynchronizationActive);
  }

  private String mapIsolationLevelToName(Integer isolationLevel) {
    if (isolationLevel == null) {
      return "기본값";
    }
    return switch (isolationLevel) {
      case TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
      case TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
      case TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
      case TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
      default -> "알 수 없음";
    };
  }
}