// 2. File: src/main/java/com/aja/internshipportal/repository/BookmarkRepository.java

package com.aja.internshipportal.repository;

import com.aja.internshipportal.entity.Bookmark;
import com.aja.internshipportal.entity.Question;
import com.aja.internshipportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUserOrderByCreatedAtDesc(User user);
    Optional<Bookmark> findByUserAndQuestion(User user, Question question);
}
