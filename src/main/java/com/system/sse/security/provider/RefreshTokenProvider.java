package com.system.sse.security.provider;

import com.system.sse.security.config.JwtProperties;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenProvider extends BaseTokenProvider {

    public RefreshTokenProvider(JwtProperties jwtProperties) {
        super(jwtProperties, jwtProperties.getRefreshTokenValidityInMs());
    }

    @Override
    protected String getTokenType() {
        return "refresh";
    }

    /**
     * Refresh Token 생성 (Refresh Token 특화 메서드)
     */
    public String createRefreshToken(Authentication authentication) {
        return createBaseTokenBuilder(authentication).compact();
    }
}
