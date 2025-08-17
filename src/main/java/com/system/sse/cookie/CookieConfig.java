package com.system.sse.cookie;

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
