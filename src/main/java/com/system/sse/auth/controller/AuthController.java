package com.system.sse.auth.controller;


import com.system.sse.auth.entity.AuthRequest;
import com.system.sse.auth.entity.AuthResponse;
import com.system.sse.auth.entity.MsaAuth;
import com.system.sse.auth.jwt.TokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final TokenProvider tokenProvider;

    @PostMapping
    public ResponseEntity<AuthResponse> auth(@RequestBody AuthRequest request, HttpServletResponse response) {
        final MsaAuth msaAuth = new MsaAuth(request.sessionId());
        String token = tokenProvider.createToken(msaAuth);

        // 쿠키로 토큰 내려주기
        Cookie cookie = new Cookie("access_token", token);
        cookie.setHttpOnly(true);          // JavaScript 접근 불가
        cookie.setSecure(true);            // HTTPS 연결에서만 전송
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(new AuthResponse(token));
    }
}