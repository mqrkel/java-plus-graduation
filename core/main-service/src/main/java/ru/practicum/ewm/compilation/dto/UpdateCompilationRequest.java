package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCompilationRequest {

    /**
     * Заголовок подборки.
     * Если null — заголовок не меняется.
     * Если указан, длина должна быть от 1 до 50 символов.
     */
    @Size(min = 1, max = 50, message = "Длина заголовка должна быть от 1 до 50 символов")
    private String title;

    /**
     * Закреплена ли подборка на главной странице сайта.
     * Если null — состояние не меняется.
     */
    private Boolean pinned;

    /**
     * Полный список id событий подборки для замены текущего.
     * Если null — список событий не меняется.
     */
    private Set<Long> events;
}
