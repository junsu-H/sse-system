package com.system.sse.application.sender.service;

import java.util.Set;

public interface SubscriptionQueryService {
    /**
     * 전체 활성 구독자 ID 목록 조회
     */
    Set<String> listAllClients();

    /**
     * 특정 토픽·그룹별 활성 구독자 ID 목록 조회
     */
    Set<String> listClientsByTopic(String topic);
}