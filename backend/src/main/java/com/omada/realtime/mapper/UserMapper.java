package com.omada.realtime.mapper;

import com.omada.realtime.dto.request.RegisterRequest;
import com.omada.realtime.dto.response.UserResponse;
import com.omada.realtime.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * MapStruct mapper for User entity and DTOs.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toEntity(RegisterRequest req);

    @Mapping(source = "id", target = "id")
    UserResponse toResponse(User user);

    void updateEntity(RegisterRequest req, @MappingTarget User user);
}
