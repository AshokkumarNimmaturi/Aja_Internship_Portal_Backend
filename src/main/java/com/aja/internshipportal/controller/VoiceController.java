//package com.aja.internshipportal.controller;
//
//import com.aja.internshipportal.entity.SupportCall;
//import com.aja.internshipportal.entity.User;
//import com.aja.internshipportal.repository.SupportCallRepository;
//import com.aja.internshipportal.repository.UserRepository;
//import com.aja.internshipportal.service.UserService;
//import com.twilio.Twilio;
//import com.twilio.base.ResourceSet;
//import com.twilio.jwt.accesstoken.AccessToken;
//import com.twilio.jwt.accesstoken.VoiceGrant;
//import com.twilio.rest.api.v2010.account.Call;
//import com.twilio.rest.api.v2010.account.queue.Member;
//import com.twilio.twiml.VoiceResponse;
//import com.twilio.twiml.voice.*;
//import com.twilio.twiml.voice.Number;
//import com.twilio.type.PhoneNumber;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.*;
//
//import java.net.URI;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/voice")
//@RequiredArgsConstructor
//@Slf4j
//public class VoiceController {
//
//    private final UserRepository userRepository;
//    private final UserService userService;
//    private final SupportCallRepository supportCallRepository;
//
//    @Value("${twilio.account.sid}") private String accountSid;
//    @Value("${twilio.api.key.sid}") private String apiKeySid;
//    @Value("${twilio.api.key.secret}") private String apiKeySecret;
//    @Value("${twilio.twiml.app.sid}") private String twimlAppSid;
//    @Value("${twilio.phone.number}") private String twilioPhoneNumber;
//
//    @GetMapping("/availability")
//    public Map<String, Object> getAvailability() {
//        List<User> readyAgents = userRepository.findAvailableSupportAgents();
//        long onlineCount = userRepository.countOnlineAgents();
//        Map<String, Object> response = new HashMap<>();
//        response.put("readyCount", readyAgents.size());
//        response.put("status", !readyAgents.isEmpty() ? "AVAILABLE" : (onlineCount > 0 ? "BUSY" : "OFFLINE"));
//        return response;
//    }
//
//    @GetMapping("/token")
//    public String getAccessToken(@RequestParam String identity) {
//        VoiceGrant grant = new VoiceGrant();
//        grant.setOutgoingApplicationSid(twimlAppSid);
//        return new AccessToken.Builder(accountSid, apiKeySid, apiKeySecret).identity(identity).grant(grant).build().toJwt();
//    }
//
//    @PostMapping(value = "/twiml", produces = MediaType.APPLICATION_XML_VALUE)
//    @ResponseBody
//    public String getTwiML(@RequestParam(required = false) String To, @RequestParam(required = false) String From, @RequestParam(required = false) String CallSid, HttpServletRequest request) {
//        log.info("[VOICE] Incoming Call. SID: {}. To: {}", CallSid, To);
//        // Log attempt for ANY call (Incoming or Outbound Call-Back)
//        logCallAttempt(From, CallSid, To);
//        
//        if (To == null || To.equalsIgnoreCase("support") || To.equals(twilioPhoneNumber)) {
//            return startSupportHunt(0, From, request);
//        }
//        return dialDirect(To, request);
//    }
//
//    @PostMapping(value = "/hunt", produces = MediaType.APPLICATION_XML_VALUE)
//    @ResponseBody
//    public String supportHunt(@RequestParam int agentIndex, @RequestParam(required = false) String From, HttpServletRequest request) {
//        return startSupportHunt(agentIndex, From, request);
//    }
//
//    private String startSupportHunt(int index, String fromNumber, HttpServletRequest request) {
//        List<User> readyAgents = userRepository.findAvailableSupportAgents();
//        String baseUrl = getBaseUrl(request);
//        
//        if (!readyAgents.isEmpty() && index < readyAgents.size()) {
//            User agent = readyAgents.get(index);
//            String agentPhone = formatPhoneNumber(agent.getPhone());
//            log.info("[HUNT] Dialing agent: {}", agent.getEmail());
//            
//            // FIXED: Using Absolute URL for status callback
//            Number number = new Number.Builder(agentPhone)
//                    .statusCallback(baseUrl + "/api/voice/status-callback")
//                    .statusCallbackEvents(Arrays.asList(Number.Event.INITIATED, Number.Event.ANSWERED, Number.Event.COMPLETED))
//                    .build();
//            
//            // FIXED: Using Absolute URL for dial action
//            Dial dial = new Dial.Builder().callerId(twilioPhoneNumber).timeout(15).action(baseUrl + "/api/voice/hunt?agentIndex=" + (index + 1) + "&From=" + fromNumber).number(number).build();
//            return new VoiceResponse.Builder().say(new Say.Builder("Connecting you to a specialist.").build()).dial(dial).build().toXml();
//        }
//        if (userRepository.countOnlineAgents() > 0) return enterQueue();
//        return new VoiceResponse.Builder().say(new Say.Builder("All specialists are currently offline. Queuing for callback.").build()).enqueue(new Enqueue.Builder("SupportQueue").build()).build().toXml();
//    }
//
//    private String enterQueue() {
//        return new VoiceResponse.Builder().say(new Say.Builder("All specialists are busy. Please stay on the line.").build()).enqueue(new Enqueue.Builder("SupportQueue").build()).build().toXml();
//    }
//
//    @PostMapping(value = "/bridge-queue", produces = MediaType.APPLICATION_XML_VALUE)
//    @ResponseBody
//    public String bridgeToQueue(@RequestParam(name = "OriginalCallSid", required = false) String originalSid) {
//        log.info("[BRIDGE-TWIML] Bridging Agent to Original Call SID: {}", originalSid);
//        return new VoiceResponse.Builder().say(new Say.Builder("Connecting.").build()).dial(new Dial.Builder().queue(new com.twilio.twiml.voice.Queue.Builder("SupportQueue").build()).build()).build().toXml();
//    }
//
//    @PostMapping("/status-callback")
//    public void statusCallback(@RequestParam(required = false) String To, @RequestParam String CallStatus, @RequestParam(required = false) String CallSid, @RequestParam(required = false) String ParentCallSid, @RequestParam(name = "OriginalCallSid", required = false) String originalSid, @RequestParam(required = false) String CallDuration, HttpServletRequest request) {
//        String targetSid = (originalSid != null) ? originalSid : ((ParentCallSid != null) ? ParentCallSid : CallSid);
//        log.info("[CALLBACK] Status: {} for {}. Target SID: {}", CallStatus, To, targetSid);
//
//        if (To != null) {
//            boolean isAnswered = CallStatus.equalsIgnoreCase("answered") || CallStatus.equalsIgnoreCase("in-progress");
//            userService.setInCallStatus(To, isAnswered);
//
//            if (CallStatus.equalsIgnoreCase("answered")) {
//                userService.markCallAsAnswered(To, targetSid);
//            }
//            if (CallStatus.equalsIgnoreCase("completed")) {
//                updateCallLog(To, CallDuration, targetSid);
//                checkQueueAndBridge(To, request);
//            }
//        }
//    }
//
//    private void logCallAttempt(String from, String callSid, String to) {
//        // If 'to' is a number (Call-Back), we log the user number, else log the caller ('from')
//        String targetNumber = (to != null && to.startsWith("+")) ? to : (from != null ? from : "WEB");
//        supportCallRepository.save(SupportCall.builder()
//                .callSid(callSid)
//                .callerNumber(targetNumber)
//                .status(SupportCall.CallStatus.MISSED)
//                .build());
//    }
//
//    private void updateCallLog(String agentPhone, String duration, String callSid) {
//        String suffix = agentPhone.replaceAll("[^0-9]", "");
//        if (suffix.length() > 10) suffix = suffix.substring(suffix.length() - 10);
//        Optional<User> agentOpt = userRepository.findByPhoneEndingWith(suffix);
//
//        supportCallRepository.findByCallSid(callSid).ifPresentOrElse(c -> {
//            c.setAgentNumber(agentPhone);
//            agentOpt.ifPresent(u -> c.setAgentName(u.getFullName()));
//            c.setStatus(SupportCall.CallStatus.COMPLETED);
//            c.setDuration(duration != null ? Integer.parseInt(duration) : 0);
//            supportCallRepository.save(c);
//            log.info("[FINAL-SYNC] SID {} matched and closed by {}", callSid, c.getAgentName());
//        }, () -> {
//            log.warn("[FINAL-SYNC] SID {} match missed. Attempting Fuzzy Recovery.", callSid);
//            supportCallRepository.findTop20ByOrderByTimestampDesc().stream()
//                .filter(call -> call.getStatus() == SupportCall.CallStatus.MISSED)
//                .findFirst()
//                .ifPresent(call -> {
//                    call.setAgentNumber(agentPhone);
//                    agentOpt.ifPresent(u -> call.setAgentName(u.getFullName()));
//                    call.setStatus(SupportCall.CallStatus.COMPLETED);
//                    call.setDuration(duration != null ? Integer.parseInt(duration) : 0);
//                    supportCallRepository.save(call);
//                    log.info("[FALLBACK] SID {} recovered using Fuzzy Match to {}", callSid, call.getAgentName());
//                });
//        });
//    }
//
//    private void checkQueueAndBridge(String agentPhone, HttpServletRequest request) {
//        Twilio.init(accountSid, apiKeySecret);
//        try {
//            ResourceSet<Member> members = Member.reader("SupportQueue").read();
//            if (members.iterator().hasNext()) {
//                String subSid = members.iterator().next().getCallSid();
//                String baseUrl = getBaseUrl(request);
//
//                log.info("[BRIDGE-GEN] Linking Agent {} to Subscriber {} via {}", agentPhone, subSid, baseUrl);
//                
//                Call.creator(new PhoneNumber(agentPhone), new PhoneNumber(twilioPhoneNumber), new URI(baseUrl + "/api/voice/bridge-queue?OriginalCallSid=" + subSid))
//                    .setStatusCallback(URI.create(baseUrl + "/api/voice/status-callback?OriginalCallSid=" + subSid)).create();
//            }
//        } catch (Exception e) { log.error("Bridge Error: " + e.getMessage()); }
//    }
//
//    private String dialDirect(String dest, HttpServletRequest request) {
//        String baseUrl = getBaseUrl(request);
//        Number number = new Number.Builder(formatPhoneNumber(dest))
//                .statusCallback(baseUrl + "/api/voice/status-callback")
//                .statusCallbackEvents(Arrays.asList(Number.Event.INITIATED, Number.Event.ANSWERED, Number.Event.COMPLETED))
//                .build();
//                
//        return new VoiceResponse.Builder()
//                .dial(new Dial.Builder().callerId(twilioPhoneNumber).number(number).build())
//                .build().toXml();
//    }
//
//    private String getBaseUrl(HttpServletRequest request) {
//        String domain = request.getServerName();
//        String scheme = request.getScheme();
//        String proto = request.getHeader("X-Forwarded-Proto") != null ? request.getHeader("X-Forwarded-Proto") : scheme;
//        String baseUrl = proto + "://" + domain;
//        if (domain.contains("localhost") || domain.contains("127.0.0.1")) {
//            baseUrl += ":" + request.getServerPort();
//        }
//        return baseUrl;
//    }
//
//    private String formatPhoneNumber(String phone) {
//        if (phone == null) return "";
//        String cleaned = phone.replaceAll("[^0-9+]", "");
//        if (cleaned.length() == 10 && !cleaned.startsWith("+")) return "+91" + cleaned;
//        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
//    }
//}
//
//package com.aja.internshipportal.controller;
//
//import com.aja.internshipportal.entity.SupportCall;
//import com.aja.internshipportal.entity.User;
//import com.aja.internshipportal.repository.SupportCallRepository;
//import com.aja.internshipportal.repository.UserRepository;
//import com.aja.internshipportal.service.UserService;
//import com.twilio.Twilio;
//import com.twilio.base.ResourceSet;
//import com.twilio.jwt.accesstoken.AccessToken;
//import com.twilio.jwt.accesstoken.VoiceGrant;
//import com.twilio.rest.api.v2010.account.Call;
//import com.twilio.rest.api.v2010.account.queue.Member;
//import com.twilio.twiml.VoiceResponse;
//import com.twilio.twiml.voice.*;
//import com.twilio.twiml.voice.Number;
//import com.twilio.type.PhoneNumber;
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.MediaType;
//import org.springframework.web.bind.annotation.*;
//
//import java.net.URI;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//@RestController
//@RequestMapping("/api/voice")
//@RequiredArgsConstructor
//@Slf4j
//public class VoiceController {
//
//    private final UserRepository userRepository;
//    private final UserService userService;
//    private final SupportCallRepository supportCallRepository;
//
//    @Value("${twilio.account.sid}") private String accountSid;
//    @Value("${twilio.api.key.sid}") private String apiKeySid;
//    @Value("${twilio.api.key.secret}") private String apiKeySecret;
//    @Value("${twilio.twiml.app.sid}") private String twimlAppSid;
//    @Value("${twilio.phone.number}") private String twilioPhoneNumber;
//
//    @GetMapping("/availability")
//    public Map<String, Object> getAvailability() {
//        List<User> readyAgents = userRepository.findAvailableSupportAgents();
//        long onlineCount = userRepository.countOnlineAgents();
//        Map<String, Object> response = new HashMap<>();
//        response.put("readyCount", readyAgents.size());
//        response.put("status", !readyAgents.isEmpty() ? "AVAILABLE" : (onlineCount > 0 ? "BUSY" : "OFFLINE"));
//        return response;
//    }
//
//    @GetMapping("/token")
//    public String getAccessToken(@RequestParam String identity) {
//        VoiceGrant grant = new VoiceGrant();
//        grant.setOutgoingApplicationSid(twimlAppSid);
//        return new AccessToken.Builder(accountSid, apiKeySid, apiKeySecret).identity(identity).grant(grant).build().toJwt();
//    }
//
//    @PostMapping(value = "/twiml", produces = MediaType.APPLICATION_XML_VALUE)
//    @ResponseBody
//    public String getTwiML(@RequestParam(required = false) String To, @RequestParam(required = false) String From, @RequestParam(required = false) String CallSid, HttpServletRequest request) {
//        log.info("[VOICE] Incoming Call. SID: {}. To: {}", CallSid, To);
//        
//        // Log attempt automatically
//        logCallAttempt(From, CallSid, To);
//        
//        if (To == null || To.equalsIgnoreCase("support") || To.equals(twilioPhoneNumber)) {
//            return startSupportHunt(0, From, request);
//        }
//        return dialDirect(To, request);
//    }
//
//    @PostMapping(value = "/hunt", produces = MediaType.APPLICATION_XML_VALUE)
//    @ResponseBody
//    public String supportHunt(@RequestParam int agentIndex, @RequestParam(required = false) String From, HttpServletRequest request) {
//        return startSupportHunt(agentIndex, From, request);
//    }
//
//    private String startSupportHunt(int index, String fromNumber, HttpServletRequest request) {
//        List<User> readyAgents = userRepository.findAvailableSupportAgents();
//        String baseUrl = getBaseUrl(request);
//        
//        if (!readyAgents.isEmpty() && index < readyAgents.size()) {
//            User agent = readyAgents.get(index);
//            String agentPhone = formatPhoneNumber(agent.getPhone());
//            log.info("[HUNT] Dialing agent: {}", agent.getEmail());
//            
//            Number number = new Number.Builder(agentPhone)
//                    .statusCallback(baseUrl + "/api/voice/status-callback")
//                    .statusCallbackEvents(Arrays.asList(Number.Event.INITIATED, Number.Event.ANSWERED, Number.Event.COMPLETED))
//                    .build();
//            
//            Dial dial = new Dial.Builder().callerId(twilioPhoneNumber).timeout(15).action(baseUrl + "/api/voice/hunt?agentIndex=" + (index + 1) + "&From=" + fromNumber).number(number).build();
//            return new VoiceResponse.Builder().say(new Say.Builder("Connecting you to a specialist.").build()).dial(dial).build().toXml();
//        }
//        if (userRepository.countOnlineAgents() > 0) return enterQueue();
//        return new VoiceResponse.Builder().say(new Say.Builder("All specialists are currently offline. Queuing for callback.").build()).enqueue(new Enqueue.Builder("SupportQueue").build()).build().toXml();
//    }
//
//    private String enterQueue() {
//        return new VoiceResponse.Builder().say(new Say.Builder("All specialists are busy. Please stay on the line.").build()).enqueue(new Enqueue.Builder("SupportQueue").build()).build().toXml();
//    }
//
//    @PostMapping(value = "/bridge-queue", produces = MediaType.APPLICATION_XML_VALUE)
//    @ResponseBody
//    public String bridgeToQueue(@RequestParam(name = "OriginalCallSid", required = false) String originalSid) {
//        log.info("[BRIDGE-TWIML] Bridging Agent to Original Call SID: {}", originalSid);
//        return new VoiceResponse.Builder().say(new Say.Builder("Connecting.").build()).dial(new Dial.Builder().queue(new com.twilio.twiml.voice.Queue.Builder("SupportQueue").build()).build()).build().toXml();
//    }
//
//    @PostMapping("/status-callback")
//    public void statusCallback(@RequestParam(required = false) String To, @RequestParam String CallStatus, @RequestParam(required = false) String CallSid, @RequestParam(required = false) String ParentCallSid, @RequestParam(name = "OriginalCallSid", required = false) String originalSid, @RequestParam(required = false) String CallDuration, HttpServletRequest request) {
//        String targetSid = (originalSid != null) ? originalSid : ((ParentCallSid != null) ? ParentCallSid : CallSid);
//        log.info("[CALLBACK] Status: {} for {}. Target SID: {}", CallStatus, To, targetSid);
//
//        if (To != null) {
//            boolean isAnswered = CallStatus.equalsIgnoreCase("answered") || CallStatus.equalsIgnoreCase("in-progress");
//            userService.setInCallStatus(To, isAnswered);
//
//            if (CallStatus.equalsIgnoreCase("answered")) {
//                userService.markCallAsAnswered(To, targetSid);
//            }
//            if (CallStatus.equalsIgnoreCase("completed")) {
//                updateCallLog(To, CallDuration, targetSid);
//                checkQueueAndBridge(To, request);
//            }
//        }
//    }
//
//    private void logCallAttempt(String from, String callSid, String to) {
//        // Log the real identity (client email or phone number)
//        String targetNumber = (to != null && (to.startsWith("+") || to.startsWith("client:"))) ? to : (from != null ? from : "WEB");
//        supportCallRepository.save(SupportCall.builder()
//                .callSid(callSid)
//                .callerNumber(targetNumber)
//                .status(SupportCall.CallStatus.MISSED)
//                .build());
//    }
//
//    private void updateCallLog(String agentPhone, String duration, String callSid) {
//        // Resolve agent by phone suffix
//        String suffix = agentPhone.replaceAll("[^0-9]", "");
//        if (suffix.length() > 10) suffix = suffix.substring(suffix.length() - 10);
//        Optional<User> agentOpt = userRepository.findByPhoneEndingWith(suffix);
//
//        supportCallRepository.findByCallSid(callSid).ifPresentOrElse(c -> {
//            c.setAgentNumber(agentPhone);
//            agentOpt.ifPresent(u -> c.setAgentName(u.getFullName()));
//            c.setStatus(SupportCall.CallStatus.COMPLETED);
//            c.setDuration(duration != null ? Integer.parseInt(duration) : 0);
//            supportCallRepository.save(c);
//            log.info("[FINAL-SYNC] SID {} matched and closed", callSid);
//        }, () -> {
//            log.warn("[FINAL-SYNC] SID {} match missed. Attempting Fuzzy Recovery.", callSid);
//            supportCallRepository.findTop20ByOrderByTimestampDesc().stream()
//                .filter(call -> call.getStatus() == SupportCall.CallStatus.MISSED)
//                .findFirst()
//                .ifPresent(call -> {
//                    call.setAgentNumber(agentPhone);
//                    agentOpt.ifPresent(u -> call.setAgentName(u.getFullName()));
//                    call.setStatus(SupportCall.CallStatus.COMPLETED);
//                    call.setDuration(duration != null ? Integer.parseInt(duration) : 0);
//                    supportCallRepository.save(call);
//                });
//        });
//    }
//
//    private void checkQueueAndBridge(String agentPhone, HttpServletRequest request) {
//        Twilio.init(accountSid, apiKeySecret);
//        try {
//            ResourceSet<Member> members = Member.reader("SupportQueue").read();
//            if (members.iterator().hasNext()) {
//                String subSid = members.iterator().next().getCallSid();
//                String baseUrl = getBaseUrl(request);
//                Call.creator(new PhoneNumber(agentPhone), new PhoneNumber(twilioPhoneNumber), new URI(baseUrl + "/api/voice/bridge-queue?OriginalCallSid=" + subSid))
//                    .setStatusCallback(URI.create(baseUrl + "/api/voice/status-callback?OriginalCallSid=" + subSid)).create();
//            }
//        } catch (Exception e) { log.error("Bridge Error: " + e.getMessage()); }
//    }
//
//    private String dialDirect(String dest, HttpServletRequest request) {
//        String baseUrl = getBaseUrl(request);
//        Dial.Builder dialBuilder = new Dial.Builder().callerId(twilioPhoneNumber);
//
//        // FIX: Check if we are calling a browser (Client) or a phone number
//        if (dest.startsWith("client:")) {
//            String clientIdentity = dest.replace("client:", "");
//            Client client = new Client.Builder(clientIdentity)
//                    .statusCallback(baseUrl + "/api/voice/status-callback")
//                    .statusCallbackEvents(Arrays.asList(Client.Event.INITIATED, Client.Event.ANSWERED, Client.Event.COMPLETED))
//                    .build();
//            return new VoiceResponse.Builder().dial(dialBuilder.client(client).build()).build().toXml();
//        } else {
//            Number number = new Number.Builder(formatPhoneNumber(dest))
//                    .statusCallback(baseUrl + "/api/voice/status-callback")
//                    .statusCallbackEvents(Arrays.asList(Number.Event.INITIATED, Number.Event.ANSWERED, Number.Event.COMPLETED))
//                    .build();
//            return new VoiceResponse.Builder().dial(dialBuilder.number(number).build()).build().toXml();
//        }
//    }
//
//    private String getBaseUrl(HttpServletRequest request) {
//        // FIX: Check for ngrok public host headers
//        String host = request.getHeader("X-Forwarded-Host");
//        if (host == null) host = request.getServerName();
//        
//        String proto = request.getHeader("X-Forwarded-Proto");
//        if (proto == null) proto = request.getScheme();
//        
//        String baseUrl = proto + "://" + host;
//        
//        // Append port only if it's actually localhost and not a known standard proxy port
//        if ((host.contains("localhost") || host.contains("127.0.0.1")) && request.getServerPort() != 80 && request.getServerPort() != 443) {
//            baseUrl += ":" + request.getServerPort();
//        }
//        return baseUrl;
//    }
//
//    private String formatPhoneNumber(String phone) {
//        if (phone == null) return "";
//        String cleaned = phone.replaceAll("[^0-9+]", "");
//        if (cleaned.length() == 10 && !cleaned.startsWith("+")) return "+91" + cleaned;
//        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
//    }
//}

