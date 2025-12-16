package ru.astalavista.taskManagSys.model.dto.task;

import lombok.Data;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;
import ru.astalavista.taskManagSys.model.enums.TaskType;

import java.time.LocalDateTime;
import java.util.List;

// DTO для обновления задачи
@Data
public class TaskUpdateDTO {

    // Тип
    private TaskType type;

    // Заголовок
    private String title;

    // Подробное описание
    private String description;

    // Дата и время, до которой нужно выполнить задачу
    private LocalDateTime dueDate;

    // Затраченное время на задачу в часах
    private Integer timeSpent;

    // Статус
    private TaskStatus status;

    // ID проекта, к которому относится задача
    private Long projectId;

    // ID исполнителя (сотрудника), если он назначен
    private Long assigneeId;

    // ID связанных задач
    private List<Long> linkedTaskIds;
}
