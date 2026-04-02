package ru.practicum.main_service.event.dto.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventByUserRequest {
    private Long userId;
    private Long eventId;

}
