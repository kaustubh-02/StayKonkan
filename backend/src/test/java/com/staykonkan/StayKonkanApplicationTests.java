package com.staykonkan;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: verifies the full Spring application context (security,
 * JPA, JWT config, exception handling, etc.) wires up without errors.
 * This is the single most valuable test in the foundation phase — a
 * green run here proves every bean in this phase is correctly configured
 * and none of the wiring in later phases has to second-guess it.
 */
@SpringBootTest
@ActiveProfiles("dev")
class StayKonkanApplicationTests {

    @Test
    void contextLoads() {
        // Intentionally empty: a failure to load the ApplicationContext
        // fails this test automatically.
    }
}
