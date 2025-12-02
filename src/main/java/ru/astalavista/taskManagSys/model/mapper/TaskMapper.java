package ru.astalavista.taskManagSys.model.mapper;

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
        if (tasks == null)
            return null;

        return tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
    }
}
