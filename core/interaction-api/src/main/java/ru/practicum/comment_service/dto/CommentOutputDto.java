package ru.practicum.comment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.comment_service.entity.CommentsStatus;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.user_service.dto.UserShortDto;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentOutputDto {

    private Long id;

    private UserShortDto user;

    private EventShortDto event;

    private String text;

    private LocalDateTime created;

    private CommentsStatus status;

}
