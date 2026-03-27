package com.aja.internshipportal.service;

import com.aja.internshipportal.dto.response.PackageResponse;

import java.util.List;

public interface PackageService {

    // Public — list all active packages
    List<PackageResponse> getAllPackages();

    // Public — get single package by id
    PackageResponse getPackageById(Long id);
}