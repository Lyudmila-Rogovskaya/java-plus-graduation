package ru.practicum.stats_client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyzerGrpcClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getUserPredictions(long userId, int maxResults) {
        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        try {
            Iterator<RecommendedEventProto> iterator = client.getUserPredictions(request);
            return asStream(iterator);
        } catch (Exception e) {
            log.error("Не удалось получить предсказания для пользователя с ID={}", userId, e);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                .setEventId(eventId)
                .setUserId(userId)
                .setMaxResults(maxResults)
                .build();

        try {
            Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);
            return asStream(iterator);
        } catch (Exception e) {
            log.error("Не удалось получить похожие события для eventId={} и userId={}", eventId, userId, e);
            return Stream.empty();
        }
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                .addAllEventId(eventIds)
                .build();
        try {
            Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);
            return asStream(iterator);
        } catch (Exception e) {
            log.error("Не удалось получить количество взаимодействий для eventIds={}", eventIds, e);
            return Stream.empty();
        }
    }

    private Stream<RecommendedEventProto> asStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }

}
