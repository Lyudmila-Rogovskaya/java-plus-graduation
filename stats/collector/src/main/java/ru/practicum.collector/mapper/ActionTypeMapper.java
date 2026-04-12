package ru.practicum.collector.mapper;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;

public final class ActionTypeMapper {

    private ActionTypeMapper() {
    }

    public static ActionTypeAvro map(ActionTypeProto proto) {
        return switch (proto) {
            case ACTION_VIEW -> ActionTypeAvro.ACTION_VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.ACTION_REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.ACTION_LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + proto);
        };
    }

}
