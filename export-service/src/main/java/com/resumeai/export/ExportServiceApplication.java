package com.resumeai.export;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/** Application entry point for export-service. */

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class ExportServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExportServiceApplication.class, args);
	}

}



