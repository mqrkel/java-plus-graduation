package ru.practicum.ewm.user.service;

import java.util.List;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDtoOut;

public interface UserService {
    UserDtoOut createUser(NewUserRequest request);

    List<UserDtoOut> getUsers(List<Long> ids, int from, int size);

    void deleteUser(Long userId);
}