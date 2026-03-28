package ru.practicum.stat_server.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.stat_dto.EndpointHitDto;
import ru.practicum.stat_dto.ViewStatsDto;
import ru.practicum.stat_server.exception.ValidationException;
import ru.practicum.stat_server.mapper.StatMapper;
import ru.practicum.stat_server.model.EndpointHit;
import ru.practicum.stat_server.repository.StatRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatRepository statRepository;
    private final StatMapper statMapper;

    public void saveHit(EndpointHitDto hitDto) {
        EndpointHit hit = statMapper.toEntity(hitDto);
        statRepository.save(hit);
    }

    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, Boolean unique) {
        LocalDateTime startTime = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(end, FORMATTER);
        if (startTime.isAfter(endTime)) {
            throw new ValidationException("старт дата должны быть до конечной даты");
        }
        if (startTime.isAfter(LocalDateTime.now())) {
            throw new ValidationException("Дата начала не может быть в будущем");
        }

        if (Boolean.TRUE.equals(unique)) {
            return statRepository.getUniqueStats(startTime, endTime, uris);
        } else {
            return statRepository.getStats(startTime, endTime, uris);
        }
    }
}