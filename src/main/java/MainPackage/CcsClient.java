package MainPackage;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.sm.predicates.ForEveryStanza;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlpull.v1.XmlPullParser;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CcsClient implements StanzaListener, ReconnectionListener, ConnectionListener, PingFailedListener {

    private static final Logger logger = LoggerFactory.getLogger(CcsClient.class);
    private XMPPTCPConnection connection;
    private final String apiKey;
    private final boolean debuggable;
    private final String userName;
    private boolean isConnectionDraining;

    // downstream messages to sync with acks and nacks
    private final Map<String, FcmMessage> syncMessages = new ConcurrentHashMap<>();

    // messages from backoff failures
    private final Map<String, FcmMessage> pendingMessages = new ConcurrentHashMap<>();

    public CcsClient(String projectId, String apiKey, boolean debuggable) {
        ProviderManager.addExtensionProvider(Utils.FCM_ELEMENT_NAME, Utils.FCM_NAMESPACE,
                new ExtensionElementProvider<FcmPacketExtension>() {
                    @Override
                    public FcmPacketExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
                        final String json = parser.nextText();
                        return new FcmPacketExtension(json);
                    }
                });
        this.apiKey = apiKey;
        this.debuggable = debuggable;
        this.userName = projectId + "@" + Utils.FCM_SERVER_AUTH_CONNECTION;

    }

    public void connect() throws XMPPException, SmackException, IOException, InterruptedException,
            NoSuchAlgorithmException, KeyManagementException {

        logger.info("Initiating connection ...");

        isConnectionDraining = false; // Set connection draining to false when there is a new connection


        // create connection configuration
        XMPPTCPConnection.setUseStreamManagementResumptionDefault(true);
        XMPPTCPConnection.setUseStreamManagementDefault(true);

        final SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, null, new SecureRandom());
        SmackConfiguration.DEBUG = debuggable;

        final XMPPTCPConnectionConfiguration.Builder config = XMPPTCPConnectionConfiguration.builder();
        logger.info("Connecting to the server ...");
        config.setXmppDomain("jabb3r.de");
        config.setHost(Utils.FCM_SERVER);
        config.setPort(Utils.FCM_PORT);
        config.setSendPresence(false);
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.ifpossible);
        config.setCompressionEnabled(true);
        config.setSocketFactory(sslContext.getSocketFactory());
        config.setCustomSSLContext(sslContext);

        connection = new XMPPTCPConnection(config.build());

        try {
            connection.connect();
        } catch (Exception e) {
            logger.info("Failed to connect:  {}", e.getLocalizedMessage());
        }

        // Enable automatic reconnection and add the listener
        ReconnectionManager.getInstanceFor(connection).enableAutomaticReconnection();
        ReconnectionManager.getInstanceFor(connection).addReconnectionListener(this);

        // Disable Roster at login
        Roster.getInstanceFor(connection).setRosterLoadedAtLogin(false);

        // Security checks
        SASLAuthentication.unBlacklistSASLMechanism("PLAIN"); // FCM CCS requires a SASL PLAIN authentication mechanism
        SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");
        logger.info("SASL PLAIN authentication enabled ? {}", SASLAuthentication.isSaslMechanismRegistered("PLAIN"));
        logger.info("Is compression enabled ? {}", connection.isUsingCompression());
        logger.info("Is the connection secure ? {}", connection.isSecureConnection());

        // Handle connection errors
        connection.addConnectionListener(this);

        // Handle incoming packets and reject messages that are not from FCM CCS
        connection.addAsyncStanzaListener(this, stanza -> stanza.hasExtension(Utils.FCM_ELEMENT_NAME, Utils.FCM_NAMESPACE));

        // Log all outgoing packets
        connection.addStanzaInterceptor(stanza -> logger.info("Sent: {}", stanza.toXML(null)), ForEveryStanza.INSTANCE);

        // Set the ping interval
        final PingManager pingManager = PingManager.getInstanceFor(connection);
        pingManager.setPingInterval(100);
        pingManager.registerPingFailedListener(this);

        connection.login(userName, apiKey);
        logger.info("User logged in: {}", userName);
    }

    @Override
    public void connected(XMPPConnection connection) {
        logger.info("Connection is established.");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        logger.info("Connection is authenticated.");
        onUserAuthentication();
    }

    @Override
    public void connectionClosed() {
        logger.info("Connection closed. The current connectionDraining flag is: {}", isConnectionDraining);
        if (isConnectionDraining) {
            reconnect();
        }
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        logger.error("Connection is closed on error, {}", e.getMessage());
    }

    @Override
    public void reconnectingIn(int seconds) {
        logger.info("Reconnecting in {} ...", seconds);
    }

    @Override
    public void reconnectionFailed(Exception e) {
        logger.error("Reconnection failed, {}", e.getMessage());
    }

    @Override
    public void processStanza(Stanza packet) {
        logger.info("Processing packet in thread {} - {}", Thread.currentThread().getName(), Thread.currentThread().getId());
        logger.info("Received: {}", packet.toXML(null));
        final FcmPacketExtension fcmPacket = (FcmPacketExtension) packet.getExtension(Utils.FCM_NAMESPACE);
        final String json = fcmPacket.getJson();
        final Map<String, Object> jsonMap = MessageMapper.toMapFromJsonString(json);

        assert jsonMap != null;
        final Optional<Object> messageTypeObj = Optional.ofNullable(jsonMap.get("message_type"));
        if (!messageTypeObj.isPresent()) {
            UpstreamMessage upMsg = MessageMapper.upstreamMessageFrom(jsonMap);
            try {
                handleUpstreamMessage(upMsg);
            } catch (InterruptedException | ExecutionException e) {
                logger.info("Exception handling an upstream message:: {}", e.getLocalizedMessage());
            }
            return;
        }

        final String messageType = messageTypeObj.get().toString();
        switch (messageType) {
            case "ack":
                handleAckMsg(jsonMap);
                break;
            case "nack":
                handelNackMsg(jsonMap);
                break;
            case "control":
                handleControlMsg(jsonMap);
                break;
            default:
                logger.info("Received unknown FCM message type: {}", messageType);
        }
    }

    @Override
    public void pingFailed() {
        logger.info("The ping failed, restarting the ping interval...");
        final PingManager pingManager = PingManager.getInstanceFor(connection);
        pingManager.setPingInterval(100);
    }


    private void onUserAuthentication() {
        isConnectionDraining = false;
        sendQueuedMessages();
    }

    private synchronized void reconnect() {
        logger.info("Initiating reconnection...");
        final BackOffStrategy backOffStrategy = new BackOffStrategy(5, 1000);
        while (backOffStrategy.shouldRetry()) {
            try {
                connect();
                sendQueuedMessages();
                backOffStrategy.doNotRetry();
            } catch (XMPPException | SmackException | IOException | InterruptedException | KeyManagementException
                    | NoSuchAlgorithmException e) {
                logger.info("The notifier server couldn't reconnect after the connection draining message.");
                backOffStrategy.errorOcurred();
            }
        }
    }

    private void sendQueuedMessages() {
        final Map<String, FcmMessage> pendingMessagesToResend = new HashMap<>(pendingMessages);
        final Map<String, FcmMessage> syncMessagesToResend = new HashMap<>(syncMessages);
        sendQueuedPendingMessages(pendingMessagesToResend);
        sendQueuedSyncMessages(syncMessagesToResend);
    }

    private void sendQueuedPendingMessages(Map<String, FcmMessage> pendingMessagesToResend) {
        logger.info("Sending queued pending messages through the new connection.");
        logger.info("Pending messages size: {}", pendingMessages.size());
        final Map<String, FcmMessage> filtered = pendingMessagesToResend.entrySet().stream()
                .filter(entry -> entry.getValue() != null).sorted(compareTimestampsAscending())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        logger.info("Filtered pending messages size: {}", filtered.size());
        filtered.entrySet().stream().forEach(entry -> {
            final String messageId = entry.getKey();
            final FcmMessage pendingMessage = entry.getValue();
            pendingMessages.remove(messageId);
            sendDownstreamMessage(messageId, pendingMessage.getJsonRequest());
        });
    }

    private void sendQueuedSyncMessages(Map<String, FcmMessage> syncMessagesToResend) {
        logger.info("Sending queued sync messages...");
        logger.info("Sync messages size: {}", syncMessages.size());
        final Map<String, FcmMessage> filtered = syncMessagesToResend.entrySet().stream().filter(isOldSyncMessageQueued())
                .sorted(compareTimestampsAscending())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
        logger.info("Filtered sync messages size: {}", filtered.size());
        filtered.entrySet().stream().forEach(entry -> {
            final String messageId = entry.getKey();
            final FcmMessage syncMessage = entry.getValue();
            removeMessageFromSyncMessages(messageId);
            sendDownstreamMessage(messageId, syncMessage.getJsonRequest());
        });
    }

    public void removeMessageFromSyncMessages(String messageId) {
        syncMessages.remove(messageId);
    }

    private Predicate<Map.Entry<String, FcmMessage>> isOldSyncMessageQueued() {
        return entry -> entry.getValue() != null
                && entry.getValue().getTimestamp() < System.currentTimeMillis() - 5000;
    }

    private Comparator<Map.Entry<String, FcmMessage>> compareTimestampsAscending() {
        return Comparator.comparing(e -> e.getValue().getTimestamp());
    }

    public void sendDownstreamMessage(String messageId, String jsonRequest) {
        logger.info("Sending downstream message...");
        putMessageToSyncMessages(messageId, jsonRequest);
        if (!isConnectionDraining) {
            sendDownstreamMessageInternal(messageId, jsonRequest);
        }
    }

    private void putMessageToSyncMessages(String messageId, String jsonRequest) {
        syncMessages.put(messageId, FcmMessage.from(jsonRequest));
    }

    private void sendDownstreamMessageInternal(String messageId, String jsonRequest) {
        final Stanza request = new FcmPacketExtension(jsonRequest).toPacket();
        final BackOffStrategy backoff = new BackOffStrategy();
        while (backoff.shouldRetry()) {
            try {
                connection.sendStanza(request);
                backoff.doNotRetry();
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                logger.info("The packet could not be sent due to a connection problem. Backing off the packet: {}",
                        request.toXML(null));
                try {
                    backoff.errorOccured2();
                } catch (Exception e2) {
                    removeMessageFromSyncMessages(messageId);
                    pendingMessages.put(messageId, FcmMessage.from(jsonRequest));
                }
            }
        }
    }

    private void handleUpstreamMessage(UpstreamMessage upMsg) throws InterruptedException, ExecutionException {
        final Optional<String> typeObj = Optional.ofNullable(upMsg.getDataPayload().get("type"));
        if (!typeObj.isPresent()) {
            throw new IllegalStateException("Nothing in the Type field");
        }
        final String type = typeObj.get();

        // send an Ack to the FCM
        final String ackJsonRequest = MessageMapper.createJsonAck(upMsg.getFrom(), upMsg.getMessageId());
        sendAck(ackJsonRequest);

        // handle the message according to its type
        switch (type) {

            case Utils.TO_CLIENT: {

                logger.info("to client received");

                final String messageId = Utils.getUniqueMessageId();
                final Map<String, String> data = upMsg.getDataPayload();
                final String to = data.get(Utils.TOKEN);

                DownstreamMessage downMsg = new DownstreamMessage(to, messageId, data);
                final String jsonRequest = MessageMapper.toJsonString(downMsg);
                sendDownstreamMessage(messageId, jsonRequest);

                break;
            }
            case Utils.TO_ADMIN: {

                logger.info("to admin type received");

                final Map<String, String> data = upMsg.getDataPayload();

                for (String token : Utils.ADMINS) {
                    final String messageId = Utils.getUniqueMessageId();
                    DownstreamMessage downstreamMessage = new DownstreamMessage(token, messageId, data);
                    final String jsonRequest = MessageMapper.toJsonString(downstreamMessage);
                    sendDownstreamMessage(messageId, jsonRequest);
                }

                break;
            }
            case Utils.TO_ADMIN_NEW: {

                logger.info("new to admin type received");

                final String messageId = Utils.getUniqueMessageId();
                final Map<String, String> data = upMsg.getDataPayload();
                final String to = data.get(Utils.ADMIN_TOKEN);

                DownstreamMessage downMsg = new DownstreamMessage(to, messageId, data);
                final String jsonRequest = MessageMapper.toJsonString(downMsg);
                sendDownstreamMessage(messageId, jsonRequest);
                break;
            }
        }
    }

    private void sendAck(String jsonRequest) {
        logger.info("Sending an Ack...");
        final Stanza packet = new FcmPacketExtension(jsonRequest).toPacket();
        final BackOffStrategy backoff = new BackOffStrategy();
        while (backoff.shouldRetry()) {
            try {
                connection.sendStanza(packet);
                backoff.doNotRetry();
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                logger.info("The packet could not be sent due to a connection problem. Backing off the packet: {}",
                        packet.toXML(null));
                backoff.errorOcurred();
            }
        }
    }

    private void handleAckMsg(Map<String, Object> jsonMap) {
        removeMessageFromSyncMessages(jsonMap);
    }

    private void removeMessageFromSyncMessages(Map<String, Object> jsonMap) {
        final Optional<String> messageIdObj = Optional.ofNullable((String) jsonMap.get("message_id"));
        if (messageIdObj.isPresent()) {
            removeMessageFromSyncMessages(messageIdObj.get());
        }
    }

    private void handelNackMsg(Map<String, Object> jsonMap) {
        removeMessageFromSyncMessages(jsonMap);
        Optional<String> errorCodeObj = Optional.ofNullable((String) jsonMap.get("error"));
        if (!errorCodeObj.isPresent()) {
            logger.error("Received null FCM Error code");
            return;
        }
        final String errorCode = errorCodeObj.get();
        switch (errorCode) {
            case "INVALID_JSON":
            case "BAD_REGISTRATION":
            case "DEVICE_UNREGISTERED":
            case "BAD_ACK":
            case "TOPICS_MESSAGE_RATE_EXCEEDED":
            case "DEVICE_MESSAGE_RATE_EXCEEDED":
                logger.info("Device error: {} -> {}", jsonMap.get("error"), jsonMap.get("error_description"));
                break;
            case "SERVICE_UNAVAILABLE":
            case "INTERNAL_SERVER_ERROR":
                logger.info("Server error: {} -> {}", jsonMap.get("error"), jsonMap.get("error_description"));
                break;
            case "CONNECTION_DRAINING":
                logger.info("Connection draining from Nack...");
                handleConnectionDraining();
                break;
            default:
                logger.info("Received unknown FCM Error Code: {}", errorCode);
                break;
        }
    }

    private void handleControlMsg(Map<String, Object> jsonMap) {
        final String controlType = (String) jsonMap.get("control_type");
        if (controlType.equals("CONNECTION_DRAINING")) {
            handleConnectionDraining();
        } else {
            logger.info("Received unknown Fcm Control message: {}", controlType);
        }
    }

    private void handleConnectionDraining() {
        logger.info("Connection is draining.");
        isConnectionDraining = true;
    }
}
