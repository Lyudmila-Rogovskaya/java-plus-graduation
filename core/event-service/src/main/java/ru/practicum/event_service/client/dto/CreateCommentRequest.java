package ru.practicum.event_service.client.dto; // новый

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    private Long eventId;
    private Long adminId;
    private String commentText;

}
