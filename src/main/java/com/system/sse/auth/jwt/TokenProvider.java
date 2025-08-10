package com.system.sse.auth.jwt;

import com.system.sse.auth.entity.MsaAuth;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
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

    public String createToken(MsaAuth msaAuth) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(msaAuth.getSessionId())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validityMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
}