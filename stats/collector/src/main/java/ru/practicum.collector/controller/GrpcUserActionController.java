package ru.practicum.collector.controller;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import ru.practicum.collector.mapper.ActionTypeMapper;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcUserActionController extends UserActionControllerGrpc.UserActionControllerImplBase {

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;

    @Value("${kafka.topics.user-actions}")
    private String userActionsTopic;

    @Override
    public void collectUserAction(UserActionProto request, StreamObserver<Empty> responseObserver) {
        try {
            log.debug("Получено действие пользователя: userId={}, eventId={}, actionType={}",
                    request.getUserId(), request.getEventId(), request.getActionType());

            UserActionAvro avro = UserActionAvro.newBuilder()
                    .setUserId(request.getUserId())
                    .setEventId(request.getEventId())
                    .setActionType(ActionTypeMapper.map(request.getActionType()))
                    .setTimestamp(Instant.ofEpochMilli(protoTimestampToMillis(request.getTimestamp())))
                    .build();

            kafkaTemplate.send(userActionsTopic, String.valueOf(avro.getEventId()), avro)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Не удалось отправить действие пользователя в Kafka", ex);
                        } else {
                            log.debug("Действие пользователя отправлено в Kafka: offset={}", result.getRecordMetadata().offset());
                        }
                    });

            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка при обработке действия пользователя", e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Внутренняя ошибка сервера: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    private long protoTimestampToMillis(Timestamp timestamp) {
        return timestamp.getSeconds() * 1000 + timestamp.getNanos() / 1_000_000;
    }

}
