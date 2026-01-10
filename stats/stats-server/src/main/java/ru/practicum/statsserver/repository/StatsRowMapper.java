package ru.practicum.statsserver.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.practicum.statsserver.model.Stats;

@Component
@RequiredArgsConstructor
public class StatsRowMapper implements RowMapper<Stats> {
    @Override
    public Stats mapRow(ResultSet rs, int rowNum) throws SQLException {
        Stats stats = new Stats();
        stats.setHits(rs.getInt("hits_count"));
        stats.setService(rs.getString("service"));
        stats.setUri(rs.getString("uri"));
        return stats;
    }
}
