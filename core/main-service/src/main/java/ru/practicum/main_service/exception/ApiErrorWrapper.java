package ru.practicum.main_service.exception; // перенесла в юзер-сервис

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorWrapper {
    private ApiError error;
}
