package com.aja.internshipportal.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "technologies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Technology {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Backend, Frontend, DevOps, Salesforce, Python
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 255)
    private String iconUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}