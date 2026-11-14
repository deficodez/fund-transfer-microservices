package com.defi.fundtransfer.account;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class: one real PostgreSQL per test JVM. We test against Postgres (not H2) because row locks,
 * unique constraints and isolation behaviour are exactly what these tests prove. Requires Docker.
 */
@SpringBootTest
@Testcontainers
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
}
