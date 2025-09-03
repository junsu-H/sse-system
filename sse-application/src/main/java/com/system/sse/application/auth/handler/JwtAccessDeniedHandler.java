package com.system.sse.application.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 403 처리
 * 이 핸들러는 인증된 사용자가 리소스에 접근할 권한이 없을 때 호출됩니다.
 *
 * 향후 개선 사항:
 *    1. 권한 체크 상세화
 *       - 메서드 레벨 보안 정보와 연동
 *       - @PreAuthorize, @Secured 등의 정보 활용
 *
 *    2. 사용자 친화적 가이드
 *       - 필요한 권한과 현재 권한의 차이점 명시
 *       - 권한 요청 방법 안내
 *
 *    3. 보안 감사
 *       - 권한 없는 접근 시도 패턴 분석
 *       - 의심스러운 활동 탐지 및 알림
 *
 *    4. 캐시 최적화
 *       - 자주 요청되는 권한 정보 캐싱
 *
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = (authentication != null) ? authentication.getName() : "anonymous";

        log.warn("JwtAccessDeniedHandler.handle: 권한 없는 접근 시도 username={}, method={} requestURI={}, message={}",
                username, method, requestURI, accessDeniedException.getMessage(), accessDeniedException);

        // 메시지 없이 상태 코드만 전송
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    }
}
