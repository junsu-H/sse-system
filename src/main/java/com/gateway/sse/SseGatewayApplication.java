package com.gateway.sse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class SseGatewayApplication {
	public static void main(String[] args) {
		SpringApplication.run(SseGatewayApplication.class, args);
	}

}
