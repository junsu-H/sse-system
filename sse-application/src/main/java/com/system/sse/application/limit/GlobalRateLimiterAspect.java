package com.system.sse.application.limit;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.springframework.http.HttpStatus;

@Aspect
@Component
public class GlobalRateLimiterAspect {

    private final RateLimiter rateLimiter;

    public GlobalRateLimiterAspect(RateLimiterRegistry registry) {
        this.rateLimiter = registry.rateLimiter("global");
    }

    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object applyRateLimiter(ProceedingJoinPoint pjp) throws Throwable {
        if (!rateLimiter.acquirePermission()) {
            // Rate limit 초과 시 429 Too Many Requests 반환
            return ResponseEntity
                    .status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Rate limit exceeded");
        }
        return pjp.proceed();
    }
}
