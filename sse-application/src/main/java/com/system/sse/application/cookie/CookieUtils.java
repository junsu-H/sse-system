package com.system.sse.application.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.experimental.UtilityClass;
import org.springframework.http.ResponseCookie;

import java.util.Arrays;
import java.util.Optional;

@UtilityClass
public class CookieUtils {

    public static void addCookie(HttpServletResponse response, CookieConfig cookieConfig) {
        // ResponseCookie를 사용하여 SameSite 속성 지원
        ResponseCookie cookie = ResponseCookie.from(cookieConfig.name(), cookieConfig.value())
                .httpOnly(cookieConfig.httpOnly())
                .secure(cookieConfig.secure()) // HTTPS 환경에서만 전송
                .path(cookieConfig.path())
                .maxAge(cookieConfig.maxAge())
                .sameSite(cookieConfig.sameSite())
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * 쿠키 삭제 (maxAge=0으로 설정)
     */
    public static void deleteCookie(HttpServletResponse response, CookieConfig cookieConfig) {
        // 삭제용 설정: value 빈 문자열, maxAge=0
        CookieConfig deleteConfig = new CookieConfig(
                cookieConfig.name(),
                "",
                0,
                cookieConfig.sameSite(),
                cookieConfig.httpOnly(),
                cookieConfig.secure(),
                cookieConfig.path()
        );

        addCookie(response, deleteConfig);
    }

    public static Optional<String> getCookieValue(HttpServletRequest request, CookieConfig config) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> config.name().equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
