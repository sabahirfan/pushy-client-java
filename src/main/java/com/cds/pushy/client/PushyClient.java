package com.cds.pushy.client;

import com.turo.pushy.apns.ApnsClient;
import com.turo.pushy.apns.ApnsClientBuilder;
import com.turo.pushy.apns.PushNotificationResponse;
import com.turo.pushy.apns.auth.ApnsSigningKey;
import com.turo.pushy.apns.util.SimpleApnsPushNotification;
import com.turo.pushy.apns.util.TokenUtil;
import com.turo.pushy.apns.util.concurrent.PushNotificationFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.turo.pushy.apns.ApnsClientBuilder.DEVELOPMENT_APNS_HOST;
import static com.turo.pushy.apns.ApnsClientBuilder.PRODUCTION_APNS_HOST;

@Component
public class PushyClient {

    private static final Logger logger = LoggerFactory.getLogger(PushyClient.class);

    private final String teamId;
    private final String keyId;
    private final String keyFilePath;
    private final String bundleIdentifier;
    private final Boolean isProduction;

    private ApnsClient apnsClient;

    @Autowired
    public PushyClient(
            @Value("${apns.team.id}") String teamId,
            @Value("${apns.key.id}") String keyId,
            @Value("${apns.p8.key.file.path}") String keyFilePath,
            @Value("${apns.bundle.id}") String bundleIdentifier,
            @Value("${apns.server.prod:false}") Boolean isProduction) {

        this.teamId = teamId;
        this.keyId = keyId;
        this.keyFilePath = keyFilePath;
        this.bundleIdentifier = bundleIdentifier;
        this.isProduction = isProduction;
    }

    private ApnsClient getApnsClient() throws IOException, InvalidKeyException, NoSuchAlgorithmException {
        if (this.apnsClient == null) {
            apnsClient = new ApnsClientBuilder()
                    .setApnsServer(this.isProduction ? PRODUCTION_APNS_HOST : DEVELOPMENT_APNS_HOST)
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(keyFilePath), teamId, keyId))
                    .build();
        }
        return apnsClient;
    }

    public void send(String deviceId, String payload) throws IOException, InvalidKeyException, NoSuchAlgorithmException {

        logger.info("Payload: " + payload);
        final String token = TokenUtil.sanitizeTokenString(deviceId);
        final SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, bundleIdentifier, payload);

        final PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>>
                sendNotificationFuture = getApnsClient().sendNotification(pushNotification);

        sendNotificationFuture.addListener(future -> {

            // When using a listener, callers should check for a failure to send a
            // notification by checking whether the future itself was successful
            // since an exception will not be thrown.
            if (future.isSuccess()) {
                final PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse =
                        sendNotificationFuture.getNow();
                if (pushNotificationResponse.isAccepted()) {
                    logger.info("Push notification accepted by APNs gateway.");
                } else {
                    logger.error("Notification rejected by the APNs gateway: " +
                            pushNotificationResponse.getRejectionReason());

                    if (pushNotificationResponse.getTokenInvalidationTimestamp() != null) {
                        logger.error("\t…and the token is invalid as of " +
                                pushNotificationResponse.getTokenInvalidationTimestamp());
                    }
                    logger.error("Failed Notification :: " + pushNotificationResponse.getPushNotification().toString());
                }

                // Handle the push notification response as before from here.
            } else {
                // Something went wrong when trying to send the notification to the
                // APNs gateway. We can find the exception that caused the failure
                // by getting future.cause().

                logger.error(future.cause().getMessage(), future.cause());
                future.cause().printStackTrace();
            }
        });

    }


}
