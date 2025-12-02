package ru.astalavista.taskManagSys.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.astalavista.taskManagSys.model.entity.Project;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

}
