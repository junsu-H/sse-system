package com.system.sse.auth.entity;
public record RefreshRequest(Long accountId, String sessionId, String uuid, String refreshToken) {}
