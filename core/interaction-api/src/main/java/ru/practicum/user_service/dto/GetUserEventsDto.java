package ru.practicum.user_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUserEventsDto {
    private Long userId;
    private int from;
    private int size;
}
