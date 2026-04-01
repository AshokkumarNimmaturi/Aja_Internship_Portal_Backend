package com.aja.internshipportal.controller;

import com.aja.internshipportal.dto.request.TechnologyRequest;
import com.aja.internshipportal.dto.response.ApiResponse;
import com.aja.internshipportal.dto.response.TechnologyResponse;
import com.aja.internshipportal.service.TechnologyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/technologies")
@RequiredArgsConstructor
public class TechnologyController {

    private final TechnologyService technologyService;

    // GET /api/technologies — public, no token needed
    // frontend calls this to populate technology dropdowns
    @GetMapping
    public ResponseEntity<List<TechnologyResponse>> getAllTechnologies() {
        return ResponseEntity.ok(
            technologyService.getAllActiveTechnologies()
        );
    }

    // POST /api/technologies — ADMIN + TUTOR only
    // creates new technology from submit question page
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TUTOR')")
    public ResponseEntity<TechnologyResponse> createTechnology(
            @Valid @RequestBody TechnologyRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(technologyService.createTechnology(request));
    }

    // DELETE /api/technologies/{id} — ADMIN only
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse> deactivateTechnology(
            @PathVariable Long id) {
        technologyService.deactivateTechnology(id);
        return ResponseEntity.ok(
            ApiResponse.success("Technology deactivated successfully")
        );
    }
}