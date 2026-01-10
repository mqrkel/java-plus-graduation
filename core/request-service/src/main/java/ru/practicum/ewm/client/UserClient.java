package ru.practicum.ewm.client;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.ewm.dto.UserDtoOut;

@FeignClient(
        name = "user-service",
        path = "/admin/users"
)
public interface UserClient {

    @GetMapping
    List<UserDtoOut> getUsers(@RequestParam(required = false) List<Long> ids);

    default UserDtoOut getUserById(Long userId) {
        List<UserDtoOut> users = getUsers(List.of(userId));
        return users.isEmpty() ? null : users.getFirst();
    }
}