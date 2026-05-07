package com.aja.internshipportal.dto.request;

import com.aja.internshipportal.entity.CoursePackage.PackageType;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PackageRequest {
    private String name;
    private String description;
    private PackageType packageType;
    private String technologyName;
    private BigDecimal basicPrice;
    private BigDecimal standardPrice;
    private BigDecimal premiumPrice;
    private BigDecimal bundlePrice;
}
