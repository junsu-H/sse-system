package com.system.sse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SseSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(SseSystemApplication.class, args);
	}

}
