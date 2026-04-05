# Explore With Me

Платформа для публикации мероприятий и участия в них. Пользователи создают события, подают заявки, администраторы управляют категориями, подборками и модерируют события.

---

## Архитектура

Микросервисная архитектура с централизованной конфигурацией и discovery. Все сервисы регистрируются в Eureka, получают конфигурацию через Config Server. Внешние запросы идут через Gateway Server (порт `8080`).

**Инфраструктура:**
- **Discovery Server** (Eureka, порт `8761`) — реестр сервисов.
- **Config Server** — раздаёт конфигурации (native profile).
- **Gateway Server** — маршрутизация запросов к микросервисам.

**Основные сервисы (каждый со своей БД PostgreSQL):**
- `user-service` — управление пользователями.
- `event-service` — события, категории, подборки.
- `request-service` — заявки на участие.
- `moderation-service` — комментарии модерации.

**Сервис статистики:**
- `stats-server` — сбор и выдача статистики по запросам (используется `stats-client`).

**Взаимодействие:** OpenFeign + Circuit Breaker (Resilience4j), динамические порты (`server.port: 0`).

**Где хранятся конфигурации:**  
Все настройки сервисов централизованно лежат в Config Server по пути  
`infra/config-server/src/main/resources/config/`  
(файлы `user-service.yaml`, `event-service.yaml`, `request-service.yaml`, `moderation-service.yaml`, `stats-server.yaml`, `gateway-server.yaml`).

---

## Внутренний API (для взаимодействия сервисов)

Эндпоинты с префиксом `/internal` доступны только другим микросервисам, напрямую извне не вызываются.

### User Service — `/internal/users`
- `GET /internal/users/{userId}` — получить пользователя (UserDto)

### Event Service — `/internal/events`
- `GET /internal/events/{eventId}/validate` — проверить, можно ли участвовать в событии (возвращает EventValidationDto)
- `GET /internal/events/{eventId}` — получить базовую информацию о событии (EventDto)

### Request Service — `/internal/requests`
- `GET /internal/requests/count?eventIds=1,2,3` — получить количество подтверждённых заявок по списку событий (список ConfirmedRequestsDto)

### Moderation Service — `/internal/comments`
- `GET /internal/comments?eventIds=1,2,3` — получить комментарии модерации для событий
- `POST /internal/comments` — создать комментарий модерации (при публикации/отклонении)

---

## Внешний API (спецификация)

OpenAPI-спецификации находятся в корне проекта:
- [`ewm-main-service-spec.json`](./ewm-main-service-spec.json) — основное API (события, категории, подборки, заявки, комментарии модерации)
- [`ewm-stats-service-spec.json`](./ewm-stats-service-spec.json) — API сервиса статистики

Для просмотра можно использовать [Swagger Editor](https://editor.swagger.io/).

### Краткий перечень внешних эндпоинтов

#### Публичные
- `GET /events` — поиск событий (фильтрация, сортировка)
- `GET /events/{id}` — детали события
- `GET /categories` / `/compilations` — категории и подборки

#### Приватные (пользовательские)
- `POST /users/{userId}/events` — создание события
- `PATCH /users/{userId}/events/{eventId}` — редактирование
- `POST /users/{userId}/requests?eventId=...` — подача заявки
- `PATCH /users/{userId}/requests/{requestId}/cancel` — отмена заявки
- `GET /users/{userId}/events/{eventId}/moderation-comments` — просмотр комментариев модерации

#### Административные
- `POST /admin/users` / `GET /admin/users` / `DELETE /admin/users/{userId}`
- `POST /admin/categories` / `PATCH` / `DELETE`
- `POST /admin/compilations` / `PATCH` / `DELETE`
- `GET /admin/events` — поиск событий по любым критериям
- `PATCH /admin/events/{eventId}` — публикация/отклонение события

## Фича: модерация событий с комментариями

Администратор может модерировать события, ожидающие публикации:
- `GET /admin/events/moderation` — список событий со статусом `PENDING` с комментариями модерации.
- `PATCH /admin/events/{eventId}/moderate` — публикация или отклонение. При отклонении обязателен комментарий (причина), который сохраняется в `moderation-service` и доступен автору события.

---

## Запуск

```bash

docker-compose up -d
```
Gateway: http://localhost:8080
Discovery: http://localhost:8761

## Остановка

```bash

docker-compose down
```

## Технологии
Java 21, Spring Boot 3.3.4, Spring Cloud (Eureka, Config, Gateway, OpenFeign), Resilience4j, PostgreSQL, Docker, Maven, Lombok, MapStruct.

