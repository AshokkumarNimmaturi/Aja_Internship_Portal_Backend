package com.aja.internshipportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync  // enables @Async — emails send in background thread
public class AjaInternshipPortalServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(AjaInternshipPortalServerApplication.class, args);
	}

}
