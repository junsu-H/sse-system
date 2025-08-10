package com.system.sse.auth.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class TokenAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (authException != null) {
            log.warn("TokenAuthenticationEntryPoint.commence: {}", authException.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        final Object e = request.getAttribute(TokenMessage.EXCEPTION);

        if (e instanceof ExpiredJwtException) {
            log.warn("TokenAuthenticationEntryPoint.commence: JWT Token is expired. ({})",
                    ((ExpiredJwtException) e).getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        } else if (e instanceof MalformedJwtException) {
            log.warn("TokenAuthenticationEntryPoint.commence: JWT Token is malformed. ({})",
                    ((MalformedJwtException) e).getMessage());
        } else if (e instanceof UnsupportedJwtException) {
            log.warn("TokenAuthenticationEntryPoint.commence: JWT Token is unsupported ({})",
                    ((UnsupportedJwtException) e).getMessage());
        } else if (e instanceof IllegalArgumentException) {
            log.warn("TokenAuthenticationEntryPoint.commence: JWT claims string is empty. ({})",
                    ((IllegalArgumentException) e).getMessage());
        } else if (e instanceof Exception){
            log.warn("TokenAuthenticationEntryPoint.commence: Unknown excpetion. ({})", ((Exception) e).getMessage());
        }

        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
}