package com.aja.internshipportal.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class AnswerResponse {

    private Long id;
    private String content;
    private String authorName;
    
    // ✅ ADDED: Required for the "View Discussion" link to work
    private Long questionId;
    
    private String questionTitle;
    private int upvoteCount;
    private boolean upvotedByCurrentUser;
    private boolean accepted;
    private LocalDateTime createdAt;
}
