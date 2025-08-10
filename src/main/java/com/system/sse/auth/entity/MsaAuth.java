package com.system.sse.auth.entity;

import lombok.Data;

@Data
public class MsaAuth {
    private Long accountId;
    private String sessionId;

    public MsaAuth(String sessionId) {
        this.sessionId = sessionId;
    }
}
