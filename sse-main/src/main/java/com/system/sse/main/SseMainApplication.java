package com.system.sse.main;

import com.system.sse.application.auth.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication(scanBasePackages = "com.system.sse")
@EnableConfigurationProperties(JwtProperties.class)
public class SseMainApplication {

	public static void main(String[] args) {
		SpringApplication.run(SseMainApplication.class, args);
	}

}
