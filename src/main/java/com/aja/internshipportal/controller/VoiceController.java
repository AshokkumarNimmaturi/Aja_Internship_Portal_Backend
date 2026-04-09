package com.aja.internshipportal.controller;

import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VoiceGrant;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Dial;
import com.twilio.twiml.voice.Number;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.api.key.sid}")
    private String apiKeySid;

    @Value("${twilio.api.key.secret}")
    private String apiKeySecret;

    @Value("${twilio.twiml.app.sid}")
    private String twimlAppSid;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    /**
     * Endpoint for frontend to get a JWT token.
     * This token allows the browser to communicate with Twilio.
     */
    @GetMapping("/token")
    public String getAccessToken(@RequestParam String identity) {
        VoiceGrant grant = new VoiceGrant();
        grant.setOutgoingApplicationSid(twimlAppSid);

        AccessToken token = new AccessToken.Builder(accountSid, apiKeySid, apiKeySecret)
                .identity(identity)
                .grant(grant)
                .build();

        return token.toJwt();
    }

    /**
     * Endpoint Twilio calls to get TwiML instructions when a call is initiated.
     * This is the "bridge" that connects the browser to a real phone number.
     */
    @PostMapping(value = "/twiml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String getTwiML(@RequestParam String To) {
        // Clean the number (remove any non-numeric characters except +)
        String formattedTo = To.replaceAll("[^0-9+]", "");
        
        // ✅ AUTO-FORMAT: If number is 10 digits (India), prepend '+91'
        // This ensures the call matches your "Verified Caller IDs" exactly.
        if (formattedTo.length() == 10 && !formattedTo.startsWith("+")) {
            formattedTo = "+91" + formattedTo;
        } else if (!formattedTo.startsWith("+") && formattedTo.length() > 0) {
            // General case for numbers that have country code but missing the '+'
            formattedTo = "+" + formattedTo;
        }

        System.out.println("DEBUG: Twilio Request Received -> Original: [" + To + "] -> To Dial: [" + formattedTo + "]"); 
        
        Number number = new Number.Builder(formattedTo).build();
        
        // CallerId must be either your Twilio number or a verified number
        Dial dial = new Dial.Builder()
                .callerId(twilioPhoneNumber)
                .number(number)
                .build();
                
        VoiceResponse response = new VoiceResponse.Builder().dial(dial).build();
        return response.toXml();
    }
}
