package com.system.sse.security.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JwtClaimNames {
    AUTH("auth"),
    ROLES("roles"),
    ACCOUNT_ID("accountId"),
    SESSION_ID("sessionId"),
    TYPE("type");

    private final String name;
}
