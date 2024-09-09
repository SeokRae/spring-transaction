package com.example.transaction;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractIntegrationTest {

  @Autowired
  protected EntityManager entityManager;

  @Container
  public static MySQLContainer<?> mysqlContainer =
    new MySQLContainer<>("mysql:8.0.32")
//      .withReuse(true) // 컨테이너 재사용 설정
      .withDatabaseName("testdb")
      .withUsername("testuser")
      .withPassword("testpass")
      // MySQL 타임아웃 설정 추가
      .withCommand(
//"--transaction-isolation=READ-COMMITTED", // 트랜잭션 격리 수준 설정
        "--wait_timeout=28800", // 대기 시간 8시간
        "--interactive_timeout=28800", // 세션 대기 시간 8시간
        "--innodb_lock_wait_timeout=5" // 락 대기 시간 5초
      );

  @DynamicPropertySource
  static void mysqlProperties(DynamicPropertyRegistry registry) {
    // JDBC URL에 autoReconnect=true 옵션을 추가
    registry.add("spring.datasource.url",
      () -> mysqlContainer.getJdbcUrl() + "?autoReconnect=true&reconnectAtTxEnd=true&useSSL=false");
    registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
    registry.add("spring.datasource.username", mysqlContainer::getUsername);
    registry.add("spring.datasource.password", mysqlContainer::getPassword);
    registry.add("spring.datasource.driver-class-name", mysqlContainer::getDriverClassName);
    // HikariCP settings
    registry.add("spring.datasource.hikari.maximum-pool-size", () -> "100");
    registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");  // 30초로 늘림
    registry.add("spring.datasource.hikari.validation-timeout", () -> "5000");   // 5초로 줄임
    registry.add("spring.datasource.hikari.idle-timeout", () -> "60000");       // 60초로 늘림
    registry.add("spring.datasource.hikari.max-lifetime", () -> "60000");      // 60초로 늘림
    registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "5000");      // 5초 이상 사용된 커넥션에 대해 경고 출력

    // Optional: 커넥션 검증 쿼리
    registry.add("spring.datasource.hikari.connection-test-query", () -> "SELECT 1");
  }
}