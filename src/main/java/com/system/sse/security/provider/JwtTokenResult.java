package com.system.sse.security.provider;

import lombok.Builder;

@Builder
public record JwtTokenResult(String accessToken, String refreshToken) {
}
