//package com.system;
//
//import org.springframework.boot.context.properties.EnableConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.reactive.CorsWebFilter;
//import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
//import org.springframework.web.servlet.function.RequestPredicates;
//import org.springframework.web.servlet.function.RouterFunction;
//import org.springframework.web.servlet.function.ServerResponse;
//
//import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.stripPrefix;
//import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
//import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
//import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
//
//@Configuration
//@EnableConfigurationProperties
//public class GatewayMvcConfig{
//
//    @Bean
//    public CorsWebFilter corsWebFilter() {
//        CorsConfiguration corsConfig = new CorsConfiguration();
//
//        // 정확한 Origin 설정
//        corsConfig.addAllowedOriginPattern("http://localhost:3001");
//        corsConfig.addAllowedOriginPattern("http://127.0.0.1:3001");
//        corsConfig.addAllowedOriginPattern("https://yourdomain.com");
//
//        // 메서드 설정
//        corsConfig.addAllowedMethod("GET");
//        corsConfig.addAllowedMethod("POST");
//        corsConfig.addAllowedMethod("PUT");
//        corsConfig.addAllowedMethod("DELETE");
//        corsConfig.addAllowedMethod("OPTIONS");
//
//        // 헤더 설정
//        corsConfig.addAllowedHeader("*");
//
//        // 중요: Credentials 허용
//        corsConfig.setAllowCredentials(true);
//
//        // Preflight 캐시 시간
//        corsConfig.setMaxAge(3600L);
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", corsConfig);
//
//        return new CorsWebFilter(source);
//    }
//
//    @Bean
//    public RouterFunction<ServerResponse> sseRoute() {
//        return route("auth-route")
//                .route(RequestPredicates.path("/sse/**"), http())
//                .before(uri("http://localhost:8090"))
//                .before(stripPrefix(1)) // /sse/** -> /**
//                .build();
//    }
//}