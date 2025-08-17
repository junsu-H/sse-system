package com.system.sse.emitter;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmitterData {

    // --- 이벤트 식별 정보 ---
    private String type;           // ex: "notification", "approval", "chat", "system"
    private String subType;        // ex: "order_complete", "user_request", "direct_message"
    private String resourceId;     // 연관 엔티티의 고유 ID

    // --- 발신/수신 정보 ---
    private String from;           // ex: 사용자 ID 또는 시스템
    private String to;             // ex: 사용자 ID, null이면 broadcast

    // --- 타임스탬프 ---
    private LocalDateTime timestamp;

    // --- 주요 컨텐츠 ---
    private String title;          // 알림/메시지 제목
    private String message;        // 본문
    private Map<String, Object> data;  // 추가 키·값 형태의 payload

    // --- 메타데이터 ---
    private String priority;       // ex: "low", "normal", "high"
    private Boolean persistent;    // 클라이언트 표시 유지 여부
}
