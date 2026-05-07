package com.aja.internshipportal.controller;

import com.aja.internshipportal.dto.request.PackageRequest;
import com.aja.internshipportal.dto.response.PackageResponse;
import com.aja.internshipportal.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // POST /api/packages — Create a new package
    @PostMapping
    public ResponseEntity<PackageResponse> createPackage(@RequestBody PackageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(packageService.createPackage(request));
    }
}
