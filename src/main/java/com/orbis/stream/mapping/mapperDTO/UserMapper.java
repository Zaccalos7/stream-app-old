package com.orbis.stream.mapping.mapperDTO;

import com.orbis.stream.dto.UserDto;
import com.orbis.stream.mapping.BaseMapper;
import com.orbis.stream.model.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<UserDto, User> {
}
