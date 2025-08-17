package com.system.sse.security.provider;

import com.system.sse.cache.RefreshTokenService;
import com.system.sse.security.entity.AuthRequest;
import com.system.sse.security.entity.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenAuthenticationService {
    private final TokenProviderFacade tokenProviderFacade;
    private final RefreshTokenService refreshTokenService;
    private final JwtTokenParser jwtTokenParser;

    /**
     * 로그인 처리 및 토큰 발급
     */
    public AuthResponse authenticate(AuthRequest request,  Collection<? extends GrantedAuthority> authorities ) {
        String sessionId = request.getCredential();

        final JwtTokenResult token = tokenProviderFacade.createToken(request.getUsername(), sessionId, authorities);

        // Refresh Token 저장
        refreshTokenService.store(request.getUsername(), token.refreshToken());

        return new AuthResponse(token.accessToken(), token.refreshToken());
    }

    /**
     * Access Token 갱신
     */
    public AuthResponse refreshAccessToken(String refreshToken, Collection<? extends GrantedAuthority> authorities) {
        // Refresh Token 유효성 검사
        if (!jwtTokenParser.isValid(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 사용자명과 세션 아이디 추출
        String username = jwtTokenParser.getUsername(refreshToken);
        String sessionId = jwtTokenParser.getCredential(refreshToken);

        // 저장된 리프레시 토큰과 일치 여부 확인
        String cachedRefreshToken = refreshTokenService.get(username);
        if (!refreshToken.equals(cachedRefreshToken)) {
            throw new IllegalArgumentException("저장된 Refresh Token과 일치하지 않습니다.");
        }

        // 새 토큰 생성
        JwtTokenResult token = tokenProviderFacade.createToken(username, sessionId, authorities);

        // Access 및 Refresh 토큰을 담아 반환
        return new AuthResponse(token.accessToken(), token.refreshToken());
    }
}
