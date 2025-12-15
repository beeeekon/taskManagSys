package ru.astalavista.taskManagSys.service;

import ru.astalavista.taskManagSys.model.dto.ProjectDTO;
import ru.astalavista.taskManagSys.model.entity.Project;
import ru.astalavista.taskManagSys.model.mapper.ProjectMapper;
import ru.astalavista.taskManagSys.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository repository;
    private final ProjectMapper mapper;

    public ProjectDTO create(ProjectDTO dto) {
        Project project = mapper.toEntity(dto);
        Project saved = repository.save(project);
        return mapper.toDTO(saved);
    }

    public Optional<ProjectDTO> update(Long id, ProjectDTO dto) {
        return repository.findById(id)
            .map(existing -> {
                mapper.updateEntityFromDTO(dto, existing);
                Project updated = repository.save(existing);
                return mapper.toDTO(updated);
            });
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<ProjectDTO> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDTO)
            .toList();
    }

    public Optional<ProjectDTO> findById(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }
}
