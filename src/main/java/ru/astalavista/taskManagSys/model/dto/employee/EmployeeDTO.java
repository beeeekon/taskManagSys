package ru.astalavista.taskManagSys.model.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO для сущности Employee
 */
@Data
public class EmployeeDTO {
    // ID
    private Long id;

    // Полное имя
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    // Email
    @Email(message = "Email should be valid")
    private String email;

    // Должность
    @Size(max = 100, message = "Position cannot exceed 100 characters")
    private String position;

    // Telegram chat ID для уведомлений
    @Size(max = 100, message = "Telegram chat ID cannot exceed 100 characters")
    private String telegramChatId;

    // Дата создания
    private LocalDateTime createdAt;

    // Дата последнего обновления
    private LocalDateTime updatedAt;

    // ID задач
    private List<Long> assignedTaskIds = new ArrayList<>();
}
