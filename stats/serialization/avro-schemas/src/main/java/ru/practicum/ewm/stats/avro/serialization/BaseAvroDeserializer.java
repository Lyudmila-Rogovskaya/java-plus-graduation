package ru.practicum.ewm.stats.avro.serialization;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final DecoderFactory decoderFactory = DecoderFactory.get();
    private final Schema schema;

    public BaseAvroDeserializer(Schema schema) {
        this.schema = schema;
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) return null;
        try {
            BinaryDecoder decoder = decoderFactory.binaryDecoder(data, null);
            DatumReader<T> reader = new SpecificDatumReader<>(schema);
            return reader.read(null, decoder);
        } catch (Exception e) {
            throw new SerializationException("Ошибка десериализации данных из топика [" + topic + "]", e);
        }
    }

}
