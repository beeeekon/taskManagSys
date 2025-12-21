package ru.astalavista.taskManagSys.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.astalavista.taskManagSys.model.dto.task.TaskCreateDTO;
import ru.astalavista.taskManagSys.model.dto.task.TaskDTO;
import ru.astalavista.taskManagSys.model.dto.task.TaskUpdateDTO;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.entity.Project;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;
import ru.astalavista.taskManagSys.model.event.TaskCompletedEvent;
import ru.astalavista.taskManagSys.model.event.TaskCreatedEvent;
import ru.astalavista.taskManagSys.model.mapper.TaskMapper;
import ru.astalavista.taskManagSys.repository.EmployeeRepository;
import ru.astalavista.taskManagSys.repository.ProjectRepository;
import ru.astalavista.taskManagSys.repository.TaskRepository;

import java.time.LocalDateTime;
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
    private final ApplicationEventPublisher taskEventPublisher;

    @Transactional
    public TaskDTO create(TaskCreateDTO dto) {
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

        // 7. Логирование аудита
        taskAuditLogger.logTaskCreation(savedTask);

        // 8. Отправляем уведомление в Telegram
        taskEventPublisher.publishEvent(
            new TaskCreatedEvent(savedTask)
        );

        return taskMapper.toDTO(savedTask);
    }

    @Transactional
    public Optional<TaskDTO> update(Long id, TaskUpdateDTO dto) {
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

            // Отправляем уведомление если задача закрыта
            if (newStatus == TaskStatus.CLOSED) {
                taskEventPublisher.publishEvent(
                    new TaskCompletedEvent(updatedTask)
                );
            }

            log.info("Task status changed: {} -> {}", oldStatus, newStatus);
            return taskMapper.toDTO(updatedTask);
        }

        return taskMapper.toDTO(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findFilteredTasks(Long assigneeId, Long projectId,
                                           TaskStatus status, boolean onlyOpen,
                                           boolean onlyOverdue) {
        log.info("Filtering tasks: assigneeId={}, projectId={}, status={}, onlyOpen={}, onlyOverdue={}",
                assigneeId, projectId, status, onlyOpen, onlyOverdue);

        // Получаем все задачи
        List<Task> allTasks = taskRepository.findAll();

        // Фильтруем
        return allTasks.stream()
            .filter(task -> assigneeId == null ||
                    (task.getAssignee() != null && task.getAssignee().getId().equals(assigneeId)))
            .filter(task -> projectId == null ||
                    (task.getProject() != null && task.getProject().getId().equals(projectId)))
            .filter(task -> status == null || task.getStatus() == status)
            .filter(task -> !onlyOpen || task.getStatus() != TaskStatus.CLOSED)
            .filter(task -> !onlyOverdue || isTaskOverdue(task))
            .map(taskMapper::toDTO)
            .toList();
    }

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

    @Transactional(readOnly = true)
    public List<TaskDTO> findAll() {
        return taskRepository.findAll().stream()
            .map(taskMapper::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<TaskDTO> findById(Long id) {
        return taskRepository.findById(id)
            .map(taskMapper::toDTO);
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findTasksByAssignee(Long assigneeId) {
        return taskRepository.findByAssigneeId(assigneeId).stream()
            .map(taskMapper::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findTasksByProject(Long projectId) {
        return taskRepository.findByProjectId(projectId).stream()
            .map(taskMapper::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
            .map(taskMapper::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findOverdueTasks() {
        return taskRepository.findOverdueTasks().stream()
            .map(taskMapper::toDTO)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskDTO> findOpenTasks() {
        return taskRepository.findOpenTasks().stream()
            .map(taskMapper::toDTO)
            .toList();
    }

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

    private void updateDependencies(Task task, TaskUpdateDTO dto) {
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

    private boolean isTaskOverdue(Task task) {
        if (task.getStatus() == TaskStatus.CLOSED) {
            return false;
        }

        if (task.getDueDate() == null) {
            return false;
        }

        return task.getDueDate().isBefore(LocalDateTime.now());
    }

    /**
     * Находит задачу по публичному ID
     */
    @Transactional(readOnly = true)
    public Optional<TaskDTO> findByPublicId(String publicId) {
        return taskRepository.findAll().stream()
                .filter(task -> publicId != null && publicId.equals(task.getPublicId()))
                .findFirst()
                .map(taskMapper::toDTO);
    }

    /**
     * Получает задачи с истекшим сроком для конкретного сотрудника
     */
    @Transactional(readOnly = true)
    public List<TaskDTO> findOverdueTasksByAssignee(Long assigneeId) {
        List<TaskDTO> allTasks = findTasksByAssignee(assigneeId);
        return allTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CLOSED)
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now()))
                .toList();
    }

    /**
     * Получает статистику по задачам сотрудника
     */
    @Transactional(readOnly = true)
    public TaskStatistics getTaskStatistics(Long employeeId) {
        List<TaskDTO> employeeTasks = findTasksByAssignee(employeeId);

        long totalTasks = employeeTasks.size();
        long openTasks = employeeTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CLOSED)
                .count();
        long overdueTasks = employeeTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CLOSED)
                .filter(task -> task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now()))
                .count();
        long completedTasks = employeeTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.CLOSED)
                .count();

        return new TaskStatistics(totalTasks, openTasks, overdueTasks, completedTasks);
    }

    /**
     * Внутренний класс для статистики задач
     */
    @Getter
    public static class TaskStatistics {
        private final long totalTasks;
        private final long openTasks;
        private final long overdueTasks;
        private final long completedTasks;

        public TaskStatistics(long totalTasks, long openTasks, long overdueTasks, long completedTasks) {
            this.totalTasks = totalTasks;
            this.openTasks = openTasks;
            this.overdueTasks = overdueTasks;
            this.completedTasks = completedTasks;
        }

        @Override
        public String toString() {
            return String.format("Total: %d, Open: %d, Overdue: %d, Completed: %d",
                    totalTasks, openTasks, overdueTasks, completedTasks);
        }
    }
}