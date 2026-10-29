package com.example.taskmanager.controller;

import com.example.taskmanager.dto.TaskRequest;
import com.example.taskmanager.dto.TaskResponse;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService service;
    public TaskController(TaskService service) { this.service = service; }
    @GetMapping public List<TaskResponse> getAll(@RequestParam(required = false) Boolean completed) { return service.findAll(completed); }
    @GetMapping("/{id}") public TaskResponse getById(@PathVariable Long id) { return service.findById(id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public TaskResponse create(@Valid @RequestBody TaskRequest request) { return service.create(request); }
    @PutMapping("/{id}") public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskRequest request) { return service.update(id, request); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id) { service.delete(id); }
}
