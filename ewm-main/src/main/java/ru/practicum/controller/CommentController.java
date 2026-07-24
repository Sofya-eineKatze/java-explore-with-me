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
@RequestMapping("/users/{userId}/comments")
public class CommentController {

    private final CommentService commentService;


    @PostMapping("/events/{eventId}")
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


    @GetMapping("/events/{eventId}")
    public List<CommentDto> getEventComments(
            @PathVariable Long userId,
            @PathVariable Long eventId) {

        log.info("Get comments for event {} by user {}", eventId, userId);

        return commentService.getEventComments(eventId);
    }


    @GetMapping
    public List<CommentDto> getUserComments(
            @PathVariable Long userId) {

        log.info("Get comments for user {}", userId);

        return commentService.getUserComments(userId);
    }


    @GetMapping("/{commentId}")
    public CommentDto getComment(
            @PathVariable Long userId,
            @PathVariable Long commentId) {

        log.info("Get comment {} by user {}", commentId, userId);

        return commentService.getComment(commentId);
    }


    @DeleteMapping("/{commentId}")
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