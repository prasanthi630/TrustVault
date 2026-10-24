
package com.trustvault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.trustvault.repository.AppUserRepository;

@Configuration
public class SecurityConfig {

    // User login authentication
    @Bean
    public UserDetailsService userDetailsService(
            AppUserRepository appUserRepository) {

        return email -> {

            var appUser = appUserRepository.findByEmail(email)
                    .orElseThrow(() ->
                            new UsernameNotFoundException(
                                    "User not found"
                            ));

            UserDetails user =
                    org.springframework.security.core.userdetails.User
                            .builder()
                            .username(appUser.getEmail())
                            .password(appUser.getPassword())
                            .roles("USER")
                            .build();

            return user;
        };
    }

    // Password encryption
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Security configuration
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http

            // Existing forms use CSRF disabled
            .csrf(csrf -> csrf.disable())

            // H2 Console requires frames
            .headers(headers ->
                    headers.frameOptions(frame ->
                            frame.sameOrigin()
                    )
            )

            // URL access permissions
            .authorizeHttpRequests(auth -> auth

                // H2 Console
                .requestMatchers("/h2-console/**").permitAll()

                // Public pages
                .requestMatchers(
                        "/",
                        "/home",
                        "/login",
                        "/register",
                        "/error",
                        "/css/**",
                        "/js/**"
                ).permitAll()

                // Other pages require login
                .anyRequest().authenticated()
            )

            // Login configuration
            .formLogin(form -> form
                    .loginPage("/login")
                    .loginProcessingUrl("/login")
                    .defaultSuccessUrl("/dashboard", true)
                    .failureUrl("/login?error")
                    .permitAll()
            )

            // Logout configuration
            .logout(logout -> logout
                    .logoutUrl("/logout")
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .logoutSuccessUrl("/login?logout")
                    .permitAll()
            );

        return http.build();
    }
}
