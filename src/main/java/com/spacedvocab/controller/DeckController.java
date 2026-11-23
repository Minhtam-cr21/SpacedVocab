package com.spacedvocab.controller;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.DeckRequest;
import com.spacedvocab.dto.DeckResponse;
import com.spacedvocab.service.DeckService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
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

import java.util.List;

/** D8 Library, D9 deck form, D10 deck header. */
@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;

    public DeckController(DeckService deckService) {
        this.deckService = deckService;
    }

    /** {@code scope=subscribed} arrives with subscriptions in M4. */
    @GetMapping
    public List<DeckResponse> list(@AuthenticationPrincipal AuthenticatedUser me,
                                   @RequestParam(defaultValue = "owned")
                                   @Pattern(regexp = "owned", message = "Hiện chỉ hỗ trợ scope=owned.")
                                   String scope) {
        return deckService.listOwned(me);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeckResponse create(@AuthenticationPrincipal AuthenticatedUser me,
                               @Valid @RequestBody DeckRequest request) {
        return deckService.create(me, request);
    }

    @GetMapping("/{deckId}")
    public DeckResponse get(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long deckId) {
        return deckService.get(deckId, me);
    }

    @PutMapping("/{deckId}")
    public DeckResponse update(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long deckId,
                               @Valid @RequestBody DeckRequest request) {
        return deckService.update(deckId, me, request);
    }

    @DeleteMapping("/{deckId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long deckId) {
        deckService.delete(deckId, me);
    }
}
