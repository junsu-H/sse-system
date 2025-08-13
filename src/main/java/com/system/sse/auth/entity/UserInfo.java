package com.system.sse.auth.entity;

import lombok.Data;

@Data
public class UserInfo {
    private Long accountId;
    private String sessionId;
    private String uuid;

    public UserInfo(String sessionId) {
        this.sessionId = sessionId;
    }
}
