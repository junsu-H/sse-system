package com.system.sse.application.auth.entity;

public record RefreshRequest(Long accountId, String sessionId, String uuid, String refreshToken) {}