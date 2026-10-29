package com.example.taskmanager.exception;

import java.time.LocalDateTime;
import java.util.List;

public record ApiError(LocalDateTime timestamp, int status, String message, List<String> errors) {}
