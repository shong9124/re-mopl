package com.practice.remopl.user.mapper;

import com.practice.remopl.user.dto.resposne.UserResponse;
import com.practice.remopl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface UserMapper {

    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "profileImageUrl", source = "user.profileImageId")
    UserResponse toDto(User user);
}
