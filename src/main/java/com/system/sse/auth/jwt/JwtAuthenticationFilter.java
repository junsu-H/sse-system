package com.system.sse.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // JWT 토큰 생성/검증 담당 클래스
    private final TokenProvider tokenProvider;

    /**
     * 이 필터를 실행하지 않을 조건을 지정하는 메서드
     * 로그인 등 토큰 검증이 필요 없는 경로를 제외시키기 위해 사용
     *
     * @param request HTTP 요청 객체
     * @return true면 필터 실행 안 함, false면 실행함
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // "/sse/auth"로 시작하는 경로는 JWT 검증 필터를 실행하지 않음 (로그인 API 등)
        return path.startsWith("/sse/auth") ||
               path.startsWith("/sse/virtual");
    }

    /**
     * 실제 JWT 토큰 검증과 인증 객체 생성 작업을 수행하는 메서드
     * 요청 헤더에서 JWT 토큰을 추출하고 유효성을 검사한 후,
     * 인증 정보를 SecurityContext에 저장함
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param chain 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException 입출력 예외
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 요청 헤더에서 Authorization 값 가져오기
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더가 존재하고, "Bearer "로 시작하면 토큰을 추출
        if (header != null && header.startsWith(TokenMessage.BEARER)) {
            // "Bearer " 접두어 제거하고 실제 토큰 문자열만 추출
            String token = header.substring(7);

            try {
                // 토큰 유효성 검증 (서명 확인, 만료시간 확인 등)
                if (tokenProvider.validateToken(token)) {
                    // 토큰에서 사용자 이름(아이디) 추출
                    String username = tokenProvider.getUsername(token);

                    // UserDetailsService를 통해 DB 등에서 사용자 정보와 권한 로드
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(username, null, List.of());

                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // 인증 객체에 요청 정보를 추가 (IP, 세션 ID 등)
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContext에 인증 객체 저장 -> 이후 컨트롤러, 서비스 등에서 인증된 사용자로 인식 가능
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ex) {
                // JWT 파싱 및 검증 중 예외 발생 시 예외 정보를 request 속성에 저장
                // 나중에 인증 실패 처리 로직에서 예외 정보를 사용할 수 있게 함
                request.setAttribute(TokenMessage.EXCEPTION, ex);
            }
        }

        // 필터 체인 계속 진행 (다음 필터 또는 최종 목적지로 요청 전달)
        chain.doFilter(request, response);
    }
}
