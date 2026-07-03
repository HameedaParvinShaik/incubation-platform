package com.startupincubator.security;

import com.startupincubator.entity.User;
import com.startupincubator.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("===== LOADING USER =====");
        log.info("Email: {}", email);

        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> {
                    log.error("❌ User not found: {}", email);
                    return new UsernameNotFoundException("User not found: " + email);
                });

        log.info("✅ User found: {}", user.getEmail());
        log.info("✅ Roles count: {}", user.getRoles().size());

        user.getRoles().forEach(role -> log.info("   - Role: {}", role.getName()));

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getIsActive() != null && user.getIsActive(),
                true,
                true,
                true,
                user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority(role.getName()))
                        .collect(Collectors.toList())
        );
    }
}