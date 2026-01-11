package ru.practicum.ewm.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDtoOut {
    public Long id;
    @NotBlank(message = "Поле 'name' должно быть заполнено")
    @Size(min = 1, max = 50, message = "Размер поля 'name' должен быть в диапазоне от 1 до 50 символов")
    public String name;
}
