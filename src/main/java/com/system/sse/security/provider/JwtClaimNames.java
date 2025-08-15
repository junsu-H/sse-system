package com.system.sse.security.provider;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JwtClaimNames {
    ROLES("roles"),
    SESSION_ID("sessionId"),
    TYPE("type");

    private final String name;
}
