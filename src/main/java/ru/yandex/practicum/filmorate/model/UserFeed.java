package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserFeed {
    private Long eventId;
    private Long userId;
    private EventType eventType;
    private Operation operation;
    private Long entityId;
    private Long timestamp;
}