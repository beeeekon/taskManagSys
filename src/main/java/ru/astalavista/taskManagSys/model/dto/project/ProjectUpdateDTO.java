package ru.astalavista.taskManagSys.model.dto.project;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// DTO для обновления существующего проекта
@Data
public class ProjectUpdateDTO {

    // Уникальный код
    private String code;

    // Название
    private String name;

    // Подробное описание
    private String description;

    // ID задач
    private List<Long> taskIds = new ArrayList<>();
}
