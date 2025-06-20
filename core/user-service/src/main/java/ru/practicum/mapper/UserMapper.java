package ru.practicum.mapper;

import org.apache.logging.log4j.util.Strings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.practicum.model.User;
import ru.practicum.user_service.dto.UserDto;
import ru.practicum.user_service.dto.UserShortDto;


import java.util.List;

@Mapper(componentModel = "spring", imports = {Strings.class})
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name", qualifiedByName = "mapNonBlankString")
    @Mapping(target = "email", source = "email", qualifiedByName = "mapNonBlankString")
    User mapUserDtoToUser(UserDto userDto);

    UserDto mapUserToUserDto(User user);

    UserShortDto toShortDto(User user);

    List<UserDto> mapUsersListToDtoList(List<User> users);

    @Named("mapNonBlankString")
    default String mapNonBlankString(String value) {
        return Strings.isBlank(value) ? null : value;
    }
}