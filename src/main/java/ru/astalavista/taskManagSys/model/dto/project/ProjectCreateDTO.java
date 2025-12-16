package ru.astalavista.taskManagSys.model.dto.project;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// DTO для создания нового проекта
@Data
public class ProjectCreateDTO {

    // Уникальный код
    @NotBlank(message = "Project code is required")
    private String code;

    // Название
    @NotBlank(message = "Project name is required")
    private String name;

    // Подробное описание
    private String description;

    // ID задач
    private List<Long> taskIds = new ArrayList<>();
}
