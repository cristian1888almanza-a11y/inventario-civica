package com.example.taskmanager.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("No se encontró el producto con id " + id);
    }
}
