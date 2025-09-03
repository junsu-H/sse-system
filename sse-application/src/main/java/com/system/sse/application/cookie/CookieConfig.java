package com.system.sse.application.cookie;

public record CookieConfig(
        String name,
        String value,
        int maxAge,
        String sameSite,
        boolean httpOnly,
        boolean secure,
        String path
) {
}
