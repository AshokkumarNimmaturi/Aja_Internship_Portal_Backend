package com.aja.internshipportal.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="packages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePackage {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	private String name;
	
    // SINGLE = one technology, BUNDLE = all 5
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PackageType packageType;

    // null for BUNDLE type
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technology_id")
    private Technology technology;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    // SINGLE tier prices
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basicPrice;    // ₹299 / 30 days

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal standardPrice; // ₹699 / 90 days

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal premiumPrice;  // ₹1199 / 180 days

    // Only used for BUNDLE — ₹999 flat
    @Column(precision = 10, scale = 2)
    private BigDecimal bundlePrice;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    
    // Legacy price field to satisfy database constraints
    private BigDecimal price;


    public enum PackageType {
        SINGLE, BUNDLE
    }


	
	
	
}
