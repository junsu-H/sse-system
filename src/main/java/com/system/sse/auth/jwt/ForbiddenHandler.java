package com.system.sse.auth.jwt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class ForbiddenHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (accessDeniedException != null) {
            log.warn("ForbiddenHandler.handle: {}", accessDeniedException.getMessage());

            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
}
