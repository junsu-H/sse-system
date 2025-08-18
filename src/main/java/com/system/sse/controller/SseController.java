package com.system.sse.controller;

import com.system.sse.cookie.CookieConfig;
import com.system.sse.cookie.CookieUtils;
import com.system.sse.service.SseEmitterService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    /**
     * SSE 구독 엔드포인트
     * JWT로 인증된 사용자만 접근 가능
     * @param authentication Spring Security의 Authentication 객체
     * @return SseEmitter 객체
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            HttpServletRequest request,
            Authentication authentication,
            @RequestHeader(value = "last-event-id", required = false) String lastEventIdHeader) throws IOException {

        String userId = authentication.getName();
        Long lastOffset = null;
        if (lastEventIdHeader != null) {
            try {
                lastOffset = Long.parseLong(lastEventIdHeader);
            } catch (NumberFormatException ex) {
                log.warn("유효하지 않은 Last-Event-ID: {}", lastEventIdHeader, ex);
            }
        }
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitter.send(SseEmitter.event().name("INIT").data("connected"));

        final Optional<String> cookieValue = CookieUtils.getCookieValue(request, new CookieConfig(
                "access_token",
                null,
                0,
                null,
                false,
                false,
                null
        ));

        final String s = cookieValue.get();

        return sseEmitterService.addEmitter(userId, emitter, s,lastOffset);
    }
}
