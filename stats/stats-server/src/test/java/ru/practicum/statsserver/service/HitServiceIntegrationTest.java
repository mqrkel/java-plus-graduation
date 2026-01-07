package ru.practicum.statsserver.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statsdto.HitDto;
import ru.practicum.statsdto.StatsDtoOut;
import ru.practicum.statsserver.exception.ParameterInvalidException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HitServiceIntegrationTest {

    @Autowired
    private HitService hitService;

    private final LocalDateTime testNow = LocalDateTime.now();

    @BeforeEach
    void setup() {
        hitService.add(new HitDto("service1", "/test", "192.168.1.1", testNow.minusHours(1)));
        hitService.add(new HitDto("service1", "/test", "192.168.1.2", testNow.minusMinutes(30)));
        hitService.add(new HitDto("service1", "/test", "192.168.1.1", testNow.minusMinutes(15)));
        hitService.add(new HitDto("service1", "/other", "10.0.0.1", testNow.minusMinutes(10)));
    }

    @Test
    @DisplayName("Должен выбросить ParameterInvalidException, если start позже end")
    void shouldThrowException_whenStartAfterEnd() {
        LocalDateTime start = testNow.plusDays(1);
        LocalDateTime end = testNow;

        ParameterInvalidException ex = assertThrows(
                ParameterInvalidException.class,
                () -> hitService.getStatistics(start, end, null, false)
        );

        assertTrue(ex.getMessage().contains("'start' date must be before the 'end' date"));
    }

    @Test
    @DisplayName("Возвращает статистику по всем URI, если uris=null")
    void shouldReturnStatsForAllUris_whenUrisIsNull() {
        Collection<StatsDtoOut> stats = hitService.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                null,
                false
        );

        assertNotNull(stats);
        assertEquals(2, stats.size(), "Должны быть статистики по 2 URI");

        stats.forEach(stat -> {
            switch (stat.getUri()) {
                case "/test" -> assertEquals(3, stat.getHits(), "Должно быть 3 хита для /test");
                case "/other" -> assertEquals(1, stat.getHits(), "Должен быть 1 хит для /other");
                default -> fail("Неожиданный URI: " + stat.getUri());
            }
        });
    }

    @Test
    @DisplayName("Возвращает статистику с правильными значениями для конкретного URI")
    void shouldReturnStatsForSpecificUri() {
        String testUri = "/test";

        ArrayList<String> uris = new ArrayList<>();
        uris.add(testUri);

        Collection<StatsDtoOut> stats = hitService.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                uris,
                false
        );

        assertFalse(stats.isEmpty());
        StatsDtoOut stat = stats.iterator().next();

        assertEquals(testUri, stat.getUri());
        assertEquals(3, stat.getHits(), "Должно быть 3 хита для /test");
        assertEquals("service1", stat.getService());
    }

    @Test
    @DisplayName("Возвращает пустую коллекцию, если uris пустой список")
    void shouldReturnStatsForAllUris_whenUrisIsEmptyList() {
        Collection<StatsDtoOut> stats = hitService.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                new ArrayList<>(),
                false
        );

        assertNotNull(stats);
        assertEquals(2, stats.size(), "Должны быть статистики по 2 URI");

        stats.forEach(stat -> {
            switch (stat.getUri()) {
                case "/test" -> assertEquals(3, stat.getHits());
                case "/other" -> assertEquals(1, stat.getHits());
                default -> fail("Неожиданный URI: " + stat.getUri());
            }
        });
    }
}