package com.resumeai.jobmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class JobmatchServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobmatchServiceApplication.class, args);
	}

}
