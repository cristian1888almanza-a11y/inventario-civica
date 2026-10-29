package com.example.taskmanager.controller;

import com.example.taskmanager.dto.AccountRequest;
import com.example.taskmanager.dto.AccountResponse;
import com.example.taskmanager.dto.LoginRequest;
import com.example.taskmanager.entity.InventoryUser;
import com.example.taskmanager.exception.AdminSetupCompleteException;
import com.example.taskmanager.service.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AccountService accountService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final com.example.taskmanager.service.ActivityLogService activityLog;
    private final SecurityContextHolderStrategy contextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public AuthController(AccountService accountService, AuthenticationManager authenticationManager,
                          SecurityContextRepository contextRepository,
                          com.example.taskmanager.service.ActivityLogService activityLog) {
        this.accountService = accountService;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.activityLog = activityLog;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken) {
        return Map.of("token", csrfToken.getToken());
    }

    @GetMapping("/setup-status")
    public Map<String, Boolean> setupStatus() {
        return Map.of("setupRequired", accountService.setupRequired());
    }

    @PostMapping("/setup")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse setup(@Valid @RequestBody AccountRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        InventoryUser administrator;
        try {
            administrator = accountService.setupFirstAdmin(request);
        } catch (AdminSetupCompleteException exception) {
            throw exception;
        }
        authenticate(administrator.getUsername(), request.password(), httpRequest, response);
        return AccountResponse.from(administrator);
    }

    @PostMapping("/login")
    public AccountResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username().trim(), request.password()));
        } catch (BadCredentialsException exception) {
            throw new BadCredentialsException("El usuario o la contraseña no son correctos");
        } catch (DisabledException exception) {
            throw new DisabledException("Esta cuenta está desactivada. Contacta al administrador.");
        }
        saveAuthentication(authentication, httpRequest, response);
        activityLog.record(authentication.getName(), "LOGIN", "Inició sesión");
        return accountService.findAll().stream()
            .filter(account -> account.username().equalsIgnoreCase(authentication.getName()))
            .findFirst().orElseThrow(() -> new BadCredentialsException("No se pudo cargar el perfil de usuario"));
    }

    @GetMapping("/me")
    public AccountResponse me(Authentication authentication) {
        return accountService.findAll().stream()
            .filter(account -> account.username().equalsIgnoreCase(authentication.getName()))
            .findFirst().orElseThrow(() -> new BadCredentialsException("No se pudo cargar el perfil de usuario"));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        Authentication authentication = contextHolderStrategy.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            activityLog.record(authentication.getName(), "LOGOUT", "Cerró sesión");
        }
        contextHolderStrategy.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private void authenticate(String username, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(username, password));
        saveAuthentication(authentication, request, response);
    }

    private void saveAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = contextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        contextHolderStrategy.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
