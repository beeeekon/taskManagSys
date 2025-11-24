package ru.astalavista.taskManagSys.service;

import org.springframework.stereotype.Service;
import ru.astalavista.taskManagSys.models.Task;
import ru.astalavista.taskManagSys.models.TaskStatus;

@Service
public class TaskService {

    public Task createTask(Task task) {
        // 1. Сгенерировать красивый номер
        // 2. Сохранить задачу
        // 3. Записать в журнал "создана новая задача"
    }

    public Task changeStatus(Long taskId, TaskStatus newStatus, String whoChanged) {
        // 1. Найти задачу
        // 2. Запомнить старый статус
        // 3. Изменить статус
        // 4. Записать в журнал: "статус поменялся с X на Y"
    }
}