package com.example.taskmanager.exception;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(Long id) { super("No existe una tarea con ID " + id); }
}
