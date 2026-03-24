package com.aja.internshipportal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aja.internshipportal.entity.Technology;

@Repository
public interface TechnologyRepository extends JpaRepository<Technology, Long> {

    // Check duplicate name before saving
    boolean existsByName(String name);
    
    // Packages list page — show only active technologies
    List<Technology> findByActiveTrue();
    
    // Find by name for seeding / admin lookup
    Optional<Technology> findByName(String name);
}
