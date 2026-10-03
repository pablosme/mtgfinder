package com.pelia.mtgfinder.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.pelia.mtgfinder.dto.CardSearchResponse;
import com.pelia.mtgfinder.dto.CardSearchFilters;
import com.pelia.mtgfinder.service.CardSearchService;
import com.pelia.mtgfinder.service.CardInfoService;

@RestController
@RequestMapping("/cards")
public class CardController {

    private final CardSearchService cardSearchService;

    private final CardInfoService cardInfoService;

    public CardController(CardSearchService cardSearchService, CardInfoService cardInfoService) {
        this.cardSearchService = cardSearchService;
        this.cardInfoService = cardInfoService;
    }

    @GetMapping("/{oracleId}")
    public CardSearchResponse.OracleCard getCardInfo(@PathVariable UUID oracleId) {
        return cardInfoService.findByOracleId(oracleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Card not found: " + oracleId));
    }

    @GetMapping
    public CardSearchResponse searchByName(@RequestParam String name, @RequestParam MultiValueMap<String, String> parameters) {
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be blank");
        }

        System.out.println(parameters);

        return cardSearchService.searchByName(name, CardSearchFilters.from(parameters));
    }

    @GetMapping("/fuzzy")
    public CardSearchResponse searchByFuzzyName(@RequestParam String name, @RequestParam MultiValueMap<String, String> parameters) {
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name must not be blank");
        }

        return cardSearchService.searchByFuzzyName(name, CardSearchFilters.from(parameters));
    }

    @GetMapping("/vector")
    public CardSearchResponse searchByPrompt(@RequestParam String prompt, @RequestParam MultiValueMap<String, String> parameters) {
        if (prompt.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prompt must not be blank");
        return cardSearchService.searchByPrompt(prompt, CardSearchFilters.from(parameters));
    }
}
