package ru.astalavista.taskManagSys.models;

public enum TaskStatus {
    REGISTERED,         // зарегистрирован
    IN_PROGRESS,        // в реализации
    UNDER_REVIEW,       // на проверке
    BUG_FIXING,         // на исправлении ошибок
    CLOSED              // закрыт
}
