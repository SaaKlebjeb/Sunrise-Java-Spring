package com.sunrise.lendingdesk;

import com.sunrise.lendingdesk.controller.LendingController;
import com.sunrise.lendingdesk.domain.Book;
import com.sunrise.lendingdesk.domain.LendingResult;
import com.sunrise.lendingdesk.repository.BookRepository;
import com.sunrise.lendingdesk.service.LendingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The slice. Only the web layer is loaded: this controller, the MVC infrastructure,
 * the converters. LendingService and BookRepository are NOT real beans here --
 * they are mocks, supplied below with @MockitoBean.
 *
 * Note @MockitoBean, not @MockBean. @MockBean was removed in Spring Boot 4.
 * Every tutorial older than 2024 will tell you otherwise. They are wrong.
 *
 * Seven tests. Tests 1-4 go green at checkpoint 2; 5, 6 and 7 need POST /loans,
 * so they go green at checkpoint 3. Test 7 is the deliberate 415 -- the one test
 * here that asserts a failure, and the only one where your method never runs.
 */
@WebMvcTest(LendingController.class)
@DisplayName("The lending desk, over HTTP")
class LendingControllerWebTest {

    private static final Book CLEAN_CODE =
            new Book("978-0132350884", "Clean Code", 3);
    private static final Book EFFECTIVE_JAVA =
            new Book("978-0134685991", "Effective Java", 2);
    private static final Book OUT_OF_STOCK =
            new Book("978-0201616224", "The Pragmatic Programmer", 0);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookRepository books;

    @MockitoBean
    private LendingService lending;

    @Test
    @DisplayName("1. GET /books returns every book on the shelf")
    void listsEveryBook() throws Exception {
        when(books.findAll()).thenReturn(List.of(CLEAN_CODE, EFFECTIVE_JAVA, OUT_OF_STOCK));

        mockMvc.perform(get("/books"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(3))
               .andExpect(jsonPath("$[0].isbn").value("978-0132350884"));
    }

    @Test
    @DisplayName("2. GET /books/{isbn} returns that one book")
    void returnsOneBook() throws Exception {
        when(books.findByIsbn("978-0132350884")).thenReturn(Optional.of(CLEAN_CODE));

        mockMvc.perform(get("/books/978-0132350884"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.title").value("Clean Code"))
               .andExpect(jsonPath("$.copies").value(3));
    }

    @Test
    @DisplayName("3. GET /books/{isbn} returns 404 when the shelf has never heard of it")
    void unknownIsbnIs404() throws Exception {
        when(books.findByIsbn(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/books/not-a-real-isbn"))
               .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("4. GET /books?available=true hides the books with no copies left")
    void filtersToAvailable() throws Exception {
        when(books.findAll()).thenReturn(List.of(CLEAN_CODE, EFFECTIVE_JAVA, OUT_OF_STOCK));

        mockMvc.perform(get("/books").param("available", "true"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("5. POST /loans lends the book and answers with the result")
    void lendsABook() throws Exception {
        when(lending.lend(eq("978-0132350884"), eq("sokha")))
                .thenReturn(new LendingResult(true, "lent to sokha"));

        mockMvc.perform(post("/loan")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("""
                               {"isbn": "978-0132350884", "member": "sokha"}
                               """))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ok").value(true))
               .andExpect(jsonPath("$.message").value("lent to sokha"));
    }

    @Test
    @DisplayName("6. POST /loans says so when there is nothing left to lend")
    void refusesWhenNoCopiesLeft() throws Exception {
        when(lending.lend(any(), any()))
                .thenReturn(new LendingResult(false, "no copies left"));

        mockMvc.perform(post("/loans")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("""
                               {"isbn": "978-0201616224", "member": "sokha"}
                               """))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.ok").value(false))
               .andExpect(jsonPath("$.message").value("no copies left"));
    }

    @Test
    @DisplayName("7. POST /loans without a Content-Type is refused before the method runs")
    void bodyWithoutAContentTypeIs415() throws Exception {
        mockMvc.perform(post("/loans")
                       .content("""
                               {"isbn": "978-0132350884", "member": "sokha"}
                               """))
               .andExpect(status().isUnsupportedMediaType());
    }
}
