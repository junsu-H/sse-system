package com.system.auth.resolver;

import jakarta.servlet.http.HttpServletRequest;

public interface TokenResolver {
    /**
     * 요청에서 JWT 토큰을 추출합니다.
     * @param request HTTP 요청
     * @return 토큰 문자열 또는 null
     */
    String resolve(HttpServletRequest request);
}
