package com.example.taskmanager.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ExcelReportWriter {
    private ExcelReportWriter() {
    }

    public static byte[] write(List<String> headers, List<List<Object>> rows) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte");
            writeRow(sheet.createRow(0), headers);
            for (int index = 0; index < rows.size(); index++) {
                writeRow(sheet.createRow(index + 1), rows.get(index));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new UncheckedIOException("No se pudo generar el archivo Excel", exception);
        }
    }

    private static void writeRow(Row row, List<?> values) {
        for (int index = 0; index < values.size(); index++) {
            Object value = values.get(index);
            if (value instanceof Number number) {
                row.createCell(index).setCellValue(number.doubleValue());
            } else if (value != null) {
                row.createCell(index).setCellValue(value.toString());
            }
        }
    }
}