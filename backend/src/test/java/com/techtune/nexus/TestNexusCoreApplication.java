package com.techtune.nexus;

import org.springframework.boot.SpringApplication;

public class TestNexusCoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(NexusCoreApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
