package com.aja.internshipportal.repository;

import com.aja.internshipportal.entity.SupportCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional; // ✅ ADDED

@Repository
public interface SupportCallRepository extends JpaRepository<SupportCall, Long> {
    
    // ✅ NEW: Precise lookup for status updates
    Optional<SupportCall> findByCallSid(String callSid);

    // Fetches the most recent 20 calls for the Admin Panel history table
    List<SupportCall> findTop20ByOrderByTimestampDesc();
}
