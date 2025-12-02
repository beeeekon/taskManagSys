package ru.astalavista.taskManagSys.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.astalavista.taskManagSys.model.dto.ProjectDTO;
import ru.astalavista.taskManagSys.model.entity.Project;
import ru.astalavista.taskManagSys.model.entity.Task;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface ProjectMapper {
    @Mapping(target = "taskIds", source = "tasks", qualifiedByName = "mapTaskListToIds")
    ProjectDTO toDTO(Project project);

    Project toEntity(ProjectDTO dto);

    void updateEntityFromDTO(ProjectDTO dto, @MappingTarget Project entity);

    @Named("mapTaskListToIds")
    default List<Long> mapTaskListToIds(List<Task> tasks) {
        if (tasks == null)
            return null;

        return tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
    }
}