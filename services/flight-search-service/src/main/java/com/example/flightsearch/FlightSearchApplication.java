package com.example.flightsearch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;

@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class FlightSearchApplication {
  public static void main(String[] args) {
    SpringApplication.run(FlightSearchApplication.class, args);
  }
}
