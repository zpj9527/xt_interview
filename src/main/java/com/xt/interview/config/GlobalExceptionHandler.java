package com.xt.interview.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", exception.getMessage());
        return ResponseEntity.badRequest().body(body);
    }
}
