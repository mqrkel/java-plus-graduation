package ru.practicum.ewm.statsclient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.statsclient.StatsClient;

@Component
public class StatsClientImpl extends StatsClient {

    @Autowired
    public StatsClientImpl(@Value("${statsserver.url}") String serverUrl) {
        super(serverUrl);
    }
}
