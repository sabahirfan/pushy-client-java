package com.cds.pushy.client;

import com.turo.pushy.apns.util.ApnsPayloadBuilder;
import com.turo.pushy.apns.util.TokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

@SpringBootApplication
public class PushyClientApplication implements CommandLineRunner {

    @Autowired
    private PushyClient pushyClient;

    @Value("${device.token}")
    private String tokenId;

    public static void main(String[] args) {
        SpringApplication.run(PushyClientApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        final ApnsPayloadBuilder payloadBuilder = new ApnsPayloadBuilder();
        payloadBuilder.setAlertBody("You got a new notification");
        payloadBuilder.setBadgeNumber(1);
        payloadBuilder.setSoundFileName("default");
        payloadBuilder.setAlertTitle("New Alert");

        payloadBuilder.addCustomProperty("data", new MessagePayload("hi", now().format(ISO_DATE_TIME)));

        final String payload = payloadBuilder.buildWithDefaultMaximumLength();
        final String token = TokenUtil.sanitizeTokenString(tokenId);

        pushyClient.send(token, payload);

    }
}
