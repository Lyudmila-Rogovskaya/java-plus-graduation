package ru.practicum.main_service.compilation.dto.param;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewCompilationDto {
    @NotBlank(message = "Title не может быть пустым")
    @Size(max = 50, message = "Title должен быть короче 50 символов")
    private String title;

    @Builder.Default
    private Boolean pinned = false;
    private Set<Long> events;
}