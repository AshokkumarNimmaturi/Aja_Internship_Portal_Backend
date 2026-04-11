package com.aja.internshipportal.config;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {

        Dotenv dotenv = Dotenv.load();

        System.setProperty("FAST2SMS_API_KEY", dotenv.get("FAST2SMS_API_KEY"));
        System.setProperty("TWILIO_ACCOUNT_SID", dotenv.get("TWILIO_ACCOUNT_SID"));
        System.setProperty("TWILIO_AUTH_TOKEN", dotenv.get("TWILIO_AUTH_TOKEN"));
        System.setProperty("TWILIO_TWIML_APP_SID", dotenv.get("TWILIO_TWIML_APP_SID"));
        System.setProperty("TWILIO_PHONE_NUMBER", dotenv.get("TWILIO_PHONE_NUMBER"));
        System.setProperty("TWILIO_API_KEY_SID", dotenv.get("TWILIO_API_KEY_SID"));
        System.setProperty("TWILIO_API_KEY_SECRET", dotenv.get("TWILIO_API_KEY_SECRET"));
    }
}