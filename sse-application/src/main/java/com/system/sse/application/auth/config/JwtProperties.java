package com.system.sse.application.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;

    /** Access Token 유효 기간 */
    private long accessTokenValidityInMs;

    /** Refresh Token 유효 기간 */
    private long refreshTokenValidityInMs;
}
