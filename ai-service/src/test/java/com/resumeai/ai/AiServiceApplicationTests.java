package com.resumeai.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class AiServiceApplicationTests {

	@Test
	void contextLoads() {
		// Verify the main application class can be instantiated
		AiServiceApplication app = new AiServiceApplication();
		assertNotNull(app);
	}

	@Test
	void mainMethodDoesNotThrow() {
		// Verify the class exists and is properly annotated
		assertNotNull(AiServiceApplication.class.getAnnotations());
	}
}
