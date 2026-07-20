package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.Constants;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewEventDto {

    @NotBlank
    @Size(min = Constants.MIN_TITLE_LENGTH,
            max = Constants.MAX_TITLE_LENGTH)
    private String title;


    @NotBlank
    @Size(min = Constants.MIN_ANNOTATION_LENGTH,
            max = Constants.MAX_ANNOTATION_LENGTH)
    private String annotation;


    @NotBlank
    @Size(min = Constants.MIN_DESCRIPTION_LENGTH,
            max = Constants.MAX_DESCRIPTION_LENGTH)
    private String description;


    @NotNull
    private Long category;


    @NotNull
    @Future
    @JsonFormat(pattern = Constants.DATE_TIME_FORMAT)
    private LocalDateTime eventDate;


    @NotNull
    private LocationDto location;


    private Boolean paid = false;


    private Integer participantLimit = 0;


    private Boolean requestModeration = true;
}