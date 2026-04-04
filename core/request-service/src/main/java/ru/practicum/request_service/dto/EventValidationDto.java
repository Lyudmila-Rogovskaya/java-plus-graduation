package ru.practicum.request_service.dto; // новый

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventValidationDto {

    private Long eventId;
    private Boolean published;
    private Integer participantLimit;
    private Boolean requestModeration;
    private Long initiatorId;
    private String title;

}
