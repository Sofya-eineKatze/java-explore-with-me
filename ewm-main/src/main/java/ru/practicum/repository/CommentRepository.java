package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.Comment;

import java.util.List;

public interface CommentRepository
        extends JpaRepository<Comment, Long> {


    List<Comment> findByEventIdOrderByCreatedAsc(Long eventId);


    List<Comment> findByAuthorIdOrderByCreatedAsc(Long userId);
}