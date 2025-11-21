package ru.astalavista.taskManagSys.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
@Data
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Уникальный код проекта
    @Column(name = "code", unique = true, nullable = false, length = 50)
    private String code;

    // Название проекта
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // Описание проекта
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Автоматически устанавливается при создании
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Автоматически обновляется при изменении
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Связь "один проект → много задач"
    // mappedBy = "project" означает, что связь управляется из класса Task
    // cascade = CascadeType.ALL - при удалении проекта удалятся все его задачи
    // fetch = FetchType.LAZY - задачи загружаются только когда к ним обращаются
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private java.util.List<Task> tasks;
}
