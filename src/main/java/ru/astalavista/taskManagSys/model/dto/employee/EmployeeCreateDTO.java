package ru.astalavista.taskManagSys.model.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// DTO для создания нового сотрудника
@Data
public class EmployeeCreateDTO {

    // Полное имя
    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    // Email
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    private String email;

    // Должность
    @NotBlank(message = "Position is required")
    @Size(max = 100, message = "Position cannot exceed 100 characters")
    private String position;

    // Telegram chat ID для уведомлений
    @NotBlank(message = "Telegram chat id is required")
    @Size(max = 100, message = "Telegram chat ID cannot exceed 100 characters")
    private String telegramChatId;

    // ID задач
    private List<Long> assignedTaskIds = new ArrayList<>();
}
