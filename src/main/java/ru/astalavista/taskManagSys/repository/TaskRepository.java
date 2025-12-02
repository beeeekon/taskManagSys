package ru.astalavista.taskManagSys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // Найти все задачи конкретного исполнителя
    List<Task> findByAssignee(Employee assignee);

    // Найти просроченные задачи
    List<Task> findByDueDateBeforeAndStatusNot(LocalDateTime date, TaskStatus status);
}