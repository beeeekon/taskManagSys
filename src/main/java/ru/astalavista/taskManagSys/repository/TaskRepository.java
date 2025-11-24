package ru.astalavista.taskManagSys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.astalavista.taskManagSys.models.Employee;
import ru.astalavista.taskManagSys.models.Task;
import ru.astalavista.taskManagSys.models.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Найти все задачи конкретного исполнителя
    List<Task> findByAssignee(Employee assignee);

    // Найти просроченные задачи
    List<Task> findByDueDateBeforeAndStatusNot(LocalDateTime date, TaskStatus status);
}