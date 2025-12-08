package ru.astalavista.taskManagSys.contoller;

import jakarta.validation.Valid;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.service.TaskService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;



    @PostMapping
    public TaskDTO create(@Valid @RequestBody TaskDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public TaskDTO update(@PathVariable Long id, @RequestBody TaskDTO dto) {
        return service.update(id, dto).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping
    public List<TaskDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TaskDTO getById(@PathVariable Long id) {
        return service.findById(id).orElse(null);
    }
}
