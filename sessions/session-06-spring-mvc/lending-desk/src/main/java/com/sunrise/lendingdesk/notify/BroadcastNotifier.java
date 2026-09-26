package com.sunrise.lendingdesk.notify;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BroadcastNotifier {

    private final List<NotificationSender> senders;

    public BroadcastNotifier(List<NotificationSender> senders) {
        this.senders = List.copyOf(senders);
    }

    public void announce(String member, String message) {
        senders.forEach(sender -> sender.send(member, message));
    }
}
