package com.system.sse.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfo {
    private Long accountId;
    private String sessionId;
    private String uuid;
}
