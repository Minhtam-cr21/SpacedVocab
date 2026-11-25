package com.spacedvocab.controller;

import com.spacedvocab.dto.OfficialDeckResponse;
import com.spacedvocab.service.PathAdminService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * D20 / D26: OFFICIAL decks for admins. Only the list exists for now (to pick lessons);
 * creating and editing OFFICIAL decks arrives with M4.
 */
@RestController
@RequestMapping("/api/admin/decks")
public class AdminDeckController {

    private final PathAdminService pathAdminService;

    public AdminDeckController(PathAdminService pathAdminService) {
        this.pathAdminService = pathAdminService;
    }

    @GetMapping
    public List<OfficialDeckResponse> list() {
        return pathAdminService.listOfficialDecks();
    }
}
