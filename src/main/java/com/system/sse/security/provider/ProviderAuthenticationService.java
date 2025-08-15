package com.system.sse.security.provider;

import com.system.sse.cache.RefreshTokenService;
import com.system.sse.security.entity.AuthRequest;
import com.system.sse.security.entity.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderAuthenticationService {

    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * 로그인 처리 및 토큰 발급
     */
    public AuthResponse authenticate(AuthRequest request) {
        String sessionId = request.getCredential();

        // Authentication 객체 생성 (실제 인증 로직은 여기서 수행)
        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                sessionId,
                authorities
        );

        // 토큰 생성
        String accessToken = accessTokenProvider.createAccessToken(auth);
        String refreshToken = refreshTokenProvider.createRefreshToken(auth);

        // Refresh Token 저장
        refreshTokenService.store(sessionId, refreshToken);

        return new AuthResponse(accessToken, refreshToken);
    }

    /**
     * Access Token 갱신
     */
    public AuthResponse refreshAccessToken(String refreshToken, String sessionId) {
        // Refresh Token 유효성 검증
        if (!refreshTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 Refresh Token");
        }

        // 캐시된 토큰과 비교
        String cachedToken = refreshTokenService.get(sessionId);
        if (cachedToken == null || !cachedToken.equals(refreshToken)) {
            refreshTokenService.delete(sessionId);
            throw new IllegalArgumentException("Refresh Token이 일치하지 않음");
        }

        // 토큰에서 사용자 정보 추출
        String username = refreshTokenProvider.getUsername(refreshToken);
        if (username == null) {
            throw new IllegalArgumentException("토큰에서 사용자 정보를 추출할 수 없음");
        }

        // 새 토큰 생성
        Collection<? extends GrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                username,
                sessionId,
                authorities
        );

        String newAccessToken = accessTokenProvider.createAccessToken(newAuth);
        String newRefreshToken = refreshTokenProvider.createRefreshToken(newAuth);

        // 새 Refresh Token 저장
        refreshTokenService.store(sessionId, newRefreshToken);

        return new AuthResponse(newAccessToken, newRefreshToken);
    }
}
