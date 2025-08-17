package com.system.sse.security.provider;

import com.system.sse.security.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * JWT 토큰 공통 기능을 제공하는 추상 베이스 클래스
 * Access/Refresh Provider들의 중복 코드를 제거하기 위한 공통 클래스
 */
@Slf4j
public abstract class BaseTokenProvider {
    private static final String ISSUER = "sse-gateway";

    private final SecretKey secretKey;
    protected final long tokenValidityInMs;

    protected BaseTokenProvider(JwtProperties jwtProperties, long tokenValidityInMs) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
        this.tokenValidityInMs = tokenValidityInMs;
    }

    /**
     * 토큰 타입을 반환하는 추상 메서드
     * 각 Provider에서 구현해야 함 (access/refresh)
     */
    protected abstract String getTokenType();

    /**
     * 공통 토큰 빌더 생성
     * 기본적인 토큰 구조를 설정하고 하위 클래스에서 추가 클레임을 설정할 수 있도록 함
     */
    protected JwtBuilder createBaseTokenBuilder(Authentication authentication) {
        Instant now = Instant.now();
        Instant validity = now.plusMillis(tokenValidityInMs);

        String username = authentication.getName();
        String sessionId = authentication.getCredentials().toString();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .subject(username)
                .claim(JwtClaimNames.SESSION_ID.getName(), sessionId)
                .claim(JwtClaimNames.TYPE.getName(), getTokenType())
                .issuedAt(Date.from(now))
                .expiration(Date.from(validity))
                .signWith(secretKey);
    }
}
