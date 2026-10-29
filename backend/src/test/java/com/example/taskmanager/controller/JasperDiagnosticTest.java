package com.example.taskmanager.controller;

import com.example.taskmanager.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class JasperDiagnosticTest {
    @Autowired ProductService service;

    @Test
    void dumpReportError() {
        try {
            byte[] pdf = service.exportReport();
            System.out.println("PDF length: " + (pdf == null ? "null" : pdf.length));
        } catch (Throwable t) {
            System.out.println("EXCEPTION: " + t);
            t.printStackTrace();
        }
    }
}