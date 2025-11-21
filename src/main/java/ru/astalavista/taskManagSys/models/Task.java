package ru.astalavista.taskManagSys.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
@Entity
@Table(name = "tasks")
@Data
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // номер по шаблону <Код проекта>-<Порядковый номер>
    @Column(name = "public_id", unique = true, length = 50)
    private String publicId;

    // Тип задачи
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TaskType type;

    // Заголовок задачи
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    // Подробное описание
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Дата, до которой нужно выполнить
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    // Затраченное время в часах
    @Column(name = "time_spent")
    private Integer timeSpent = 0;

    // Связь с проектом (многие задачи → один проект)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    // Связь с исполнителем (многие задачи → один сотрудник)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private Employee assignee;

    // Связь "многие-ко-многим" самой с собой (связанные задачи)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "task_links", // название таблицы-связки
            joinColumns = @JoinColumn(name = "task_id"), // столбец для этой задачи
            inverseJoinColumns = @JoinColumn(name = "linked_task_id") // столбец для связанной задачи
    )
    private java.util.List<Task> linkedTasks;

    // Автоматически устанавливается при создании
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Автоматически обновляется при изменении
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Конструктор по умолчанию (обязателен для JPA)
    public Task() {
        TaskStatus status = TaskStatus.REGISTERED;
        this.timeSpent = 0;
    }
}
