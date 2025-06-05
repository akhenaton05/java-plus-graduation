package ru.practicum.comments.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.comments.dto.CommentEconomDto;
import ru.practicum.comments.dto.CommentOutputDto;
import ru.practicum.comments.model.Comment;
import ru.practicum.events.mapper.EventMapper;
import ru.practicum.user_service.dto.UserShortDto;
import ru.practicum.user_service.feign.UserClient;

@Component
@RequiredArgsConstructor
public class CommentMapper {
    private final EventMapper eventMapper;
    private final UserClient userClient;

    public CommentOutputDto commentToOutputDto(Comment comment) {
        UserShortDto user = userClient.getUser(comment.getUserId()).getBody();

        return CommentOutputDto.builder()
                .id(comment.getId())
                .user(user)
                .event(eventMapper.toEventShortDto(comment.getEvent()))
                .text(comment.getText())
                .created(comment.getCreated())
                .status(comment.getStatus())
                .build();
    }

    public CommentEconomDto commentToEconomDto(Comment comment) {
        return CommentEconomDto.builder()
                .id(comment.getId())
                .userId(comment.getUserId())
                .eventId(comment.getEvent().getId())
                .text(comment.getText())
                .created(comment.getCreated())
                .status(comment.getStatus())
                .build();
    }
}
