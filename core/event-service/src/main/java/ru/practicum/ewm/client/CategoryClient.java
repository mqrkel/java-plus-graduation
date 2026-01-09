package ru.practicum.ewm.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.ewm.dto.CategoryDtoOut;

@FeignClient(
        name = "category-service",
        contextId = "categoryClient",
        path = "/categories"
)
public interface CategoryClient {

    @GetMapping("/{id}")
    CategoryDtoOut getCategoryById(@PathVariable("id") Long id);
}