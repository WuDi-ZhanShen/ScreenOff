package com.tile.screenoff;

import java.util.Map;

public class HttpRequest {
    private final StartLine startLine;
    private final Map<String, String> headers;

    public HttpRequest(StartLine startLine, Map<String, String> headers) {
        this.startLine = startLine;
        this.headers = headers;
    }

    public String getRequestTarget() {
        return startLine.requestTarget;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public static class StartLine {
        private final String requestTarget;

        public StartLine(String requestTarget) {
            this.requestTarget = requestTarget;
        }
    }
}
