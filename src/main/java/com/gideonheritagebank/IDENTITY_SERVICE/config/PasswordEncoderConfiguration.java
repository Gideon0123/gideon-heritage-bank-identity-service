package com.gideonheritagebank.IDENTITY_SERVICE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;

@Configuration
public class PasswordEncoderConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                16,      // Salt length in bytes
                32,      // Hash length in bytes
                1,       // Parallelism
                65536,   // Memory in KiB: 64 MiB
                3        // Iterations
        );
    }
}
