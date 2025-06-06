package ru.practicum.event_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.practicum.event_service.entity.StateEvent;
import ru.practicum.user_service.config.DateConfig;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EventFullDto extends EventShortDto {
    private String createdOn;
    private String description;
    private LocationDto location;
    private int participantLimit;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateConfig.FORMAT)
    private String publishedOn;
    private boolean requestModeration;
    private StateEvent state;
}
