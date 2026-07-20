package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {

    @NotBlank(message = "Annotation must not be blank")
    @Size(
            min = 20,
            max = 2000,
            message = "Annotation length must be between 20 and 2000"
    )
    private String annotation;


    @NotNull(message = "Category must not be null")
    private Long category;


    @NotBlank(message = "Description must not be blank")
    @Size(
            min = 20,
            max = 7000,
            message = "Description length must be between 20 and 7000"
    )
    private String description;


    @NotNull(message = "Event date must not be null")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDate;


    @NotNull(message = "Location must not be null")
    private LocationDto location;


    private Boolean paid = false;


    @PositiveOrZero(
            message = "Participant limit must be positive or zero"
    )
    private Integer participantLimit = 0;


    private Boolean requestModeration = true;


    @NotBlank(message = "Title must not be blank")
    @Size(
            min = 3,
            max = 120,
            message = "Title length must be between 3 and 120"
    )
    private String title;
}