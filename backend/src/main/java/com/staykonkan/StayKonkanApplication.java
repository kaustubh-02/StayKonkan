package com.staykonkan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * StayKonkan backend entry point.
 *
 * This phase (Phase 3) wires only the enterprise foundation: security,
 * JWT scaffolding, global exception handling, base entities/DTOs, logging,
 * and configuration. Business modules (Hotel, Booking, Restaurant, Cab,
 * Payment) are intentionally NOT present yet — they are added in later
 * phases on top of this foundation without changing anything here.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class StayKonkanApplication {

    public static void main(String[] args) {
        SpringApplication.run(StayKonkanApplication.class, args);
    }
}
