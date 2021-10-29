package MainPackage;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

public class EntryPoint extends CcsClient {

    private static final Logger logger = LoggerFactory.getLogger(EntryPoint.class);

    public EntryPoint(String projectId, String apiKey, boolean debuggable) {
        super(projectId, apiKey, debuggable);

        try {
            connect();
        } catch (XMPPException | InterruptedException | KeyManagementException | NoSuchAlgorithmException
                | SmackException | IOException e) {
            logger.error("Error trying to connect. Error: {}", e.getMessage());
        }

        try {
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        } catch (InterruptedException e) {
            logger.error("An error occurred while latch was waiting. Error: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        final String fcmProjectSenderId = "1096226926741";
        final String fcmServerKey = "AAAA_zw3cJU:APA91bGKWeyveEy2sdDcGVy3VyHMOeR_-8nVqxsdEKFbOlzBqDtaVJG7jajch-Qlt6tM2_gxKo9qNHUrKNFilWXejhJ98qh7EkBn7uRldbeNMJK8x4wSPpKCQpe6hFg5UFVC5U43wVrD";
        new EntryPoint(fcmProjectSenderId, fcmServerKey, false);
    }
}
