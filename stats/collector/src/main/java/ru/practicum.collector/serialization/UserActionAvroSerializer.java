package ru.practicum.collector.serialization;

import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class UserActionAvroSerializer implements Serializer<UserActionAvro> {

    private final EncoderFactory encoderFactory = EncoderFactory.get();
    private BinaryEncoder encoder;

    @Override
    public byte[] serialize(String topic, UserActionAvro data) {
        if (data == null) {
            return null;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            encoder = encoderFactory.binaryEncoder(out, encoder);
            DatumWriter<UserActionAvro> writer = new SpecificDatumWriter<>(UserActionAvro.class);
            writer.write(data, encoder);
            encoder.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Error serializing UserActionAvro for topic [" + topic + "]", e);
        }
    }

    @Override
    public void close() {
    }

}
