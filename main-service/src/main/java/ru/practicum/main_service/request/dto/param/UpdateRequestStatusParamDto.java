package ru.practicum.main_service.request.dto.param;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main_service.request.dto.EventRequestStatusUpdateRequest;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRequestStatusParamDto {
    @NotNull
    private Long userId;
    @NotNull
    private Long eventId;
    @NotNull
    private EventRequestStatusUpdateRequest updateRequest;
}