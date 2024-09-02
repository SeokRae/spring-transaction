package com.example.transaction;

import org.springframework.boot.SpringApplication;

public class TestSpringTransactionApplication {

  public static void main(String[] args) {
    SpringApplication.from(SpringTransactionApplication::main).with(TestcontainersConfiguration.class).run(args);
  }

}
