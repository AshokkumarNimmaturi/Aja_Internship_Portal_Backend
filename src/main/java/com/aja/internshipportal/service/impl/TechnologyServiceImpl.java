package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.dto.request.TechnologyRequest;
import com.aja.internshipportal.dto.response.TechnologyResponse;
import com.aja.internshipportal.entity.Technology;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.TechnologyRepository;
import com.aja.internshipportal.service.TechnologyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TechnologyServiceImpl implements TechnologyService {

    private final TechnologyRepository technologyRepository;

    // ── List all active technologies ──
    // called by frontend to populate dropdowns
    @Override
    public List<TechnologyResponse> getAllActiveTechnologies() {
        return technologyRepository.findByActiveTrue()
                .stream()
                .map(this::mapToTechnologyResponse)
                .collect(Collectors.toList());
    }

    // ── Create new technology — ADMIN + TUTOR ──
    @Override
    @Transactional
    public TechnologyResponse createTechnology(TechnologyRequest request) {

        // check duplicate name — case insensitive
        if (technologyRepository.existsByName(request.getName())) {
            throw AppException.conflict(
                "Technology '" + request.getName() + "' already exists"
            );
        }

        Technology technology = Technology.builder()
                .name(request.getName())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .active(true)
                .build();

        technologyRepository.save(technology);
        return mapToTechnologyResponse(technology);
    }

    // ── Deactivate technology — ADMIN only ──
    @Override
    @Transactional
    public void deactivateTechnology(Long id) {
        Technology technology = technologyRepository.findById(id)
                .orElseThrow(() ->
                    AppException.notFound("Technology not found")
                );
        technology.setActive(false);
        technologyRepository.save(technology);
    }

    // ── helper ──
    public TechnologyResponse mapToTechnologyResponse(Technology technology) {
        return TechnologyResponse.builder()
                .id(technology.getId())
                .name(technology.getName())
                .description(technology.getDescription())
                .iconUrl(technology.getIconUrl())
                .active(technology.isActive())
                .build();
    }
}