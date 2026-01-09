package ru.practicum.ewm.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateRequest {

    @NotEmpty(message = "Список ID заявок не должен быть пустым")
    List<Long> requestIds;

    @NotNull(message = "Поле 'status' обязательно для заполнения")
    @Pattern(
            regexp = "CONFIRMED|REJECTED",
            message = "Допустимые значения для поля 'status': CONFIRMED или REJECTED"
    )
    String status;
}
