// 5. File: src/main/java/com/aja/internshipportal/controller/BookmarkController.java

package com.aja.internshipportal.controller;

import com.aja.internshipportal.dto.response.QuestionResponse;
import com.aja.internshipportal.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping("/{questionId}")
    public ResponseEntity<Boolean> toggleBookmark(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookmarkService.toggleBookmark(userDetails.getUsername(), questionId));
    }

    @GetMapping
    public ResponseEntity<List<QuestionResponse>> getMyBookmarks(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookmarkService.getMyBookmarks(userDetails.getUsername()));
    }

    @GetMapping("/ids")
    public ResponseEntity<List<Long>> getMyBookmarkIds(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(bookmarkService.getMyBookmarkIds(userDetails.getUsername()));
    }
}
