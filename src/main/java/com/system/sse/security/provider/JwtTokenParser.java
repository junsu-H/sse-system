package com.system.sse.security.provider;

import com.system.sse.security.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenParser {
    private final JwtProperties jwtProperties;

    /**
     * JWT 토큰에서 Claims 정보 추출
     */
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * JWT 토큰에서 사용자명 추출
     */
    public String getUsername(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JwtTokenParser.getUsername: 토큰에서 사용자명 추출 실패. message={}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰에서 권한 정보 추출
     */
    public String getCredential(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get(JwtClaimNames.SESSION_ID.getName(), String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("getCredential 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰에서 권한 정보 추출
     */
    public String getAuthorities(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get(JwtClaimNames.ROLES.getName(), String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("토큰에서 권한 정보 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * JWT 토큰 유효성 검증
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT 토큰이 잘못되었습니다: {}", e.getMessage());
        }

        return false;
    }

    /**
     * 토큰 만료 시간 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * 토큰 타입 확인 (access/refresh)
     */
    public String getTokenType(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get(JwtClaimNames.TYPE.getName(), String.class);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("토큰 타입 확인 실패: {}", e.getMessage());
            return null;
        }
    }
}
