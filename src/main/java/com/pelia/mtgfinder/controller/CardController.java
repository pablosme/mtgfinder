package com.pelia.mtgfinder.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pelia.mtgfinder.dto.CardSearchResponse;
import com.pelia.mtgfinder.service.CardSearchService;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardSearchService cardSearchService;

    public CardController(CardSearchService cardSearchService) {
        this.cardSearchService = cardSearchService;
    }

    @GetMapping
    public CardSearchResponse searchByName(@RequestParam String name) {
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be blank");
        }

        return cardSearchService.searchByName(name);
    }

    @GetMapping("/vector")
    public CardSearchResponse searchByPrompt(@RequestParam String prompt) {
        return cardSearchService.searchByPrompt(prompt);
    }
}
