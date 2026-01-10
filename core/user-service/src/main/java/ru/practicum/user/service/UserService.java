package ru.practicum.user.service;

import java.util.List;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDtoOut;

public interface UserService {
    UserDtoOut createUser(NewUserRequest request);

    List<UserDtoOut> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);
}