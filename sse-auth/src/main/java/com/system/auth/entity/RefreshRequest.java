package com.system.auth.entity;

public record RefreshRequest(Long accountId, String sessionId, String uuid, String refreshToken) {}