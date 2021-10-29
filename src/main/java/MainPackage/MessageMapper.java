package MainPackage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MessageMapper {

    private static final Logger logger = LoggerFactory.getLogger(MessageMapper.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Creates a json string from a map object
     */
    public static String toJsonString(Map<String, Object> map) {
        try {
            return mapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.error("Error parsing JSON map: {}", map.values());
        }
        return null;
    }

    /**
     * Creates a JSON from an outgoing FCM message
     */
    public static String toJsonString(DownstreamMessage downMessage) {
        return toJsonString(mapFrom(downMessage));
    }

    /**
     * Creates a JSON from an incoming FCM message
     */
    public static String toJsonString(UpstreamMessage upMessage) {
        return toJsonString(mapFrom(upMessage));
    }


    /**
     * Creates a map from an outgoing FCM message
     */
    public static Map<String, Object> mapFrom(DownstreamMessage downMessage) {
        final Map<String, Object> map = new HashMap<>();
        if (downMessage.getTo() != null) {
            map.put("to", downMessage.getTo());
        }
        if (downMessage.getMessageId() != null) {
            map.put("message_id", downMessage.getMessageId());
        }
        if (downMessage.getDataPayload() != null) {
            map.put("data", downMessage.getDataPayload());
        }
        if (downMessage.getTimeToLive() != null) {
            map.put("time_to_live", downMessage.getTimeToLive());
        }
        return map;
    }

    /**
     * Creates a map from an incoming FCM message
     */
    public static Map<String, Object> mapFrom(UpstreamMessage upMessage) {
        final Map<String, Object> map = new HashMap<>();

        if (upMessage.getCategory() != null) {
            map.put("category", upMessage.getCategory());
        }
        if (upMessage.getMessageId() != null) {
            map.put("message_id", upMessage.getMessageId());
        }
        if (upMessage.getFrom() != null) {
            map.put("from", upMessage.getFrom());
        }
        if (upMessage.getDataPayload() != null) {
            map.put("data", upMessage.getDataPayload());
        }
        return map;
    }

    /**
     * Creates a POJO upstream message from a map
     */
    @SuppressWarnings("unchecked")
    public static UpstreamMessage upstreamMessageFrom(Map<String, Object> map) {
        String from = null;
        String category = null;
        String messageId = null;
        Map<String, String> data = null;

        if (map.get("from") != null) {
            from = map.get("from").toString();
        }
        if (map.get("category") != null) {
            category = map.get("category").toString();
        }
        if (map.get("message_id") != null) {
            messageId = map.get("message_id").toString();
        }
        if (map.get("data") != null) {
            data = (Map<String, String>) map.get("data");
        }

        final UpstreamMessage msg = new UpstreamMessage(from, category, messageId, data);
        return msg;
    }

    /**
     * Creates a map from a JSON
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> toMapFromJsonString(String json) {
        try {
            return mapper.readValue(json, HashMap.class);
        } catch (IOException e) {
            logger.error("Error parsing JSON string: {}", json);
        }
        return null;
    }

    /**
     * Creates a JSON ACK for an incoming FCM message
     */
    public static String createJsonAck(String to, String messageId) {
        final Map<String, Object> map = new HashMap<>();
        map.put("message_type", "ack");
        map.put("message_id", messageId);
        map.put("to", to);
        return toJsonString(map);
    }
}
