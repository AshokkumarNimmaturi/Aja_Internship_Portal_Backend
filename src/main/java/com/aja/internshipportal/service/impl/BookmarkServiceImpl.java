// 4. File: src/main/java/com/aja/internshipportal/service/impl/BookmarkServiceImpl.java

package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.dto.response.QuestionResponse;
import com.aja.internshipportal.entity.Bookmark;
import com.aja.internshipportal.entity.Question;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.BookmarkRepository;
import com.aja.internshipportal.repository.QuestionRepository;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public boolean toggleBookmark(String email, Long questionId) {
        User user = getUserByEmail(email);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question not found"));

        Optional<Bookmark> existing = bookmarkRepository.findByUserAndQuestion(user, question);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            return false;
        } else {
            bookmarkRepository.save(Bookmark.builder().user(user).question(question).build());
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getMyBookmarks(String email) {
        User user = getUserByEmail(email);
        return bookmarkRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(bookmark -> mapToQuestionResponse(bookmark.getQuestion()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMyBookmarkIds(String email) {
        User user = getUserByEmail(email);
        return bookmarkRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(b -> b.getQuestion().getId())
                .collect(Collectors.toList());
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    // Reuse your existing mapping logic from QuestionServiceImpl
    private QuestionResponse mapToQuestionResponse(Question q) {
        return QuestionResponse.builder()
                .id(q.getId())
                .title(q.getTitle())
                .content(q.getContent())
                .technologyName(q.getTechnology().getName())
                .status(q.getStatus())
                .difficulty(q.getDifficulty())
                .createdAt(q.getCreatedAt())
                .build();
    }
}
