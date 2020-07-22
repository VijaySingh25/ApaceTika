package com.appdirect.tika.api.exceptionhandling;

import java.io.FileNotFoundException;

import org.apache.tika.exception.TikaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class ControllerExceptionHandler {

	@ExceptionHandler(FileNotFoundException.class)
	public Mono<ResponseEntity<String>> errorResponse(FileNotFoundException e) {
		return Mono.just(new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST));
	}

	@ExceptionHandler(TikaException.class)
	public Mono<ResponseEntity<String>> errorResponse(TikaException e) {
		return Mono.just(new ResponseEntity<>("Error while extracting content: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
	}

	@ExceptionHandler(Exception.class)
	public Mono<ResponseEntity<String>> errorResponse(Exception e) {
		return Mono.just(new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR));
	}
}
