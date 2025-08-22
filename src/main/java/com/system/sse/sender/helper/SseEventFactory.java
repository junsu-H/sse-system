package com.system.sse.sender.helper;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Duration;

@Component
public class SseEventFactory {
    /**
     * 새로운 SseEmitter 인스턴스를 생성합니다.
     *
     * @return 타임아웃이 설정된 SseEmitter
     */
    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(0L);
        // 기본 콜백 설정 (필요시 로깅 추가)
        emitter.onCompletion(() -> {/* optional cleanup */});
        emitter.onTimeout(emitter::complete);
        emitter.onError(emitter::completeWithError);
        return emitter;
    }

    /**
     * 데이터 전송용 SseEventBuilder를 생성합니다.
     *
     * @param eventName 이벤트 이름
     * @param data      전송할 페이로드
     * @return SseEventBuilder
     */
    public SseEmitter.SseEventBuilder createDataEvent(String eventName, Object data) {
        return SseEmitter.event()
                .name(eventName)
                .data(data);
    }

    /**
     * 연결 확인용 INIT 이벤트를 생성합니다.
     *
     * @return SseEventBuilder with name "INIT" and data "connected"
     */
    public SseEmitter.SseEventBuilder createInitEvent() {
        return SseEmitter.event()
                .name("INIT")
                .data("connected");
    }

    /**
     * 하트비트 전송용 빈 SseEventBuilder를 생성합니다.
     *
     * @return SseEventBuilder with comment-only ping
     */
    public SseEmitter.SseEventBuilder createHeartbeatEvent() {
        // 빈 코멘트로 ping: ":\n\n"
        return SseEmitter.event()
                .comment("ping");
    }
}