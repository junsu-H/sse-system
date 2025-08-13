package com.system.sse.auth.controller;


import com.system.sse.auth.entity.AuthRequest;
import com.system.sse.auth.entity.AuthResponse;
import com.system.sse.auth.entity.RefreshRequest;
import com.system.sse.auth.entity.UserInfo;
import com.system.sse.auth.jwt.TokenProvider;
import com.system.sse.cache.RefreshTokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class AuthController {
    private final TokenProvider tokenProvider;

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/auth")
    public ResponseEntity<AuthResponse> auth(@RequestBody AuthRequest req,
                                             HttpServletResponse res) {
        String sessionId = req.sessionId();
        String accessToken = tokenProvider.createAccessToken(new UserInfo(req.accountId(), sessionId, req.uuid()));

        String refreshToken = UUID.randomUUID().toString();
        refreshTokenService.store(sessionId, refreshToken);

        addCookie(res, "access_token", accessToken);
        addCookie(res, "refresh_token", refreshToken);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest req,
                                                HttpServletResponse res) {
        String sessionId = req.sessionId();
        String cached = refreshTokenService.get(sessionId);
        if (cached == null || !cached.equals(req.refreshToken())) {
            return ResponseEntity.status(401).build();
        }

        String newAccess = tokenProvider.createAccessToken(new UserInfo(req.accountId(), sessionId, req.uuid()));
        String newRefresh = UUID.randomUUID().toString();
        refreshTokenService.store(sessionId, newRefresh);

        addCookie(res, "access_token", newAccess);
        addCookie(res, "refresh_token", newRefresh);

        return ResponseEntity.ok(new AuthResponse(newAccess, newRefresh));
    }

    private void addCookie(HttpServletResponse res, String name, String val) {
        Cookie c = new Cookie(name, val);
        c.setHttpOnly(true);
        c.setSecure(true);
        c.setPath("/");
        res.addCookie(c);
    }
}