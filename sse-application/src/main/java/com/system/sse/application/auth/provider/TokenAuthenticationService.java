package com.system.sse.application.auth.provider;

import com.system.sse.application.auth.entity.AuthRequest;
import com.system.sse.application.auth.entity.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenAuthenticationService {
    private final TokenProviderFacade tokenProviderFacade;
//    private final RefreshTokenCacheService refreshTokenCacheService;
    private final JwtTokenParser jwtTokenParser;

    /**
     * 로그인 처리 및 토큰 발급
     */
    public AuthResponse authenticate(AuthRequest request) {
        String sessionId = request.getCredential();

        Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        final JwtTokenResult token = tokenProviderFacade.createToken(request.getUsername(), sessionId, authorities);

        // Refresh Token 저장
//        refreshTokenCacheService.store(request.getUsername(), token.refreshToken());

        return new AuthResponse(token.accessToken(), token.refreshToken());
    }

    /**
     * Access Token 갱신
     */
    public AuthResponse refreshAccessToken(String refreshToken) {
        // Refresh Token 유효성 검사
        if (!jwtTokenParser.isValid(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token입니다.");
        }

        // 사용자명과 세션 아이디 추출
        String username = jwtTokenParser.getUsername(refreshToken);
        String sessionId = jwtTokenParser.getCredential(refreshToken);

        // 저장된 리프레시 토큰과 일치 여부 확인
//        String cachedRefreshToken = refreshTokenCacheService.get(username);
//        if (!refreshToken.equals(cachedRefreshToken)) {
//            throw new IllegalArgumentException("저장된 Refresh Token과 일치하지 않습니다.");
//        }

        Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        // 새 토큰 생성
        JwtTokenResult token = tokenProviderFacade.createToken(username, sessionId, authorities);

        // Access 및 Refresh 토큰을 담아 반환
        return new AuthResponse(token.accessToken(), token.refreshToken());
    }
}
