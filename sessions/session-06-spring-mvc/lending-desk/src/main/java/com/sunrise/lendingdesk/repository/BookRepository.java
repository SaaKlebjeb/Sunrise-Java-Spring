package com.sunrise.lendingdesk.repository;

import com.sunrise.lendingdesk.domain.Book;
import org.springframework.expression.spel.ast.Operator;

import java.util.List;
import java.util.Optional;

public interface BookRepository {

    Optional<Book> findByIsbn(String isbn);

    List<Book> findAll();

    boolean checkOut(String isbn);
}
