package com.aja.internshipportal.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class TechnologyResponse {

    private Long id;
    private String name;
    private String description;
    private String iconUrl;
    private boolean active;
}