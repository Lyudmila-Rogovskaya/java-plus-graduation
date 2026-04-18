package ru.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.entity.EventSimilarity;
import ru.practicum.analyzer.repository.EventSimilarityRepository;
import ru.practicum.ewm.stats.avro.EventsSimilarityAvro;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventSimilarityService {

    private final EventSimilarityRepository eventSimilarityRepository;

    @Transactional
    public void processSimilarity(EventsSimilarityAvro avro) {

        long eventA = avro.getEventA();
        long eventB = avro.getEventB();

        if (eventA > eventB) {
            long tmp = eventA;
            eventA = eventB;
            eventB = tmp;
        }

        EventSimilarity similarity = EventSimilarity.builder()
                .eventA(eventA)
                .eventB(eventB)
                .score(avro.getScore())
                .updated(Instant.ofEpochMilli(avro.getTimestamp().toEpochMilli()))
                .build();

        eventSimilarityRepository.save(similarity);
        log.debug("Saved similarity: eventA={}, eventB={}, score={}", eventA, eventB, avro.getScore());
    }

}
