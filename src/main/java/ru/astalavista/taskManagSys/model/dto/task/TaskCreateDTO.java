package ru.astalavista.taskManagSys.model.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;
import ru.astalavista.taskManagSys.model.enums.TaskType;

import java.time.LocalDateTime;
import java.util.List;

// DTO для создания новой задачи
@Data
public class TaskCreateDTO {

    // Тип
    @NotNull(message = "Task type is required")
    private TaskType type;

    // Заголовок
    @NotBlank(message = "Title is required")
    private String title;

    // Подробное описание
    private String description;

    // Дата и время, до которой нужно выполнить задачу
    private LocalDateTime dueDate;

    // Затраченное время на задачу в часах
    private Integer timeSpent;

    // Статус
    @NotNull(message = "Task status is required")
    private TaskStatus status;

    // ID проекта, к которому относится задача
    @NotNull(message = "Project ID is required")
    private Long projectId;

    // ID исполнителя (сотрудника), если он назначен
    private Long assigneeId;

    // ID связанных задач
    private List<Long> linkedTaskIds;
}
