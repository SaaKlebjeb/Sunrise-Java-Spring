package com.sunrise.lendingdesk.web;

import com.sunrise.lendingdesk.repository.BookRepository;
import com.sunrise.lendingdesk.service.LendingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Checkpoint 4. These two are yours.
 *
 * Everything you need is already wired up above -- a MockMvc, and mocks for both
 * collaborators. Delete the fail(...) line and write the test.
 *
 * Two tests. Until you write them, `mvn test` is red, and that is the point:
 * the red list is the to-do list.
 */
@WebMvcTest(LendingController.class)
@DisplayName("Your turn")
class YourTurnTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BookRepository books;

    @MockitoBean
    private LendingService lending;

    /**
     * 8. POST /loans with a body that is missing the member.
     *
     * Send {"isbn": "978-0132350884"} and nothing else. Decide what you think should
     * happen, then assert it. Then look at what Spring ACTUALLY does before you argue
     * with it -- the answer is more interesting than you expect, and the next sessions
     * are about making it the answer you wanted.
     *
     * Hint: `member` is a String field on a record. What does Jackson put there when
     * the JSON does not mention it?
     */
    @Test
    @DisplayName("8. POST /loans with a member missing from the body")
    void loanWithoutAMember() throws Exception {
        fail("TODO: write this test");
    }

    /**
     * 9. GET /books when the shelf is empty.
     *
     * Stub findAll() to return an empty list. Assert the status, and assert the body
     * is an empty JSON array -- not null, not a 404.
     *
     * Empty is not the same as missing. One of the commonest API bugs in the world is
     * a service that 404s an empty collection, and the client that has to special-case
     * it forever.
     */
    @Test
    @DisplayName("9. GET /books with nothing on the shelf")
    void emptyShelf() throws Exception {
        fail("TODO: write this test");
    }
}
