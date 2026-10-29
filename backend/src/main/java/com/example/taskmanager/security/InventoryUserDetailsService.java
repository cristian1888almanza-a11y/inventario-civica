package com.example.taskmanager.security;

import com.example.taskmanager.repository.InventoryUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class InventoryUserDetailsService implements UserDetailsService {
    private final InventoryUserRepository repository;

    public InventoryUserDetailsService(InventoryUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var account = repository.findByUsernameIgnoreCase(username.trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos"));
        var authorities = Stream.concat(
                Stream.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())),
                account.getPermissions().stream().map(permission -> new SimpleGrantedAuthority(permission.name()))
            ).toList();
        return User.withUsername(account.getUsername())
            .password(account.getPasswordHash())
            .authorities(authorities)
            .disabled(!account.isEnabled())
            .build();
    }
}
