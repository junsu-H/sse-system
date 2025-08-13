package com.system.sse.auth.entity;

public record AuthRequest(Long accountId, String sessionId, String uuid) {}