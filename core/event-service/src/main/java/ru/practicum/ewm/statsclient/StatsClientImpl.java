package ru.practicum.ewm.statsclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Component;
import ru.practicum.statsclient.StatsClient;

@Component
public class StatsClientImpl extends StatsClient {

    public StatsClientImpl(
            @Value("${stats.service-id:stats-server}") String statsServiceId,
            DiscoveryClient discoveryClient) {
        super(statsServiceId, discoveryClient);
    }
}