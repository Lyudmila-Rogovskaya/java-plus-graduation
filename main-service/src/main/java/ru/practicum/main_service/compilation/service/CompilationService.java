package ru.practicum.main_service.compilation.service;

import ru.practicum.main_service.compilation.dto.CompilationDto;
import ru.practicum.main_service.compilation.dto.param.GetCompilationsDto;
import ru.practicum.main_service.compilation.dto.param.NewCompilationDto;
import ru.practicum.main_service.compilation.dto.param.UpdateCompilationRequest;

import java.util.List;

public interface CompilationService {

    List<CompilationDto> getCompilations(GetCompilationsDto params);

    CompilationDto getCompilationById(Long compId);

    CompilationDto createCompilation(NewCompilationDto compilationDto);

    void deleteCompilation(Long compId);

    CompilationDto updateCompilation(Long compId, UpdateCompilationRequest updateRequest);

}