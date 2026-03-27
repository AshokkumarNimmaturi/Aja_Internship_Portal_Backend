package com.aja.internshipportal.controller;

import com.aja.internshipportal.dto.response.PackageResponse;
import com.aja.internshipportal.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Public controller — no authentication needed
// already whitelisted in SecurityConfig PUBLIC_URLS
@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class PackageController {

    private final PackageService packageService;

    // GET /api/packages — list all active packages
    @GetMapping
    public ResponseEntity<List<PackageResponse>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    // GET /api/packages/{id} — get one package
    @GetMapping("/{id}")
    public ResponseEntity<PackageResponse> getPackageById(
            @PathVariable Long id) {
        return ResponseEntity.ok(packageService.getPackageById(id));
    }
}