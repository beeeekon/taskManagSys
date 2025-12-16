package ru.astalavista.taskManagSys.contoller;

import ru.astalavista.taskManagSys.model.dto.taskaudit.TaskAuditDTO;
import ru.astalavista.taskManagSys.service.TaskAuditService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task-audits")
@RequiredArgsConstructor
public class TaskAuditController {

    private final TaskAuditService service;

    // Данные аудита можно только получить,
    // заполнение бд происходит автоматически при каких-либо действиях (логирование)

    @GetMapping
    public List<TaskAuditDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TaskAuditDTO getById(@PathVariable Long id) {
        return service.findById(id).orElse(null);
    }
}
