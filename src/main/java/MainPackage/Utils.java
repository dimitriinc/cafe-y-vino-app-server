package MainPackage;

import java.util.Random;

public class Utils {
    public static final String FCM_SERVER = "fcm-xmpp.googleapis.com";
    public static final int FCM_PORT = 5235;
    public static final String FCM_ELEMENT_NAME = "gcm";
    public static final String FCM_NAMESPACE = "google:mobile:data";
    public static final String FCM_SERVER_AUTH_CONNECTION = "gcm.googleapis.com";
    public static final String TOKEN = "token";
    public static final String TO_ADMIN = "toAdmin";
    public static final String TO_CLIENT = "toClient";
    public static final String TO_ADMIN_NEW = "toAdminNew";
    public static final String ADMIN_TOKEN = "adminToken";

    public static final String[] ADMINS =
            {"cwt7J3v2TGaoUJtY2vlyO5:APA91bHeZL7tlCmILmUQC2LK_VmudHFMnnDYYMtesdafZPLe-6WLZBN7TRkUDNoD16f0OWHquZgsC4nzfOnmyONl7rjRxKwLjrYuuVfeqzw8pGCuieGg7WIMYUbdMr3EJ4pufU4wg1bE",
            "d8wL1YCHSlqPQ6rZtlkxCR:APA91bEHNNYB5adD6QJ0TgDZ0uFSaVkoPB60XR9cUrGd8GmY9QCgIQfs-af__neADCAIm_zjIco0WiLT7HnBz9fDA1XgOnNK9pcBA2yZdaCOkuBDXByscy6pWnH_C_u0suGOGx6PD7eF",
            "cfCFX4YNQhaGSr4qlVRHyB:APA91bFK-hD2GXxlCzVq_stkuIuMD9QoABMWd1QFM52NKaw9YstCepLaw5uIICvdxZg2KX7gyVtvWyQk33_TwBOAyjQA415L4vnM-yg_7E-EOsotQBdUbOYY2eTa4fzTcfgeV34e-tdU"};

    public static String getUniqueMessageId() {
        return "m-" + new Random().nextLong();
    }
}
