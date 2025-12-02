package ru.astalavista.taskManagSys;

import org.springframework.boot.SpringApplication;

public class TestTaskManagSysApplication {

	public static void main(String[] args) {
		SpringApplication.from(TaskManagSysApplication::main).with(TestContainersConfiguration.class).run(args);
	}

}
