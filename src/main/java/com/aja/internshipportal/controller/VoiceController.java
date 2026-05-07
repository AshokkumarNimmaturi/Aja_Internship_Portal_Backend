package com.aja.internshipportal.controller;

import com.aja.internshipportal.service.VoiceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
@Slf4j
public class VoiceController {

    private final VoiceService voiceService;

    @GetMapping("/availability")
    public Map<String, Object> getAvailability() {
        return voiceService.getAvailability();
    }

    @GetMapping("/token")
    public String getAccessToken(@RequestParam String identity) {
        return voiceService.getAccessToken(identity);
    }

    @PostMapping(value = "/twiml", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String getTwiML(@RequestParam(required = false) String To, 
                          @RequestParam(required = false) String From, 
                          @RequestParam(required = false) String CallSid, 
                          HttpServletRequest request) {
        return voiceService.handleTwiML(To, From, CallSid, getBaseUrl(request));
    }

    @PostMapping(value = "/hunt", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String supportHunt(@RequestParam int agentIndex, 
                             @RequestParam(required = false) String From, 
                             HttpServletRequest request) {
        return voiceService.handleHunt(agentIndex, From, getBaseUrl(request));
    }

    @PostMapping(value = "/bridge-queue", produces = MediaType.APPLICATION_XML_VALUE)
    @ResponseBody
    public String bridgeToQueue(@RequestParam(name = "OriginalCallSid", required = false) String originalSid) {
        return voiceService.handleBridgeToQueue(originalSid);
    }

    @PostMapping("/status-callback")
    public void statusCallback(@RequestParam(required = false) String To, 
                              @RequestParam String CallStatus, 
                              @RequestParam(required = false) String CallSid, 
                              @RequestParam(required = false) String ParentCallSid, 
                              @RequestParam(name = "OriginalCallSid", required = false) String originalSid, 
                              @RequestParam(required = false) String CallDuration, 
                              HttpServletRequest request) {
        voiceService.handleStatusCallback(To, CallStatus, CallSid, ParentCallSid, originalSid, CallDuration, getBaseUrl(request));
    }

    private String getBaseUrl(HttpServletRequest request) {
        String host = request.getHeader("X-Forwarded-Host");
        if (host == null) host = request.getServerName();
        
        String proto = request.getHeader("X-Forwarded-Proto");
        if (proto == null) proto = request.getScheme();
        
        if (host.contains("ngrok") || host.contains("app")) proto = "https";
        
        String baseUrl = proto + "://" + host;
        
        if ((host.contains("localhost") || host.contains("127.0.0.1")) && 
             request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }
        return baseUrl;
    }
}