package com.aja.internshipportal.controller;

import com.aja.internshipportal.entity.SupportCall;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.repository.SupportCallRepository;
import com.aja.internshipportal.repository.UserRepository;
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
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final SupportCallRepository supportCallRepository;

    @Value("${twilio.account.sid}") private String accountSid;
    @Value("${twilio.api.key.sid}") private String apiKeySid;
    @Value("${twilio.api.key.secret}") private String apiKeySecret;
    @Value("${twilio.twiml.app.sid}") private String twimlAppSid;
    @Value("${twilio.phone.number}") private String twilioPhoneNumber;

    @GetMapping("/availability")
    public Map<String, Object> getAvailability() {
        List<User> readyAgents = userRepository.findAvailableSupportAgents();
        long onlineCount = userRepository.countOnlineAgents();
        Map<String, Object> response = new HashMap<>();
        response.put("readyCount", readyAgents.size());
        response.put("status", !readyAgents.isEmpty() ? "AVAILABLE" : (onlineCount > 0 ? "BUSY" : "OFFLINE"));
        return response;
    }

    @GetMapping("/token")
    public String getAccessToken(@RequestParam String identity) {
        VoiceGrant grant = new VoiceGrant();
        grant.setOutgoingApplicationSid(twimlAppSid);
        return new AccessToken.Builder(accountSid, apiKeySid, apiKeySecret).identity(identity).grant(grant).build().toJwt();
    }

    @PostMapping(value = "/twiml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String getTwiML(@RequestParam(required = false) String To, @RequestParam(required = false) String From, @RequestParam(required = false) String CallSid, HttpServletRequest request) {
        log.info("[VOICE] Handshake SID: {}. Target: {}", CallSid, To);
        
        // Smart Log: Link this outbound CallSid to the existing UNRESOLVED row
        logOrUpdateCallAttempt(From, CallSid, To);
        
        if (To == null || To.equalsIgnoreCase("support") || To.equals(twilioPhoneNumber)) {
            return startSupportHunt(0, From, request);
        }
        return dialDirect(To, request);
    }

    @PostMapping(value = "/hunt", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String supportHunt(@RequestParam int agentIndex, @RequestParam(required = false) String From, HttpServletRequest request) {
        return startSupportHunt(agentIndex, From, request);
    }

    private String startSupportHunt(int index, String fromNumber, HttpServletRequest request) {
        List<User> readyAgents = userRepository.findAvailableSupportAgents();
        String baseUrl = getBaseUrl(request);
        
        if (!readyAgents.isEmpty() && index < readyAgents.size()) {
            User agent = readyAgents.get(index);
            String agentPhone = formatPhoneNumber(agent.getPhone());
            
            Number number = new Number.Builder(agentPhone)
                    .statusCallback(baseUrl + "/api/voice/status-callback")
                    .statusCallbackEvents(Arrays.asList(Number.Event.INITIATED, Number.Event.ANSWERED, Number.Event.COMPLETED))
                    .build();
            
            Dial dial = new Dial.Builder().callerId(twilioPhoneNumber).timeout(15).action(baseUrl + "/api/voice/hunt?agentIndex=" + (index + 1) + "&From=" + fromNumber).number(number).build();
            return new VoiceResponse.Builder().say(new Say.Builder("Connecting you to a specialist.").build()).dial(dial).build().toXml();
        }
        if (userRepository.countOnlineAgents() > 0) return enterQueue();
        return new VoiceResponse.Builder().say(new Say.Builder("All specialists are currently offline. Queuing for callback.").build()).enqueue(new Enqueue.Builder("SupportQueue").build()).build().toXml();
    } 

    private String enterQueue() {
        return new VoiceResponse.Builder().say(new Say.Builder("All specialists are busy. Please stay on the line.").build()).enqueue(new Enqueue.Builder("SupportQueue").build()).build().toXml();
    }

    @PostMapping(value = "/bridge-queue", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String bridgeToQueue(@RequestParam(name = "OriginalCallSid", required = false) String originalSid) {
        return new VoiceResponse.Builder().say(new Say.Builder("Connecting.").build()).dial(new Dial.Builder().queue(new com.twilio.twiml.voice.Queue.Builder("SupportQueue").build()).build()).build().toXml();
    }

    @PostMapping("/status-callback")
    public void statusCallback(@RequestParam(required = false) String To, @RequestParam String CallStatus, @RequestParam(required = false) String CallSid, @RequestParam(required = false) String ParentCallSid, @RequestParam(name = "OriginalCallSid", required = false) String originalSid, @RequestParam(required = false) String CallDuration, HttpServletRequest request) {
        String targetSid = (originalSid != null) ? originalSid : ((ParentCallSid != null) ? ParentCallSid : CallSid);
        log.info("[CALLBACK] Status: {} for {}. Target SID: {}", CallStatus, To, targetSid);

        if (To != null || targetSid != null) {
            boolean isAnswered = CallStatus.equalsIgnoreCase("answered") || CallStatus.equalsIgnoreCase("in-progress");
            
            if (To != null && To.startsWith("+")) {
                userService.setInCallStatus(To, isAnswered);
                if (CallStatus.equalsIgnoreCase("answered")) {
                    userService.markCallAsAnswered(To, targetSid);
                }
            }
            
            if (CallStatus.equalsIgnoreCase("completed")) {
                updateCallLog(To, CallDuration, targetSid);
                if (To != null && To.startsWith("+")) checkQueueAndBridge(To, request);
            }
        }
    }

    private void logOrUpdateCallAttempt(String from, String callSid, String to) {
        // REDIRECTION LOGIC: If Admin is calling a client email...
        if (to != null && to.startsWith("client:")) {
            Optional<SupportCall> recentMissed = supportCallRepository.findTop20ByOrderByTimestampDesc().stream()
                .filter(c -> c.getCallerNumber().equals(to))
                .filter(c -> c.getStatus() == SupportCall.CallStatus.MISSED)
                .findFirst();

            if (recentMissed.isPresent()) {
                SupportCall call = recentMissed.get();
                call.setCallSid(callSid); // LINK TO NEW CALL
                supportCallRepository.save(call);
                log.info("[SYNC] Linked Call {} to existing record for {}", callSid, to);
                return;
            }
        }

        // New Incoming Call Log
        String targetNumber = (to != null && (to.startsWith("+") || to.startsWith("client:"))) ? to : (from != null ? from : "WEB");
        supportCallRepository.save(SupportCall.builder()
                .callSid(callSid)
                .callerNumber(targetNumber)
                .status(SupportCall.CallStatus.MISSED)
                .build());
    }

    private void updateCallLog(String agentPhone, String duration, String callSid) {
        String suffix = (agentPhone != null) ? agentPhone.replaceAll("[^0-9]", "") : "";
        if (suffix.length() > 10) suffix = suffix.substring(suffix.length() - 10);
        final String finalSuffix = suffix;
        
        Optional<User> agentOpt = finalSuffix.isEmpty() ? Optional.empty() : userRepository.findByPhoneEndingWith(finalSuffix);

        supportCallRepository.findByCallSid(callSid).ifPresentOrElse(c -> {
            c.setAgentNumber(agentPhone);
            agentOpt.ifPresent(u -> c.setAgentName(u.getFullName()));
            c.setStatus(SupportCall.CallStatus.COMPLETED);
            c.setDuration(duration != null ? Integer.parseInt(duration) : 0);
            supportCallRepository.save(c);
        }, () -> {
            log.warn("[SYNC] SID {} logic recovery triggered.", callSid);
            supportCallRepository.findTop20ByOrderByTimestampDesc().stream()
                .filter(call -> call.getStatus() == SupportCall.CallStatus.MISSED)
                .findFirst()
                .ifPresent(call -> {
                    call.setStatus(SupportCall.CallStatus.COMPLETED);
                    call.setDuration(duration != null ? Integer.parseInt(duration) : 0);
                    supportCallRepository.save(call);
                });
        });
    }

    private void checkQueueAndBridge(String agentPhone, HttpServletRequest request) {
        Twilio.init(accountSid, apiKeySecret);
        try {
            ResourceSet<Member> members = Member.reader("SupportQueue").read();
            if (members.iterator().hasNext()) {
                String subSid = members.iterator().next().getCallSid();
                String baseUrl = getBaseUrl(request);
                Call.creator(new PhoneNumber(agentPhone), new PhoneNumber(twilioPhoneNumber), new URI(baseUrl + "/api/voice/bridge-queue?OriginalCallSid=" + subSid))
                    .setStatusCallback(URI.create(baseUrl + "/api/voice/status-callback?OriginalCallSid=" + subSid)).create();
            }
        } catch (Exception e) { log.error("Bridge Error: " + e.getMessage()); }
    }

    private String dialDirect(String dest, HttpServletRequest request) {
        String baseUrl = getBaseUrl(request);
        String finalDest = dest;

        // REDIRECT client:email TO ACTUAL PHONE NUMBER
        if (dest.startsWith("client:")) {
            String email = dest.replace("client:", "");
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent() && userOpt.get().getPhone() != null) {
                finalDest = userOpt.get().getPhone();
                log.info("[REDIRECT] Routing browser call for {} to physical phone {}", email, finalDest);
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

    private String getBaseUrl(HttpServletRequest request) {
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) host = request.getServerName();
        
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null) proto = request.getScheme();
        
        if (host.contains("ngrok") || host.contains("app")) proto = "https";
        
        String baseUrl = proto + "://" + host;
        
        if ((host.contains("localhost") || host.contains("127.0.0.1")) && request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }
        return baseUrl;
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null) return "";
        String cleaned = phone.replaceAll("[^0-9+]", "");
        if (cleaned.length() == 10 && !cleaned.startsWith("+")) return "+91" + cleaned;
        return cleaned.startsWith("+") ? cleaned : "+" + cleaned;
    }
}
