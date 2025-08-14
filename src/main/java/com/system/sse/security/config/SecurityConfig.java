package com.system.sse.security.config;

import com.system.sse.security.filter.JwtAuthenticationFilter;
import com.system.sse.security.handler.JwtAccessDeniedHandler;
import com.system.sse.security.handler.JwtAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;


/*
 * TODO: 추가 커스터마이징 포인트들
 *
 * 1. AuthenticationManager 빈 등록
 *    - 사용자 정의 인증 로직이 필요한 경우
 *
 * 2. UserDetailsService 빈 등록
 *    - DB에서 사용자 정보를 조회하는 서비스
 *
 * 3. JwtAuthenticationProvider 구현
 *    - JWT 토큰 기반 사용자 정의 인증 프로세스
 *
 * 4. SecurityEventListener 설정
 *    - 로그인 성공/실패 등 보안 이벤트 로깅
 *
 * 5. Remember-Me 설정
 *    - 자동 로그인 기능이 필요한 경우
 *
 * 6. 다중 인증 설정 (Multi-Factor Authentication)
 *    - SMS, Email OTP 등 2차 인증
 *
 * 7. Rate Limiting 설정
 *    - API 호출 빈도 제한
 *
 * 8. Method Level Security 세부 설정
 *    - @PreAuthorize, @PostAuthorize 표현식 커스터마이징
 */
@Slf4j
@Configuration
@EnableWebSecurity
//@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * 정적 자원에 대해서는 Security 설정을 적용하지 않음
     * 성능 향상을 위해 필터 체인을 거치지 않도록 설정
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()) // 정적 리소스
                .requestMatchers("/favicon.ico", "/error"); // 추가 제외 경로
    }

    /**
     * 메인 보안 설정
     * JWT를 사용하므로 세션을 사용하지 않고, CSRF도 비활성화
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                // CSRF 보호 비활성화 (API 서버에서 주로 비활성화)
                .csrf(AbstractHttpConfigurer::disable)

                // 세션 사용하지 않고 JWT 기반 상태 없는(stateless) 인증 설정
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
//                        .dispatcherTypeMatchers(
//                                DispatcherType.FORWARD,
//                                DispatcherType.ERROR,
//                                DispatcherType.ASYNC
//                        ).permitAll()

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

    /**
     * CORS 설정
     * 프론트엔드 도메인과의 통신을 위한 설정
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin 설정 (개발: localhost, 운영: 실제 도메인)
        configuration.setAllowedOriginPatterns(Arrays.asList("*")); // 개발용
        // configuration.setAllowedOrigins(Arrays.asList("https://yourdomain.com")); // 운영용

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        // 노출할 헤더 (프론트엔드에서 접근 가능한 헤더)
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Authorization-refresh"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder 빈 등록
     * BCrypt 알고리즘 사용 (강력한 해시 함수)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // strength: 12 (기본 10보다 강화)
    }
}