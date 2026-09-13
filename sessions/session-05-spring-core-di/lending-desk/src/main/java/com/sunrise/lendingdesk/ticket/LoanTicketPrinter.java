package com.sunrise.lendingdesk.ticket;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

@Component
@Scope("prototype")
public class LoanTicketPrinter {

    private final int serial = ThreadLocalRandom.current().nextInt(1000, 10000);

    public String print(String isbn, String member) {
        return "ticket " + serial + " | " + isbn + " | " + member;
    }
}
