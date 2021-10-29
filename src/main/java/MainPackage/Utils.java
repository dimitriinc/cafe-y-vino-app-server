package MainPackage;

import java.util.Random;

public class Utils {
    public static final String FCM_SERVER = "fcm-xmpp.googleapis.com"; // prod
    public static final int FCM_PORT = 5235; // prod
    public static final String FCM_ELEMENT_NAME = "gcm";
    public static final String FCM_NAMESPACE = "google:mobile:data";
    public static final String FCM_SERVER_AUTH_CONNECTION = "gcm.googleapis.com";
    public static final String TOKEN = "token";
    public static final String TO_ADMIN = "toAdmin";
    public static final String TO_CLIENT = "toClient";

    public static String getUniqueMessageId() {
        return "m-" + new Random().nextLong();
    }
}
