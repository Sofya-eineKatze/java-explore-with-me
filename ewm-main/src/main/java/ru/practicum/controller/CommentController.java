package ru.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CommentController {

    private final CommentService commentService;


    @PostMapping("/users/{userId}/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto addComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @Valid @RequestBody NewCommentDto dto) {

        log.info("Add comment by user {} for event {}", userId, eventId);

        return commentService.addComment(
                userId,
                eventId,
                dto
        );
    }


    @GetMapping("/events/{eventId}/comments")
    public List<CommentDto> getEventComments(
            @PathVariable Long eventId) {

        log.info("Get comments for event {}", eventId);

        return commentService.getEventComments(eventId);
    }


    @GetMapping("/users/{userId}/comments")
    public List<CommentDto> getUserComments(
            @PathVariable Long userId) {

        log.info("Get comments for user {}", userId);

        return commentService.getUserComments(userId);
    }


    @DeleteMapping("/users/{userId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {

        log.info("Delete comment {} by user {}", commentId, userId);

        commentService.deleteComment(
                userId,
                commentId
        );
    }
}