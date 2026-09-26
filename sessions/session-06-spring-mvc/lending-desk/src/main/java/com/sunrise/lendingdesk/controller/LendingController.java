package com.sunrise.lendingdesk.controller;

import com.sunrise.lendingdesk.domain.Book;
import com.sunrise.lendingdesk.domain.LendingRequest;
import com.sunrise.lendingdesk.domain.LendingResult;
import com.sunrise.lendingdesk.repository.BookRepository;
import com.sunrise.lendingdesk.service.LendingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
public class LendingController {
    private final BookRepository bookRepository;
    private final LendingService lendingService;

    public LendingController(BookRepository bookRepository,LendingService lendingService){
        this.bookRepository = bookRepository;
        this.lendingService = lendingService;
    }

    @GetMapping("/books")
    public List<Book> all(){
        return bookRepository.findAll();
    }

    @GetMapping("/book/{isbn}")
    public Optional<Book> byId(@PathVariable String isbn){
        return bookRepository.findByIsbn(isbn);
    }

    @GetMapping("/book")
    public ResponseEntity<List<Book>> findBook(@RequestParam(defaultValue = "false") boolean available){
        List<Book> books =bookRepository.findAll();
         if(!available){
             return ResponseEntity.notFound().build();
         }

        return ResponseEntity.ok(books);
    }
    //POST
    @PostMapping("/loan")
    public ResponseEntity<LendingResult> lend(@RequestBody LendingRequest lendingRequest){
        LendingResult lendingResult;
        if(lendingRequest.member().isEmpty()){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new LendingResult(false,"Missing parameter"));
        }
        LendingResult result = lendingService.lend(lendingRequest.isbn(),lendingRequest.member());
        return ResponseEntity.ok(result);
    }

}
