package com.system.sse.application.auth.provider;

import lombok.Builder;

@Builder
public record JwtTokenResult(String accessToken, String refreshToken) {
}
