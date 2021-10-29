package MainPackage;

/**
 * Represent a message for the sync and pending lists
 */
public class FcmMessage {

    private final Long timestamp;
    private final String jsonRequest;

    public static FcmMessage from(String jsonRequest) {
        return new FcmMessage(System.currentTimeMillis(), jsonRequest);
    }

    public FcmMessage(long timestamp, String jsonRequest) {
        this.timestamp = timestamp;
        this.jsonRequest = jsonRequest;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getJsonRequest() {
        return jsonRequest;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (jsonRequest == null ? 0 : jsonRequest.hashCode());
        result = prime * result + (timestamp == null ? 0 : timestamp.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FcmMessage other = (FcmMessage) obj;
        if (jsonRequest == null) {
            if (other.jsonRequest != null) {
                return false;
            }
        } else if (!jsonRequest.equals(other.jsonRequest)) {
            return false;
        }
        if (timestamp == null) {
            return other.timestamp == null;
        } else return timestamp.equals(other.timestamp);
    }
}
