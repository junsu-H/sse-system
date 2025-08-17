package com.system.sse.security.provider;

import com.system.sse.cache.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
public class TokenProviderFacade {
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenService refreshTokenService;

    public JwtTokenResult createToken(String username, String credential, Collection<? extends GrantedAuthority> authorities) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                username,
                credential,
                authorities
        );

        // accessToken 생성
        String accessToken = accessTokenProvider.createAccessToken(auth);

        // refreshToken 생성
        String refreshToken = refreshTokenProvider.createRefreshToken(auth);

        // refreshToken 저장
        refreshTokenService.store(username, refreshToken);

        return JwtTokenResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
