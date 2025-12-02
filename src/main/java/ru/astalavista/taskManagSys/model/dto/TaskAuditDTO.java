package ru.astalavista.taskManagSys.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * DTO для сущности TaskAudit
 */
@Data
public class TaskAuditDTO {
    // ID
    private Long id;

    // ID задачи, к которой относится запись аудита
    private Long taskId;

    // Название поля, которое было изменено
    private String fieldName;

    // Старое значение поля
    private String oldValue;

    // Новое значение поля
    private String newValue;

    // Кто сделал изменение (имя пользователя или система)
    private String changedBy;

    // Источник изменения (WEB, TELEGRAM_BOT, API)
    private String changeSource;

    // Дата и время изменения
    private LocalDateTime changedAt;
}
