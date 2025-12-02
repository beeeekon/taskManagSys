package ru.astalavista.taskManagSys.model.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum TaskStatus {
    REGISTERED,         // Зарегистрирована
    IN_PROGRESS,        // В реализации
    UNDER_REVIEW,       // На проверке
    BUG_FIXING,         // На исправлении ошибок
    CLOSED              // Закрыта
}
