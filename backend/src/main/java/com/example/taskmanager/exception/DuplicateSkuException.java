package com.example.taskmanager.exception;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String sku) {
        super("Ya existe un producto con el SKU " + sku);
    }
}
