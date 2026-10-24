package com.sunrise.recipeapi.mapper;

import com.sunrise.recipeapi.domain.AppUser;
import com.sunrise.recipeapi.dto.UserRegisterRequest;
import com.sunrise.recipeapi.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "id", ignore = true)
  AppUser toAppUser(UserRegisterRequest request);

  UserResponse toUserResponse(AppUser user);
}
