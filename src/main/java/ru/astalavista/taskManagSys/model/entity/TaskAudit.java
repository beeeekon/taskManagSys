package ru.astalavista.taskManagSys.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "task_audit_log")
public class TaskAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Связь с задачей, которую изменили
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    // Какое поле изменили
    @Column(name = "field_name", nullable = false, length = 100)
    private String fieldName;

    // Старое значение
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    // Новое значение
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    // Кто изменил (имя пользователя или система)
    @Column(name = "changed_by", nullable = false, length = 255)
    private String changedBy;

    // Откуда пришло изменение (WEB, TELEGRAM_BOT, API)
    @Column(name = "change_source", nullable = false, length = 50)
    private String changeSource;

    // Когда изменили
    @CreationTimestamp
    @Column(name = "changed_at", updatable = false)
    private LocalDateTime changedAt;
}
