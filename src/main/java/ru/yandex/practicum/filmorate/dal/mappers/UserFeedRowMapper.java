package ru.yandex.practicum.filmorate.dal.mappers;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.UserFeed;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class UserFeedRowMapper implements RowMapper<UserFeed> {

    @Override
    public UserFeed mapRow(ResultSet rs, int rowNum) throws SQLException {
        UserFeed userFeed = new UserFeed();

        userFeed.setEventId(rs.getLong("event_id"));
        userFeed.setUserId(rs.getLong("user_id"));
        userFeed.setTimestamp(rs.getLong("event_timestamp"));
        userFeed.setEntityId(rs.getLong("entity_id"));

        String eventTypeStr = rs.getString("event_type");
        userFeed.setEventType(EventType.valueOf(eventTypeStr));

        String operationStr = rs.getString("operation");
        userFeed.setOperation(Operation.valueOf(operationStr));

        return userFeed;
    }
}