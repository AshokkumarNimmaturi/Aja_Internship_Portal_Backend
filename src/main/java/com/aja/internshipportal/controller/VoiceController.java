package com.aja.internshipportal.controller;

import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.UserService;
import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VoiceGrant;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import com.twilio.twiml.voice.Number;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final UserRepository userRepository;
    private final UserService userService;

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
     * ENTRY POINT for all calls.
     */
    @PostMapping(value = "/twiml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String getTwiML(@RequestParam(required = false) String To) {
        // If 'To' is support or empty, start the Hunt Group flow
        if (To == null || To.trim().isEmpty() || To.equalsIgnoreCase("support")) {
            return startSupportHunt(0);
        }
        
        // Otherwise, standard direct dialing
        return dialDirect(To);
    }

    /**
     * SEQUENTIAL DIALING (HUNT GROUP) logic.
     * Rings agents one by one. If one fails, it calls this endpoint again for the next agent.
     */
    @PostMapping(value = "/hunt", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String supportHunt(@RequestParam int agentIndex) {
        return startSupportHunt(agentIndex);
    }

    private String startSupportHunt(int index) {
        // 1. Get all potential agents (Admins/Tutors) who are Available and NOT in a call
        List<User> availableAgents = userRepository.findByRoleInAndAvailableTrueAndInCallFalseAndEnabledTrue(
                Arrays.asList(User.Role.ADMIN, User.Role.TUTOR)
        );

        // 2. If we reach the end of the list or no one is online -> Go to QUEUE
        if (availableAgents.isEmpty() || index >= availableAgents.size()) {
            return enterQueue();
        }

        User agent = availableAgents.get(index);
        String agentPhone = formatPhoneNumber(agent.getPhone());
        
        log.info("Hunting Agent {}: {}", index + 1, agent.getEmail());

        // 3. Dial the agent. 
        // 'action' tells Twilio what to do if the agent doesn't answer (Go to next agent)
        // 'statusCallback' tells our server when the call actually starts/ends
        Number number = new Number.Builder(agentPhone)
                .statusCallback("/api/voice/status-callback")
                .statusCallbackEvents(Arrays.asList(Number.Event.INITIATED, Number.Event.ANSWERED, Number.Event.COMPLETED))
                .build();

        Dial dial = new Dial.Builder()
                .callerId(twilioPhoneNumber)
                .timeout(15) // Ring for 15 seconds before trying next agent
                .action("/api/voice/hunt?agentIndex=" + (index + 1)) 
                .number(number)
                .build();

        return new VoiceResponse.Builder()
                .say(new Say.Builder("Please wait while we connect you to an agent.").build())
                .dial(dial)
                .build().toXml();
    }

    private String enterQueue() {
        log.info("No agents available. Placing caller in Support Queue.");
        
        Say waitMsg = new Say.Builder("All our agents are currently busy assisting other subscribers. Please stay on the line.").build();
        Enqueue enqueue = new Enqueue.Builder("SupportQueue").build();
        
        return new VoiceResponse.Builder()
                .say(waitMsg)
                .enqueue(enqueue)
                .build().toXml();
    }

    private String dialDirect(String destination) {
        String formattedTo = formatPhoneNumber(destination);
        Number number = new Number.Builder(formattedTo).build();
        Dial dial = new Dial.Builder().callerId(twilioPhoneNumber).number(number).build();
        return new VoiceResponse.Builder().dial(dial).build().toXml();
    }

    /**
     * WEBHOOK: Twilio calls this whenever a call status changes.
     * We use this to mark agents as 'Busy' or 'Free'.
     */
    @PostMapping("/status-callback")
    public void statusCallback(@RequestParam String To, @RequestParam String CallStatus) {
        boolean inCall = CallStatus.equalsIgnoreCase("in-progress") || CallStatus.equalsIgnoreCase("answered");
        userService.setInCallStatus(To, inCall);
    }

    private String formatPhoneNumber(String phone) {
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.length() == 10 && !cleaned.startsWith("+")) {
            return "+91" + cleaned;
        }
        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
    }
}
