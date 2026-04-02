package ru.practicum.main_service.compilation.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main_service.compilation.dto.CompilationDto;
import ru.practicum.main_service.compilation.dto.param.GetCompilationsDto;
import ru.practicum.main_service.compilation.dto.param.NewCompilationDto;
import ru.practicum.main_service.compilation.dto.param.UpdateCompilationRequest;
import ru.practicum.main_service.compilation.mapper.CompilationMapper;
import ru.practicum.main_service.compilation.model.Compilation;
import ru.practicum.main_service.compilation.repository.CompilationRepository;
import ru.practicum.main_service.event.model.Event;
import ru.practicum.main_service.event.repository.EventRepository;
import ru.practicum.main_service.exception.ConflictException;
import ru.practicum.main_service.exception.NotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {
    private static final String COMPILATION_NOT_FOUND = "Подборка с ID %s не найдена";

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;

    @Override
    public List<CompilationDto> getCompilations(GetCompilationsDto params) {
        log.info("Получение подборок: pinned={}, from={}, size={}", params.getPinned(), params.getFrom(), params.getSize());

        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());

        List<Compilation> compilations = (params.getPinned() != null) ? compilationRepository.findAllByPinned(params.getPinned(), pageable).getContent() : compilationRepository.findAll(pageable).getContent();

        return compilations.stream().map(compilationMapper::toDto).toList();
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки с ID: {}", compId);

        Compilation compilation = compilationRepository.findByIdWithEvents(compId).orElseThrow(() -> new NotFoundException(String.format(COMPILATION_NOT_FOUND, compId)));

        return compilationMapper.toDto(compilation);
    }

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto compilationDto) {
        log.info("Создание новой подборки: {}", compilationDto.getTitle());

        if (compilationRepository.existsByTitle(compilationDto.getTitle())) {
            throw new ConflictException("Подборка с названием '" + compilationDto.getTitle() + "' уже существует");
        }

        Compilation compilation = compilationMapper.toEntity(compilationDto);

        Set<Event> events = new HashSet<>();
        if (compilationDto.getEvents() != null && !compilationDto.getEvents().isEmpty()) {
            events = new HashSet<>(eventRepository.findAllById(compilationDto.getEvents()));
        }
        compilation.setEvents(events);

        Compilation saved = compilationRepository.save(compilation);
        log.info("Подборка создана с ID: {}", saved.getId());

        return compilationMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с ID: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException(String.format(COMPILATION_NOT_FOUND, compId));
        }

        compilationRepository.deleteById(compId);
        log.info("Подборка с ID {} удалена", compId);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest) {
        log.info("Обновление подборки с ID: {}", compId);

        Compilation compilation = compilationRepository.findByIdWithEvents(compId).orElseThrow(() -> new NotFoundException(String.format(COMPILATION_NOT_FOUND, compId)));

        if (updateRequest.getTitle() != null && !updateRequest.getTitle().isBlank()) {
            if (!compilation.getTitle().equals(updateRequest.getTitle()) && compilationRepository.existsByTitle(updateRequest.getTitle())) {
                throw new ConflictException("Подборка с названием '" + updateRequest.getTitle() + "' уже существует");
            }
            compilation.setTitle(updateRequest.getTitle());
        }

        if (updateRequest.getPinned() != null) {
            compilation.setPinned(updateRequest.getPinned());
        }

        if (updateRequest.getEvents() != null) {
            Set<Event> events = new HashSet<>(eventRepository.findAllById(updateRequest.getEvents()));
            compilation.setEvents(events);
        }

        Compilation updated = compilationRepository.save(compilation);
        log.info("Подборка с ID {} обновлена", compId);

        return compilationMapper.toDto(updated);
    }
}