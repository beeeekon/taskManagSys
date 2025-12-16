package ru.astalavista.taskManagSys.contoller;

import jakarta.validation.Valid;
import ru.astalavista.taskManagSys.model.dto.employee.EmployeeCreateDTO;
import ru.astalavista.taskManagSys.model.dto.employee.EmployeeDTO;
import ru.astalavista.taskManagSys.model.dto.employee.EmployeeUpdateDTO;
import ru.astalavista.taskManagSys.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService service;

    @GetMapping
    public List<EmployeeDTO> getAll() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public EmployeeDTO getById(@PathVariable Long id) {
        return service.getById(id).orElse(null);
    }

    @PostMapping
    public EmployeeDTO create(@Valid @RequestBody EmployeeCreateDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public EmployeeDTO update(@PathVariable Long id, @RequestBody EmployeeUpdateDTO dto) {
        return service.update(id, dto).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
