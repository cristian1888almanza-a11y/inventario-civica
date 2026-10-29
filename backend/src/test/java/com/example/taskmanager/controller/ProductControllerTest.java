package com.example.taskmanager.controller;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.taskmanager.dto.ProductRequest;
import com.example.taskmanager.repository.CashMovementRepository;
import com.example.taskmanager.repository.CashRegisterRepository;
import com.example.taskmanager.repository.OperationRepository;
import com.example.taskmanager.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "ADMIN")
class ProductControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository repository;
    @Autowired OperationRepository operations;
    @Autowired CashMovementRepository movements;
    @Autowired CashRegisterRepository registers;

    @BeforeEach
    void clearProducts() {
        movements.deleteAll();
        operations.deleteAll();
        registers.deleteAll();
        repository.deleteAll();
    }

    @Test
    void createListUpdateDeleteAndSummary() throws Exception {
        String body = mapper.writeValueAsString(product("tec-001", "Teclado", 4, 5));
        String response = mockMvc.perform(post("/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.sku").value("TEC-001"))
            .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/products")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/products/summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalProducts").value(1))
            .andExpect(jsonPath("$.totalUnits").value(4))
            .andExpect(jsonPath("$.inventoryValue").value(40.0))
            .andExpect(jsonPath("$.lowStock").value(1));

        mockMvc.perform(put("/products/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(product("tec-001", "Teclado", 8, 5))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.stock").value(8));
        mockMvc.perform(delete("/products/" + id).with(csrf())).andExpect(status().isNoContent());
    }

    @Test
    void rejectsDuplicateSkuAndInvalidStock() throws Exception {
        String body = mapper.writeValueAsString(product("dup-01", "Mouse", 3, 2));
        mockMvc.perform(post("/products").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(product("DUP-01", "Mouse 2", 3, 2))))
            .andExpect(status().isConflict());
        mockMvc.perform(post("/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(product("invalid", "Producto", -1, 0))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void explainsWhyProductReferencedByAnOperationCannotBeDeleted() throws Exception {
        String productResponse = mockMvc.perform(post("/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(product("ref-01", "Producto con historial", 3, 1))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(productResponse).get("id").asLong();
        String quoteBody = "{\"customer\":\"Cliente\",\"items\":[{\"productId\":" + id + ",\"quantity\":1}]}";

        mockMvc.perform(post("/quotes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(quoteBody))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/products/" + id).with(csrf()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(
                "No se puede eliminar este producto porque está asociado a operaciones comerciales existentes. El producto se conservó para proteger el historial."));
        mockMvc.perform(get("/products/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.sku").value("REF-01"));
    }

    @Test
    void exportsJasperPdf() throws Exception {
        mockMvc.perform(post("/products").with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(product("pdf-01", "Producto de prueba", 2, 1))))
            .andExpect(status().isCreated());
        mockMvc.perform(get("/products/report"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("reporte-inventario.pdf")))
            .andExpect(result -> assertTrue(result.getResponse().getContentAsByteArray().length > 4));
    }

    @Test
    void allowsLocalhostAndLoopbackFrontendOrigins() throws Exception {
        mockMvc.perform(options("/products")
                .header(HttpHeaders.ORIGIN, "http://127.0.0.1:4200")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:4200"));
    }

    @Test
    void allowsCommonLocalDevPorts() throws Exception {
        mockMvc.perform(options("/products")
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    private ProductRequest product(String sku, String name, int stock, int minStock) {
        return new ProductRequest(sku, name, "Electrónica", "Producto de prueba", new BigDecimal("10.00"), stock, minStock, "Proveedor");
    }
}
