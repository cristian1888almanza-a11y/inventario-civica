package com.example.taskmanager.exception;

public class AdminSetupCompleteException extends RuntimeException {
    public AdminSetupCompleteException() {
        super("La cuenta de administrador ya fue configurada");
    }
}
