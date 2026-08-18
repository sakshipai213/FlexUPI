package com.fintech.upisimulator;

import com.fintech.upisimulator.model.UserAccount;
import com.fintech.upisimulator.repository.UserAccountRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication
public class UpiSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(UpiSimulatorApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UserAccountRepository repository) {
        return args -> {
            if (repository.findByVpa("john@upi").isEmpty()) {
                repository.save(UserAccount.builder()
                        .vpa("john@upi")
                        .creditLimit(new BigDecimal("50000.00"))
                        .availableCredit(new BigDecimal("50000.00"))
                        .build());
                System.out.println("✅ Seeded default user 'john@upi' with 50,000 credit limit.");
            }
        };
    }
}