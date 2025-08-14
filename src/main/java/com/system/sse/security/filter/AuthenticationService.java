package com.system.sse.security.filter;

import com.system.sse.security.provider.JwtTokenProvider;
import com.system.sse.security.resolver.TokenResolverChain;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 인증 처리 로직을 담당하는 서비스 클래스.
 * 요청에서 토큰을 추출하고 검증한 뒤 SecurityContext에 Authentication을 설정합니다.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

    /**
     * TokenResolverChain: 여러 TokenResolver 구현체를 순차적으로 호출해
     * 유효한 토큰을 추출합니다.
     */
    private final TokenResolverChain tokenResolver;

    /**
     * JwtTokenProvider: JWT 생성, 검증, 클레임 추출 등의 기능을 제공합니다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 클라이언트 요청에서 JWT를 추출·검증하고, 인증 정보를 SecurityContext에 설정합니다.
     *
     * @param request HTTP 요청 객체
     */
    public void authenticate(HttpServletRequest request) {
        // 1. 요청에서 토큰을 추출
        String token = tokenResolver.resolve(request);

        // 2. 토큰이 유효하지 않으면 SecurityContext를 초기화하고 메서드 종료
        if (!isValidToken(token)) {
            clearContext();
            return;
        }

        // 3. 유효한 토큰이면 Authentication 객체를 생성해 SecurityContext에 저장
        Authentication auth = buildAuthentication(token, request);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    /**
     * 토큰의 유효성을 검증합니다.
     * - 토큰 문자열이 비어 있지 않고,
     * - JwtTokenProvider.validateToken()이 true를 반환해야 합니다.
     *
     * @param token 추출된 JWT 문자열
     * @return 유효하면 true, 그렇지 않으면 false
     */
    private boolean isValidToken(String token) {
        return StringUtils.hasText(token) && jwtTokenProvider.validateToken(token);
    }

    /**
     * 유효한 JWT 토큰을 기반으로 Authentication 객체를 생성합니다.
     * - UsernamePasswordAuthenticationToken을 사용해 사용자명, 토큰, 권한 정보를 설정
     * - WebAuthenticationDetailsSource를 통해 추가 요청 세부 정보를 포함
     *
     * @param token   검증된 JWT 문자열
     * @param request HTTP 요청 객체 (세부 정보 획득용)
     * @return Authentication 객체
     */
    private Authentication buildAuthentication(String token, HttpServletRequest request) {
        // 1. 토큰에서 사용자명(claim) 추출
        String username = jwtTokenProvider.getUsername(token);

        // 2. 토큰에서 권한(claim) 추출하여 SimpleGrantedAuthority 리스트 생성
        List<SimpleGrantedAuthority> authorities = extractAuthorities(token);

        // 3. Authentication 객체 생성 및 요청 세부 정보 설정
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(username, token, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        return authentication;
    }

    /**
     * JWT의 권한(claims) 문자열(콤마 구분)을 파싱해
     * SimpleGrantedAuthority 리스트로 변환합니다.
     *
     * @param token 검증된 JWT 문자열
     * @return 권한 정보 리스트
     */
    private List<SimpleGrantedAuthority> extractAuthorities(String token) {
        // 1. 콤마(,)로 구분된 권한 문자열 획득
        String auths = jwtTokenProvider.getAuthorities(token);

        // 2. 문자열을 분할한 뒤, 빈 값은 필터링하고 SimpleGrantedAuthority로 매핑
        return Arrays.stream(auths.split(","))
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    /**
     * SecurityContext에 저장된 인증 정보를 제거해
     * 인증 상태를 초기화합니다.
     */
    private void clearContext() {
        SecurityContextHolder.clearContext();
    }
}
