package ru.astalavista.taskManagSys.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Полное имя сотрудника
    @Column(name = "full_name", nullable = false)
    private String fullName;

    // Email сотрудника
    @Column(name = "email", unique = true)
    private String email;

    // Должность
    @Column(name = "position", length = 100)
    private String position;

    // ID чата Telegram для уведомлений
    @Column(name = "telegram_chat_id", unique = true, length = 100)
    private String telegramChatId;

    // Автоматически устанавливается при создании
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Автоматически обновляется при изменении
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Связь "один сотрудник → много задач" (где он исполнитель)
    @OneToMany(mappedBy = "assignee", fetch = FetchType.LAZY)
    private java.util.List<Task> assignedTasks;
}
