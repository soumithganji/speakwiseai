package chat.gpt.speakwise.gpt3.ai.chatbot;

public class Message {
    public static String SENT_BY_ME = "user";
    public static String SENT_BY_BOT = "assistant";

    private String message;
    private String sentBy;

    private boolean showCopyOptions;

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

    public boolean showCopyOptions() {
        return showCopyOptions;
    }

    public void setShowCopyOptions(boolean showCopyOptions) {
        this.showCopyOptions = showCopyOptions;
    }

    public Message(String message, String sentBy, boolean showCopyOptions) {
        this.message = message;
        this.sentBy = sentBy;
        this.showCopyOptions = showCopyOptions;
    }
}
