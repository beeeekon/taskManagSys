package ru.astalavista.taskManagSys.model.event;

import ru.astalavista.taskManagSys.model.entity.Task;

public record TaskCreatedEvent(Task task) {

}
