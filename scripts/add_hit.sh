#!/bin/bash

# Проверка наличия обязательных параметров
if [ "$#" -ne 4 ]; then
    echo "Usage: $0 <service> <uri> <ip> <timestamp>"
    echo "Example: $0 \"EWM\" \"/path/2\" \"192.168.10.100\" \"2025-01-01 10:00:00\""
    exit 1
fi

# Параметры
SERVICE="$1"
URI="$2"
IP="$3"
TIMESTAMP="$4"

# Формирование JSON
HIT_DATA=$(jq -n \
    --arg service "$SERVICE" \
    --arg uri "$URI" \
    --arg ip "$IP" \
    --arg timestamp "$TIMESTAMP" \
    '{app: $service, uri: $uri, ip: $ip, timestamp: $timestamp}')

echo $HIT_DATA

# URL API
API_URL="http://localhost:9090/hit"

# Выполнение запроса
response=$(curl -s -X POST "$API_URL" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d "$HIT_DATA")

# Вывод результата
echo "$response" | jq .
