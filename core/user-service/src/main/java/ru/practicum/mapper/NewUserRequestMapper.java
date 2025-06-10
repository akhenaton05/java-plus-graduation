package ru.practicum.mapper;

import org.apache.logging.log4j.util.Strings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.model.User;
import ru.practicum.user_service.dto.NewUserRequest;


@Mapper(componentModel = "spring", imports = {Strings.class})
public interface NewUserRequestMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name", qualifiedByName = "mapNonBlankString")
    @Mapping(target = "email", source = "email", qualifiedByName = "mapNonBlankString")
    User mapNewUserRequestToUser(NewUserRequest newUserRequest);

    @Named("mapNonBlankString")
    default String mapNonBlankString(String value) {
        return Strings.isBlank(value) ? null : value;
    }
}