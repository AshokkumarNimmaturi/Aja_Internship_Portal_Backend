package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.dto.request.PackageRequest;
import com.aja.internshipportal.dto.response.PackageResponse;
import com.aja.internshipportal.entity.CoursePackage;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.PackageRepository;
// import com.aja.internshipportal.repository.TechnologyRepository;
import com.aja.internshipportal.service.PackageService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageServiceImpl implements PackageService {

    private final PackageRepository packageRepository;
    // private final TechnologyRepository technologyRepository; // Uncomment if needed

    // ── List all active packages ──
    @Override
    @Transactional(readOnly = true) // 🔥 FIX: keeps session open
    public List<PackageResponse> getAllPackages() {
        return packageRepository.findByActiveTrue()
                .stream()
                .map(this::mapToPackageResponse)
                .collect(Collectors.toList());
    }

    // ── Get single package ──
    @Override
    @Transactional(readOnly = true) // 🔥 FIX here also
    public PackageResponse getPackageById(Long id) {
        CoursePackage coursePackage = packageRepository.findById(id)
                .orElseThrow(() ->
                        AppException.notFound("Package not found")
                );

        return mapToPackageResponse(coursePackage);
    }

    // ── Create Package ──
    @Override
    @Transactional
    public PackageResponse createPackage(PackageRequest request) {
        CoursePackage coursePackage = new CoursePackage();
        coursePackage.setName(request.getName());
        coursePackage.setDescription(request.getDescription());
        coursePackage.setPackageType(request.getPackageType());
        coursePackage.setBasicPrice(request.getBasicPrice());
        coursePackage.setPrice(request.getBasicPrice()); // ADD THIS LINE TO FIX 
        coursePackage.setStandardPrice(request.getStandardPrice());
        coursePackage.setPremiumPrice(request.getPremiumPrice());
        coursePackage.setBundlePrice(request.getBundlePrice());
        coursePackage.setActive(true);

        // Note: If you map technologies to packages, you'll need to fetch and set it here
        // if (request.getTechnologyName() != null) {
        //     Technology tech = technologyRepository.findByName(request.getTechnologyName())
        //            .orElseThrow(() -> AppException.notFound("Tech not found"));
        //     coursePackage.setTechnology(tech);
        // }

        CoursePackage savedPackage = packageRepository.save(coursePackage);
        return mapToPackageResponse(savedPackage);
    }

    // ── helper ──
    private PackageResponse mapToPackageResponse(CoursePackage pkg) {

        // ✅ SAFE TECHNOLOGY ACCESS
        String techName = null;
        if (pkg.getTechnology() != null) {
            techName = pkg.getTechnology().getName();
        }

        return PackageResponse.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .packageType(pkg.getPackageType())
                .technologyName(techName) // ✅ FIXED
                .basicPrice(pkg.getBasicPrice())
                .standardPrice(pkg.getStandardPrice())
                .premiumPrice(pkg.getPremiumPrice())
                .bundlePrice(pkg.getBundlePrice())
                .active(pkg.isActive())
                .build();
    }
}
