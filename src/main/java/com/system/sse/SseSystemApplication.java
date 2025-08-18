package com.system.sse;

import com.system.auth.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {
		"com.system.sse",
		"com.system.auth.**"
})
@EnableConfigurationProperties(JwtProperties.class)
public class SseSystemApplication {
	public static void main(String[] args) {
		SpringApplication.run(SseSystemApplication.class, args);
	}

}
