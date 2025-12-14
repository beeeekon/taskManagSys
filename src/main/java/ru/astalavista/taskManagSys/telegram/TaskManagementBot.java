package ru.astalavista.taskManagSys.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.astalavista.taskManagSys.telegram.TelegramBotConfig;
import ru.astalavista.taskManagSys.telegram.TelegramBotService;

import jakarta.annotation.PostConstruct;

/**
 * Основной класс Telegram бота
 * Использует Long Polling для получения обновлений
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskManagementBot extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final TelegramBotService botService;

    /**
     * Инициализация бота при старте приложения
     */
    @PostConstruct
    public void init() {
        if (!config.isConfigured()) {
            log.warn("Telegram bot is not configured properly. Check telegram.bot.token in application.properties");
            return;
        }

        log.info("Telegram Bot initialized: {}", config.getBotUsername());
        log.info("Bot token: {}", config.getMaskedToken());

        // Отправляем сообщение администратору при старте
        sendStartupNotification();
    }

    /**
     * Обрабатывает входящие обновления от Telegram
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (!config.isConfigured()) {
            log.warn("Bot is not configured, ignoring update");
            return;
        }

        try {
            log.debug("Processing update ID: {}", update.getUpdateId());

            SendMessage response = botService.handleUpdate(update);
            if (response != null) {
                execute(response);
                log.debug("Response sent for update ID: {}", update.getUpdateId());
            }
        } catch (TelegramApiException e) {
            log.error("Telegram API error while processing update", e);
            sendErrorMessage(update, e);
        } catch (Exception e) {
            log.error("Unexpected error while processing update", e);
            sendErrorMessage(update, e);
        }
    }

    /**
     * Возвращает username бота
     */
    @Override
    public String getBotUsername() {
        return config.getBotUsername();
    }

    /**
     * Возвращает токен бота
     */
    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    /**
     * Отправляет уведомление
     */
    public void sendNotification(String chatId, String message) {
        if (!config.isConfigured() || chatId == null || message == null) {
            return;
        }

        try {
            SendMessage notification = new SendMessage();
            notification.setChatId(chatId);
            notification.setText(message);
            notification.setParseMode("Markdown");
            notification.setDisableWebPagePreview(true);

            execute(notification);
            log.debug("Notification sent to chatId: {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send notification to chatId: {}", chatId, e);
        }
    }

    /**
     * Отправляет уведомление о новой задаче
     */
    public void sendNewTaskNotification(String employeeChatId, String taskTitle, String taskPublicId) {
        String message = String.format(
                "🎯 *Новая задача назначена!*\n\n" +
                        "*Задача:* %s\n" +
                        "*Номер:* #%s\n\n" +
                        "Используйте /tasks для просмотра",
                taskTitle,
                taskPublicId
        );

        sendNotification(employeeChatId, message);
    }

    /**
     * Отправляет уведомление о завершении задачи
     */
    public void sendTaskCompletedNotification(String employeeChatId, String taskTitle, String taskPublicId) {
        String message = String.format(
                "🎉 *Задача выполнена!*\n\n" +
                        "*Задача:* %s\n" +
                        "*Номер:* #%s\n\n" +
                        "Отличная работа! 👏",
                taskTitle,
                taskPublicId
        );

        sendNotification(employeeChatId, message);
    }

    /**
     * Отправляет уведомление администратору при старте
     */
    private void sendStartupNotification() {
        if (config.getAdminId() != null && !config.getAdminId().isEmpty()) {
            try {
                SendMessage startupMessage = new SendMessage();
                startupMessage.setChatId(config.getAdminId());
                startupMessage.setText("🤖 *Бот управления задачами запущен!*\n\n" +
                        "Имя бота: " + config.getBotUsername() + "\n" +
                        "Время запуска: " + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")));
                startupMessage.setParseMode("Markdown");
                execute(startupMessage);
                log.info("Startup message sent to admin");
            } catch (TelegramApiException e) {
                log.error("Failed to send startup message to admin", e);
            }
        }
    }

    /**
     * Отправляет сообщение об ошибке пользователю
     */
    private void sendErrorMessage(Update update, Exception e) {
        try {
            Long chatId = null;

            if (update.hasMessage()) {
                chatId = update.getMessage().getChatId();
            } else if (update.hasCallbackQuery()) {
                chatId = update.getCallbackQuery().getMessage().getChatId();
            }

            if (chatId != null) {
                SendMessage errorMessage = new SendMessage();
                errorMessage.setChatId(chatId.toString());
                errorMessage.setText("❌ *Произошла ошибка*\n\n" +
                        "При обработке вашего запроса произошла ошибка. " +
                        "Попробуйте позже или обратитесь к администратору.\n\n" +
                        "Ошибка: " + e.getClass().getSimpleName());
                errorMessage.setParseMode("Markdown");

                execute(errorMessage);
                log.info("Error message sent to chatId: {}", chatId);
            }
        } catch (TelegramApiException telegramError) {
            log.error("Failed to send error message to user", telegramError);
        }
    }

    /**
     * Маскирует токен для безопасного логирования
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 10) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }

    /**
     * Проверяет, доступен ли бот
     */
    public boolean testConnection() {
        try {
            // Простая проверка соединения
            SendMessage testMessage = new SendMessage();
            testMessage.setChatId(config.getAdminId());
            testMessage.setText("🔄 Проверка соединения бота...");
            execute(testMessage);
            return true;
        } catch (TelegramApiException e) {
            log.error("Bot connection test failed", e);
            return false;
        }
    }

    /**
     * Отправляет статистику администратору
     */
    public void sendStatisticsToAdmin(int activeUsers, int activeTasks, int overdueTasks) {
        if (config.getAdminId() == null || config.getAdminId().isEmpty()) {
            return;
        }

        String statsMessage = String.format(
                "📊 *Статистика системы*\n\n" +
                        "Активных пользователей: %d\n" +
                        "Активных задач: %d\n" +
                        "Просроченных задач: %d\n\n" +
                        "Время: %s",
                activeUsers,
                activeTasks,
                overdueTasks,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
        );

        sendNotification(config.getAdminId(), statsMessage);
    }
}