package com.resumeai.section;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Application entry point for section-service. */

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class SectionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SectionServiceApplication.class, args);
	}
}



