package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.UserShortDto;
import ru.practicum.model.Comment;

@Component
public class CommentMapper {

    public CommentDto toDto(Comment comment) {

        return CommentDto.builder()
                .id(comment.getId())
                .text(comment.getText())
                .created(comment.getCreated())
                .event(comment.getEvent().getId())
                .author(
                        UserShortDto.builder()
                                .id(comment.getAuthor().getId())
                                .name(comment.getAuthor().getName())
                                .build()
                )
                .build();
    }
}