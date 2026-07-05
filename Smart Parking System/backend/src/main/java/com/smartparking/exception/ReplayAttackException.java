package com.smartparking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ReplayAttackException extends RuntimeException {
    public ReplayAttackException(String message) {
        super(message);
    }
}
