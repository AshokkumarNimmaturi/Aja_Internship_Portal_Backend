package com.aja.internshipportal.service;

import java.util.Map;

public interface VoiceService {
    Map<String, Object> getAvailability();
    
    String getAccessToken(String identity);
    
    String handleTwiML(String to, String from, String callSid, String baseUrl);
    
    String handleHunt(int agentIndex, String from, String baseUrl);
    
    String handleBridgeToQueue(String originalSid);
    
    void handleStatusCallback(String to, String callStatus, String callSid, 
                             String parentCallSid, String originalSid, 
                             String callDuration, String baseUrl);
}
