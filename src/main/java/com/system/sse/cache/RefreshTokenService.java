package com.system.sse.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    /**
     * 캐시에 리프레시 토큰 저장.
     * 기존 토큰이 있으면 덮어쓴다.
     */
    @CachePut(value = "refreshTokens", key = "#sessionId")
    public String store(String sessionId, String refreshToken) {
        return refreshToken;
    }

    /**
     * 캐시에서 리프레시 토큰 조회.
     * 없으면 null 반환.
     */
    @Cacheable(value = "refreshTokens", key = "#sessionId")
    public String get(String sessionId) {
        return null;  // 실제 로직는 프레임워크가 캐시에서 가져옴
    }

    /**
     * 캐시에서 리프레시 토큰 삭제.
     */
    @CacheEvict(value = "refreshTokens", key = "#sessionId")
    public void delete(String sessionId) {
    }
}