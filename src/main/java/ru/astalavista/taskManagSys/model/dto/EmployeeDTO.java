package ru.astalavista.taskManagSys.model.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO для сущности Employee
 */
@Data
public class EmployeeDTO {
    // ID
    private Long id;

    // Полное имя
    private String fullName;

    // Email
    private String email;

    // Должность
    private String position;

    // Telegram chat ID для уведомлений
    private String telegramChatId;

    // Дата создания
    private LocalDateTime createdAt;

    // Дата последнего обновления
    private LocalDateTime updatedAt;

    // ID задач
    private List<Long> assignedTaskIds;
}
