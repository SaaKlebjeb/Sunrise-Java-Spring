package com.sunrise.lendingdesk.service;

import com.sunrise.lendingdesk.ticket.LoanTicketPrinter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class TicketDesk {

    /*
     * Step one of the checkpoint holds a LoanTicketPrinter here, and both tickets come out with
     * the same serial: a singleton's dependencies are injected once, and that was the last time
     * the container was involved. The field below holds the ability to ask for a printer instead.
     */
    private final ObjectProvider<LoanTicketPrinter> printers;

    public TicketDesk(ObjectProvider<LoanTicketPrinter> printers) {
        this.printers = printers;
    }

    public String issue(String isbn, String member) {
        return printers.getObject().print(isbn, member);
    }
}
