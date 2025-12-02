package ru.astalavista.taskManagSys.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.astalavista.taskManagSys.model.dto.TaskAuditDTO;
import ru.astalavista.taskManagSys.model.entity.TaskAudit;

@Mapper(componentModel = "spring")
public interface TaskAuditMapper {
    @Mapping(target = "taskId", source = "task.id")
    TaskAuditDTO toDTO(TaskAudit audit);

    TaskAudit toEntity(TaskAuditDTO dto);

    void updateEntityFromDTO(TaskAuditDTO dto, @MappingTarget TaskAudit entity);
}
