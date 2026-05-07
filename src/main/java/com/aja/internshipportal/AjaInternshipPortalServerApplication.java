package com.aja.internshipportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableAsync  // enables @Async — emails send in background thread
public class AjaInternshipPortalServerApplication {

	public static void main(String[] args) {
		// Load .env variables before Spring Boot starts so ALL properties can access them
		Dotenv.load().entries().forEach(entry -> 
			System.setProperty(entry.getKey(), entry.getValue())
		);
		
		SpringApplication.run(AjaInternshipPortalServerApplication.class, args);
	}

}
