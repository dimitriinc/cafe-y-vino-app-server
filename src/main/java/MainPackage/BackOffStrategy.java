package MainPackage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

public class BackOffStrategy {

    private static final Logger logger = LoggerFactory.getLogger(BackOffStrategy.class);

    private static final int DEFAULT_RETRIES = 3;
    private static final long DEFAULT_WAIT_TIME_IN_MILLI = 1000;

    private final int numberOfRetries;
    private int numberOfTriesLeft;
    private final long defaultTimeToWait;
    private long timeToWait;
    private final Random random = new Random();

    public BackOffStrategy() {
        this(DEFAULT_RETRIES, DEFAULT_WAIT_TIME_IN_MILLI);
    }

    public BackOffStrategy(int numberOfRetries, long defaultTimeToWait) {
        this.numberOfRetries = numberOfRetries;
        this.numberOfTriesLeft = numberOfRetries;
        this.defaultTimeToWait = defaultTimeToWait;
        this.timeToWait = defaultTimeToWait;
    }

    public boolean shouldRetry() {
        return numberOfTriesLeft > 0;
    }

    public void errorOcurred() {
        numberOfTriesLeft--;
        if (!shouldRetry()) {
            logger.info("Retry Failed: Total of attempts: {}. Total waited time: {} ms.", numberOfRetries, timeToWait);
        }
        waitUntilNextTry();
        timeToWait *= 2;
        timeToWait += random.nextInt(500);
    }

    public void errorOccured2() throws Exception {
        numberOfTriesLeft--;
        if (!shouldRetry()) {
            throw new Exception("Retry Failed: Total of attempts: " + numberOfRetries + ". Total waited time: "
                    + timeToWait + "ms.");
        }
        waitUntilNextTry();
        timeToWait *= 2;
        timeToWait += random.nextInt(500);
    }

    private void waitUntilNextTry() {
        try {
            Thread.sleep(timeToWait);
        } catch (InterruptedException e) {
            logger.info("Error waiting until next try for the backoff strategy. Error: {}", e.getMessage());
        }
    }

    public void doNotRetry() {numberOfTriesLeft = 0;}

    public void reset() {
        this.numberOfTriesLeft = numberOfRetries;
        this.timeToWait = defaultTimeToWait;
    }
}
