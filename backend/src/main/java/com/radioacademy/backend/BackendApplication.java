package com.radioacademy.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

	@Bean
	public CommandLineRunner imprimirHash(
			org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
		return args -> {
			System.out.println("------------------------------------------------");
			System.out.println("🔑 TU HASH PARA 'admin' ES: " + passwordEncoder.encode("admin"));
			System.out.println("------------------------------------------------");
		};
	}

}