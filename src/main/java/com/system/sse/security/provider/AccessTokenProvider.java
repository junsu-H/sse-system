package com.system.sse.security.provider;

import com.system.sse.security.config.JwtProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccessTokenProvider extends BaseJwtProvider {
    public AccessTokenProvider(JwtProperties jwtProperties) {
        super(jwtProperties, jwtProperties.getAccessTokenValidityInMs());
    }

    @Override
    protected String getTokenType() {
        return "access";
    }

    /**
     * Access Token 생성 (Access Token 특화 메서드)
     */
    public String createAccessToken(Authentication authentication) {
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return createBaseTokenBuilder(authentication)
                .claim(JwtClaimNames.ROLES.getName(), roles)
                .compact();
    }
}
