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
import ru.astalavista.taskManagSys.model.dto.task.TaskDTO;
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
        } else if (lowerText.contains("/refresh") || lowerText.contains("обновить")) {
            return sendEmployeeTasks(chatId, employee.getId());
        } else {
            return createMessage(chatId,
                    "❓ Неизвестная команда. Используйте /help для списка команд.");
        }
    }

    /**
     * Обрабатывает нажатия на inline-кнопки
     */
    private void updateEmployeeTelegramChatId(Employee employee, Long chatId) {
        String chatIdStr = String.valueOf(chatId);
        if (!chatIdStr.equals(employee.getTelegramChatId())) {
            employee.setTelegramChatId(chatIdStr);
            employeeRepository.save(employee);
            log.info("Updated telegram chatId for employee {}: {}", employee.getEmail(), chatId);
        }
    }

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
            return handleRegistration(chatId, parts[1]);
        }

        return createMessage(chatId, "❌ Неизвестная команда");
    }


    /**
     * Обрабатывает регистрацию сотрудника
     */
    private SendMessage handleRegistration(Long chatId, String employeeIdStr) {
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

    private Optional<Employee> findEmployeeByTelegramData(Long chatId, String telegramUsername) {
        if (chatId != null) {
            Optional<Employee> byChatId = employeeRepository.findAll().stream()
                    .filter(e -> String.valueOf(chatId).equals(e.getTelegramChatId()))
                    .findFirst();

            if (byChatId.isPresent()) return byChatId;
        }

        if (telegramUsername != null) {
            return findEmployeeByTelegramUsername(telegramUsername);
        }

        return Optional.empty();
    }

    private Optional<Employee> findEmployeeByTelegramUsername(String telegramUsername) {
        if (telegramUsername == null) return Optional.empty();
        String cleanUsername = telegramUsername.replace("@", "");

        return employeeRepository.findAll().stream()
                .filter(e -> cleanUsername.equals(e.getTelegramChatId()))
                .findFirst();
    }

    private Optional<Employee> findEmployeeByTelegramChatId(Long chatId) {
        if (chatId == null) return Optional.empty();
        return employeeRepository.findAll().stream()
                .filter(e -> chatId.equals(Long.valueOf(e.getTelegramChatId())))
                .findFirst();
    }

    private SendMessage createRegistrationMessage(Long chatId, String telegramUsername) {
        List<Employee> potentialEmployees = employeeRepository.findAll();

        if (potentialEmployees.isEmpty()) {
            return createMessage(chatId,
                    "🔐 *Требуется регистрация*\n\n" +
                            "Обратитесь к администратору для получения доступа.\n" +
                            "*Ваш Telegram ID:* " + chatId
            );
        }

        if (potentialEmployees.size() == 1) {
            return confirmRegistration(chatId, potentialEmployees.get(0));
        }

        return createEmployeeSelectionMessage(chatId, potentialEmployees);
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
    private SendMessage createEmployeeSelectionMessage(Long chatId, List<Employee> employees) {
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

    private SendMessage createMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode("Markdown");
        message.setDisableWebPagePreview(true);
        return message;
    }

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

    private SendMessage showTaskDetails(Long chatId, String taskIdStr, String telegramUsername) {
        try {
            Long taskId = Long.parseLong(taskIdStr);
            TaskDTO task = taskService.findById(taskId).orElseThrow();
            return createMessage(chatId,
                    "📋 *Детали задачи #" + task.getPublicId() + "*\n" +
                            "*Заголовок:* " + task.getTitle() + "\n" +
                            "*Статус:* " + translateStatus(task.getStatus())
            );
        } catch (Exception e) {
            log.error("Error showing task details", e);
            return createMessage(chatId, "❌ Ошибка при получении данных задачи");
        }
    }

    private SendMessage handleTaskStatusChange(Long chatId, String[] parts, String telegramUsername) {
        try {
            Long taskId = Long.parseLong(parts[1]);
            TaskStatus newStatus = TaskStatus.valueOf(parts[3]);

            log.info("handleTaskStatusChange: telegramUsername={}, chatId={}", telegramUsername, chatId);
            Optional<Employee> employeeOpt = findEmployeeByTelegramChatId(chatId);
            log.info("Found employee: {}", employeeOpt.isPresent());

            if (employeeOpt.isEmpty()) return createMessage(chatId, "❌ Вы не зарегистрированы");

            Employee employee = employeeOpt.get();
            TaskDTO task = taskService.findById(taskId).orElseThrow();

            if (task.getAssigneeId() == null || !task.getAssigneeId().equals(employee.getId()))
                return createMessage(chatId, "❌ У вас нет прав для этой задачи");

            auditContextProvider.setContext("TELEGRAM_BOT", employee.getEmail());
            try {
                taskService.changeStatus(taskId, newStatus);
            } finally {
                auditContextProvider.clearContext();
            }

            return createMessage(chatId, "✅ Статус задачи #" + task.getPublicId() + " изменен на " + translateStatus(newStatus));

        } catch (Exception e) {
            log.error("Error preparing task completed notification", e);
            return createMessage(chatId, "❌ Ошибка при изменении статуса задачи");
        }
    }
}