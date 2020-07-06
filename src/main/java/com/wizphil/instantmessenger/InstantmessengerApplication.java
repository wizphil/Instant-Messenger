package com.wizphil.instantmessenger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class InstantmessengerApplication {

	public static void main(String[] args) {
		log.info("Application starting.");
		SpringApplication.run(InstantmessengerApplication.class, args);
		log.info("Application started.");
		log.info("Swagger URL: http://localhost:8080/swagger-ui.html");
	}

}
