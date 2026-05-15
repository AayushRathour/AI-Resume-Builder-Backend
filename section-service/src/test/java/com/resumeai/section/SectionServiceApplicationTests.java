package com.resumeai.section;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false"
})
class SectionServiceApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertNotNull(applicationContext, "Application context should load successfully");
	}

}
