package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import ru.practicum.request_service.dto.ParticipationRequestDto;
import ru.practicum.model.ParticipationRequest;


@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    ParticipationRequestMapper INSTANCE = Mappers.getMapper(ParticipationRequestMapper.class);

    @Mapping(target = "requester", source = "userId")
    @Mapping(target = "event", source = "eventId")
    ParticipationRequestDto mapToDto(ParticipationRequest request);
}