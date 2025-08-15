package com.system.sse.security;

import com.system.sse.security.config.JwtProperties;
import com.system.sse.security.entity.AuthRequest;
import com.system.sse.security.entity.AuthResponse;
import com.system.sse.security.provider.ProviderAuthenticationService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtProperties jwtProperties;
    private final ProviderAuthenticationService providerAuthenticationService;

    /**
     * 로그인 엔드포인트
     */
    @PostMapping()
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request,
                                              HttpServletResponse response) {
        try {
            AuthResponse authResponse = providerAuthenticationService.authenticate(request);

            // 쿠키 설정
            addCookie(response, "access_token", authResponse.accessToken(),
                    (int) (jwtProperties.getAccessTokenValidityInMs() / 1000), "Strict");
            addCookie(response, "refresh_token", authResponse.refreshToken(),
                    (int) (jwtProperties.getRefreshTokenValidityInMs() / 1000), "Strict");

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 토큰 갱신 엔드포인트
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(Authentication authentication,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        try {
            // 쿠키에서 refresh_token 추출
            String refreshToken = extractRefreshTokenFromCookie(request);
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 세션 ID 추출
            String sessionId = authentication.getCredentials().toString();

            // 토큰 갱신
            AuthResponse authResponse = providerAuthenticationService.refreshAccessToken(refreshToken, sessionId);

            // 새 쿠키 설정
            addCookie(response, "access_token", authResponse.accessToken(),
                    (int) (jwtProperties.getAccessTokenValidityInMs() / 1000), "Strict");
            addCookie(response, "refresh_token", authResponse.refreshToken(),
                    (int) (jwtProperties.getRefreshTokenValidityInMs() / 1000), "Strict");

            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 쿠키에서 Refresh Token 추출
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        return Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 쿠키 추가 헬퍼 메서드
     */
    private void addCookie(HttpServletResponse response, String name, String value,
                           int maxAge, String sameSite) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // HTTPS 환경에서만 전송
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        // SameSite 설정은 Spring Boot 버전에 따라 다르게 처리
        response.addCookie(cookie);
    }
}