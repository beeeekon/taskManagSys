package ru.astalavista.taskManagSys.model.enums;

import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum TaskType {
    TASK,               // Задача
    BUG,                // Ошибка
    REQUIREMENT         // Требование
}
