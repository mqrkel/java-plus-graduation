package ru.practicum.statsserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.statsdto.HitDto;
import ru.practicum.statsdto.StatsDtoOut;
import ru.practicum.statsserver.exception.ParameterInvalidException;
import ru.practicum.statsserver.service.HitService;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HitController.class)
class HitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HitService hitService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    void shouldAddHitAndReturn201() throws Exception {
        HitDto hit = HitDto.builder()
                .service("test-service")
                .uri("/test")
                .ip("127.0.0.1")
                .dateTime(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(hit)))
                .andExpect(status().isCreated());

        Mockito.verify(hitService, Mockito.times(1)).add(any(HitDto.class));
    }

    @Test
    void shouldReturn400IfHitDtoIsInvalid() throws Exception {
        // Отсутствует URI (обязательное поле)
        HitDto invalidHit = HitDto.builder()
                .service("test-service")
                .ip("127.0.0.1")
                .dateTime(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/hit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidHit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnStats() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        StatsDtoOut dto = new StatsDtoOut("test-service", "/test", 5);
        Mockito.when(hitService.getStatistics(any(), any(), any(), any()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/stats")
                        .param("start", now.minusHours(1).format(FORMATTER))
                        .param("end", now.plusHours(1).format(FORMATTER))
                        .param("uris", "/test")
                        .param("unique", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].app").value("test-service"))
                .andExpect(jsonPath("$[0].uri").value("/test"))
                .andExpect(jsonPath("$[0].hits").value(5));
    }

    @Test
    void shouldReturn400IfStartParamIsMissing() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("end", "2025-01-01 10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400IfEndParamIsMissing() throws Exception {
        mockMvc.perform(get("/stats")
                        .param("start", "2025-01-01 10:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400IfStartAfterEnd() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Mockito.when(hitService.getStatistics(any(), any(), any(), any()))
                .thenThrow(new ParameterInvalidException("start > end"));

        mockMvc.perform(get("/stats")
                        .param("start", now.plusHours(1).format(FORMATTER))
                        .param("end", now.format(FORMATTER))
                        .param("unique", "false"))
                .andExpect(status().isBadRequest());
    }
}