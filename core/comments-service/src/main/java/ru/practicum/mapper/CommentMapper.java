package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.comment_service.dto.CommentEconomDto;
import ru.practicum.comment_service.dto.CommentOutputDto;
import ru.practicum.event_service.dto.EventShortDto;
import ru.practicum.event_service.feign.EventClient;
import ru.practicum.model.Comment;
import ru.practicum.user_service.dto.UserShortDto;
import ru.practicum.user_service.feign.UserClient;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    private final UserClient userClient;
    private final EventClient eventClient;

    public CommentOutputDto commentToOutputDto(Comment comment) {
        UserShortDto user = userClient.getUser(comment.getUserId()).getBody();
        EventShortDto event = eventClient.getEventById(comment.getEventId()).getBody();

        return CommentOutputDto.builder()
                .id(comment.getId())
                .user(user)
                .event(event)
                .text(comment.getText())
                .created(comment.getCreated())
                .status(comment.getStatus())
                .build();
    }

    public CommentEconomDto commentToEconomDto(Comment comment) {
        return CommentEconomDto.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .eventId(comment.getEventId())
                .text(comment.getText())
                .created(comment.getCreated())
                .status(comment.getStatus())
                .build();
    }
}
