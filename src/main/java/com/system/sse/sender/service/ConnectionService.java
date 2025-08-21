package com.system.sse.sender.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 클라이언트와의 연결 관리
 */
public interface ConnectionService {
    SseEmitter connect(String clientId, String lastEventId) throws IOException;
}
