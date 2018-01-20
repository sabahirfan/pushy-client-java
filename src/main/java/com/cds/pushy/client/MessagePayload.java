package com.cds.pushy.client;

public class MessagePayload {

    private final String type;
    private final String message;
    private final String timestamp;

    public MessagePayload(String message, String timestamp) {
        this.message = message;
        this.type = "MESSAGE";
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
