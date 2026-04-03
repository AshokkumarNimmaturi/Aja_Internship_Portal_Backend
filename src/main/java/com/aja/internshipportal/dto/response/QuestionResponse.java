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

    // ✅ Client name field for elite interview intel tracking
    private String clientName;

    // ✅ The first proposed answer (used by reviewers)
    private String initialAnswer; 

    // ✅ NEW: The final, polished master answer approved by Aja Tutors
    private String officialAnswer;

    private String technologyName;
    private Question.Status status;
    private Question.Difficulty difficulty;
    private String tags;
    private boolean sample;
    private String submittedByName;
    private String reviewedByName;
    private String rejectionReason;
    private long answerCount;
    private LocalDateTime createdAt;
}
