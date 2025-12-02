package ru.astalavista.taskManagSys.contoller;

import ru.astalavista.taskManagSys.model.dto.TaskAuditDTO;
import ru.astalavista.taskManagSys.service.TaskAuditService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task-audits")
@RequiredArgsConstructor
public class TaskAuditController {

    private final TaskAuditService service;



    @PostMapping
    public TaskAuditDTO create(@RequestBody TaskAuditDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public TaskAuditDTO update(@PathVariable Long id, @RequestBody TaskAuditDTO dto) {
        return service.update(id, dto).orElse(null);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping
    public List<TaskAuditDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TaskAuditDTO getById(@PathVariable Long id) {
        return service.findById(id).orElse(null);
    }
}
