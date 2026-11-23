package com.spacedvocab.controller;

import com.spacedvocab.config.security.AuthenticatedUser;
import com.spacedvocab.dto.DueCardsResponse;
import com.spacedvocab.dto.ReviewRequest;
import com.spacedvocab.dto.ReviewResponse;
import com.spacedvocab.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** D7 dashboard counts and the D13 review session. */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Cards to study today, optionally for one deck. {@code limit=0} returns only the counts. */
    @GetMapping("/due")
    public DueCardsResponse due(@AuthenticationPrincipal AuthenticatedUser me,
                                @RequestParam(required = false) Long deckId,
                                @RequestParam(defaultValue = "50")
                                @Min(value = 0, message = "limit từ 0 đến 200.")
                                @Max(value = 200, message = "limit từ 0 đến 200.") int limit) {
        return reviewService.due(me, deckId, limit);
    }

    @PostMapping
    public ReviewResponse review(@AuthenticationPrincipal AuthenticatedUser me,
                                 @Valid @RequestBody ReviewRequest request) {
        return reviewService.review(me, request);
    }
}
