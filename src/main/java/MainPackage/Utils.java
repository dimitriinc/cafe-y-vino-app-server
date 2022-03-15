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
    public static final String ADMIN_REGISTRATION = "admin_registration";

    public static final String[] ADMINS =
            {"cV8T95TgQNqhH4-28pKx-p:APA91bEA3ymNuYYCdWcS0L_81op8udfTIkWUj7RxGZYbM4JrfVPliM5kq5Hn444ssXZsh7HCu0IJeYfzMV1v0vvJs9drtuFU_8GEyPaIDQHBJyUoCvOnWd3V3EUiv-gQvESCREvnhi4e",
            "fz4GIRrWSfSV_KEmwxRGAD:APA91bEyiRn_bfpf1PUJUk1_Bo2RNNIDxcvUfUZGRJoPc2grsXMbYBaNq9miSU8obKPN4c2GkqBNloSLCPZLXWGCaKV6Ft_Z3KCodu7znNpiUQRY5H0iQOw1Rn11GUd4ppRQITfYbpxJ",
            "e4WeHLJeRCeRGtoSS2-sH9:APA91bHkHB1q3MgPalHlrGxLTUEhyASTI8exlnlN_hE2eXEZmf6JfpnGUL-iuU6yhke4CN1p8zLsJbRJR_3VXOKl8Grjz3vmeYXj9tisSwMml-itBd7XrH-PNYHnboke3tZwrQ_SP0np"};

    public static String getUniqueMessageId() {
        return "m-" + new Random().nextLong();
    }
}
