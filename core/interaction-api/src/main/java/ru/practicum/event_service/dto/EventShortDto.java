package ru.practicum.event_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import ru.practicum.category_service.dto.CategoryDto;
import ru.practicum.user_service.config.DateConfig;
import ru.practicum.user_service.dto.UserShortDto;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class EventShortDto {
    private Long id;
    private String annotation;
    private CategoryDto category;
    private int confirmedRequests;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DateConfig.FORMAT)
    private String eventDate;
    private UserShortDto initiator;
    private boolean paid;
    private String title;
    private double rating;
}
