package com.appdirect.tika;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Slf4j
@SpringBootApplication
@EnableAspectJAutoProxy
public class ApacheTikaApplication {

	public static void main(String[] args) {

		SpringApplication.run(ApacheTikaApplication.class, args);

	}
}
