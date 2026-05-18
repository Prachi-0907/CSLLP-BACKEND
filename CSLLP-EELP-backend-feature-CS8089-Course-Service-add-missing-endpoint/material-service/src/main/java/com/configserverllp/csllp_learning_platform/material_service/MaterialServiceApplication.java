package com.configserverllp.csllp_learning_platform.material_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MaterialServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MaterialServiceApplication.class, args);
	}

}
