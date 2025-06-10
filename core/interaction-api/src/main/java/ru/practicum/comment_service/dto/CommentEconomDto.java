package ru.practicum.comment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.comment_service.entity.CommentsStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentEconomDto {

    private Long id;

    private Long userId;

    private Long eventId;

    private String text;

    private LocalDateTime created;

    private CommentsStatus status;

}
