package com.aja.internshipportal.dto.response;

import java.math.BigDecimal;

import com.aja.internshipportal.entity.CoursePackage;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PackageResponse {

	private Long id;
	private String name;
	private String description;
	private CoursePackage.PackageType packageType;
	private String technologyName;
	
	   // All 4 prices sent — frontend shows the right one
    private BigDecimal basicPrice;
    private BigDecimal standardPrice;
    private BigDecimal premiumPrice;
    private BigDecimal bundlePrice;

    private boolean active;
	
}
