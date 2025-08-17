package com.system.sse.global;

public class ErrorResponse {
    private int status;
    private String message;
    private String path;
    private String method;

    public ErrorResponse(int status, String message, String path, String method) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.method = method;
    }
}
