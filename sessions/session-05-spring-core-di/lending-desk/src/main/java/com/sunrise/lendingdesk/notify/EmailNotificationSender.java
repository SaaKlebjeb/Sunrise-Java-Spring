package com.sunrise.lendingdesk.notify;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class EmailNotificationSender implements NotificationSender {

    @Override
    public void send(String member, String message) {
        System.out.println("[email] to " + member + ": " + message);
    }
}
