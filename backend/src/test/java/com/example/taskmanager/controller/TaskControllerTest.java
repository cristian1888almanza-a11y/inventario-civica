package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.entity.Task;
import com.example.taskmanager.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "EMPLOYEE")
class TaskControllerTest {
    @Autowired MockMvc mockMvc; @Autowired ObjectMapper mapper; @Autowired TaskRepository repository;
    @Test void createListUpdateDelete() throws Exception {
        String body = mapper.writeValueAsString(new TaskRequest("Prueba", "Desc", LocalDate.now(), false));
        String response = mockMvc.perform(post("/tasks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long id = mapper.readTree(response).get("id").asLong();
        mockMvc.perform(get("/tasks")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        String update = mapper.writeValueAsString(new TaskRequest("Actualizada", "Desc", LocalDate.now(), true));
        mockMvc.perform(put("/tasks/" + id).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        mockMvc.perform(delete("/tasks/" + id).with(csrf())).andExpect(status().isNoContent());
    }
    @Test void rejectsBlankTitle() throws Exception {
        String body = mapper.writeValueAsString(new TaskRequest("", "Desc", LocalDate.now(), false));
        mockMvc.perform(post("/tasks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }
    @Test void rejectsPastDate() throws Exception {
        String body = mapper.writeValueAsString(new TaskRequest("Tarea", "Desc", LocalDate.now().minusDays(1), false));
        mockMvc.perform(post("/tasks").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }
    @Test void missingTaskIs404() throws Exception { mockMvc.perform(get("/tasks/999999")).andExpect(status().isNotFound()); }
}
