package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.entity.SupportCall;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.repository.SupportCallRepository;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.VoiceService;
import com.aja.internshipportal.service.UserService;
import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.jwt.accesstoken.AccessToken;
import com.twilio.jwt.accesstoken.VoiceGrant;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.queue.Member;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import com.twilio.twiml.voice.Number;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceServiceImpl implements VoiceService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final SupportCallRepository supportCallRepository;

    @Value("${twilio.account.sid}") private String accountSid;
    @Value("${twilio.api.key.sid}") private String apiKeySid;
    @Value("${twilio.api.key.secret}") private String apiKeySecret;
    @Value("${twilio.twiml.app.sid}") private String twimlAppSid;
    @Value("${twilio.phone.number}") private String twilioPhoneNumber;

    @Override
    public Map<String, Object> getAvailability() {
        try {
            List<User> readyAgents = userRepository.findAvailableSupportAgents();
            long onlineCount = userRepository.countOnlineAgents();
            Map<String, Object> response = new HashMap<>();
            response.put("readyCount", readyAgents.size());
            response.put("status", !readyAgents.isEmpty() ? "AVAILABLE" : (onlineCount > 0 ? "BUSY" : "OFFLINE"));
            return response;
        } catch (Exception e) {
            log.error("[VOICE] Availability check error: {}", e.getMessage());
            return Map.of("status", "ERROR", "readyCount", 0);
        }
    }

    @Override
    public String getAccessToken(String identity) {
        VoiceGrant grant = new VoiceGrant();
        grant.setOutgoingApplicationSid(twimlAppSid);
        return new AccessToken.Builder(accountSid, apiKeySid, apiKeySecret)
                .identity(identity)
                .grant(grant)
                .build()
                .toJwt();
    }

    @Override
    public String handleTwiML(String to, String from, String callSid, String baseUrl) {
        try {
            log.info("[VOICE] Entry Point. To: {}, From: {}, SID: {}", to, from, callSid);
            logOrUpdateCallAttempt(from, callSid, to);
            
            if (isSupportTarget(to)) {
                return startSupportHunt(0, from, baseUrl);
            }
            return dialDirect(to, baseUrl);
        } catch (Exception e) {
            log.error("[VOICE] CRITICAL CRASH in handleTwiML: ", e);
            return emergencyResponse("System handshake error.");
        }
    }

    @Override
    public String handleHunt(int agentIndex, String from, String baseUrl) {
        try {
            return startSupportHunt(agentIndex, from, baseUrl);
        } catch (Exception e) {
            log.error("[VOICE] CRITICAL CRASH in handleHunt: ", e);
            return emergencyResponse("Routing failure.");
        }
    }

    @Override
    public String handleBridgeToQueue(String originalSid) {
        return new VoiceResponse.Builder()
                .say(new Say.Builder("Hold for connection.").build())
                .dial(new Dial.Builder().queue(new com.twilio.twiml.voice.Queue.Builder("SupportQueue").build()).build())
                .build().toXml();
    }

    @Override
    public void handleStatusCallback(String to, String callStatus, String callSid, 
                                   String parentCallSid, String originalSid, 
                                   String callDuration, String baseUrl) {
        try {
            String targetSid = (originalSid != null) ? originalSid : ((parentCallSid != null) ? parentCallSid : callSid);
            log.info("[VOICE] Callback: {} for To: {}. SID: {}", callStatus, to, targetSid);

            if (to != null && to.startsWith("+")) {
                boolean isActive = callStatus.equalsIgnoreCase("initiated") || 
                                 callStatus.equalsIgnoreCase("ringing") || 
                                 callStatus.equalsIgnoreCase("answered") || 
                                 callStatus.equalsIgnoreCase("in-progress");
                
                userService.setInCallStatus(to, isActive);
                if (callStatus.equalsIgnoreCase("answered")) {
                    userService.markCallAsAnswered(to, targetSid);
                }
            }
            
            if (callStatus.equalsIgnoreCase("completed") || callStatus.equalsIgnoreCase("no-answer") || callStatus.equalsIgnoreCase("busy")) {
                if (to != null && to.startsWith("+")) {
                    userService.setInCallStatus(to, false); 
                }
                updateCallLog(to, callDuration, targetSid);
                if (callStatus.equalsIgnoreCase("completed") && to != null && to.startsWith("+")) {
                    checkQueueAndBridge(to, baseUrl);
                }
            }
        } catch (Exception e) {
            log.warn("[VOICE] Callback non-fatal error: {}", e.getMessage());
        }
    }

    // --- Private Logic ---

    private String startSupportHunt(int index, String fromNumber, String baseUrl) {
        String dialStatus = "unknown";
        try {
            jakarta.servlet.http.HttpServletRequest req = ((org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()).getRequest();
            dialStatus = req.getParameter("DialStatus");
        } catch (Exception e) {}

        log.info("[VOICE] Hunt Phase {}. Prev Status: {}", index, dialStatus);

        // Uses proper Twilio Builder for Hangup
        if (dialStatus != null && (dialStatus.equalsIgnoreCase("completed") || dialStatus.equalsIgnoreCase("answered"))) {
            return new VoiceResponse.Builder()
                    .hangup(new Hangup.Builder().build())
                    .build().toXml();
        }

        List<User> readyAgents = userRepository.findAvailableSupportAgents();
        if (readyAgents != null && !readyAgents.isEmpty() && index < readyAgents.size()) {
            User agent = readyAgents.get(index);
            String agentPhone = formatPhoneNumber(agent.getPhone());
            
            log.info("[HUNT] Dialing Specialist: {} at {}", agent.getFullName(), agentPhone);

            Number number = new Number.Builder(agentPhone)
                    .statusCallback(baseUrl + "/api/voice/status-callback")
                    .statusCallbackEvents(Arrays.asList(Number.Event.INITIATED, Number.Event.ANSWERED, Number.Event.COMPLETED))
                    .build();
            
            String encodedFrom = URLEncoder.encode(fromNumber != null ? fromNumber : "WEB", StandardCharsets.UTF_8);
            String actionUrl = baseUrl + "/api/voice/hunt?agentIndex=" + (index + 1) + "&From=" + encodedFrom;

            Dial dial = new Dial.Builder()
                    .callerId(twilioPhoneNumber)
                    .timeout(15)
                    .action(actionUrl)
                    .number(number)
                    .build();
            
            return new VoiceResponse.Builder()
                    .say(new Say.Builder("Connecting.").build())
                    .dial(dial)
                    .build().toXml();
        }

        if (userRepository.countOnlineAgents() > 0) return enterQueue();
        
        return new VoiceResponse.Builder()
                .say(new Say.Builder("Support is currently offline. Returning to queue.").build())
                .enqueue(new Enqueue.Builder("SupportQueue").build())
                .build().toXml();
    }

    private boolean isSupportTarget(String to) {
        if (to == null || to.isEmpty() || to.equalsIgnoreCase("support")) return true;
        if (twilioPhoneNumber == null) return true;
        String cleanSupport = twilioPhoneNumber.replaceAll("[^0-9]", "");
        String cleanTo = to.replaceAll("[^0-9]", "");
        return cleanTo.endsWith(cleanSupport);
    }

    private String emergencyResponse(String message) {
        // Uses proper Twilio Builder for Hangup
        return new VoiceResponse.Builder()
                .say(new Say.Builder("Fatal error: " + message).build())
                .hangup(new Hangup.Builder().build())
                .build().toXml();
    }

    private String enterQueue() {
        return new VoiceResponse.Builder()
                .say(new Say.Builder("All specialists are busy. Please hold.").build())
                .enqueue(new Enqueue.Builder("SupportQueue").build())
                .build().toXml();
    }

    private void logOrUpdateCallAttempt(String from, String callSid, String to) {
        try {
            String target = (to != null && (to.startsWith("+") || to.startsWith("client:"))) ? to : (from != null ? from : "WEB");
            if (callSid == null) return;

            supportCallRepository.findByCallSid(callSid).ifPresentOrElse(c -> {}, () -> {
                supportCallRepository.save(SupportCall.builder()
                        .callSid(callSid)
                        .callerNumber(target)
                        .status(SupportCall.CallStatus.MISSED)
                        .build());
            });
        } catch (Exception e) { log.warn("[DB] Call log error: {}", e.getMessage()); }
    }

    private void updateCallLog(String agentPhone, String duration, String callSid) {
        try {
            if (callSid == null) return;
            String suffix = (agentPhone != null) ? agentPhone.replaceAll("[^0-9]", "") : "";
            if (suffix.length() > 10) suffix = suffix.substring(suffix.length() - 10);
            
            final String finalSuffix = suffix;
            Optional<User> agentOpt = finalSuffix.isEmpty() ? Optional.empty() : userRepository.findByPhoneEndingWith(finalSuffix);

            supportCallRepository.findByCallSid(callSid).ifPresent(c -> {
                c.setAgentNumber(agentPhone);
                agentOpt.ifPresent(u -> c.setAgentName(u.getFullName()));
                c.setStatus(SupportCall.CallStatus.COMPLETED);
                c.setDuration(duration != null ? Integer.parseInt(duration) : 0);
                supportCallRepository.save(c);
            });
        } catch (Exception e) {}
    }

    private void checkQueueAndBridge(String agentPhone, String baseUrl) {
        try {
            // Using correct init signature for API Keys
            Twilio.init(apiKeySid, apiKeySecret, accountSid);
            ResourceSet<Member> members = Member.reader("SupportQueue").read();
            if (members.iterator().hasNext()) {
                String subSid = members.iterator().next().getCallSid();
                Call.creator(new PhoneNumber(agentPhone), new PhoneNumber(twilioPhoneNumber), new URI(baseUrl + "/api/voice/bridge-queue?OriginalCallSid=" + subSid))
                    .setStatusCallback(URI.create(baseUrl + "/api/voice/status-callback?OriginalCallSid=" + subSid))
                    .create();
            }
        } catch (Exception e) { log.error("[QUEUE] Bridge Failure: {}", e.getMessage()); }
    }

    private String dialDirect(String dest, String baseUrl) {
        String finalDest = dest;
        if (dest.startsWith("client:")) {
            String email = dest.replace("client:", "");
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent() && userOpt.get().getPhone() != null) {
                finalDest = userOpt.get().getPhone();
            }
        }

        Number number = new Number.Builder(formatPhoneNumber(finalDest))
                .statusCallback(baseUrl + "/api/voice/status-callback")
                .statusCallbackEvents(Arrays.asList(Number.Event.INITIATED, Number.Event.ANSWERED, Number.Event.COMPLETED))
                .build();
                
        return new VoiceResponse.Builder()
                .dial(new Dial.Builder().callerId(twilioPhoneNumber).number(number).build())
                .build().toXml();
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.length() == 10 && !cleaned.startsWith("+")) return "+91" + cleaned;
        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
    }
}
