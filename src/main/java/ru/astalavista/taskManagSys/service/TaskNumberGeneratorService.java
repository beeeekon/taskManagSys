package ru.astalavista.taskManagSys.service;

import org.springframework.stereotype.Service;
import ru.astalavista.taskManagSys.model.entity.Project;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.repository.TaskRepository;

import java.util.Optional;

@Service
public class TaskNumberGeneratorService {

    private TaskRepository taskRepository;
    public String generateNumber(Project project) {
        // Найти последнюю задачу в проекте
        // Взять её номер, увеличить на 1
        // Вернуть "ALV-013"

        if (project == null || project.getCode() == null) {
            return "TEMP-001";
        }

        // Найти последнюю задачу в проекте по publicId
        Optional<Task> lastTask = taskRepository
                .findTopByProjectOrderByPublicIdDesc(project);

        String projectCode = project.getCode().toUpperCase();
        int nextNumber = 1;

        if (lastTask.isPresent() && lastTask.get().getPublicId() != null) {
            String lastPublicId = lastTask.get().getPublicId();
            try {
                String[] parts = lastPublicId.split("-");
                if (parts.length == 2 && parts[0].equals(projectCode)) {
                    nextNumber = Integer.parseInt(parts[1]) + 1;
                }
            } catch (NumberFormatException e) {
                //TODO Логирование ошибки
            }
        }

        return String.format("%s-%03d", projectCode, nextNumber);
    }
}
