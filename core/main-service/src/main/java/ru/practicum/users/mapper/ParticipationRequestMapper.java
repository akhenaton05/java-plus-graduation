package ru.practicum.users.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.practicum.users.dto.ParticipationRequestDto;
import ru.practicum.users.model.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    ParticipationRequestMapper INSTANCE = Mappers.getMapper(ParticipationRequestMapper.class);

    @Mapping(target = "requester", source = "user.id")
    @Mapping(target = "event", source = "event.id")
    ParticipationRequestDto mapToDto(ParticipationRequest request);
}