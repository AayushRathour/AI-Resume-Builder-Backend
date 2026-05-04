package com.resumeai.jobmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@org.springframework.cache.annotation.EnableCaching
public class JobmatchServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobmatchServiceApplication.class, args);
	}

}
