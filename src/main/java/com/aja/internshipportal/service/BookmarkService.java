// 3. File: src/main/java/com/aja/internshipportal/service/BookmarkService.java

package com.aja.internshipportal.service;

import com.aja.internshipportal.dto.response.QuestionResponse;
import java.util.List;

public interface BookmarkService {
    // Returns true if bookmarked, false if removed
    boolean toggleBookmark(String email, Long questionId);
    
    // Get all bookmarked questions for the user
    List<QuestionResponse> getMyBookmarks(String email);
    
    // List of question IDs the user has bookmarked
    List<Long> getMyBookmarkIds(String email);
}
