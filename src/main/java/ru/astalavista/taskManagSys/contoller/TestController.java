package ru.astalavista.taskManagSys.contoller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// Тестовый контроллер
@RestController
public class TestController {

    @GetMapping("/")
    public String hello() {
        return "Hello Docker!";
    }
}  