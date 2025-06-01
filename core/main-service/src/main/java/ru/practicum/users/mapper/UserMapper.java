package ru.practicum.users.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;
import org.apache.logging.log4j.util.Strings;
import ru.practicum.users.dto.UserDto;
import ru.practicum.users.model.User;

import java.util.List;

@Mapper(componentModel = "spring", imports = {Strings.class})
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "name", source = "name", qualifiedByName = "mapNonBlankString")
    @Mapping(target = "email", source = "email", qualifiedByName = "mapNonBlankString")
    User mapUserDtoToUser(UserDto userDto);

    UserDto mapUserToUserDto(User user);

    List<UserDto> mapUsersListToDtoList(List<User> users);

    @Named("mapNonBlankString")
    default String mapNonBlankString(String value) {
        return Strings.isBlank(value) ? null : value;
    }
}