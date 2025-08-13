package com.system.sse.auth.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // 인증 실패 시 처리할 핸들러 (예: 토큰 없거나 유효하지 않을 때)
    private final JwtAuthenticationEntryPoint entryPoint;

    // 권한 부족(403 Forbidden) 발생 시 처리할 핸들러
    private final JwtAccessDeniedHandler accessDeniedHandler;

    // JWT 토큰 검증 필터
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 스프링 시큐리티 필터 체인을 설정하는 메서드
     *
     * @param http HttpSecurity 객체
     * @return 구성된 SecurityFilterChain 빈
     * @throws Exception 예외 발생 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF 보호 비활성화 (API 서버에서 주로 비활성화)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용하지 않고 JWT 기반 상태 없는(stateless) 인증 설정
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 인증/인가 예외 발생 시 처리 핸들러 등록
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint)      // 인증 실패 처리
                        .accessDeniedHandler(accessDeniedHandler)  // 권한 부족 처리
                )

                // JWT 토큰 검증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }
}
