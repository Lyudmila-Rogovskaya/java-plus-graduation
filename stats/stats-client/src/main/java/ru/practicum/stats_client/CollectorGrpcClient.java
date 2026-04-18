package ru.practicum.stats_client;

import com.google.protobuf.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionControllerGrpc;
import ru.practicum.ewm.stats.proto.UserActionProto;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorGrpcClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    public void sendUserAction(long userId, long eventId, ActionTypeProto actionType) {
        try {
            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.newBuilder()
                    .setSeconds(now.getEpochSecond())
                    .setNanos(now.getNano())
                    .build();

            UserActionProto request = UserActionProto.newBuilder()
                    .setUserId(userId)
                    .setEventId(eventId)
                    .setActionType(actionType)
                    .setTimestamp(timestamp)
                    .build();

            client.collectUserAction(request);
            log.debug("Действие пользователя отправлено: userId={}, eventId={}, actionType={}", userId, eventId, actionType);
        } catch (Exception e) {
            log.error("Не удалось отправить действие пользователя в collector: userId={}, eventId={}, actionType={}",
                    userId, eventId, actionType, e);
        }
    }

}
