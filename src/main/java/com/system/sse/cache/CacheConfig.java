package com.system.sse.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;


@EnableCaching
@Configuration
public class CacheConfig {

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager cm = new CaffeineCacheManager("refreshTokens");
        cm.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(Duration.ofDays(7))
                        .maximumSize(10_000)
        );

        return cm;
    }
}
