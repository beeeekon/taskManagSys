package ru.astalavista.taskManagSys.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для сущности Project
 */
@Data
public class ProjectDTO {
    // ID
    private Long id;

    // Уникальный код
    private String code;

    // Название
    private String name;

    // Подробное описание
    private String description;

    // Дата создания
    private LocalDateTime createdAt;

    // Дата последнего обновления
    private LocalDateTime updatedAt;

    // ID задач
    private List<Long> taskIds;
}
