package com.sunrise.lendingdesk.repository;

import com.sunrise.lendingdesk.domain.Book;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class InMemoryBookRepository implements BookRepository {

    private final Map<String, Book> shelf = new LinkedHashMap<>();

    @PostConstruct
    void seed() {
        put(new Book("978-0132350884", "Clean Code", 3));
        put(new Book("978-0134685991", "Effective Java", 2));
        put(new Book("978-0201616224", "The Pragmatic Programmer", 1));
        System.out.println("seeded " + shelf.size() + " books");
    }

    private void put(Book book) {
        shelf.put(book.isbn(), book);
    }

    @Override
    public Optional<Book> findByIsbn(String isbn) {
        return Optional.ofNullable(shelf.get(isbn));
    }

    @Override
    public List<Book> findAll() {
        return List.copyOf(shelf.values());
    }

    @Override
    public boolean checkOut(String isbn) {
        Book book = shelf.get(isbn);
        if (book == null || book.copies() == 0) {
            return false;
        }
        shelf.put(isbn, new Book(book.isbn(), book.title(), book.copies() - 1));
        return true;
    }
}
