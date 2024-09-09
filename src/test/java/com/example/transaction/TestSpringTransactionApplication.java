package com.example.transaction;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.SpringApplication;

@Disabled
public class TestSpringTransactionApplication {

  public static void main(String[] args) {
    SpringApplication.from(SpringTransactionApplication::main).with(TestcontainersConfiguration.class).run(args);
  }

}
