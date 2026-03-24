

package com.aja.internshipportal.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aja.internshipportal.entity.CoursePackage;


@Repository
public interface PackageRepository extends JpaRepository<CoursePackage, Long> {

	 // Packages listing page — show only active packages
    List<CoursePackage> findByActiveTrue();
    
    // Filter by type — SINGLE or BUNDLE
    List<CoursePackage> findByPackageTypeAndActiveTrue(CoursePackage.PackageType packageType);
    
    // Check duplicate name before creating
    boolean existsByName(String name);
}
