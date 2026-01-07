#!/bin/sh
docker run -p 9090:8080 \
    -e API_URL=https://raw.githubusercontent.com/yandex-praktikum/java-explore-with-me/main/ewm-stats-service-spec.json \
    swaggerapi/swagger-ui 

# docker run -p 9090:8080 \
#     -e API_URL=https://raw.githubusercontent.com/yandex-praktikum/java-explore-with-me/main/ewm-main-service-spec.json \
#     swaggerapi/swagger-ui 