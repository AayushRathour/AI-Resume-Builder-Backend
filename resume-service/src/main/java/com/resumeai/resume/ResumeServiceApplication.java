package com.resumeai.resume;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Resume-service bootstrap for resume CRUD and ATS workflows.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ResumeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ResumeServiceApplication.class, args);
	}

}
