package ru.astalavista.taskManagSys.contollers;

import org.springframework.web.bind.annotation.*;
import ru.astalavista.taskManagSys.models.Task;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    @GetMapping
    public List<Task> getTasks(
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String project,
            @RequestParam(required = false) Boolean overdueOnly
    ) {
        // Передаём фильтры (Service)
        // Возвращаем результат наружу
    }

    @PostMapping
    public Task createTask(@RequestBody Task newTask) {
        // Отдаём  новую задачу
        // Возвращаем созданную задачу с красивым номером
    }
}
