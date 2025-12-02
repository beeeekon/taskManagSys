package ru.astalavista.taskManagSys.service;

import ru.astalavista.taskManagSys.model.dto.TaskAuditDTO;
import ru.astalavista.taskManagSys.model.entity.TaskAudit;
import ru.astalavista.taskManagSys.model.mapper.TaskAuditMapper;
import ru.astalavista.taskManagSys.repository.TaskAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskAuditService {

    private final TaskAuditRepository repository;
    private final TaskAuditMapper mapper;


    public TaskAuditDTO create(TaskAuditDTO dto) {
        TaskAudit audit = mapper.toEntity(dto);
        TaskAudit saved = repository.save(audit);
        return mapper.toDTO(saved);
    }

    public Optional<TaskAuditDTO> update(Long id, TaskAuditDTO dto) {
        return repository.findById(id)
                .map(existing -> {
                    mapper.updateEntityFromDTO(dto, existing);
                    TaskAudit updated = repository.save(existing);
                    return mapper.toDTO(updated);
                });
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<TaskAuditDTO> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    public Optional<TaskAuditDTO> findById(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }
}
