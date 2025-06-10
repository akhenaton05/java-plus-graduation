package ru.practicum.event_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.user_service.config.DateConfig;
import ru.practicum.event_service.validation.TimePresentOrFuture;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEventUserRequest {
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;
    @Size(min = 20, max = 2000)
    private String annotation;
    private int category;
    @Size(min = 20, max = 7000)
    private String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateConfig.FORMAT)
    @TimePresentOrFuture
    private String eventDate;
    private LocationDto location;
    private boolean paid;
    @Positive
    private Integer participantLimit;
    private boolean requestModeration;
    private String stateAction;
    @Size(min = 3, max = 120)
    private String title;
}
