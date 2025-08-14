package com.system.sse.security.entity;

public record AuthRequest(Long accountId, String sessionId, String uuid) {

    public String getUsername() {
        return accountId + ":" + uuid;
    }

    public String getCredential() {
        return sessionId;
    }
}