package ru.astalavista.taskManagSys.telegram;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Telegram бота
 * Содержит настройки из application.properties
 */
@Configuration
@Getter
public class TelegramBotConfig {

    @Value("${telegram.bot.username:}")
    private String botUsername;

    @Value("${telegram.bot.token:}")
    private String botToken;

    @Value("${telegram.bot.admin-id:}")
    private String adminId;

    @Value("${telegram.bot.webhook-path:/telegram-webhook}")
    private String webhookPath;

    @Value("${telegram.bot.webhook-url:}")
    private String webhookUrl;

    @Value("${telegram.bot.enabled:true}")
    private boolean enabled;

    @Value("${telegram.bot.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Value("${telegram.bot.daily-report-time:09:00}")
    private String dailyReportTime;

    @Value("${telegram.bot.check-overdue-interval:30}")
    private int checkOverdueInterval;

    /**
     * Проверяет, корректно ли настроен бот
     */
    public boolean isConfigured() {
        return enabled &&
                botToken != null && !botToken.isEmpty() &&
                !botToken.equals("ВАШ_ТОКЕН_ЗДЕСЬ");
    }

    /**
     * Маскирует токен для логов
     */
    public String getMaskedToken() {
        if (botToken == null || botToken.isEmpty()) {
            return "NOT_SET";
        }
        if (botToken.length() <= 10) {
            return "***";
        }
        return botToken.substring(0, 4) + "***" + botToken.substring(botToken.length() - 4);
    }
}