package ru.astalavista.taskManagSys.contoller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;
import ru.astalavista.taskManagSys.service.TaskService;

import java.util.List;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    // ---------- CRUD ----------
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

    // ---------- READ ----------
    @GetMapping
    public List<TaskDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public TaskDTO getById(@PathVariable Long id) {
        return service.findById(id).orElse(null);
    }

    // ---------- SPECIAL OPERATIONS ----------
    @PatchMapping("/{id}/status/{status}")
    public TaskDTO changeStatus(@PathVariable Long id, @PathVariable TaskStatus status) {
        return service.changeStatus(id, status);
    }

    // API для внешних интеграций с возможностью указать источник
    @PostMapping("/api/create")
    public TaskDTO createViaApi(
            @Valid @RequestBody TaskDTO dto,
            @RequestHeader("X-Change-Source") String changeSource,
            @RequestHeader("X-API-Key") String apiKey) {
        // Валидация API ключа
        validateApiKey(apiKey);

        // Для API можно использовать специальный контекст
        setApiContext(changeSource, "API_USER");

        return service.create(dto);
    }

    // Telegram-specific endpoint
    @PostMapping("/telegram/create")
    public TaskDTO createViaTelegram(
            @Valid @RequestBody TaskDTO dto,
            @RequestParam Long telegramChatId) {

        // Найти сотрудника по telegramChatId
        Employee employee = findEmployeeByTelegramChatId(telegramChatId);

        // Установить Telegram контекст
        setTelegramContext(employee);

        return service.create(dto);
    }

    // ---------- ФИЛЬТРАЦИЯ ----------
    @GetMapping("/filter")
    public List<TaskDTO> getFilteredTasks(
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(defaultValue = "false") boolean onlyOpen,
            @RequestParam(defaultValue = "false") boolean onlyOverdue) {

        return service.findFilteredTasks(assigneeId, projectId, status, onlyOpen, onlyOverdue);
    }

    // ---------- PRIVATE METHODS ----------
    private void setApiContext(String source, String user) {
        // Установить контекст для API запросов
        // Например, через ThreadLocal или отдельный сервис
    }

    private void setTelegramContext(Employee employee) {
        // Установить контекст для Telegram запросов
    }

    private Employee findEmployeeByTelegramChatId(Long chatId) {
        // Найти сотрудника по Telegram chat ID
        return null; // Реализация через репозиторий
    }

    private void validateApiKey(String apiKey) {
        // Валидация API ключа
        if (!isValidApiKey(apiKey)) {
            throw new SecurityException("Invalid API key");
        }
    }

    private boolean isValidApiKey(String apiKey) {
        // Проверка API ключа
        return true; // Реализация через сервис API ключей
    }
}