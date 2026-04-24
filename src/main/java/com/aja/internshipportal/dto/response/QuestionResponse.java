package com.aja.internshipportal.dto.response;

import java.time.LocalDateTime;
import com.aja.internshipportal.entity.Question;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class QuestionResponse {

    private Long id;
    private String title;
    private String content;

    // ✅ Client tracking
    private String clientName;

    private String initialAnswer; 
    private String technologyName;
    private Question.Status status;
    private Question.Difficulty difficulty;
    private String tags;
    private boolean sample;
    private String submittedByName;
    
    // ✅ ADDED: Unique identifier for grouping
    private String submittedByEmail;

    private String reviewedByName;
    private String rejectionReason;
    private long answerCount;
    private LocalDateTime createdAt;
}
