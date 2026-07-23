package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.service.CommentService;


@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;


    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable Long commentId) {

        log.info("Admin delete comment {}", commentId);

        commentService.deleteAdminComment(commentId);
    }
}