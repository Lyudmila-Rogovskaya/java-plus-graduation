package ru.practicum.main_service.event.dto.param;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main_service.event.validation.ValidModerationComment;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventAdminRequestWithComment {

    @Valid
    @NotNull
    private UpdateEventAdminRequest updateEvent;

    @ValidModerationComment
    private String moderationComment;

}
