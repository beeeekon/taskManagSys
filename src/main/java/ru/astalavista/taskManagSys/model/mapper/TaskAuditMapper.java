package ru.astalavista.taskManagSys.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.astalavista.taskManagSys.model.dto.taskaudit.TaskAuditDTO;
import ru.astalavista.taskManagSys.model.entity.TaskAudit;

@Mapper(componentModel = "spring")
public interface TaskAuditMapper {
    @Mapping(target = "taskId", source = "task.id")
    TaskAuditDTO toDTO(TaskAudit audit);
}
