package com.system.sse.security.provider;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
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

/**
 * JWT 토큰 생성, 검증, 추출을 담당하는 Provider
 * JJWT 0.12.x 버전 사용 (최신 버전)
 *
 * 1. 토큰 블랙리스트 처리
 *    - Redis를 이용한 로그아웃된 토큰 관리
 *
 * 2. 토큰 갱신 로직
 *    - Refresh Token을 이용한 Access Token 재발급
 *
 * 3. 다양한 클레임 지원
 *    - 사용자 ID, 역할, 추가 메타데이터 등
 *
 * 4. 토큰 암호화
 *    - JWE(JSON Web Encryption) 지원
 */
@Slf4j
@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenValidityInMs;
    private final long refreshTokenValidityInMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-in-ms:3600000}") long accessTokenValidityInMs, // 2시간
            @Value("${jwt.refresh-token-validity-in-ms:604800000}") long refreshTokenValidityInMs // 4일
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMs = accessTokenValidityInMs;
        this.refreshTokenValidityInMs = refreshTokenValidityInMs;
    }

    /**
     * 사용자 인증 정보를 바탕으로 Access Token 생성
     */
    public String createAccessToken(Authentication authentication) {
        String username = authentication.getName(); // accountId:uuid 조합으로 사용할 예정
        String credentials = authentication.getCredentials().toString();

        String jti       = UUID.randomUUID().toString();


        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Instant now = Instant.now();
        Instant validity = now.plusMillis(accessTokenValidityInMs);

        return Jwts.builder()
                .id(jti)
                .issuer("gateway")
                .subject(username)
                .claim(JwtClaimNames.SESSION_ID.getName(), credentials)
                .claim(JwtClaimNames.AUTH.getName(), authorities)
                .claim(JwtClaimNames.TYPE.getName(), "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(validity))
                .signWith(secretKey) // JJWT 0.12.x 방식
                .compact();
    }

    /**
     * Authentication 객체에서 accountId:uuid and sessionId 정보를 얻어
     * Refresh Token을 생성합니다.
     */
//    public String createRefreshToken(Authentication authentication) {
//        String[] parts = authentication.getName().split(":");
//        String accountId = parts[0];
//        String uuid      = parts.length > 1 ? parts[1] : "";
//        String sessionId = authentication.getCredentials().toString();
//
//        Instant now    = Instant.now();
//        Instant expiry = now.plusMillis(refreshTokenValidityInMs);
//        String jti     = UUID.randomUUID().toString();
//
//        return Jwts.builder()
//                .setId(jti)
//                .setSubject(accountId)
//                .setIssuer("gateway")
//                .claim("uuid", uuid)
//                .claim("sessionId", sessionId)
//                .claim("type", "refresh")
//                .setIssuedAt(Date.from(now))
//                .setExpiration(Date.from(expiry))
//                .signWith(secretKey)
//                .compact();
//    }

    /**
     * 사용자명을 바탕으로 Refresh Token 생성
     */
    public String createRefreshToken(String username) {
        Instant now = Instant.now();
        Instant validity = now.plusMillis(refreshTokenValidityInMs);


        return Jwts.builder()
                .subject(username)
                .claim("type", "refresh") // 토큰 타입
                .issuedAt(Date.from(now))
                .expiration(Date.from(validity))
                .signWith(secretKey)
                .compact();
    }

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
            return claims.get("auth", String.class);
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
                .verifyWith(secretKey)
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

    public long getAccessTokenValidityInSeconds() {
        return accessTokenValidityInMs / 1000;
    }

    /**
     * Refresh 토큰 유효 기간을 초 단위로 반환합니다.
     */
    public long getRefreshTokenValidityInSeconds() {
        return refreshTokenValidityInMs / 1000;
    }

}
