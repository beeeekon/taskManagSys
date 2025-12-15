package ru.astalavista.taskManagSys.model.event;

import ru.astalavista.taskManagSys.model.entity.Task;

public record TaskCompletedEvent(Task task) {

}
