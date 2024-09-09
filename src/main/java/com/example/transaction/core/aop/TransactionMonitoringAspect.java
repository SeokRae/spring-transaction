package com.example.transaction.core.aop;

import com.example.transaction.core.listner.TransactionMonitoringListener;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TransactionMonitoringAspect {

  // 트랜잭션 메서드 포인트컷 (Transactional 어노테이션이 있는 경우)
  @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
  private void transactionalMethod() {
  }

  // 트랜잭션 시작 전 로그를 출력하고 트랜잭션 리스너 등록
  @Before("transactionalMethod()")
  public void registerTransactionListener(JoinPoint joinPoint) {
    String className = joinPoint.getTarget().getClass().getSimpleName();
    String methodName = joinPoint.getSignature().getName();

    // @Transactional이 있는지 확인한 후에 트랜잭션 정보를 추출
    TransactionDefinition def = extractTransactionDefinition(joinPoint);
    if (def != null) {
      String propagation = transPropagation(def);
      String isolation = transIsolationLevel(def);
      log.info("트랜잭션 시작 - 클래스: {}, 메서드: {} | 전파 수준: {}, 격리 수준: {}",
        className, methodName, propagation, isolation);

      // 비관적 락 감지
      if (isPessimisticLock(joinPoint)) {
        log.info("비관적 락(Pessimistic Lock) 사용 - 클래스: {}, 메서드: {}", className, methodName);
      }

      // 낙관적 락 감지
      if (isOptimisticLock(joinPoint)) {
        log.info("낙관적 락(Optimistic Lock) 사용 - 클래스: {}, 메서드: {}", className, methodName);
      }

      // 트랜잭션 모니터링 리스너를 등록
      TransactionMonitoringListener listener = new TransactionMonitoringListener(className, methodName);
      TransactionSynchronizationManager.registerSynchronization(listener);

      log.info("트랜잭션 모니터링 리스너가 등록되었습니다 - 클래스: {}, 메서드: {}", className, methodName);
    } else {
      log.info("트랜잭션이 없는 메서드 - 클래스: {}, 메서드: {}", className, methodName);
    }
  }

  // 비관적 락이 사용된 메서드를 감지
  private boolean isPessimisticLock(JoinPoint joinPoint) {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    Lock lock = AnnotationUtils.findAnnotation(method, Lock.class);

    // 로그 추가
    if (lock != null) {
      LockModeType lockMode = lock.value();
      log.info("비관적 락 감지됨 - 메서드: {}, 락 모드: {}", method.getName(), lockMode);
      return lockMode == LockModeType.PESSIMISTIC_WRITE || lockMode == LockModeType.PESSIMISTIC_READ;
    }
    return false;
  }

  // 낙관적 락이 사용된 엔티티를 감지
  private boolean isOptimisticLock(JoinPoint joinPoint) {
    Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
    // 엔티티 클래스의 필드 중 @Version 어노테이션이 있는지 확인
    Class<?> returnType = method.getReturnType();
    return Arrays.stream(returnType.getDeclaredFields())
      .anyMatch(field -> field.isAnnotationPresent(Version.class));
  }

  private String transPropagation(TransactionDefinition def) {
    return switch (def.getPropagationBehavior()) {
      case TransactionDefinition.PROPAGATION_REQUIRED -> "REQUIRED (필수)";
      case TransactionDefinition.PROPAGATION_REQUIRES_NEW -> "REQUIRES_NEW (새 트랜잭션)";
      case TransactionDefinition.PROPAGATION_MANDATORY -> "MANDATORY (필수)";
      case TransactionDefinition.PROPAGATION_SUPPORTS -> "SUPPORTS (지원)";
      case TransactionDefinition.PROPAGATION_NOT_SUPPORTED -> "NOT_SUPPORTED (비지원)";
      case TransactionDefinition.PROPAGATION_NEVER -> "NEVER (절대 안 됨)";
      case TransactionDefinition.PROPAGATION_NESTED -> "NESTED (중첩)";
      default -> "알 수 없음";
    };
  }

  private String transIsolationLevel(TransactionDefinition def) {
    return switch (def.getIsolationLevel()) {
      case TransactionDefinition.ISOLATION_DEFAULT -> "기본값";
      case TransactionDefinition.ISOLATION_READ_UNCOMMITTED -> "READ_UNCOMMITTED (커밋되지 않은 읽기)";
      case TransactionDefinition.ISOLATION_READ_COMMITTED -> "READ_COMMITTED (커밋된 읽기)";
      case TransactionDefinition.ISOLATION_REPEATABLE_READ -> "REPEATABLE_READ (반복 가능한 읽기)";
      case TransactionDefinition.ISOLATION_SERIALIZABLE -> "SERIALIZABLE (직렬화 가능)";
      default -> "알 수 없음";
    };
  }

  private TransactionDefinition extractTransactionDefinition(JoinPoint joinPoint) {
    try {
      Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
      Transactional transactional = AnnotationUtils.findAnnotation(method, Transactional.class);

      if (transactional != null) {
        return new DefaultTransactionDefinition() {{
          setPropagationBehavior(transactional.propagation().value());
          setIsolationLevel(transactional.isolation().value());
          setTimeout(transactional.timeout());
          setReadOnly(transactional.readOnly());
        }};
      }
    } catch (Exception e) {
      log.warn("트랜잭션 정의를 추출하는 데 실패했습니다", e);
    }
    return null;
  }
}