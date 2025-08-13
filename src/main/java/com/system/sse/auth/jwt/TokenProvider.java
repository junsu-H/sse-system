package com.system.sse.auth.jwt;

import com.system.sse.auth.entity.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;

import static javax.management.timer.Timer.ONE_HOUR;

@Component
public class TokenProvider {
    private final Key key;
    private final long validityMs;

    public TokenProvider(
            @Value("${jwt.secret}") String base64Key) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Key);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.validityMs = 2 * ONE_HOUR;
    }


    /**
     * Access Token 생성
     */
    public String createAccessToken(UserInfo userInfo) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(validityMs, java.time.temporal.ChronoUnit.MILLIS);

        return Jwts.builder()
                .setSubject(userInfo.getSessionId())
                .claim("accountId", userInfo.getAccountId())
                .claim("uuid", userInfo.getUuid())
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiresAt))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * JWT 토큰 유효성 검사
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 로깅 필요 시 추가
            return false;
        }
    }

    /**
     * 토큰으로부터 Subject(여기서는 sessionId) 추출
     */
    public String getSessionId(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰 내 모든 Claims 추출
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}