package ru.astalavista.taskManagSys.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.astalavista.taskManagSys.model.entity.Project;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.repository.TaskRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskNumberGeneratorService {

    private final TaskRepository taskRepository;

    public String generateNumber(Project project) {
        if (project == null || project.getCode() == null || project.getId() == null) {
            return "TEMP-001";
        }

        // Используем новый метод: findTopByProjectIdOrderByPublicIdDesc
        Optional<Task> lastTask = taskRepository
                .findTopByProjectIdOrderByPublicIdDesc(project.getId());

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
                // Логирование ошибки
                System.err.println("Error parsing publicId: " + lastPublicId + " - " + e.getMessage());
            }
        }

        return String.format("%s-%03d", projectCode, nextNumber);
    }

    // Альтернативный метод для генерации номера по ID проекта
    public String generateNumber(Long projectId, String projectCode) {
        if (projectId == null || projectCode == null) {
            return "TEMP-001";
        }

        Optional<Task> lastTask = taskRepository
                .findTopByProjectIdOrderByPublicIdDesc(projectId);

        String code = projectCode.toUpperCase();
        int nextNumber = 1;

        if (lastTask.isPresent() && lastTask.get().getPublicId() != null) {
            String lastPublicId = lastTask.get().getPublicId();
            try {
                String[] parts = lastPublicId.split("-");
                if (parts.length == 2 && parts[0].equals(code)) {
                    nextNumber = Integer.parseInt(parts[1]) + 1;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error parsing publicId: " + lastPublicId + " - " + e.getMessage());
            }
        }

        return String.format("%s-%03d", code, nextNumber);
    }
}