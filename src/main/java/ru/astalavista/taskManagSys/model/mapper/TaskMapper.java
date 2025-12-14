/*package ru.astalavista.taskManagSys.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Task;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TaskMapper {
    @Mapping(target = "linkedTaskIds", source = "linkedTasks", qualifiedByName = "mapTaskListToIds")
    TaskDTO toDTO(Task task);

    Task toEntity(TaskDTO dto);

    void updateEntityFromDTO(TaskDTO dto, @MappingTarget Task entity);

    @Named("mapTaskListToIds")
    default List<Long> mapTaskListToIds(List<Task> tasks) {
        if (tasks == null|| tasks.isEmpty())
            return List.of();

        return tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
    }
}


 */
package ru.astalavista.taskManagSys.model.mapper;

import org.mapstruct.*;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Project;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    @Mapping(target = "linkedTaskIds", source = "linkedTasks", qualifiedByName = "mapTaskListToIds")
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "assigneeId", source = "assignee.id")
    TaskDTO toDTO(Task task);

    @Mapping(target = "project", source = "projectId", qualifiedByName = "mapProjectIdToEntity")
    @Mapping(target = "assignee", source = "assigneeId", qualifiedByName = "mapEmployeeIdToEntity")
    @Mapping(target = "linkedTasks", ignore = true) // Обрабатывается отдельно в сервисе
    @Mapping(target = "status", defaultValue = "REGISTERED")
    Task toEntity(TaskDTO dto);

    @Mapping(target = "project", source = "projectId", qualifiedByName = "mapProjectIdToEntity")
    @Mapping(target = "assignee", source = "assigneeId", qualifiedByName = "mapEmployeeIdToEntity")
    @Mapping(target = "linkedTasks", ignore = true)
    void updateEntityFromDTO(TaskDTO dto, @MappingTarget Task entity);

    @Named("mapTaskListToIds")
    default List<Long> mapTaskListToIds(List<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return List.of();
        }
        return tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
    }

    @Named("mapProjectIdToEntity")
    default Project mapProjectIdToEntity(Long projectId) {
        if (projectId == null) {
            return null;
        }
        Project project = new Project();
        project.setId(projectId);
        return project;
    }

    @Named("mapEmployeeIdToEntity")
    default Employee mapEmployeeIdToEntity(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        Employee employee = new Employee();
        employee.setId(employeeId);
        return employee;
    }

    @AfterMapping
    default void setDefaultStatus(@MappingTarget Task task) {
        if (task.getStatus() == null) {
            task.setStatus(TaskStatus.REGISTERED);
        }
    }
}