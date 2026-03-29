package com.legalrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class LegalRagAssistantApplication {

	public static void main(String[] args) {
		SpringApplication.run(LegalRagAssistantApplication.class, args);
	}

}
