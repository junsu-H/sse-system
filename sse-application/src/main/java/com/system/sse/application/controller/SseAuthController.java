package com.system.sse.application.controller;

import com.system.sse.application.auth.config.JwtProperties;
import com.system.sse.application.auth.entity.AuthRequest;
import com.system.sse.application.auth.entity.AuthResponse;
import com.system.sse.application.auth.provider.TokenAuthenticationService;
import com.system.sse.application.cookie.CookieConfig;
import com.system.sse.application.cookie.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class SseAuthController {
    private final JwtProperties jwtProperties;
    private final TokenAuthenticationService tokenAuthenticationService;

    @PostMapping
    public ResponseEntity<AuthResponse> auth(@RequestBody AuthRequest request,
                                             HttpServletResponse response) {
        try {
            AuthResponse authResponse = tokenAuthenticationService.authenticate(request);

            // 쿠키 설정
            CookieUtils.addCookie(response,
                    new CookieConfig(
                            "access_token",
                            authResponse.accessToken(),
                            (int) (jwtProperties.getAccessTokenValidityInMs() / 1000),
                            "Strict",
                            false,
                            false,
                            "/"
                    )
            );

            CookieUtils.addCookie(response,
                    new CookieConfig(
                            "refresh_token",
                            authResponse.refreshToken(),
                            (int) (jwtProperties.getRefreshTokenValidityInMs() / 1000),
                            "Strict",
                            true,
                            true,
                            "/"
                    )
            );

            return ResponseEntity.ok(authResponse);
        } catch (Exception e) {
            log.error("인증 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * 토큰 갱신 엔드포인트
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                                HttpServletResponse response) {
        try {
            // 쿠키에서 refresh_token 추출\
            String refreshToken = CookieUtils.getCookieValue(
                    request,
                    new CookieConfig("refresh_token", "", 0, "Strict", true, true, "/")
            ).orElse(null);
            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // 토큰 갱신
            AuthResponse authResponse = tokenAuthenticationService.refreshAccessToken(refreshToken);

            // 새 쿠키 설정
            CookieUtils.addCookie(response,
                    new CookieConfig(
                            "access_token",
                            authResponse.accessToken(),
                            (int) (jwtProperties.getAccessTokenValidityInMs() / 1000),
                            "Strict",
                            true,
                            true,
                            "/"
                    )
            );

            CookieUtils.addCookie(response,
                    new CookieConfig(
                            "refresh_token",
                            authResponse.refreshToken(),
                            (int) (jwtProperties.getRefreshTokenValidityInMs() / 1000),
                            "Strict",
                            true,
                            true,
                            "/"
                    )
            );
            return ResponseEntity.ok(authResponse);

        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}