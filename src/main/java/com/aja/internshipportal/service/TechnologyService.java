package com.aja.internshipportal.service;

import com.aja.internshipportal.dto.request.TechnologyRequest;
import com.aja.internshipportal.dto.response.TechnologyResponse;

import java.util.List;

public interface TechnologyService {

    // Public — list all active technologies
    // used by frontend dropdowns
    List<TechnologyResponse> getAllActiveTechnologies();

    // ADMIN + TUTOR — create new technology
    TechnologyResponse createTechnology(TechnologyRequest request);

    // ADMIN only — deactivate technology
    void deactivateTechnology(Long id);
}