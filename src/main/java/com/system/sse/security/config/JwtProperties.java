package com.system.sse.security.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;

    /** Access Token 유효 기간 */
    private long accessTokenValidityInMs;

    /** Refresh Token 유효 기간 */
    private long refreshTokenValidityInMs;
}
