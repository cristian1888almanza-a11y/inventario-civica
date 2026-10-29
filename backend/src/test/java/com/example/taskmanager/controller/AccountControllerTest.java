package com.example.taskmanager.controller;

import com.example.taskmanager.dto.AccountRequest;
import com.example.taskmanager.repository.ActivityLogRepository;
import com.example.taskmanager.repository.InventoryUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper mapper;
    @Autowired InventoryUserRepository repository;
    @Autowired ActivityLogRepository activityLogs;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearAccounts() {
        activityLogs.deleteAll();
        repository.deleteAll();
    }

    @Test
    void createsOneAdminAndAuthenticatesItsSession() throws Exception {
        mockMvc.perform(get("/auth/setup-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.setupRequired").value(true));

        MockHttpSession setupSession = (MockHttpSession) mockMvc.perform(post("/auth/setup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(account("admin", "Administradora", "SecurePassword123"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andReturn().getRequest().getSession(false);
        mockMvc.perform(get("/auth/me").session(setupSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.username").value("admin"));

        assertTrue(passwordEncoder.matches("SecurePassword123", repository.findByUsernameIgnoreCase("admin").orElseThrow().getPasswordHash()));
        mockMvc.perform(post("/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"SecurePassword123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.role").value("ADMIN"));
        mockMvc.perform(get("/auth/setup-status").session(setupSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.setupRequired").value(false));
        mockMvc.perform(post("/auth/setup").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(account("second-admin", "Otro administrador", "AnotherPassword123"))))
            .andExpect(status().isConflict());
    }

    @Test
    void administratorCreatesEmployeesAndEmployeesCannotManageProfiles() throws Exception {
        String employee = mapper.writeValueAsString(account("jlopez", "Juan López", "EmployeePassword123"));
        mockMvc.perform(post("/users").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(employee))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("EMPLOYEE"));

        mockMvc.perform(get("/users").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/users").with(user("jlopez").roles("EMPLOYEE")))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/users").with(user("jlopez").roles("EMPLOYEE")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(employee))
            .andExpect(status().isForbidden());
    }

    @Test
    void administratorCanCreateAdditionalAdministrators() throws Exception {
        String request = "{\"fullName\":\"Otra Administradora\",\"username\":\"admin.dos\","
            + "\"password\":\"AnotherPassword123\",\"role\":\"ADMIN\"}";
        String response = mockMvc.perform(post("/users").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(request))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.username").value("admin.dos"))
            .andReturn().getResponse().getContentAsString();
        long administratorId = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/users/{id}/status", administratorId).param("enabled", "false")
                .with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().isBadRequest());
    }

    @Test
    void inactiveEmployeeCannotSignIn() throws Exception {
        repository.save(accountEntity("inactive", "Empleado inactivo", "EmployeePassword123", false));
        mockMvc.perform(post("/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"inactive\",\"password\":\"EmployeePassword123\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void administratorCanReviewActivityAndRemoveEmployeeAccessWithoutDeletingHistory() throws Exception {
        String employee = mapper.writeValueAsString(account("jlopez", "Juan López", "EmployeePassword123"));
        String response = mockMvc.perform(post("/users").with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(employee))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        long employeeId = mapper.readTree(response).get("id").asLong();

        mockMvc.perform(patch("/users/{id}/status", employeeId).param("enabled", "false")
                .with(user("admin").roles("ADMIN")).with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));
        assertTrue(repository.findById(employeeId).isPresent());

        mockMvc.perform(get("/admin/activity").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].actorUsername").value("admin"))
            .andExpect(jsonPath("$[0].action").value("EMPLOYEE_ACCESS_REMOVED"))
            .andExpect(jsonPath("$[1].action").value("EMPLOYEE_CREATED"));
        mockMvc.perform(get("/admin/activity").with(user("jlopez").roles("EMPLOYEE")))
            .andExpect(status().isForbidden());
    }

    private AccountRequest account(String username, String fullName, String password) {
        return new AccountRequest(fullName, username, password, null);
    }

    private com.example.taskmanager.entity.InventoryUser accountEntity(String username, String fullName, String password, boolean enabled) {
        var account = new com.example.taskmanager.entity.InventoryUser();
        account.setUsername(username);
        account.setFullName(fullName);
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setRole(com.example.taskmanager.entity.UserRole.EMPLOYEE);
        account.setEnabled(enabled);
        return account;
    }
}
