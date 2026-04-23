package com.example.atleastonce.model;

public record OrderEvent(String orderId, String customerId, String status, double amount) {}
