package com.system.sse.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.util.Map;

/*
 * 401 Unauthorized 응답을 커스터마이징
 * 인증되지 않은 사용자가 보호된 리소스에 접근했을 때 처리하는 핸들러
 *
 * 1. 다국어 지원
 *    - MessageSource를 이용한 에러 메시지 다국어 처리
 *
 * 2. 보안 강화
 *    - 과도한 인증 실패 시 IP 차단 기능
 *    - Rate Limiting 적용
 *
 * 3. 로깅 개선
 *    - 구조화된 로깅 (JSON 형태)
 *    - 보안 감사 로그 별도 관리
 *
 * 4. 알림 기능
 *    - 비정상 접근 시도에 대한 알림 발송
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Map<Class<? extends AuthenticationException>, String> ERROR_MESSAGES = Map.ofEntries(
            Map.entry(BadCredentialsException.class, "잘못된 인증 정보입니다."),
            Map.entry(InsufficientAuthenticationException.class, "인증이 필요합니다. 로그인 후 다시 시도해주세요."),
            Map.entry(AccountExpiredException.class, "계정이 만료되었습니다."),
            Map.entry(CredentialsExpiredException.class, "인증 정보가 만료되었습니다. 다시 로그인해주세요."),
            Map.entry(DisabledException.class, "비활성화된 계정입니다."),
            Map.entry(LockedException.class, "잠긴 계정입니다.")
    );

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) {
        String message = ERROR_MESSAGES.getOrDefault(
                authException.getClass(),
                "인증에 실패했습니다. 로그인이 필요합니다."
        );

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null && authentication.isAuthenticated())
                ? authentication.getName()
                : "anonymous";

        log.warn("JwtAuthenticationEntryPoint.commence: 인증 실패 username={} method={} requestURI={} message={}",
                username, method, requestURI, message, authException);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }
}