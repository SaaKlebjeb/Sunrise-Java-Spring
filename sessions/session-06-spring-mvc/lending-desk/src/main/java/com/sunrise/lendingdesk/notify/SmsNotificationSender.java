package com.sunrise.lendingdesk.notify;

import org.springframework.stereotype.Component;

@Component("sms")
public class SmsNotificationSender implements NotificationSender {

    @Override
    public void send(String member, String message) {
        System.out.println("[sms] to " + member + ": " + message);
    }
}
