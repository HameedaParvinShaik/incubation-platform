package com.startupincubator.config;

import com.startupincubator.entity.Role;
import com.startupincubator.enums.RoleType;
import com.startupincubator.repository.RoleRepository;
import com.startupincubator.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CommandLineRunner initRoles(RoleRepository roleRepository) {
        return args -> {
            for (RoleType roleType : RoleType.values()) {
                if (!roleRepository.existsByName(roleType.name())) {
                    Role role = Role.builder()
                            .name(roleType.name())
                            .description(roleType.name() + " Role")
                            .build();
                    roleRepository.save(role);
                    System.out.println("✅ Created role: " + roleType.name());
                } else {
                    System.out.println("ℹ️ Role already exists: " + roleType.name());
                }
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/login", "/auth/login", "/auth/register", "/register",
                                "/css/**", "/js/**", "/images/**", "/static/**", "/webjars/**",
                                "/favicon.ico", "/error").permitAll()
                
                // ✅ Role-based access control - FIXED
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/mentor/**").hasAnyRole("ADMIN", "MENTOR")  // ← FIXED: Allow ADMIN too
                .requestMatchers("/founder/**").hasRole("FOUNDER")
                
                // ✅ All dashboard pages require authentication
                .requestMatchers("/dashboard/**").authenticated()
                .requestMatchers("/users/**").authenticated()
                .requestMatchers("/startups/**").authenticated()
                .requestMatchers("/funding/**").authenticated()
                .requestMatchers("/reports/**").authenticated()
                .requestMatchers("/settings/**").authenticated()
                .requestMatchers("/profile/**").authenticated()
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/auth/login?error=true")
                .usernameParameter("email")
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/auth/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .maximumSessions(1)
                .expiredUrl("/auth/login?expired=true")
            );

        return http.build();
    }
}