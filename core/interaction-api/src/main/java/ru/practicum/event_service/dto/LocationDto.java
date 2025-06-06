package ru.practicum.event_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationDto {
    private Long id;
    @NotNull
    private float lat;
    @NotNull
    private float lon;
}
