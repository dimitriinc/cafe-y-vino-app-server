package MainPackage;

import java.util.Map;

public class UpstreamMessage {

    private final String from;
    private final String category;
    private final String messageId;
    private final Map<String, String> dataPayload;

    public UpstreamMessage(String from, String category, String messageId, Map<String, String> dataPayload) {
        this.from = from;
        this.category = category;
        this.messageId = messageId;
        this.dataPayload = dataPayload;
    }

    public String getFrom() {
        return from;
    }

    public String getCategory() {
        return category;
    }

    public String getMessageId() {
        return messageId;
    }

    public Map<String, String> getDataPayload() {
        return dataPayload;
    }
}
