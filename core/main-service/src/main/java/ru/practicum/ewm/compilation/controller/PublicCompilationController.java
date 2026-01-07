package ru.practicum.ewm.compilation.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.compilation.dto.CompilationDto;
import ru.practicum.ewm.compilation.service.CompilationService;

import java.util.List;

/**
 * Публичный REST контроллер для работы с подборками событий (compilations).
 * Позволяет получить список подборок и получить подборку по ID.
 */
@RestController
@RequestMapping("/compilations")
@RequiredArgsConstructor
@Validated
public class PublicCompilationController {

    private final CompilationService compilationService;

    /**
     * Получить список подборок событий с возможностью фильтрации по закреплённости,
     * а также пагинации (пропуск элементов и ограничение размера выборки).
     *
     * @param pinned фильтр по закреплённости подборок (true — только закреплённые,
     *               false — только не закреплённые, null — все)
     * @param from   количество элементов, которые нужно пропустить (для пагинации), не может быть отрицательным
     * @param size   максимальное количество элементов в ответе, должно быть положительным
     * @return список DTO подборок событий, соответствующих фильтрам
     */
    @GetMapping
    public List<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @RequestParam(defaultValue = "0") @Min(0) int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        return compilationService.getCompilations(pinned, from, size);
    }

    /**
     * Получить подборку событий по её уникальному идентификатору.
     *
     * @param compId ID подборки
     * @return DTO подборки с указанным ID
     */
    @GetMapping("/{compId}")
    public CompilationDto getCompilation(@PathVariable Long compId) {
        return compilationService.getCompilationById(compId);
    }
}