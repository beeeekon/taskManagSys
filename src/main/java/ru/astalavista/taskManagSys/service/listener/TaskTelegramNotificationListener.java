package ru.astalavista.taskManagSys.service.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.event.TaskCompletedEvent;
import ru.astalavista.taskManagSys.model.event.TaskCreatedEvent;
import ru.astalavista.taskManagSys.telegram.TaskManagementBot;

@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTelegramNotificationListener {

    private final TaskManagementBot taskManagementBot;

    /**
     * Новая задача
     */
    @EventListener
    public void onTaskCreated(TaskCreatedEvent event) {
        Task task = event.task();
        Employee assignee = task.getAssignee();

        if (assignee == null || assignee.getTelegramChatId() == null) {
            log.debug("No Telegram notification sent: assignee is null or has no chatId");
            return;
        }

        try {
            log.info("Telegram: new task {} for {}", task.getPublicId(), assignee.getEmail());

            taskManagementBot.sendNewTaskNotification(
                assignee.getTelegramChatId(),
                task.getTitle(),
                task.getPublicId()
            );
        } catch (Exception e) {
            log.error("Failed to send Telegram notification for task {}", task.getPublicId(), e);
        }
    }

    /**
     * Завершение задачи
     */
    @EventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        Task task = event.task();
        Employee assignee = task.getAssignee();

        if (assignee == null || assignee.getTelegramChatId() == null) {
            return;
        }

        try {
            log.info("Telegram: task completed {} for {}", task.getPublicId(), assignee.getEmail());

            taskManagementBot.sendTaskCompletedNotification(
                    assignee.getTelegramChatId(),
                    task.getTitle(),
                    task.getPublicId()
            );
        } catch (Exception e) {
            log.error("Failed to send Telegram completion notification for task {}", task.getPublicId(), e);
        }
    }
}
