package ru.practicum.main_service.compilation.dto.param;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetCompilationsDto {
    private Boolean pinned;

    @Builder.Default
    @Min(0)
    private Integer from = 0;

    @Builder.Default
    @Min(1)
    private Integer size = 10;
}