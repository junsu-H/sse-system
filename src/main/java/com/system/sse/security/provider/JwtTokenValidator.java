package com.system.sse.security.provider;

import com.system.sse.security.config.JwtProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenValidator {
    private final JwtProperties jwtProperties;

        /**
         * JWT 토큰에서 사용자명 추출
         */
        public String getUsername(String token) {
            try {
                Claims claims = getClaimsFromToken(token);
                return claims.getSubject();
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("토큰에서 사용자명 추출 실패: {}", e.getMessage());
                return null;
            }
        }

        /**
         * JWT 토큰에서 권한 정보 추출
         */
        public String getAuthorities(String token) {
            try {
                Claims claims = getClaimsFromToken(token);
                return claims.get(JwtClaimNames.ROLES.getName(), String.class);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("토큰에서 권한 정보 추출 실패: {}", e.getMessage());
                return null;
            }
        }

        /**
         * JWT 토큰 유효성 검증
         */
        public boolean validateToken(String token) {
            try {
                getClaimsFromToken(token);
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
         * JWT 토큰에서 Claims 정보 추출
         */
        private Claims getClaimsFromToken(String token) {
            return Jwts.parser() // JJWT 0.12.x에서는 parserBuilder() 대신 parser() 사용
                    .verifyWith(Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        }

        /**
         * 토큰 만료 시간 확인
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
         * 토큰 타입 확인 (access/refresh)
         */
        public String getTokenType(String token) {
            try {
                Claims claims = getClaimsFromToken(token);
                return claims.get("type", String.class);
            } catch (JwtException | IllegalArgumentException e) {
                log.warn("토큰 타입 확인 실패: {}", e.getMessage());
                return null;
            }
        }

        public String getClaim(String token, String key) {
            try {
                return getClaimsFromToken(token).get(key, String.class);
            } catch (Exception e) {
                log.warn("'{}' 클레임 추출 실패: {}", key, e.getMessage());
                return null;
            }
        }


}
