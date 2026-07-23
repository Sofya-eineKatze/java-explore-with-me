package ru.practicum.service;

import ru.practicum.dto.CommentDto;
import ru.practicum.dto.NewCommentDto;

import java.util.List;

public interface CommentService {


    CommentDto addComment(
            Long userId,
            Long eventId,
            NewCommentDto dto
    );


    List<CommentDto> getEventComments(
            Long eventId
    );


    List<CommentDto> getUserComments(
            Long userId
    );


    void deleteComment(
            Long userId,
            Long commentId
    );


    void deleteAdminComment(
            Long commentId
    );
}