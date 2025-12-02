package ru.astalavista.taskManagSys.service;

import ru.astalavista.taskManagSys.model.dto.EmployeeDTO;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.mapper.EmployeeMapper;
import ru.astalavista.taskManagSys.repository.EmployeeRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final EmployeeMapper mapper;



    public List<EmployeeDTO> getAll() {
        return repository.findAll().stream()
            .map(mapper::toDTO)
            .collect(Collectors.toList());
    }

    public Optional<EmployeeDTO> getById(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }

    public EmployeeDTO create(EmployeeDTO dto) {
        Employee employee = mapper.toEntity(dto);
        Employee saved = repository.save(employee);
        return mapper.toDTO(saved);
    }

    public Optional<EmployeeDTO> update(Long id, EmployeeDTO dto) {
        return repository.findById(id)
            .map(existing -> {
                mapper.updateEntityFromDTO(dto, existing);
                Employee updated = repository.save(existing);
                return mapper.toDTO(updated);
            });
    }

    public void delete(Long id) {
        if (!repository.existsById(id))
            return;

        repository.deleteById(id);
    }
}
