package com.sunrise.lendingdesk;

import com.sunrise.lendingdesk.domain.LendingResult;
import com.sunrise.lendingdesk.notify.BroadcastNotifier;
import com.sunrise.lendingdesk.notify.NotificationSender;
import com.sunrise.lendingdesk.repository.BookRepository;
import com.sunrise.lendingdesk.service.LendingService;
import com.sunrise.lendingdesk.service.TicketDesk;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class LendingDeskApplication {

    public static void main(String[] args) {
        SpringApplication.run(LendingDeskApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(BookRepository books,
                           NotificationSender notifier,
                           LendingService lending,
                           BroadcastNotifier broadcast,
                           TicketDesk desk) {
        return args -> {
            System.out.println("=== catalogue ===");
            books.findAll().forEach(book ->
                    System.out.println(book.isbn() + " | " + book.title() + " | " + book.copies()));

            System.out.println("=== notify ===");
            notifier.send("sokha", "your card is active");

            System.out.println("=== lending ===");
            String isbn = "978-0201616224";
            for (int attempt = 0; attempt < 2; attempt++) {
                LendingResult result = lending.lend(isbn, "sokha");
                System.out.printf("ok=%-5s %s %s%n", result.ok(), isbn, result.message());
            }

            System.out.println("=== broadcast ===");
            broadcast.announce("sokha", "the library closes at 18:00");

            System.out.println("=== tickets ===");
            System.out.println(desk.issue("978-0132350884", "sokha"));
            System.out.println(desk.issue("978-0134685991", "dara"));
        };
    }
}
