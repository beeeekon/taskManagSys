package ru.astalavista.taskManagSys.service;

import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Task;
import ru.astalavista.taskManagSys.model.mapper.TaskMapper;
import ru.astalavista.taskManagSys.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TaskService {

//    public Task createTask(Task task) {
//        // 1. Сгенерировать красивый номер
//        // 2. Сохранить задачу
//        // 3. Записать в журнал "создана новая задача"
//    }
//
//    public Task changeStatus(Long taskId, TaskStatus newStatus, String whoChanged) {
//        // 1. Найти задачу
//        // 2. Запомнить старый статус
//        // 3. Изменить статус
//        // 4. Записать в журнал: "статус поменялся с X на Y"
//    }

    private final TaskRepository repository;
    private final TaskMapper mapper;

    public TaskService(TaskRepository repository, TaskMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public TaskDTO create(TaskDTO dto) {
        Task task = mapper.toEntity(dto);
        Task saved = repository.save(task);
        return mapper.toDTO(saved);
    }

    public Optional<TaskDTO> update(Long id, TaskDTO dto) {
        return repository.findById(id)
            .map(existing -> {
                mapper.updateEntityFromDTO(dto, existing);
                Task updated = repository.save(existing);
                return mapper.toDTO(updated);
            });
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<TaskDTO> findAll() {
        return repository.findAll().stream()
            .map(mapper::toDTO)
            .toList();
    }

    public Optional<TaskDTO> findById(Long id) {
        return repository.findById(id).map(mapper::toDTO);
    }
}