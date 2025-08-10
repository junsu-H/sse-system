package com.system.sse.pubsub.entity;

import lombok.Data;

@Data
public class SseEvent {
    private String eventId;
    private String eventType;
    private String  channel;
    private String  accountId;
    private String  sessionId;
    private Object data;
    private Long timestamp;
    private Integer retryCount;
    private String sourceService;
    public String getLastEventId(){return timestamp+"_"+eventId;}
}
