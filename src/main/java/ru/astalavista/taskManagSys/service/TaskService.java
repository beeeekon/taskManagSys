/*package ru.astalavista.taskManagSys.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astalavista.taskManagSys.config.AuditContextProvider;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.entity.TaskAudit;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;
import ru.astalavista.taskManagSys.model.mapper.TaskMapper;
import ru.astalavista.taskManagSys.repository.TaskAuditRepository;
import ru.astalavista.taskManagSys.repository.TaskRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repository;
    private final TaskAuditRepository auditRepository;
    private final TaskMapper mapper;
    private final TaskNumberGeneratorService numberGeneratorService;
    @Autowired
    private AuditContextProvider auditContextProvider;

    // ---------- CREATE ----------
    @Transactional
    public TaskDTO create(TaskDTO dto, String changedBy, String changeSource) {

        //TODO кто изменил есть инфа в dto, откуда меняли скорее всего тянуть из security
        Task task = mapper.toEntity(dto);
        task.setPublicId(numberGeneratorService.generateNumber(task.getProject()));

        Task saved = repository.save(task);

        createAudit(saved, null, saved.toString(), changedBy, changeSource, "CREATE");

        return mapper.toDTO(saved);
    }

    // ---------- UPDATE ----------
    @Transactional
    public Optional<TaskDTO> update(Long id, TaskDTO dto, String changedBy, String changeSource) {
        return repository.findById(id)
                .map(existing -> {
                    String oldValue = existing.toString();

                    mapper.updateEntityFromDTO(dto, existing);
                    Task updated = repository.save(existing);

                    if (!oldValue.equals(updated.toString())) {
                        createAudit(updated, oldValue, updated.toString(), changedBy, changeSource, "UPDATE");
                    }

                    return mapper.toDTO(updated);
                });
    }

    // ---------- DELETE ----------
    @Transactional
    public void delete(Long id, String changedBy, String changeSource) {
        repository.findById(id).ifPresent(task -> {
            String oldValue = task.toString();
            repository.delete(task);

            createAudit(task, oldValue, null, changedBy, changeSource, "DELETE");
        });
    }

    // ---------- CHANGE STATUS ----------
    @Transactional
    public TaskDTO changeStatus(Long taskId, TaskStatus newStatus, String changedBy, String changeSource) {
        Task task = repository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (task.getStatus() != newStatus) {
            String oldValue = task.toString();
            task.setStatus(newStatus);
            Task updated = repository.save(task);

            createAudit(updated, oldValue, updated.toString(), changedBy, changeSource, "UPDATE");
            return mapper.toDTO(updated);
        }
        return mapper.toDTO(task);
    }

    // ---------- READ ----------
    public List<TaskDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDTO).toList();
    }

    public Optional<TaskDTO> findById(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }

    public List<TaskDTO> getTasksByProject(Long projectId) {
        // TODO: реализовать через repository
        return List.of();
    }

    public List<TaskDTO> getOverdueTasks() {
        // TODO: реализовать через кастомный метод репозитория
        return List.of();
    }

    // ---------- AUDIT ----------
    private void createAudit(Task task, String oldValue, String newValue, String changedBy, String changeSource, String action) {
        TaskAudit audit = new TaskAudit();
        audit.setTask(task);
        audit.setFieldName(action);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setChangedBy(changedBy);
        audit.setChangeSource(changeSource);
        auditRepository.save(audit);
    }
}
*/

