package com.system.sse.security;

import com.system.sse.cache.RefreshTokenService;
import com.system.sse.security.entity.AuthRequest;
import com.system.sse.security.entity.AuthResponse;
import com.system.sse.security.provider.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    /**
     * 로그인 엔드포인트
     * UserInfo를 받아 AccessToken, RefreshToken 생성 후 반환
     */
    @PostMapping("/auth")
    public ResponseEntity<AuthResponse> auth(@RequestBody AuthRequest req,
                                             HttpServletResponse res) {
        String sessionId = req.sessionId();
        // UserInfo 객체를 Authentication 객체로 사용 (사용자 코드 패턴에 따름)
        Collection<? extends GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        Authentication auth = new UsernamePasswordAuthenticationToken(req.getUsername(), req.getCredential(), authorities);

        String accessToken = jwtTokenProvider.createAccessToken(auth);
        String refreshToken = jwtTokenProvider.createRefreshToken(auth.getName()); // Refresh Token 생성

        refreshTokenService.store(sessionId, refreshToken);

        addCookie(res, "access_token", accessToken, 3600, "Strict");
        addCookie(res, "refresh_token", refreshToken, 7 * 24 * 3600, "Strict");

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 1. 쿠키에서 refresh_token 꺼내기
        String refreshToken = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[0]))
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);

        // 2. 세션ID 및 캐시된 토큰 조회
        String sessionId   = authentication.getCredentials().toString();
        String cachedToken = refreshTokenService.get(sessionId);

        // 3. 유효성 검증
        if (refreshToken == null
                || cachedToken == null
                || !cachedToken.equals(refreshToken)
                || !jwtTokenProvider.validateToken(refreshToken)
                || !"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))
        ) {
            refreshTokenService.delete(sessionId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 4. 새 토큰 발급
        String principal = authentication.getName(); // "accountId:uuid"
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        UsernamePasswordAuthenticationToken newAuth =
                new UsernamePasswordAuthenticationToken(principal, sessionId, authorities);

        String newAccess  = jwtTokenProvider.createAccessToken(newAuth);
        String newRefresh = jwtTokenProvider.createRefreshToken(newAuth.getName());

        // 5. 캐시 갱신 및 쿠키 재설정
        refreshTokenService.store(sessionId, newRefresh);
        addCookie(response, "access_token",  newAccess,  (int)jwtTokenProvider.getAccessTokenValidityInSeconds(),  "Strict");
        addCookie(response, "refresh_token", newRefresh, (int)jwtTokenProvider.getRefreshTokenValidityInSeconds(), "Strict");

        return ResponseEntity.ok(new AuthResponse(newAccess, newRefresh));
    }




    @GetMapping("/check")
    public String check(@AuthenticationPrincipal String username) {
        return username;
    }

    public void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            int maxAgeSec,
            String sameSite
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(maxAgeSec)
                .sameSite(sameSite)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
