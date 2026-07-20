package ru.practicum.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.UserDto;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;
import ru.practicum.service.UserService;
import ru.practicum.Constants;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto addUser(UserDto userDto) {
        try {
            User user = User.builder()
                    .name(userDto.getName())
                    .email(userDto.getEmail())
                    .registered(LocalDateTime.now())
                    .state(Constants.USER_STATE_ACTIVE)
                    .build();
            User saved = userRepository.save(user);
            log.info("Added user with id: {}", saved.getId());
            return toDto(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("User with this email already exists");
        } catch (Exception e) {
            log.error("Error creating user: {}", e.getMessage());
            throw new RuntimeException("Failed to create user");
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserEntity(userId);
        userRepository.delete(user);
        log.info("Deleted user with id: {}", userId);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<User> users;
        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findAllById(ids);
        } else {
            users = userRepository.findAll(pageable).getContent();
        }
        return users.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private User getUserEntity(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}