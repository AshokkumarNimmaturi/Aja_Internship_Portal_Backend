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
    
    // ✅ ADDED: This will allow the "My Answers" page to show the question name
    private String questionTitle;
    
    private int upvoteCount;

    // Did the currently logged-in user upvote this answer
    private boolean upvotedByCurrentUser;

    private boolean accepted;
    private LocalDateTime createdAt;
}
