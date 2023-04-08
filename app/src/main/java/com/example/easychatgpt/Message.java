package com.example.easychatgpt;

public class Message {
    public static String SENT_BY_ME = "user";
    public static String SENT_BY_BOT = "assistant";

    private String message;
    private String sentBy;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public Message(String message, String sentBy) {
        this.message = message;
        this.sentBy = sentBy;
    }
}
