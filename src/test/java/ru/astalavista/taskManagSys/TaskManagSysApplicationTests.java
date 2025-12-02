package ru.astalavista.taskManagSys;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestContainersConfiguration.class)
@SpringBootTest
class TaskManagSysApplicationTests {

	@Test
	void contextLoads() {
	}

}
