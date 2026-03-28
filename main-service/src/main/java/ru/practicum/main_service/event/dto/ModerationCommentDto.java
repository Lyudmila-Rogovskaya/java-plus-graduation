package ru.practicum.main_service.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationCommentDto {

    private Long id;
    private Long eventId;
    private Long adminId;
    private String commentText;
    private LocalDateTime createdOn;

}
