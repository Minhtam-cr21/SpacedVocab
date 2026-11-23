package com.spacedvocab.controller;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.CardRequest;
import com.spacedvocab.dto.CardResponse;
import com.spacedvocab.dto.PageResponse;
import com.spacedvocab.service.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** D10 card list and D11 card form. */
@RestController
@RequestMapping("/api/decks/{deckId}/cards")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /** Search word or meaning; {@code page} is 0-based. */
    @GetMapping
    public PageResponse<CardResponse> list(@AuthenticationPrincipal AuthenticatedUser me,
                                           @PathVariable Long deckId,
                                           @RequestParam(defaultValue = "")
                                           @Size(max = 100, message = "Từ khóa tối đa 100 ký tự.") String q,
                                           @RequestParam(defaultValue = "0")
                                           @Min(value = 0, message = "page phải ≥ 0.") int page,
                                           @RequestParam(defaultValue = "20")
                                           @Min(value = 1, message = "size từ 1 đến 100.")
                                           @Max(value = 100, message = "size từ 1 đến 100.") int size) {
        return cardService.search(deckId, me, q, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long deckId,
                               @Valid @RequestBody CardRequest request) {
        return cardService.create(deckId, me, request);
    }

    @GetMapping("/{cardId}")
    public CardResponse get(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long deckId,
                            @PathVariable Long cardId) {
        return cardService.get(deckId, cardId, me);
    }

    @PutMapping("/{cardId}")
    public CardResponse update(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long deckId,
                               @PathVariable Long cardId, @Valid @RequestBody CardRequest request) {
        return cardService.update(deckId, cardId, me, request);
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long deckId,
                       @PathVariable Long cardId) {
        cardService.delete(deckId, cardId, me);
    }
}
