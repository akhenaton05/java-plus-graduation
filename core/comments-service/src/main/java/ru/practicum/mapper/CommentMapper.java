package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import ru.practicum.comment_service.dto.CommentEconomDto;
import ru.practicum.comment_service.dto.CommentOutputDto;
import ru.practicum.event_service.feign.EventClient;
import ru.practicum.model.Comment;
import ru.practicum.user_service.feign.UserClient;

@Mapper(componentModel = "spring")
public abstract class CommentMapper {

    @Autowired
    protected UserClient userClient;

    @Autowired
    protected EventClient eventClient;

    @Mapping(target = "user", expression = "java(userClient.getUser(comment.getUserId()).getBody())")
    @Mapping(target = "event", expression = "java(eventClient.getEventById(comment.getEventId()).getBody())")
    public abstract CommentOutputDto commentToOutputDto(Comment comment);

    public abstract CommentEconomDto commentToEconomDto(Comment comment);
}