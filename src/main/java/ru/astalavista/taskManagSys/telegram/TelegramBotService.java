package ru.astalavista.taskManagSys.telegram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import ru.astalavista.taskManagSys.config.AuditContextProvider;
import ru.astalavista.taskManagSys.model.dto.TaskDTO;
import ru.astalavista.taskManagSys.model.entity.Employee;
import ru.astalavista.taskManagSys.model.enums.TaskStatus;
import ru.astalavista.taskManagSys.repository.EmployeeRepository;
import ru.astalavista.taskManagSys.service.TaskService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Основной сервис для обработки команд Telegram бота
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TaskService taskService;
    private final EmployeeRepository employeeRepository;
    private final AuditContextProvider auditContextProvider;

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /**
     * Обрабатывает входящее обновление от Telegram
     */
    public SendMessage handleUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            return handleMessage(
                    update.getMessage().getChatId(),
                    update.getMessage().getText(),
                    update.getMessage().getFrom().getUserName()
            );
        } else if (update.hasCallbackQuery()) {
            return handleCallbackQuery(
                    update.getCallbackQuery().getMessage().getChatId(),
                    update.getCallbackQuery().getData(),
                    update.getCallbackQuery().getFrom().getUserName()
            );
        }
        log.warn("Unsupported update type: {}", update);
        return null;
    }

    /**
     * Обрабатывает текстовые сообщения
     */
    private SendMessage handleMessage(Long chatId, String text, String telegramUsername) {
        log.info("Message from {} (chatId: {}): {}", telegramUsername, chatId, text);

        // Ищем сотрудника по telegramChatId или username
        Optional<Employee> employeeOpt = findEmployeeByTelegramData(chatId, telegramUsername);

        // Если сотрудник не найден, предлагаем зарегистрироваться
        if (employeeOpt.isEmpty()) {
            return createRegistrationMessage(chatId, telegramUsername);
        }

        Employee employee = employeeOpt.get();

        // Обновляем chatId если он изменился
        updateEmployeeTelegramChatId(employee, chatId);

        // Обрабатываем команды
        String lowerText = text.toLowerCase();
        if (lowerText.contains("/start") || lowerText.contains("старт")) {
            return showMainMenu(chatId, employee);
        } else if (lowerText.contains("/tasks") || lowerText.contains("задачи") || lowerText.contains("мои задачи")) {
            return sendEmployeeTasks(chatId, employee.getId());
        } else if (lowerText.contains("/overdue") || lowerText.contains("просроченные")) {
            return sendOverdueTasks(chatId, employee.getId());
        } else if (lowerText.contains("/help") || lowerText.contains("помощь")) {
            return showHelp(chatId);
        } else if (lowerText.contains("/register") || lowerText.contains("регистрация")) {
            return confirmRegistration(chatId, employee);
        } else if (lowerText.contains("обновить") || lowerText.contains("refresh")) {
            return sendEmployeeTasks(chatId, employee.getId());
        } else {
            return createMessage(chatId,
                    "❓ Неизвестная команда. Используйте /help для списка команд.");
        }
    }

    /**
     * Обрабатывает нажатия на inline-кнопки
     */
    private SendMessage handleCallbackQuery(Long chatId, String callbackData, String telegramUsername) {
        log.info("Callback from {}: {}", telegramUsername, callbackData);

        // Формат callbackData:
        // task_{taskId}_status_{newStatus} - изменение статуса
        // task_{taskId}_details - детали задачи
        // register_{employeeId} - подтверждение регистрации

        String[] parts = callbackData.split("_");

        if (parts.length >= 4 && "task".equals(parts[0]) && "status".equals(parts[2])) {
            return handleTaskStatusChange(chatId, parts, telegramUsername);
        } else if (parts.length >= 3 && "task".equals(parts[0]) && "details".equals(parts[2])) {
            return showTaskDetails(chatId, parts[1], telegramUsername);
        } else if (parts.length >= 2 && "register".equals(parts[0])) {
            return handleRegistration(chatId, parts[1], telegramUsername);
        }

        return createMessage(chatId, "❌ Неизвестная команда");
    }

    /**
     * Изменяет статус задачи
     */
    private SendMessage handleTaskStatusChange(Long chatId, String[] parts, String telegramUsername) {
        try {
            Long taskId = Long.parseLong(parts[1]);
            TaskStatus newStatus = TaskStatus.valueOf(parts[3]);

            // Находим сотрудника
            Optional<Employee> employeeOpt = findEmployeeByTelegramUsername(telegramUsername);
            if (employeeOpt.isEmpty()) {
                return createMessage(chatId, "❌ Вы не зарегистрированы в системе");
            }

            Employee employee = employeeOpt.get();

            // Проверяем, что сотрудник является исполнителем задачи
            TaskDTO task = taskService.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Задача не найдена"));

            if (task.getAssigneeId() == null || !task.getAssigneeId().equals(employee.getId())) {
                return createMessage(chatId,
                        "❌ У вас нет прав для изменения этой задачи. " +
                                "Эта задача назначена другому сотруднику.");
            }

            // Устанавливаем контекст аудита для Telegram
            auditContextProvider.setContext("TELEGRAM_BOT", employee.getEmail());

            try {
                // Изменяем статус задачи
                TaskDTO updatedTask = taskService.changeStatus(taskId, newStatus);

                return createMessage(chatId,
                        "✅ Статус задачи *#" + updatedTask.getPublicId() + "* изменен на: " +
                                translateStatus(newStatus) + "\n\n" +
                                "Задача: *" + updatedTask.getTitle() + "*"
                );
            } finally {
                auditContextProvider.clearContext();
            }

        } catch (NumberFormatException e) {
            log.error("Invalid task ID in callback", e);
            return createMessage(chatId, "❌ Неверный ID задачи");
        } catch (IllegalArgumentException e) {
            log.error("Invalid task status in callback", e);
            return createMessage(chatId, "❌ Неверный статус задачи");
        } catch (Exception e) {
            log.error("Error changing task status", e);
            return createMessage(chatId, "❌ Ошибка при изменении статуса: " + e.getMessage());
        }
    }

    /**
     * Показывает детали задачи
     */
    private SendMessage showTaskDetails(Long chatId, String taskIdStr, String telegramUsername) {
        try {
            Long taskId = Long.parseLong(taskIdStr);
            TaskDTO task = taskService.findById(taskId)
                    .orElseThrow(() -> new RuntimeException("Задача не найдена"));

            return createMessage(chatId, formatTaskDetails(task));

        } catch (Exception e) {
            log.error("Error showing task details", e);
            return createMessage(chatId, "❌ Ошибка при получении данных задачи");
        }
    }

    /**
     * Обрабатывает регистрацию сотрудника
     */
    private SendMessage handleRegistration(Long chatId, String employeeIdStr, String telegramUsername) {
        try {
            Long employeeId = Long.parseLong(employeeIdStr);
            Optional<Employee> employeeOpt = employeeRepository.findById(employeeId);

            if (employeeOpt.isEmpty()) {
                return createMessage(chatId, "❌ Сотрудник не найден");
            }

            Employee employee = employeeOpt.get();

            // Обновляем chatId
            employee.setTelegramChatId(String.valueOf(chatId));
            employeeRepository.save(employee);
            log.info("Updated telegram chatId for employee {}: {}", employee.getEmail(), chatId);

            return createMessage(chatId,
                    "✅ Регистрация успешна!\n\n" +
                            "Добро пожаловать, *" + employee.getFullName() + "*!\n" +
                            "Теперь вы можете использовать все функции бота."
            );

        } catch (Exception e) {
            log.error("Error during registration", e);
            return createMessage(chatId, "❌ Ошибка при регистрации");
        }
    }

    /**
     * Показывает главное меню
     */
    private SendMessage showMainMenu(Long chatId, Employee employee) {
        String message = String.format(
                "👋 *Добро пожаловать, %s!*\n\n" +
                        "*Ваша должность:* %s\n" +
                        "*Email:* %s\n\n" +
                        "Выберите действие:",
                employee.getFullName(),
                employee.getPosition() != null ? employee.getPosition() : "Не указана",
                employee.getEmail()
        );

        SendMessage sendMessage = createMessage(chatId, message);

        // Создаем клавиатуру с кнопками
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(true);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("📋 Мои задачи"));
        row1.add(new KeyboardButton("⚠️ Просроченные"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("ℹ️ Помощь"));
        row2.add(new KeyboardButton("🔄 Обновить"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        sendMessage.setReplyMarkup(keyboardMarkup);

        return sendMessage;
    }

    /**
     * Отправляет задачи сотрудника
     */
    private SendMessage sendEmployeeTasks(Long chatId, Long employeeId) {
        List<TaskDTO> tasks = taskService.findTasksByAssignee(employeeId)
                .stream()
                .filter(task -> task.getStatus() != TaskStatus.CLOSED)
                .limit(10)
                .toList();

        if (tasks.isEmpty()) {
            return createMessage(chatId,
                    "🎉 *Отличные новости!*\n\n" +
                            "У вас нет активных задач. Можете расслабиться или взять новую задачу!"
            );
        }

        StringBuilder message = new StringBuilder("📋 *Ваши активные задачи:*\n\n");

        for (int i = 0; i < Math.min(tasks.size(), 3); i++) {
            TaskDTO task = tasks.get(i);
            message.append(formatTaskShort(task)).append("\n\n");
        }

        if (tasks.size() > 3) {
            message.append("... и еще ").append(tasks.size() - 3).append(" задач\n");
        }

        message.append("\nИспользуйте кнопки ниже для управления задачами:");

        SendMessage sendMessage = createMessage(chatId, message.toString());

        // Добавляем inline-кнопки для первой задачи
        if (!tasks.isEmpty()) {
            InlineKeyboardMarkup keyboard = createTaskActionsKeyboard(tasks.get(0));
            sendMessage.setReplyMarkup(keyboard);
        }

        return sendMessage;
    }

    /**
     * Отправляет просроченные задачи
     */
    private SendMessage sendOverdueTasks(Long chatId, Long employeeId) {
        List<TaskDTO> allTasks = taskService.findTasksByAssignee(employeeId);
        List<TaskDTO> overdueTasks = allTasks.stream()
                .filter(task -> task.getStatus() != TaskStatus.CLOSED)
                .filter(task -> task.getDueDate() != null &&
                        task.getDueDate().isBefore(LocalDateTime.now()))
                .limit(5)
                .toList();

        if (overdueTasks.isEmpty()) {
            return createMessage(chatId,
                    "✅ *Все вовремя!*\n\n" +
                            "У вас нет просроченных задач. Так держать! 👏"
            );
        }

        StringBuilder message = new StringBuilder("⚠️ *ПРОСРОЧЕННЫЕ ЗАДАЧИ*\n\n");
        message.append("Срочно выполните эти задачи:\n\n");

        for (TaskDTO task : overdueTasks) {
            message.append(formatTaskShort(task)).append("\n\n");
        }

        message.append("⏰ *Внимание!* Эти задачи уже просрочены!");

        return createMessage(chatId, message.toString());
    }

    /**
     * Показывает справку
     */
    private SendMessage showHelp(Long chatId) {
        String helpText = """
            *📚 Помощь по боту управления задачами*
            
            *Основные команды:*
            📋 Мои задачи - показывает ваши активные задачи
            ⚠️ Просроченные - показывает просроченные задачи
            ℹ️ Помощь - эта справка
            🔄 Обновить - обновить список задач
            
            *Управление задачами:*
            • Нажмите на кнопку статуса под задачей, чтобы изменить его
            • Бот автоматически уведомит о новых задачах
            • Ежедневные отчеты приходят в 9:00
            
            *Регистрация:*
            Если вы видите это сообщение, значит регистрация прошла успешно.
            Для повторной регистрации используйте команду /register
            
            *Поддержка:*
            По вопросам работы бота обращайтесь:
            support@astalavista.ru
            """;

        return createMessage(chatId, helpText);
    }

    /**
     * Создает сообщение для регистрации
     */
    private SendMessage createRegistrationMessage(Long chatId, String telegramUsername) {
        // Ищем сотрудника по username (без @)
        String cleanUsername = telegramUsername != null ?
                telegramUsername.replace("@", "") : telegramUsername;

        List<Employee> potentialEmployees = employeeRepository.findAll().stream()
                .filter(e -> {
                    if (e.getTelegramChatId() == null) return false;
                    String employeeChatId = e.getTelegramChatId().replace("@", "");
                    return cleanUsername != null && cleanUsername.equals(employeeChatId);
                })
                .toList();

        if (potentialEmployees.isEmpty()) {
            return createMessage(chatId,
                    "🔐 *Требуется регистрация*\n\n" +
                            "Вы не зарегистрированы в системе управления задачами.\n\n" +
                            "Обратитесь к администратору для получения доступа:\n" +
                            "1. Предоставьте ваш Telegram username: " +
                            (telegramUsername != null ? "@" + telegramUsername : "не указан") + "\n" +
                            "2. Администратор добавит вас в систему\n" +
                            "3. После этого используйте команду /start\n\n" +
                            "*Ваш Telegram ID:* " + chatId
            );
        }

        // Если найден один сотрудник, предлагаем подтвердить регистрацию
        if (potentialEmployees.size() == 1) {
            Employee employee = potentialEmployees.get(0);
            return confirmRegistration(chatId, employee);
        }

        // Если найдено несколько сотрудников, предлагаем выбрать
        return createEmployeeSelectionMessage(chatId, potentialEmployees, telegramUsername);
    }

    /**
     * Подтверждает регистрацию
     */
    private SendMessage confirmRegistration(Long chatId, Employee employee) {
        String message = String.format(
                "🔐 *Подтверждение регистрации*\n\n" +
                        "Найден сотрудник:\n" +
                        "• *Имя:* %s\n" +
                        "• *Email:* %s\n" +
                        "• *Должность:* %s\n\n" +
                        "Это вы?",
                employee.getFullName(),
                employee.getEmail(),
                employee.getPosition() != null ? employee.getPosition() : "Не указана"
        );

        SendMessage sendMessage = createMessage(chatId, message);

        // Создаем inline-кнопку для подтверждения
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton confirmButton = new InlineKeyboardButton();
        confirmButton.setText("✅ Да, это я");
        confirmButton.setCallbackData("register_" + employee.getId());
        row.add(confirmButton);
        rows.add(row);

        keyboard.setKeyboard(rows);
        sendMessage.setReplyMarkup(keyboard);

        return sendMessage;
    }

    /**
     * Создает сообщение для выбора сотрудника
     */
    private SendMessage createEmployeeSelectionMessage(Long chatId, List<Employee> employees, String telegramUsername) {
        StringBuilder message = new StringBuilder("🔍 *Найдено несколько сотрудников*\n\n");
        message.append("Выберите ваш профиль:\n\n");

        for (int i = 0; i < employees.size(); i++) {
            Employee emp = employees.get(i);
            message.append(i + 1).append(". *").append(emp.getFullName()).append("*\n");
            message.append("   Email: ").append(emp.getEmail()).append("\n");
            message.append("   Должность: ").append(emp.getPosition() != null ? emp.getPosition() : "Не указана").append("\n\n");
        }

        SendMessage sendMessage = createMessage(chatId, message.toString());

        // Создаем inline-кнопки для выбора
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (int i = 0; i < employees.size(); i++) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText((i + 1) + ". " + employees.get(i).getFullName());
            button.setCallbackData("register_" + employees.get(i).getId());
            row.add(button);
            rows.add(row);
        }

        keyboard.setKeyboard(rows);
        sendMessage.setReplyMarkup(keyboard);

        return sendMessage;
    }

    /**
     * Форматирует краткое описание задачи
     */
    private String formatTaskShort(TaskDTO task) {
        return String.format(
                "🔹 *#%s* - %s\n" +
                        "📌 *Статус:* %s\n" +
                        "⏰ *Срок:* %s\n" +
                        "📊 *Время:* %s ч.",
                task.getPublicId(),
                task.getTitle(),
                translateStatus(task.getStatus()),
                task.getDueDate() != null ?
                        task.getDueDate().format(DATE_FORMATTER) : "Не установлен",
                task.getTimeSpent() != null ? task.getTimeSpent() : 0
        );
    }

    /**
     * Форматирует детальное описание задачи
     */
    private String formatTaskDetails(TaskDTO task) {
        return String.format(
                "📋 *Детали задачи #%s*\n\n" +
                        "*Заголовок:* %s\n" +
                        "*Статус:* %s\n" +
                        "*Описание:* %s\n\n" +
                        "*Срок выполнения:* %s\n" +
                        "*Затраченное время:* %s часов\n" +
                        "*Проект:* #%s\n" +
                        "*Создана:* %s\n" +
                        "*Обновлена:* %s\n\n" +
                        "*ID задачи:* %s",
                task.getPublicId(),
                task.getTitle(),
                translateStatus(task.getStatus()),
                task.getDescription() != null && !task.getDescription().isEmpty() ?
                        task.getDescription() : "Нет описания",
                task.getDueDate() != null ?
                        task.getDueDate().format(DATE_FORMATTER) : "Не установлен",
                task.getTimeSpent() != null ? task.getTimeSpent() : 0,
                task.getProjectId(),
                task.getCreatedAt() != null ?
                        task.getCreatedAt().format(DATE_FORMATTER) : "Неизвестно",
                task.getUpdatedAt() != null ?
                        task.getUpdatedAt().format(DATE_FORMATTER) : "Неизвестно",
                task.getId()
        );
    }

    /**
     * Создает клавиатуру для управления задачей
     */
    private InlineKeyboardMarkup createTaskActionsKeyboard(TaskDTO task) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Кнопка для деталей
        List<InlineKeyboardButton> detailsRow = new ArrayList<>();
        InlineKeyboardButton detailsButton = new InlineKeyboardButton();
        detailsButton.setText("📝 Подробнее");
        detailsButton.setCallbackData("task_" + task.getId() + "_details");
        detailsRow.add(detailsButton);
        rows.add(detailsRow);

        // Кнопки для изменения статуса
        List<TaskStatus> availableStatuses = getAvailableStatuses(task.getStatus());

        for (TaskStatus status : availableStatuses) {
            List<InlineKeyboardButton> statusRow = new ArrayList<>();
            InlineKeyboardButton statusButton = new InlineKeyboardButton();
            statusButton.setText("→ " + translateStatus(status));
            statusButton.setCallbackData("task_" + task.getId() + "_status_" + status);
            statusRow.add(statusButton);
            rows.add(statusRow);
        }

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    /**
     * Возвращает доступные статусы для изменения
     */
    private List<TaskStatus> getAvailableStatuses(TaskStatus currentStatus) {
        List<TaskStatus> available = new ArrayList<>();

        switch (currentStatus) {
            case REGISTERED:
                available.add(TaskStatus.IN_PROGRESS);
                break;
            case IN_PROGRESS:
                available.add(TaskStatus.UNDER_REVIEW);
                available.add(TaskStatus.BUG_FIXING);
                break;
            case UNDER_REVIEW:
                available.add(TaskStatus.CLOSED);
                available.add(TaskStatus.BUG_FIXING);
                break;
            case BUG_FIXING:
                available.add(TaskStatus.UNDER_REVIEW);
                available.add(TaskStatus.IN_PROGRESS);
                break;
            case CLOSED:
                // Нельзя менять статус закрытой задачи
                break;
        }

        return available;
    }

    /**
     * Переводит статус на русский
     */
    private String translateStatus(TaskStatus status) {
        switch (status) {
            case REGISTERED: return "📝 Зарегистрирована";
            case IN_PROGRESS: return "⚡ В работе";
            case UNDER_REVIEW: return "🔍 На проверке";
            case BUG_FIXING: return "🐛 Исправление";
            case CLOSED: return "✅ Завершена";
            default: return status.toString();
        }
    }

    /**
     * Находит сотрудника по данным Telegram
     */
    private Optional<Employee> findEmployeeByTelegramData(Long chatId, String telegramUsername) {
        // Сначала ищем по chatId (преобразуем в строку для сравнения)
        if (chatId != null) {
            String chatIdStr = String.valueOf(chatId);
            Optional<Employee> byChatId = employeeRepository.findAll().stream()
                    .filter(e -> chatIdStr.equals(e.getTelegramChatId()))
                    .findFirst();

            if (byChatId.isPresent()) {
                return byChatId;
            }
        }

        // Если не нашли по chatId, ищем по username
        if (telegramUsername != null) {
            return findEmployeeByTelegramUsername(telegramUsername);
        }

        return Optional.empty();
    }

    /**
     * Находит сотрудника по Telegram username
     */
    private Optional<Employee> findEmployeeByTelegramUsername(String telegramUsername) {
        if (telegramUsername == null) {
            return Optional.empty();
        }

        String cleanUsername = telegramUsername.replace("@", "");

        return employeeRepository.findAll().stream()
                .filter(e -> {
                    if (e.getTelegramChatId() == null) return false;
                    String employeeChatId = e.getTelegramChatId().replace("@", "");
                    return cleanUsername.equals(employeeChatId);
                })
                .findFirst();
    }

    /**
     * Обновляет chatId сотрудника
     */
    private void updateEmployeeTelegramChatId(Employee employee, Long chatId) {
        String chatIdStr = String.valueOf(chatId);
        if (!chatIdStr.equals(employee.getTelegramChatId())) {
            employee.setTelegramChatId(chatIdStr);
            employeeRepository.save(employee);
            log.info("Updated telegram chatId for employee {}: {}", employee.getEmail(), chatId);
        }
    }

    /**
     * Создает базовое сообщение
     */
    private SendMessage createMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setDisableWebPagePreview(true);
        return message;
    }

    /**
     * Отправляет уведомление о новой задаче
     */
    public void sendNewTaskNotification(TaskDTO task, Employee assignee) {
        if (assignee.getTelegramChatId() == null || assignee.getTelegramChatId().isEmpty()) {
            log.warn("Employee {} has no telegram chatId", assignee.getEmail());
            return;
        }

        try {
            String message = String.format(
                    "🎯 *НОВАЯ ЗАДАЧА!*\n\n" +
                            "*Задача:* %s\n" +
                            "*Проект:* #%s\n" +
                            "*Срок:* %s\n" +
                            "*Статус:* %s\n\n" +
                            "Используйте команду /tasks для просмотра",
                    task.getTitle(),
                    task.getProjectId(),
                    task.getDueDate() != null ?
                            task.getDueDate().format(DATE_FORMATTER) : "Не установлен",
                    translateStatus(task.getStatus())
            );

            log.info("Prepared new task notification for employee {}", assignee.getEmail());

        } catch (Exception e) {
            log.error("Error preparing new task notification", e);
        }
    }

    /**
     * Отправляет уведомление о завершении задачи
     */
    public void sendTaskCompletedNotification(TaskDTO task, Employee assignee) {
        if (assignee.getTelegramChatId() == null || assignee.getTelegramChatId().isEmpty()) {
            return;
        }

        try {
            String message = String.format(
                    "🎉 *ЗАДАЧА ВЫПОЛНЕНА!*\n\n" +
                            "*Задача:* %s (#%s)\n" +
                            "*Время выполнения:* %s часов\n\n" +
                            "Отличная работа! 👏",
                    task.getTitle(),
                    task.getPublicId(),
                    task.getTimeSpent() != null ? task.getTimeSpent() : 0
            );

            log.info("Prepared task completed notification for employee {}", assignee.getEmail());

        } catch (Exception e) {
            log.error("Error preparing task completed notification", e);
        }
    }
}