package ru.astalavista.taskManagSys.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.entity.Project;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;
import ru.astalavista.taskManagSys.model.mapper.TaskMapper;
import ru.astalavista.taskManagSys.repository.EmployeeRepository;
import ru.astalavista.taskManagSys.repository.ProjectRepository;
import ru.astalavista.taskManagSys.repository.TaskRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final TaskMapper taskMapper;
    private final TaskNumberGeneratorService numberGeneratorService;
    private final TaskAuditLoggerService taskAuditLogger;

    // ---------- CREATE ----------
    @Transactional
    public TaskDTO create(TaskDTO dto) {
        log.info("Creating new task: {}", dto.getTitle());

        // 1. Валидация и получение зависимостей
        Project project = validateAndGetProject(dto.getProjectId());
        Employee assignee = getEmployeeIfExists(dto.getAssigneeId());

        // 2. Маппинг DTO -> Entity
        Task task = taskMapper.toEntity(dto);
        task.setProject(project);
        task.setAssignee(assignee);

        // 3. Установка дефолтных значений
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.REGISTERED);
        }

        // 4. Генерация публичного ID
        task.setPublicId(numberGeneratorService.generateNumber(project));

        // 5. Обработка связанных задач
        processLinkedTasks(task, dto.getLinkedTaskIds());

        // 6. Сохранение
        Task savedTask = taskRepository.save(task);
        log.info("Task created successfully: {} with ID: {}", savedTask.getPublicId(), savedTask.getId());

        // 7. Логирование аудита (отдельный метод/сервис)
        taskAuditLogger.logTaskCreation(savedTask);

        return taskMapper.toDTO(savedTask);
    }

    // ---------- UPDATE ----------
    @Transactional
    public Optional<TaskDTO> update(Long id, TaskDTO dto) {
        log.info("Updating task with ID: {}", id);

        return taskRepository.findById(id)
                .map(existingTask -> {
                    // Сохраняем старое состояние для сравнения
                    String oldState = TaskAuditHelperService.getTaskState(existingTask);

                    // Обновляем основные поля через маппер
                    taskMapper.updateEntityFromDTO(dto, existingTask);

                    // Обновляем зависимости если изменились
                    updateDependencies(existingTask, dto);

                    // Обновляем связанные задачи
                    processLinkedTasks(existingTask, dto.getLinkedTaskIds());

                    // Сохраняем изменения
                    Task updatedTask = taskRepository.save(existingTask);
                    String newState = TaskAuditHelperService.getTaskState(updatedTask);

                    // Логируем изменения если они были
                    if (!oldState.equals(newState)) {
                        taskAuditLogger.logTaskUpdate(updatedTask, oldState, newState);
                    }

                    log.info("Task updated successfully: {}", updatedTask.getPublicId());
                    return taskMapper.toDTO(updatedTask);
                });
    }

    // ---------- DELETE ----------
    @Transactional
    public void delete(Long id) {
        log.info("Deleting task with ID: {}", id);

        taskRepository.findById(id).ifPresent(task -> {
            String oldState = TaskAuditHelperService.getTaskState(task);

            // Сначала логируем удаление
            taskAuditLogger.logTaskDeletion(task, oldState);

            // Затем удаляем
            taskRepository.delete(task);
            log.info("Task deleted successfully: {}", task.getPublicId());
        });
    }

    // ---------- CHANGE STATUS ----------
    @Transactional
    public TaskDTO changeStatus(Long taskId, TaskStatus newStatus) {
        log.info("Changing status for task ID: {} to: {}", taskId, newStatus);

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found with ID: " + taskId));

        TaskStatus oldStatus = task.getStatus();

        if (oldStatus != newStatus) {
            // Логируем изменение статуса
            taskAuditLogger.logStatusChange(
                    task,
                    oldStatus.toString(),
                    newStatus.toString()
            );

            // Обновляем статус
            task.setStatus(newStatus);
            Task updatedTask = taskRepository.save(task);

            log.info("Task status changed: {} -> {}", oldStatus, newStatus);
            return taskMapper.toDTO(updatedTask);
        }

        return taskMapper.toDTO(task);
    }

    // ---------- UTILITY METHODS ----------
    @Transactional
    public void addTimeSpent(Long taskId, Integer hours) {
        taskRepository.findById(taskId).ifPresent(task -> {
            Integer oldTime = task.getTimeSpent() != null ? task.getTimeSpent() : 0;
            Integer newTime = oldTime + hours;

            task.setTimeSpent(newTime);
            taskRepository.save(task);

            // Логируем изменение времени
            taskAuditLogger.logFieldChange(
                    task,
                    "timeSpent",
                    oldTime,
                    newTime
            );
        });
    }

    @Transactional
    public TaskDTO linkTasks(Long taskId, Long linkedTaskId) {
        Task mainTask = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Main task not found"));

        Task linkedTask = taskRepository.findById(linkedTaskId)
                .orElseThrow(() -> new RuntimeException("Linked task not found"));

        List<Task> linkedTasks = mainTask.getLinkedTasks();
        if (linkedTasks == null) {
            linkedTasks = new ArrayList<>();
        }

        if (!linkedTasks.contains(linkedTask)) {
            linkedTasks.add(linkedTask);
            mainTask.setLinkedTasks(linkedTasks);

            Task updatedTask = taskRepository.save(mainTask);

            // Логируем связывание задач
            taskAuditLogger.logCustomAction(
                    updatedTask,
                    "TASK_LINK",
                    String.format("Linked with task #%s (%s)",
                            linkedTask.getPublicId(), linkedTask.getTitle())
            );

            return taskMapper.toDTO(updatedTask);
        }

        return taskMapper.toDTO(mainTask);
    }

    // ---------- READ METHODS ----------
    public List<TaskDTO> findAll() {
        return taskRepository.findAll().stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    public Optional<TaskDTO> findById(Long id) {
        return taskRepository.findById(id)
                .map(taskMapper::toDTO);
    }

    public List<TaskDTO> findTasksByAssignee(Long assigneeId) {
        return taskRepository.findByAssigneeId(assigneeId).stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    public List<TaskDTO> findTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId).stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    public List<TaskDTO> findTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    public List<TaskDTO> findOverdueTasks() {
        return taskRepository.findOverdueTasks().stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    public List<TaskDTO> findOpenTasks() {
        return taskRepository.findOpenTasks().stream()
                .map(taskMapper::toDTO)
                .toList();
    }

    // ---------- PRIVATE HELPER METHODS ----------
    private Project validateAndGetProject(Long projectId) {
        if (projectId == null) {
            throw new IllegalArgumentException("Project ID is required");
        }

        return projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found with ID: " + projectId));
    }

    private Employee getEmployeeIfExists(Long employeeId) {
        if (employeeId == null) {
            return null;
        }

        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));
    }

    private void processLinkedTasks(Task task, List<Long> linkedTaskIds) {
        if (linkedTaskIds == null || linkedTaskIds.isEmpty()) {
            task.setLinkedTasks(new ArrayList<>());
            return;
        }

        List<Task> linkedTasks = new ArrayList<>();
        for (Long linkedId : linkedTaskIds) {
            Task linkedTask = taskRepository.findById(linkedId)
                    .orElseThrow(() -> new RuntimeException("Linked task not found with ID: " + linkedId));
            linkedTasks.add(linkedTask);
        }

        task.setLinkedTasks(linkedTasks);
    }

    private void updateDependencies(Task task, TaskDTO dto) {
        // Обновляем проект если изменился
        if (dto.getProjectId() != null &&
                (task.getProject() == null || !task.getProject().getId().equals(dto.getProjectId()))) {
            Project newProject = projectRepository.findById(dto.getProjectId())
                    .orElseThrow(() -> new RuntimeException("Project not found"));
            task.setProject(newProject);
        }

        // Обновляем исполнителя если изменился
        if (dto.getAssigneeId() != null &&
                (task.getAssignee() == null || !task.getAssignee().getId().equals(dto.getAssigneeId()))) {
            Employee newAssignee = employeeRepository.findById(dto.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            task.setAssignee(newAssignee);
        }
    }

    public Optional<TaskDTO> findByPublicId(String publicId) {
        return taskRepository.findAll().stream()
                .filter(task -> publicId.equals(task.getPublicId()))
                .findFirst()
                .map(taskMapper::toDTO);
    }
}