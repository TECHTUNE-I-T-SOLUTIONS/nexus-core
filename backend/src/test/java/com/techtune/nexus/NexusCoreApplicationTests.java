package com.techtune.nexus;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Disabled("Requires PostgreSQL - enable when Testcontainers integration is complete")
class NexusCoreApplicationTests {

	@Test
	void contextLoads() {
	}

}
