package com.example.transaction.core.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;

@Slf4j
public class LoggingJpaTransactionManager extends JpaTransactionManager {

  @Override
  protected void doBegin(Object transaction, TransactionDefinition definition) {
    log.info("트랜잭션 시작 - 전파 수준: {}, 격리 수준: {}",
      propagationToString(definition.getPropagationBehavior()),
      isolationToString(definition.getIsolationLevel()));
    super.doBegin(transaction, definition);
  }

  @Override
  protected void doCommit(DefaultTransactionStatus status) {
    log.info("트랜잭션 커밋");
    super.doCommit(status);
  }

  @Override
  protected void doRollback(DefaultTransactionStatus status) {
    log.info("트랜잭션 롤백");
    super.doRollback(status);
  }

  private String propagationToString(int propagationBehavior) {
    return switch (propagationBehavior) {
      case TransactionDefinition.PROPAGATION_REQUIRED -> "REQUIRED";
      case TransactionDefinition.PROPAGATION_REQUIRES_NEW -> "REQUIRES_NEW";
      case TransactionDefinition.PROPAGATION_MANDATORY -> "MANDATORY";
      case TransactionDefinition.PROPAGATION_SUPPORTS -> "SUPPORTS";
      case TransactionDefinition.PROPAGATION_NOT_SUPPORTED -> "NOT_SUPPORTED";
      case TransactionDefinition.PROPAGATION_NEVER -> "NEVER";
      case TransactionDefinition.PROPAGATION_NESTED -> "NESTED";
      default -> "UNKNOWN";
    };
  }

  private String isolationToString(int isolationLevel) {
    return switch (isolationLevel) {
      case TransactionDefinition.ISOLATION_DEFAULT -> "기본값";
      case TransactionDefinition.ISOLATION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
      case TransactionDefinition.ISOLATION_READ_COMMITTED -> "READ_COMMITTED";
      case TransactionDefinition.ISOLATION_REPEATABLE_READ -> "REPEATABLE_READ";
      case TransactionDefinition.ISOLATION_SERIALIZABLE -> "SERIALIZABLE";
      default -> "알 수 없음";
    };
  }
}