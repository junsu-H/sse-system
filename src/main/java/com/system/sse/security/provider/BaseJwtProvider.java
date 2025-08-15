package com.system.sse.security.provider;

import com.system.sse.security.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
public abstract class BaseJwtProvider {

    private final SecretKey secretKey;
    protected final long tokenValidityInMs;

    protected BaseJwtProvider(JwtProperties jwtProperties, long tokenValidityInMs) {
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

        String jti = UUID.randomUUID().toString();
        String username = authentication.getName();
        String credentials = authentication.getCredentials().toString();

        return Jwts.builder()
                .id(jti)
                .subject(username)
                .claim(JwtClaimNames.SESSION_ID.getName(), credentials)
                .issuer("sse-gateway")
                .claim(JwtClaimNames.TYPE.getName(), getTokenType())
                .issuedAt(Date.from(now))
                .expiration(Date.from(validity))
                .signWith(secretKey);
    }

    /**
     * 토큰에서 Claims 추출 (공통 메서드)
     */
    protected Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 사용자명 추출 (공통 메서드)
     */
    public String getUsername(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("{} 토큰에서 사용자명 추출 실패: {}", getTokenType(), e.getMessage());
            return null;
        }
    }

    /**
     * 토큰에서 특정 클레임 추출 (공통 메서드)
     */
    public String getClaim(String token, String claimName) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get(claimName, String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("{} 토큰에서 '{}' 클레임 추출 실패: {}", getTokenType(), claimName, e.getMessage());
            return null;
        }
    }

    /**
     * 기본 토큰 유효성 검증 (공통 메서드)
     * 토큰 파싱 가능성과 토큰 타입을 검증
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            String tokenType = claims.get("type", String.class);
            boolean isCorrectType = getTokenType().equals(tokenType);

            if (!isCorrectType) {
                log.warn("토큰 타입 불일치. 예상: {}, 실제: {}", getTokenType(), tokenType);
            }

            return isCorrectType;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 {} 토큰: {}", getTokenType(), e.getMessage());
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 {} 토큰 서명: {}", getTokenType(), e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 {} 토큰: {}", getTokenType(), e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("{} 토큰이 잘못되었습니다: {}", getTokenType(), e.getMessage());
        }
        return false;
    }

    /**
     * 토큰 만료 여부 확인 (공통 메서드)
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * 토큰 유효 기간을 초 단위로 반환 (공통 메서드)
     */
    public long getTokenValidityInSeconds() {
        return tokenValidityInMs / 1000;
    }
}
