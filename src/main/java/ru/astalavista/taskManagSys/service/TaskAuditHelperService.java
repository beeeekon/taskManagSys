package ru.astalavista.taskManagSys.service;

import lombok.experimental.UtilityClass;
import ru.astalavista.taskManagSys.model.entity.Task;

//класс для детального аудита (для замены toString)
//@UtilityClass
public class TaskAuditHelperService {
    // Приватный конструктор, чтобы нельзя было создать экземпляр
    private TaskAuditHelperService() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String getTaskState(Task task) {
        if (task == null) return "null";

        StringBuilder sb = new StringBuilder();
        sb.append("Task#").append(task.getId()).append(" {");
        sb.append("publicId: ").append(task.getPublicId()).append(", ");
        sb.append("title: ").append(task.getTitle()).append(", ");
        sb.append("status: ").append(task.getStatus()).append(", ");
        sb.append("project: ").append(task.getProject() != null ? task.getProject().getId() : "null").append(", ");
        sb.append("assignee: ").append(task.getAssignee() != null ? task.getAssignee().getId() : "null").append(", ");
        sb.append("dueDate: ").append(task.getDueDate()).append(", ");
        sb.append("timeSpent: ").append(task.getTimeSpent());
        sb.append("}");

        return sb.toString();
    }

    public static String getFieldChangeState(String fieldName, Object oldValue, Object newValue) {
        return String.format("%s: %s -> %s",
                fieldName,
                oldValue != null ? oldValue : "null",
                newValue != null ? newValue : "null");
    }
}