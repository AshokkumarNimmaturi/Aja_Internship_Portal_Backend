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
     * Endpoint Twilio calls to get TwiML instructions.
     * Updated to route to Super Admin by default or when requested.
     */
    @PostMapping(value = "/twiml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String getTwiML(@RequestParam(required = false) String To) {
        // ✅ DEFAULT: Route to Super Admin if To is missing or marked as support
        String destination = (To == null || To.trim().isEmpty()) ? "+917780131390" : To;
        
        // Clean the number (remove any non-numeric characters except +)
        String formattedTo = destination.replaceAll("[^0-9+]", "");
        
        // ✅ AUTO-FORMAT: Ensure India numbers have +91
        if (formattedTo.length() == 10 && !formattedTo.startsWith("+")) {
            formattedTo = "+91" + formattedTo;
        } else if (!formattedTo.startsWith("+") && formattedTo.length() > 0) {
            formattedTo = "+" + formattedTo;
        }

        System.out.println("DEBUG: Voice Call Routing -> Destination: [" + formattedTo + "]"); 
        
        Number number = new Number.Builder(formattedTo).build();
        
        Dial dial = new Dial.Builder()
                .callerId(twilioPhoneNumber)
                .number(number)
                .build();
                
        VoiceResponse response = new VoiceResponse.Builder().dial(dial).build();
        return response.toXml();
    }
}
