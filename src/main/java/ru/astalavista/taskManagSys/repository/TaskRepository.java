package ru.astalavista.taskManagSys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByProjectId(Long projectId);

    List<Task> findByStatus(TaskStatus status);

    @Query("SELECT t FROM Task t WHERE " +
            "t.dueDate < :now AND " +
            "t.status != ru.astalavista.taskManagSys.model.enums.TaskStatus.CLOSED")
    List<Task> findOverdueTasks(@Param("now") LocalDateTime now);

    default List<Task> findOverdueTasks() {
        return findOverdueTasks(LocalDateTime.now());
    }

    @Query("SELECT t FROM Task t WHERE t.status != ru.astalavista.taskManagSys.model.enums.TaskStatus.CLOSED")
    List<Task> findOpenTasks();

    List<Task> findByDueDateBeforeAndStatusNot(LocalDateTime date, TaskStatus status);

    // Исправленный метод: поиск по projectId, а не по Project entity
    Optional<Task> findTopByProjectIdOrderByPublicIdDesc(Long projectId);

    // Сохраняем старый метод для совместимости (опционально)
    default Optional<Task> findTopByProjectOrderByPublicIdDesc(ru.astalavista.taskManagSys.model.entity.Project project) {
        if (project == null || project.getId() == null) {
            return Optional.empty();
        }
        return findTopByProjectIdOrderByPublicIdDesc(project.getId());
    }

    // Для поиска по публичному ID
    Optional<Task> findByPublicId(String publicId);

    // Поиск по названию (содержит текст)
    @Query("SELECT t FROM Task t WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Task> findByTitleContainingIgnoreCase(@Param("keyword") String keyword);
}