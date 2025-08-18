package com.system.auth.provider;

import lombok.Builder;

@Builder
public record JwtTokenResult(String accessToken, String refreshToken) {
}
