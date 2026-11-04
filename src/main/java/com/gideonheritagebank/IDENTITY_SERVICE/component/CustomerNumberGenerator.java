package com.gideonheritagebank.IDENTITY_SERVICE.component;

import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomerNumberGenerator {

    private static final String PREFIX = "GH";
    private static final int NUMBER_LENGTH = 10;

    private final BankUserRepository bankUserRepository;

    public String generate() {
        Long sequence = bankUserRepository.nextCustomerNumberSequence();

        if (sequence == null || sequence < 1 || sequence > 9_999_999_999L) {
            throw new IllegalStateException(
                    "Unable to generate a valid customer number."
            );
        }

        return PREFIX + String.format("%010d", sequence);
    }
}