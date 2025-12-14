package ru.astalavista.taskManagSys.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.repository.EmployeeRepository;
import ru.astalavista.taskManagSys.service.TaskService;
import ru.astalavista.taskManagSys.telegram.TaskManagementBot;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableScheduling
public class NotificationService {
    
    private final TaskService taskService;
    private final EmployeeRepository employeeRepository;
    private final TaskManagementBot bot;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm");
    
    // Ежедневный отчет в 9:00
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyReports() {
        log.info("Sending daily reports...");
        
        List<Employee> employees = employeeRepository.findAll();
        
        for (Employee employee : employees) {
            if (employee.getTelegramChatId() != null && !employee.getTelegramChatId().isEmpty()) {
                try {
                    sendDailyReportToEmployee(employee);
                } catch (Exception e) {
                    log.error("Failed to send daily report to employee {}", employee.getEmail(), e);
                }
            }
        }
    }
    
    // Проверка просроченных задач каждые 30 минут
    @Scheduled(cron = "0 */30 * * * *")
    public void checkOverdueTasks() {
        log.info("Checking for overdue tasks...");
        
        List<TaskDTO> overdueTasks = taskService.findOverdueTasks();
        
        for (TaskDTO task : overdueTasks) {
            if (task.getAssigneeId() != null) {
                employeeRepository.findById(task.getAssigneeId()).ifPresent(employee -> {
                    if (employee.getTelegramChatId() != null && 
                        !employee.getTelegramChatId().isEmpty()) {
                        
                        String message = String.format(
                            "⚠️ *ВНИМАНИЕ: Просроченная задача!*\n\n" +
                            "Задача: *%s* (#%s)\n" +
                            "Срок был: %s\n" +
                            "Текущий статус: %s\n\n" +
                            "Пожалуйста, выполните задачу как можно скорее!",
                            task.getTitle(),
                            task.getPublicId(),
                            task.getDueDate() != null ? 
                                task.getDueDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")) : "Не установлен",
                            task.getStatus()
                        );
                        
                        bot.sendNotification(employee.getTelegramChatId(), message);
                    }
                });
            }
        }
    }
    
    private void sendDailyReportToEmployee(Employee employee) {
        List<TaskDTO> employeeTasks = taskService.findTasksByAssignee(employee.getId());
        long openTasks = employeeTasks.stream()
            .filter(task -> task.getStatus() != ru.astalavista.taskManagSys.model.enums.TaskStatus.CLOSED)
            .count();
        long overdueTasks = employeeTasks.stream()
            .filter(task -> task.getStatus() != ru.astalavista.taskManagSys.model.enums.TaskStatus.CLOSED)
            .filter(task -> task.getDueDate() != null && 
                           task.getDueDate().isBefore(LocalDateTime.now()))
            .count();
        
        String report = String.format(
            "📊 *Ежедневный отчет %s*\n\n" +
            "Доброе утро, %s! 👋\n\n" +
            "*Статистика на сегодня:*\n" +
            "Всего задач: %d\n" +
            "Активных задач: %d\n" +
            "Просроченных задач: %d\n\n" +
            "Хорошего дня и продуктивной работы! 💪",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            employee.getFullName(),
            employeeTasks.size(),
            openTasks,
            overdueTasks
        );
        
        bot.sendNotification(employee.getTelegramChatId(), report);
    }
}