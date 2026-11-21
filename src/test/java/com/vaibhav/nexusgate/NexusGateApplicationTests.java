package com.vaibhav.nexusgate;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = NexusGateApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NexusGateApplicationTests {

    @Test
    void contextLoads() {
        // Verifies Spring Cloud Gateway reactive context and route initialization
    }
}
