package com.example.taskmanager.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String username) {
        super("Ya existe una cuenta con el usuario " + username);
    }
}
