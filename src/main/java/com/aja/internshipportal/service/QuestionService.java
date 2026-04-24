//package com.aja.internshipportal.service;
//
//import java.util.List;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import com.aja.internshipportal.dto.request.QuestionRequest;
//import com.aja.internshipportal.dto.request.ReviewQuestionRequest;
//import com.aja.internshipportal.dto.response.QuestionResponse;
//
//public interface QuestionService {
//
//	QuestionResponse submitQuestion(String email, QuestionRequest request);
//	
//    QuestionResponse reviewQuestion(Long id, String email, ReviewQuestionRequest request);
//    
//    // ✅ ADDED: Update an existing question
//    QuestionResponse updateQuestion(Long id, String email, QuestionRequest request);
//
//    Page<QuestionResponse> getQuestions(Long technologyId, String keyword, Pageable pageable);
//    
//    QuestionResponse getQuestionById(Long id);
//    
//    List<QuestionResponse> getSampleQuestions(Long technologyId);
//    
//    List<QuestionResponse> getMyQuestions(String email);
//    
//    Page<QuestionResponse> getPendingQuestions(Pageable pageable);
//}

package com.aja.internshipportal.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.aja.internshipportal.dto.request.QuestionRequest;
import com.aja.internshipportal.dto.request.ReviewQuestionRequest;
import com.aja.internshipportal.dto.response.QuestionResponse;
import com.aja.internshipportal.entity.Question;

public interface QuestionService {
    QuestionResponse submitQuestion(String email, QuestionRequest request);
    QuestionResponse reviewQuestion(Long id, String email, ReviewQuestionRequest request);
    QuestionResponse getQuestionById(Long id);
    Page<QuestionResponse> getQuestions(Long tid, String k, Pageable p);
    List<QuestionResponse> getSampleQuestions(Long tid);
    List<QuestionResponse> getMyQuestions(String e);
    Page<QuestionResponse> getPendingQuestions(Pageable p);
    QuestionResponse updateQuestion(Long id, String email, QuestionRequest request);
    
    // ✅ ADDED: This will resolve the @Override error in your implementation
    QuestionResponse mapToQuestionResponse(Question question);
    List<QuestionResponse> getRecentQuestions(int limit);
}
