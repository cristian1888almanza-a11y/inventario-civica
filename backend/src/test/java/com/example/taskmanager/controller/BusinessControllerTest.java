package com.example.taskmanager.controller;

import com.example.taskmanager.entity.*;
import com.example.taskmanager.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
class BusinessControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository products;
    @Autowired OperationRepository operations;
    @Autowired CashMovementRepository movements;
    @Autowired CashRegisterRepository registers;
    private Product product;

    @BeforeEach
    void resetData() {
        movements.deleteAll();
        operations.deleteAll();
        registers.deleteAll();
        products.deleteAll();
        product = new Product();
        product.setSku("TEST-01"); product.setName("Producto prueba"); product.setCategory("General");
        product.setPrice(new BigDecimal("12.50")); product.setStock(10); product.setMinStock(1);
        product = products.saveAndFlush(product);
    }

    @Test
    void quotesPurchasesCashSalesReceiptsReservationsAndWorkshopArePersistent() throws Exception {
        String quoteBody = "{\"customer\":\"Cliente\",\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":2,\"unitPrice\":0.01}]}";
        String quoteResponse = mvc.perform(post("/quotes").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(quoteBody))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(25.0))
            .andExpect(jsonPath("$.status").value("DRAFT")).andReturn().getResponse().getContentAsString();
        long quoteId = id(quoteResponse);
        mvc.perform(get("/quotes")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(quoteId));
        mvc.perform(patch("/quotes/" + quoteId + "/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"SENT\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));

        String purchaseBody = "{\"supplier\":\"Proveedor\",\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":3,\"unitCost\":7.00}]}";
        String purchaseResponse = mvc.perform(post("/purchases").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(purchaseBody))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(21.0)).andReturn().getResponse().getContentAsString();
        long purchaseId = id(purchaseResponse);
        mvc.perform(post("/purchases/" + purchaseId + "/receive").with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("RECEIVED"));
        assertEquals(13, products.findById(product.getId()).orElseThrow().getStock());
        mvc.perform(post("/purchases/" + purchaseId + "/receive").with(csrf())).andExpect(status().isConflict());
        assertEquals(13, products.findById(product.getId()).orElseThrow().getStock());

        mvc.perform(post("/sales").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentMethod\":\"CASH\",\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":2}]}"))
            .andExpect(status().isConflict());
        mvc.perform(post("/cash/open").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"openingAmount\":100.00}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.balance").value(100.0));
        mvc.perform(post("/cash/movements/in").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":10.00,\"description\":\"Fondo adicional\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(110.0));
        mvc.perform(post("/cash/movements/out").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":5.00,\"description\":\"Gasto de caja\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(105.0));
        String saleResponse = mvc.perform(post("/sales").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentMethod\":\"CASH\",\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":2,\"unitPrice\":0.01}]}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.total").value(25.0))
            .andExpect(jsonPath("$.paidTotal").value(25.0)).andExpect(jsonPath("$.receiptNumber").isNotEmpty())
            .andReturn().getResponse().getContentAsString();
        JsonNode sale = mapper.readTree(saleResponse);
        String receipt = sale.get("receiptNumber").asText();
        assertEquals(11, products.findById(product.getId()).orElseThrow().getStock());
        mvc.perform(get("/receipts/" + receipt)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(sale.get("id").asLong()));
        mvc.perform(get("/receipts")).andExpect(status().isOk()).andExpect(jsonPath("$[0].receiptNumber").value(receipt));
        mvc.perform(get("/receipts/" + receipt + "/pdf")).andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF))
            .andExpect(result -> assertTrue(result.getResponse().getContentAsByteArray().length > 100));
        mvc.perform(get("/cash/active")).andExpect(status().isOk()).andExpect(jsonPath("$.balance").value(130.0));
        mvc.perform(get("/cash/movements")).andExpect(status().isOk()).andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)));
        mvc.perform(post("/cash/close").with(csrf())).andExpect(status().isOk())
            .andExpect(jsonPath("$.closingAmount").value(130.0));

        String reservationBody = "{\"customer\":\"Reserva\",\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":2}]}";
        String reservationResponse = mvc.perform(post("/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(reservationBody))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("RESERVED")).andReturn().getResponse().getContentAsString();
        long reservationId = id(reservationResponse);
        assertEquals(9, products.findById(product.getId()).orElseThrow().getStock());
        mvc.perform(patch("/reservations/" + reservationId + "/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        assertEquals(11, products.findById(product.getId()).orElseThrow().getStock());
        mvc.perform(patch("/reservations/" + reservationId + "/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}")).andExpect(status().isConflict());
        assertEquals(11, products.findById(product.getId()).orElseThrow().getStock());

        String workshopResponse = mvc.perform(post("/workshop/orders").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"Cliente taller\",\"description\":\"Diagnóstico\",\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":1}]}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("OPEN")).andReturn().getResponse().getContentAsString();
        long workshopId = id(workshopResponse);
        mvc.perform(patch("/workshop/orders/" + workshopId + "/status").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(get("/workshop/orders")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(workshopId));
    }

    @Test
    void validatesRequestsAndRequiresAuthentication() throws Exception {
        mvc.perform(post("/quotes").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"customer\":\"\",\"items\":[]}")).andExpect(status().isBadRequest());
        mvc.perform(post("/sales").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"paymentMethod\":\"CREDIT\",\"items\":[{\"productId\":" + product.getId() + ",\"quantity\":1}]}"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/quotes").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsOversellingAndRollsBackAllProductStockChanges() throws Exception {
        mvc.perform(post("/cash/open").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"openingAmount\":0}"))
            .andExpect(status().isCreated());
        String sale = "{\"paymentMethod\":\"CARD\",\"items\":[{\"productId\":" + product.getId()
            + ",\"quantity\":6},{\"productId\":" + product.getId() + ",\"quantity\":6}]}";
        mvc.perform(post("/sales").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(sale))
            .andExpect(status().isBadRequest());
        assertEquals(10, products.findById(product.getId()).orElseThrow().getStock());
        assertTrue(operations.findAllByTypeOrderByCreatedAtDesc(OperationType.SALE).isEmpty());
    }

    private long id(String json) throws Exception { return mapper.readTree(json).get("id").asLong(); }
}
