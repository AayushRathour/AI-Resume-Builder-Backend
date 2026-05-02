package com.resumeai.section;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class SectionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SectionServiceApplication.class, args);
	}
}
