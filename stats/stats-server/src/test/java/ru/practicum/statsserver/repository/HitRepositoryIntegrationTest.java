package ru.practicum.statsserver.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.statsserver.model.Hit;
import ru.practicum.statsserver.model.Stats;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HitRepositoryIntegrationTest {

    @Autowired
    private HitRepository hitRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;


    private final LocalDateTime testNow = LocalDateTime.now();

    @BeforeEach
    void setup() {
        hitRepository.save(new Hit("service1", "/test", "192.168.1.1", testNow.minusHours(1)));
        hitRepository.save(new Hit("service1", "/test", "192.168.1.2", testNow.minusMinutes(30)));
        hitRepository.save(new Hit("service1", "/test", "192.168.1.1", testNow.minusMinutes(15)));
        hitRepository.save(new Hit("service1", "/other", "10.0.0.1", testNow.minusMinutes(10)));
    }

    @Test
    @DisplayName("Возвращает статистику по всем URI, если uris=null")
    void shouldReturnStatsForAllUris_whenUriIsNull() {
        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                null,
                false
        );

        assertEquals(2, stats.size(), "Ожидается два URI в статистике");

        stats.forEach(stat -> {
            switch (stat.getUri()) {
                case "/test" -> assertEquals(3, stat.getHits(), "Все хиты для /test");
                case "/other" -> assertEquals(1, stat.getHits(), "Все хиты для /other");
                default -> fail("Неожиданный URI: " + stat.getUri());
            }
        });
    }

    @Test
    @DisplayName("Возвращает пустую коллекцию, если нет хитов в заданном диапазоне времени")
    void shouldReturnEmpty_whenNoHitsInTimeRange() {
        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusDays(5),
                testNow.minusDays(4),
                null,
                false
        );

        assertTrue(stats.isEmpty(), "Ожидается пустая коллекция статистики");
    }

    @Test
    @DisplayName("Возвращает статистику, когда start и end равны (точка времени без наносекунд)")
    void shouldReturnStats_whenStartEqualsEnd() {
        LocalDateTime exactTime = testNow.minusMinutes(30).withNano(0);
        hitRepository.save(new Hit("service1", "/exact", "192.168.1.100", exactTime));

        Collection<Stats> stats = hitRepository.getStatistics(
                exactTime,
                exactTime,
                List.of("/exact"),
                false
        );

        assertEquals(1, stats.size(), "Ожидается статистика по одному URI");

        Stats stat = stats.stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ожидался хотя бы один элемент"));

        assertEquals("/exact", stat.getUri(), "URI совпадает");
        assertEquals(1, stat.getHits(), "Подсчитывается 1 хит на точку времени");
    }

    @Test
    @DisplayName("Возвращает пустую коллекцию, если запрошенный диапазон до первой записи")
    void shouldReturnEmpty_whenRangeBeforeFirstHit() {
        LocalDateTime start = testNow.minusDays(10);
        LocalDateTime end = testNow.minusDays(9);

        Collection<Stats> stats = hitRepository.getStatistics(start, end, null, false);

        assertTrue(stats.isEmpty(), "Ожидается пустая статистика, так как нет данных в этом диапазоне");
    }

    @Test
    @DisplayName("Возвращает пустую коллекцию, если запрошенный диапазон после последней записи")
    void shouldReturnEmpty_whenRangeAfterLastHit() {
        LocalDateTime start = testNow.plusDays(1);
        LocalDateTime end = testNow.plusDays(2);

        Collection<Stats> stats = hitRepository.getStatistics(start, end, null, false);

        assertTrue(stats.isEmpty(), "Ожидается пустая статистика, так как нет данных в этом диапазоне");
    }

    @Test
    @DisplayName("Возвращает пустую коллекцию, если запрошенный диапазон между записями, где нет хитов")
    void shouldReturnEmpty_whenRangeBetweenHitsWithNoData() {
        LocalDateTime start = testNow.minusMinutes(29);
        LocalDateTime end = testNow.minusMinutes(16);

        Collection<Stats> stats = hitRepository.getStatistics(start, end, null, false);

        assertTrue(stats.isEmpty(), "Ожидается пустая статистика, так как нет хитов в этом промежутке");
    }

    @Test
    @DisplayName("Возвращает статистику по всем URI, если uris передан пустой список")
    void shouldReturnStatsForAllUris_whenUrisIsEmptyList() {
        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                List.of(),
                false
        );

        assertEquals(2, stats.size(), "Ожидается статистика по всем URI");

        stats.forEach(stat -> {
            switch (stat.getUri()) {
                case "/test" -> assertEquals(3, stat.getHits(), "Все хиты для /test");
                case "/other" -> assertEquals(1, stat.getHits(), "Все хиты для /other");
                default -> fail("Неожиданный URI: " + stat.getUri());
            }
        });
    }

    @Test
    @DisplayName("Возвращает пустую коллекцию, если запрошенные URI отсутствуют в базе")
    void shouldReturnEmpty_whenUrisNotInDatabase() {
        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                List.of("/nonexistent", "/unknown"),
                false
        );

        assertTrue(stats.isEmpty(), "Ожидается пустая коллекция, так как URI отсутствуют в базе");
    }

    @Test
    @DisplayName("Возвращает статистику по нескольким URI из списка")
    void shouldReturnStatsForMultipleUris() {
        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                List.of("/test", "/other"),
                false
        );

        assertEquals(2, stats.size(), "Ожидается статистика по двум URI");

        stats.forEach(stat -> {
            switch (stat.getUri()) {
                case "/test" -> assertEquals(3, stat.getHits(), "Все хиты для /test");
                case "/other" -> assertEquals(1, stat.getHits(), "Все хиты для /other");
                default -> fail("Неожиданный URI: " + stat.getUri());
            }
        });
    }

    @Test
    @DisplayName("Подсчёт уникальных IP с unique=true учитывает дубликаты IP по разным URI")
    void shouldCountUniqueIpsAcrossDifferentUris() {
        hitRepository.save(new Hit("service1", "/other", "192.168.1.1", testNow.minusMinutes(5)));

        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                List.of("/test", "/other"),
                true
        );

        assertEquals(2, stats.size(), "Ожидается статистика по двум URI");

        stats.forEach(stat -> {
            switch (stat.getUri()) {
                case "/test" -> assertEquals(2, stat.getHits(), "Уникальных IP для /test должно быть 2");
                case "/other" -> assertEquals(2, stat.getHits(), "Уникальных IP для /other должно быть 2");
                default -> fail("Неожиданный URI: " + stat.getUri());
            }
        });
    }

    @Test
    @DisplayName("Подсчёт всех IP с unique=false учитывает все попадания, включая повторы IP")
    void shouldCountAllIpsWhenUniqueFalse() {
        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(2),
                testNow.plusMinutes(5),
                List.of("/test", "/other"),
                false
        );

        assertEquals(2, stats.size(), "Ожидается статистика по двум URI");

        stats.forEach(stat -> {
            switch (stat.getUri()) {
                case "/test" -> assertEquals(3, stat.getHits(), "Всего хитов для /test должно быть 3");
                case "/other" -> assertEquals(1, stat.getHits(), "Всего хитов для /other должно быть 1");
                default -> fail("Неожиданный URI: " + stat.getUri());
            }
        });
    }

    @Test
    @DisplayName("Подсчёт статистики с IP 0.0.0.0, 127.0.0.1 и 255.255.255.255")
    void shouldHandleEdgeCaseIps() {
        hitRepository.save(new Hit("service1", "/ip-test", "0.0.0.0", testNow.minusMinutes(20)));
        hitRepository.save(new Hit("service1", "/ip-test", "127.0.0.1", testNow.minusMinutes(19)));
        hitRepository.save(new Hit("service1", "/ip-test", "255.255.255.255", testNow.minusMinutes(18)));

        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(1),
                testNow.plusMinutes(5),
                List.of("/ip-test"),
                true
        );

        assertEquals(1, stats.size(), "Ожидается статистика по одному URI");
        Stats stat = stats.iterator().next();
        assertEquals(3, stat.getHits(), "3 уникальных IP по /ip-test");
    }

    @Test
    @DisplayName("Хиты с null-IP не учитываются в статистике, так как COUNT(ip) пропускает null")
    void shouldIgnoreNullIpWhenCounting() {
        hitRepository.save(new Hit("service1", "/null-ip", null, testNow.minusMinutes(10)));
        hitRepository.save(new Hit("service1", "/null-ip", null, testNow.minusMinutes(5)));

        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(1),
                testNow.plusMinutes(5),
                List.of("/null-ip"),
                false
        );

        assertEquals(1, stats.size(), "Ожидается один URI /null-ip");
        Stats stat = stats.iterator().next();
        assertEquals(0, stat.getHits(), "Хиты с null-IP не учитываются");
    }

    @Test
    @DisplayName("Подсчёт статистики, если IP пустая строка")
    void shouldHandleEmptyIpWhenUniqueFalse() {
        hitRepository.save(new Hit("service1", "/empty-ip", "", testNow.minusMinutes(10)));
        hitRepository.save(new Hit("service1", "/empty-ip", "", testNow.minusMinutes(5)));

        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(1),
                testNow.plusMinutes(5),
                List.of("/empty-ip"),
                false
        );

        assertEquals(1, stats.size(), "Ожидается статистика по одному URI");
        Stats stat = stats.iterator().next();
        assertEquals(2, stat.getHits(), "Ожидается 2 хита с пустым IP");
    }

    @Test
    @DisplayName("Должен вернуть статистику в правильном порядке: по убыванию hits, затем service, затем uri")
    void shouldReturnStatsInCorrectOrder() {
        jdbcTemplate.update("DELETE FROM hits");

        hitRepository.save(new Hit("serviceA", "/alpha", "192.168.0.1", testNow.minusMinutes(20)));
        hitRepository.save(new Hit("serviceB", "/beta", "192.168.0.2", testNow.minusMinutes(15)));
        hitRepository.save(new Hit("serviceB", "/beta", "192.168.0.3", testNow.minusMinutes(10)));
        hitRepository.save(new Hit("serviceC", "/gamma", "192.168.0.4", testNow.minusMinutes(5)));
        hitRepository.save(new Hit("serviceC", "/gamma", "192.168.0.5", testNow.minusMinutes(4)));
        hitRepository.save(new Hit("serviceC", "/gamma", "192.168.0.6", testNow.minusMinutes(3)));

        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(1),
                testNow.plusMinutes(1),
                null,
                false
        );

        List<Stats> resultList = new ArrayList<>(stats);

        assertEquals(3, resultList.size(), "Ожидается 3 разных URI");

        assertAll(
                () -> assertEquals("/gamma", resultList.get(0).getUri(), "1 место: URI с 3 хитами"),
                () -> assertEquals("/beta", resultList.get(1).getUri(), "2 место: URI с 2 хитами"),
                () -> assertEquals("/alpha", resultList.get(2).getUri(), "3 место: URI с 1 хитом")
        );
    }

    @Test
    @DisplayName("Должны корректно заполняться все поля: service, uri, hits")
    void shouldFillAllStatsFieldsCorrectly() {
        jdbcTemplate.update("DELETE FROM hits");

        String service = "myService";
        String uri = "/example";
        String ip = "127.0.0.1";

        hitRepository.save(new Hit(service, uri, ip, testNow.minusMinutes(10)));
        hitRepository.save(new Hit(service, uri, "192.168.0.1", testNow.minusMinutes(5)));

        Collection<Stats> stats = hitRepository.getStatistics(
                testNow.minusHours(1),
                testNow.plusMinutes(1),
                List.of(uri),
                false
        );

        Stats result = stats.stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("Ожидался хотя бы один результат"));

        assertAll("Проверка значений полей в Stats",
                () -> assertEquals(service, result.getService(), "Service должен совпадать"),
                () -> assertEquals(uri, result.getUri(), "URI должен совпадать"),
                () -> assertEquals(2, result.getHits(), "Hits должен быть равен 2")
        );
    }
}