package com.smartparking.service;

public interface NonceService {
    String generateNonce();
    void validateNonce(String nonce, long timestamp);
}
