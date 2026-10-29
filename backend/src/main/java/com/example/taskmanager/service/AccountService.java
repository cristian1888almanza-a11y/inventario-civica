package com.example.taskmanager.service;

import com.example.taskmanager.dto.AccountRequest;
import com.example.taskmanager.dto.AccountResponse;
import com.example.taskmanager.entity.InventoryUser;
import com.example.taskmanager.entity.UserRole;
import com.example.taskmanager.exception.AccountNotFoundException;
import com.example.taskmanager.exception.AdminSetupCompleteException;
import com.example.taskmanager.exception.DuplicateUsernameException;
import com.example.taskmanager.repository.InventoryUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;

@Service
public class AccountService {
    private final InventoryUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLog;

    public AccountService(InventoryUserRepository repository, PasswordEncoder passwordEncoder, ActivityLogService activityLog) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.activityLog = activityLog;
    }

    public boolean setupRequired() {
        return repository.count() == 0;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public InventoryUser setupFirstAdmin(AccountRequest request) {
        if (repository.count() != 0) {
            throw new AdminSetupCompleteException();
        }
        InventoryUser administrator = createAccount(request, UserRole.ADMIN);
        activityLog.record(administrator.getUsername(), "ADMIN_SETUP", "Se creó el perfil administrador inicial");
        return administrator;
    }

    @Transactional
    public AccountResponse createUser(AccountRequest request) {
        UserRole role = request.role() == UserRole.ADMIN ? UserRole.ADMIN : UserRole.EMPLOYEE;
        InventoryUser account = createAccount(request, role);
        String accountType = role == UserRole.ADMIN ? "administrador" : "empleado";
        String action = role == UserRole.ADMIN ? "ADMIN_CREATED" : "EMPLOYEE_CREATED";
        activityLog.record(action, "Se creó el perfil de " + accountType + " " + account.getUsername());
        return AccountResponse.from(account);
    }

    public List<AccountResponse> findAll() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "fullName"))
            .stream().map(AccountResponse::from).toList();
    }

    @Transactional
    public AccountResponse setEnabled(Long id, boolean enabled, String actingUsername) {
        InventoryUser account = repository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));
        if (account.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("Las cuentas de administrador no pueden desactivarse");
        }
        if (account.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new IllegalArgumentException("No puedes desactivar tu propia cuenta");
        }
        account.setEnabled(enabled);
        InventoryUser updated = repository.save(account);
        activityLog.record(enabled ? "EMPLOYEE_ACCESS_RESTORED" : "EMPLOYEE_ACCESS_REMOVED",
            (enabled ? "Se restauró el acceso de " : "Se quitó el acceso de ") + account.getUsername());
        return AccountResponse.from(updated);
    }

    private InventoryUser createAccount(AccountRequest request, UserRole role) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByUsernameIgnoreCase(username)) {
            throw new DuplicateUsernameException(username);
        }
        InventoryUser account = new InventoryUser();
        account.setUsername(username);
        account.setFullName(request.fullName().trim());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setRole(role);
        account.setEnabled(true);
        account.setPermissions(request.permissions());
        return repository.save(account);
    }
}
