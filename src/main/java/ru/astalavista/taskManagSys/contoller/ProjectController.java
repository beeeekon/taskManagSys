package ru.astalavista.taskManagSys.contoller;

import ru.astalavista.taskManagSys.model.dto.ProjectDTO;
import ru.astalavista.taskManagSys.service.ProjectService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    @PostMapping
    public ProjectDTO create(@RequestBody ProjectDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ProjectDTO update(@PathVariable Long id, @RequestBody ProjectDTO dto) {
        return service.update(id, dto).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping
    public List<ProjectDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public ProjectDTO getById(@PathVariable Long id) {
        return service.findById(id).orElse(null);
    }
}
