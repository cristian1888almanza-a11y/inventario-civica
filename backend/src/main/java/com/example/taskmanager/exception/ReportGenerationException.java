package com.example.taskmanager.exception;

public class ReportGenerationException extends RuntimeException {
    public ReportGenerationException(Throwable cause) {
        super("No se pudo generar el reporte PDF", cause);
    }
}
