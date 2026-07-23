package ru.practicum.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CommentDto;
import ru.practicum.dto.NewCommentDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;
import ru.practicum.model.enums.EventState;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {


    private final CommentRepository commentRepository;

    private final EventRepository eventRepository;

    private final UserRepository userRepository;

    private final CommentMapper commentMapper;



    @Override
    @Transactional
    public CommentDto addComment(
            Long userId,
            Long eventId,
            NewCommentDto dto) {


        User user = getUserEntity(userId);

        Event event = getEventEntity(eventId);


        if (!EventState.PUBLISHED.equals(event.getState())) {

            throw new ConflictException(
                    "Cannot comment unpublished event"
            );
        }


        LocalDateTime now = LocalDateTime.now()
                .withNano(
                        (LocalDateTime.now().getNano() / 1000) * 1000
                );


        Comment comment = Comment.builder()
                .text(dto.getText())
                .created(now)
                .author(user)
                .event(event)
                .build();


        comment = commentRepository.save(comment);


        log.info(
                "Added comment with id: {} for event: {}",
                comment.getId(),
                eventId
        );


        return commentMapper.toDto(comment);
    }



    @Override
    public List<CommentDto> getEventComments(Long eventId) {


        getEventEntity(eventId);


        return commentRepository
                .findByEventIdOrderByCreatedAsc(eventId)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }



    @Override
    public List<CommentDto> getUserComments(Long userId) {


        getUserEntity(userId);


        return commentRepository
                .findByAuthorIdOrderByCreatedAsc(userId)
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }



    @Override
    @Transactional
    public void deleteComment(
            Long userId,
            Long commentId) {


        Comment comment = getCommentEntity(commentId);


        if (!comment.getAuthor()
                .getId()
                .equals(userId)) {


            throw new ConflictException(
                    "You cannot delete this comment"
            );
        }


        commentRepository.delete(comment);


        log.info(
                "Deleted comment {} by user {}",
                commentId,
                userId
        );
    }



    @Override
    @Transactional
    public void deleteAdminComment(Long commentId) {


        Comment comment = getCommentEntity(commentId);


        commentRepository.delete(comment);


        log.info(
                "Admin deleted comment {}",
                commentId
        );
    }



    private User getUserEntity(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "User not found with id: " + userId
                        ));
    }



    private Event getEventEntity(Long eventId) {

        return eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Event not found with id: " + eventId
                        ));
    }



    private Comment getCommentEntity(Long commentId) {

        return commentRepository.findById(commentId)
                .orElseThrow(() ->
                        new EntityNotFoundException(
                                "Comment not found with id: " + commentId
                        ));
    }
}