package ru.practicum.ewm.compilation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {

    /**
     * Заголовок подборки.
     * Обязательное поле, длина от 1 до 50 символов.
     */
    @NotBlank(message = "Заголовок не должен быть пустым")
    @Size(min = 1, max = 50, message = "Длина заголовка должна быть от 1 до 50 символов")
    private String title;

    /**
     * Закреплена ли подборка на главной странице сайта.
     * По умолчанию false.
     */
    @Builder.Default
    private Boolean pinned = false;

    /**
     * Список уникальных идентификаторов событий входящих в подборку.
     */
    @Builder.Default
    @NotNull(message = "Список событий не может быть null")
    private Set<@NotNull(message = "Идентификатор события не может быть пустым") Long> events = Set.of();
}
