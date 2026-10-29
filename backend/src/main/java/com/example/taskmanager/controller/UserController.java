package com.example.taskmanager.controller;

import com.example.taskmanager.dto.AccountRequest;
import com.example.taskmanager.dto.AccountResponse;
import com.example.taskmanager.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    private final AccountService service;

    public UserController(AccountService service) {
        this.service = service;
    }

    @GetMapping
    public List<AccountResponse> getAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return service.createUser(request);
    }

    @PatchMapping("/{id}/status")
    public AccountResponse setStatus(@PathVariable Long id, @RequestParam boolean enabled, Authentication authentication) {
        return service.setEnabled(id, enabled, authentication.getName());
    }
}
