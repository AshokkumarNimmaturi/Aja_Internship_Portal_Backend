package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.dto.response.PackageResponse;
import com.aja.internshipportal.entity.CoursePackage;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.PackageRepository;
import com.aja.internshipportal.service.PackageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final PackageRepository packageRepository;

    // ── List all active packages ──
    @Override
    public List<PackageResponse> getAllPackages() {
        return packageRepository.findByActiveTrue()
                .stream()
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());
    }

    // ── Get single package ──
    @Override
    public PackageResponse getPackageById(Long id) {
        CoursePackage coursePackage = packageRepository.findById(id)
                .orElseThrow(() ->
                    AppException.notFound("Package not found")
                );
        return mapToPackageResponse(coursePackage);
    }

    // ── helper ──
    public PackageResponse mapToPackageResponse(CoursePackage pkg) {
        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .packageType(pkg.getPackageType())
                .technologyName(
                    pkg.getTechnology() != null
                        ? pkg.getTechnology().getName()
                        : null
                )
                .basicPrice(pkg.getBasicPrice())
                .standardPrice(pkg.getStandardPrice())
                .premiumPrice(pkg.getPremiumPrice())
                .bundlePrice(pkg.getBundlePrice())
                .active(pkg.isActive())
                .build();
    }
}