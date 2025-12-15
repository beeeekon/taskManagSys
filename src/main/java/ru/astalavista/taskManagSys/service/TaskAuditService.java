package ru.astalavista.taskManagSys.service;

import ru.astalavista.taskManagSys.model.dto.TaskAuditDTO;
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

    public List<TaskAuditDTO> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDTO)
                .toList();
    }

    public Optional<TaskAuditDTO> findById(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }
}
