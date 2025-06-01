package ru.practicum.users.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.mapstruct.Named;
import org.apache.logging.log4j.util.Strings;
import ru.practicum.users.dto.NewUserRequest;
import ru.practicum.users.model.User;

@Mapper(componentModel = "spring", imports = {Strings.class})
public interface NewUserRequestMapper {

    NewUserRequestMapper INSTANCE = Mappers.getMapper(NewUserRequestMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name", qualifiedByName = "mapNonBlankString")
    @Mapping(target = "email", source = "email", qualifiedByName = "mapNonBlankString")
    User mapNewUserRequestToUser(NewUserRequest newUserRequest);

    @Named("mapNonBlankString")
    default String mapNonBlankString(String value) {
        return Strings.isBlank(value) ? null : value;
    }
}