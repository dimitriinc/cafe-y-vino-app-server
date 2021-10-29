package MainPackage;

import java.util.Map;

public class DownstreamMessage {
    private final String to;
    private final String messageId;
    private final Map<String, String> dataPayload;
    private Integer timeToLive;

    public DownstreamMessage(String to, String messageId, Map<String, String> dataPayload) {
        this.to = to;
        this.messageId = messageId;
        this.dataPayload = dataPayload;
    }

    public String getTo() {
        return to;
    }

    public String getMessageId() {
        return messageId;
    }

    public Map<String, String> getDataPayload() {
        return dataPayload;
    }

    public Integer getTimeToLive() {
        return timeToLive;
    }
}
