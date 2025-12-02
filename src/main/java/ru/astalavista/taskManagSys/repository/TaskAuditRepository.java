package ru.astalavista.taskManagSys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.astalavista.taskManagSys.model.entity.TaskAudit;

@Repository
public interface TaskAuditRepository extends JpaRepository<TaskAudit, Long> {

}
