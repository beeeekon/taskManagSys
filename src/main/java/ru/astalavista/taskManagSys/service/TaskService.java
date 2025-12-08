package ru.astalavista.taskManagSys.service;

import org.springframework.transaction.annotation.Transactional;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;
import ru.astalavista.taskManagSys.model.mapper.TaskMapper;
import ru.astalavista.taskManagSys.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

//    public Task createTask(Task task) {
//        // 1. Сгенерировать красивый номер
//        // 2. Сохранить задачу
//        // 3. Записать в журнал "создана новая задача"
//    }
//
//    public Task changeStatus(Long taskId, TaskStatus newStatus, String whoChanged) {
//        // 1. Найти задачу
//        // 2. Запомнить старый статус
//        // 3. Изменить статус
//        // 4. Записать в журнал: "статус поменялся с X на Y"
//    }
    private final TaskRepository repository;
    private final TaskMapper mapper;
    private final TaskNumberGeneratorService numberGeneratorService;

    public TaskDTO create(TaskDTO dto) {
        Task task = mapper.toEntity(dto);
        // Генерация номера задачи
        String publicId = numberGeneratorService.generateNumber(task.getProject());
        task.setPublicId(publicId);
        Task saved = repository.save(task);
        return mapper.toDTO(saved);
    }

    public Optional<TaskDTO> update(Long id, TaskDTO dto) {
        return repository.findById(id)
            .map(existing -> {
                mapper.updateEntityFromDTO(dto, existing);
                Task updated = repository.save(existing);
                return mapper.toDTO(updated);
            });
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<TaskDTO> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDTO)
            .toList();
    }

    public Optional<TaskDTO> findById(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }

    @Transactional
    public TaskDTO changeStatus(Long taskId, TaskStatus newStatus, String changedBy) {
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        TaskStatus oldStatus = task.getStatus();
        task.setStatus(newStatus);

        // TODO: Создать запись в TaskAudit
        // taskAuditService.logStatusChange(task, oldStatus, newStatus, changedBy);

        Task updated = repository.save(task);
        //log.info("Task {} status changed from {} to {} by {}",
          //      taskId, oldStatus, newStatus, changedBy);

        return mapper.toDTO(updated);
    }

    public List<TaskDTO> getTasksByProject(Long projectId) {
        // TODO: Реализовать
        return List.of();
    }

    public List<TaskDTO> getOverdueTasks() {
        // TODO: Реализовать используя кастомный метод репозитория
        return List.of();
    }
}