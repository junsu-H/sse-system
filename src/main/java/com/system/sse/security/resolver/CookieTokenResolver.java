package com.system.sse.security.resolver;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("cookieResolver")
public class CookieTokenResolver implements TokenResolver {
    private static final String ACCESS_TOKEN = "access_token";

    @Override
    public String resolve(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (!ACCESS_TOKEN.equalsIgnoreCase(cookie.getName())) {
                continue;
            }

            String value = cookie.getValue();
            if (StringUtils.hasText(value)) {
                return value;
            }
        }

        return null;
    }
}