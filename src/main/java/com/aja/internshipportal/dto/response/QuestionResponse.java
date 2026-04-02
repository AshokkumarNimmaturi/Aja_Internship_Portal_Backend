//package com.aja.internshipportal.dto.response;
//
//import java.time.LocalDateTime;
//
//import com.aja.internshipportal.entity.Question;
//
//import lombok.Builder;
//import lombok.Getter;
//import lombok.Setter;
//
//@Getter
//@Setter
//@Builder
//public class QuestionResponse {
//
//    private Long id;
//    private String title;
//    private String content;
//
//    // ✅ ADDED: This will allow the reviewer to see the first proposed answer
//    private String initialAnswer; 
//
//    private String technologyName;
//    private Question.Status status;
//    private Question.Difficulty difficulty;
//    private String tags;
//    private boolean sample;
//    
//    //Submitted by info
//    private String submittedByName;
//    
//    // Review info
//    private String reviewedByName;
//    private String rejectionReason;
//    
//    // Answer Count - shown on question list card
//    private long answerCount;
//    
//    private LocalDateTime createdAt;
//}


// 3. File: src/main/java/com/aja/internshipportal/dto/response/QuestionResponse.java

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

    // ✅ ADDED: Client name field
    private String clientName;

    private String initialAnswer; 
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
