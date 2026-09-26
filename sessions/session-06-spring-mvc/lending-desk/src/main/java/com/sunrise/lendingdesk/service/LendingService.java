package com.sunrise.lendingdesk.service;

import com.sunrise.lendingdesk.domain.Book;
import com.sunrise.lendingdesk.domain.LendingResult;
import com.sunrise.lendingdesk.notify.NotificationSender;
import com.sunrise.lendingdesk.repository.BookRepository;
import com.sunrise.lendingdesk.thirdparty.AuditLog;
import org.springframework.stereotype.Service;

@Service
public class LendingService {

    private final BookRepository books;
    private final NotificationSender notifier;
    private final AuditLog audit;

    public LendingService(BookRepository books, NotificationSender notifier, AuditLog audit) {
        this.books = books;
        this.notifier = notifier;
        this.audit = audit;
    }

    public LendingResult lend(String isbn, String member) {
        boolean checkedOut = books.checkOut(isbn);
        audit.record("lend " + isbn + " -> " + member + (checkedOut ? " OK" : " REFUSED"));

        if (!checkedOut) {
            return new LendingResult(false, "no copies left");
        }
        String title = books.findByIsbn(isbn).map(Book::title).orElse(isbn);
        notifier.send(member, "you borrowed " + title);
        return new LendingResult(true, "lent to " + member);
    }
}
