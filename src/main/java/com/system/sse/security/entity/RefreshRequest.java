package com.system.sse.security.entity;

public record RefreshRequest(Long accountId, String sessionId, String uuid, String refreshToken) {}