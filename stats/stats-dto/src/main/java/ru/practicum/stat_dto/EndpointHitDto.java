package ru.practicum.stat_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EndpointHitDto {
    private Long id;
    @NotBlank(message = "App не может быть пустым")
    @Size(max = 255, message = "App должны быть короче 255 символов")
    private String app;
    @NotBlank(message = "Uri cне может быть пустым")
    @Size(max = 512, message = "Uri должны быть короче 512 символов")
    private String uri;
    @NotBlank(message = "Ip не может быть пустым")
    private String ip;
    @NotBlank(message = "Timestamp не может быть пустым")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$", message = "Timestamp должен соответствовать формату yyyy-MM-dd HH:mm:ss")
    private String timestamp;
}
