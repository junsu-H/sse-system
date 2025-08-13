package com.system.sse.auth.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // JWT 토큰 생성/검증 담당 클래스
    private final TokenProvider tokenProvider;

    /**
     * doFilterInternal
     * - 실제 JWT 토큰을 검사하고 SecurityContext에 Authentication 객체를 저장
     * - 이전 코드: SecurityContextHolder.setAuthentication 호출이 중복되어 있었음
     * - 개선점: 인증 토큰 생성과 설정을 한 번만 수행하도록 가독성 및 성능 개선
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1) Authorization 헤더 추출
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // 2) "Bearer "로 시작하는지 확인
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);  // 접두어 제거

            try {
                // 3) 토큰 유효성 검증
                if (tokenProvider.validateToken(token)) {
                    // 4) 토큰에서 사용자 식별 정보(sessionId) 추출
                    String sessionId = tokenProvider.getSessionId(token);

                    // 5) Authentication 객체 생성 (권한 정보 없으므로 emptyList)
                    var authToken = new UsernamePasswordAuthenticationToken(
                            sessionId,
                            null,
                            Collections.emptyList()
                    );

                    // 6) 요청 세부정보 추가 (IP, 세션 ID 등)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // 7) SecurityContext에 인증 정보 저장
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }

            } catch (ExpiredJwtException e) {
                // 토큰 만료 예외 처리
                log.warn("JWT expired: {}", e.getMessage());
                request.setAttribute("EXCEPTION", e);

            } catch (Exception e) {
                // 기타 JWT 검증 실패 예외 처리
                log.error("JWT validation failed", e);
                request.setAttribute("EXCEPTION", e);
            }
        }

        // 8) 필터 체인 계속 진행
        chain.doFilter(request, response);
    }
}
