package ru.practicum.analyzer.grpc;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.analyzer.service.RecommendationService;
import ru.practicum.ewm.stats.proto.*;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsGrpcService extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getUserPredictions(UserPredictionsRequestProto request,
                                   StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("gRPC-запрос: getUserPredictions для userId={}, maxResults={}",
                    request.getUserId(), request.getMaxResults());
            List<RecommendedEventProto> result = recommendationService.getUserPredictions(
                    request.getUserId(), request.getMaxResults());
            result.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка в getUserPredictions", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("gRPC-запрос: getSimilarEvents для eventId={}, userId={}, maxResults={}",
                    request.getEventId(), request.getUserId(), request.getMaxResults());
            List<RecommendedEventProto> result = recommendationService.getSimilarEvents(
                    request.getEventId(), request.getUserId(), request.getMaxResults());
            result.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка в getSimilarEvents", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("gRPC-запрос: getInteractionsCount для eventIds={}", request.getEventIdList());
            List<RecommendedEventProto> result = recommendationService.getInteractionsCount(request.getEventIdList());
            result.forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Ошибка в getInteractionsCount", e);
            responseObserver.onError(e);
        }
    }

}
