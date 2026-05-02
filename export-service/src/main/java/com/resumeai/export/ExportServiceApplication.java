package com.resumeai.export;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class ExportServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExportServiceApplication.class, args);
	}

}
