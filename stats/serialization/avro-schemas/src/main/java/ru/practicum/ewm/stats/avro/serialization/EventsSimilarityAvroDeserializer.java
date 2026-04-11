package ru.practicum.ewm.stats.avro.serialization;

import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

public class EventsSimilarityAvroDeserializer extends BaseAvroDeserializer<EventsSimilarityAvro> {

    public EventsSimilarityAvroDeserializer() {
        super(EventsSimilarityAvro.getClassSchema());
    }

}
