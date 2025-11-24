package ru.astalavista.taskManagSys.service;

import org.springframework.stereotype.Service;
import ru.astalavista.taskManagSys.models.Project;

@Service
public class TaskNumberGeneratorService {

    public String generateNumber(Project project) {
        // Найти последнюю задачу в проекте
        // Взять её номер, увеличить на 1
        // Вернуть "ALV-013"

    }
}
