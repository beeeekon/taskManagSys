package ru.astalavista.taskManagSys.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.astalavista.taskManagSys.config.AuditContextProvider;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.entity.TaskAudit;
import ru.astalavista.taskManagSys.repository.TaskAuditRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TaskAuditLoggerService {

    private final TaskAuditRepository taskAuditRepository;
    private final AuditContextProvider auditContextProvider;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTaskCreation(Task task) {
        createAuditRecord(
            task,
            "CREATE",
            null,
            TaskAuditHelperService.getTaskState(task),
            "Task created"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTaskUpdate(Task task, String oldState, String newState) {
        if (!oldState.equals(newState)) {
            createAuditRecord(
                task,
                "UPDATE",
                oldState,
                newState,
                "Task updated"
            );
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTaskDeletion(Task task, String oldState) {
        createAuditRecord(
            task,
            "DELETE",
            oldState,
            null,
            "Task deleted"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logStatusChange(Task task, String oldStatus, String newStatus) {
        createAuditRecord(
            task,
            "STATUS_CHANGE",
            oldStatus,
            newStatus,
            String.format("Status changed: %s -> %s", oldStatus, newStatus)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logFieldChange(Task task, String fieldName, Object oldValue, Object newValue) {
        String changeDescription = TaskAuditHelperService.getFieldChangeState(fieldName, oldValue, newValue);

        createAuditRecord(
            task,
            "FIELD_CHANGE",
            String.valueOf(oldValue),
            String.valueOf(newValue),
            changeDescription
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCustomAction(Task task, String action, String description) {
        createAuditRecord(
            task,
            action,
            null,
            null,
            description
        );
    }

    private void createAuditRecord(Task task, String action, String oldValue,
                                   String newValue, String description) {
        TaskAudit audit = new TaskAudit();
        audit.setTask(task);
        audit.setFieldName(action);
        audit.setOldValue(oldValue);
        audit.setNewValue(newValue);
        audit.setChangedBy(auditContextProvider.getCurrentChangedBy());
        audit.setChangeSource(auditContextProvider.getCurrentChangeSource());
        audit.setChangedAt(LocalDateTime.now());

        taskAuditRepository.save(audit);
    }
}