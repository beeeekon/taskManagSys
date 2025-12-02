package ru.astalavista.taskManagSys.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import ru.astalavista.taskManagSys.model.dto.EmployeeDTO;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.entity.Task;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface EmployeeMapper {
    @Mapping(target = "assignedTaskIds", source = "assignedTasks", qualifiedByName = "mapTaskListToIds")
    EmployeeDTO toDTO(Employee employee);

    Employee toEntity(EmployeeDTO dto);

    // Обновление существующей сущности на основе DTO
    void updateEntityFromDTO(EmployeeDTO dto, @MappingTarget Employee entity);

    @Named("mapTaskListToIds")
    default List<Long> mapTaskListToIds(List<Task> tasks) {
        if (tasks == null)
            return null;

        return tasks.stream()
                .map(Task::getId)
                .collect(Collectors.toList());
    }
}
