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
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    // JWT 토큰 검증 필터
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 스프링 시큐리티 필터 체인을 설정하는 메서드
     *
     * @param httpSecurity HttpSecurity 객체
     * @return 구성된 SecurityFilterChain 빈
     * @throws Exception 예외 발생 시
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                // CSRF 보호 비활성화 (API 서버에서 주로 비활성화)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용하지 않고 JWT 기반 상태 없는(stateless) 인증 설정
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/sse/**").permitAll() // gateway 통과
                        .requestMatchers("/auth").permitAll() // 엔드포인트
                        .requestMatchers("/refresh").permitAll() // 리프레시 토큰 엔드포인트
                        .anyRequest().authenticated() // 그 외에는 인증 필요
                )

                // 인증/인가 예외 발생 시 처리 핸들러 등록
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401
                        .accessDeniedHandler(jwtAccessDeniedHandler)  // 403
                )

                // JWT 토큰 검증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }
}
