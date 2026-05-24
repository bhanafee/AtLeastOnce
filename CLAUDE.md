# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build and run all tests
./gradlew build

# Run tests only
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.atleastonce.LanguagePreferenceProducerTest"

# Format code (Google Java Format via Spotless)
./gradlew spotlessApply

# Check formatting without applying
./gradlew spotlessCheck

# OWASP dependency vulnerability check (fails build on CVSS ≥ 7)
./gradlew dependencyCheckAnalyze

# Run the application (requires Kafka on localhost:9092)
./gradlew bootRun

# Local Kafka via Docker (KRaft mode, no ZooKeeper)
./kafka-local.sh start
./kafka-local.sh stop
./kafka-local.sh status
```

## Architecture

This is a Spring Boot 4 / Java 21 application demonstrating Kafka **at-least-once delivery semantics** for `LanguagePreference` events (`customerId` + `Locale`).

### Delivery guarantee layering

**Producer side** (`producer/`):
- `KafkaConfig` sets `acks=all`, idempotence enabled, unlimited retries — Kafka-level at-least-once.
- `LanguagePreferenceProducer` wraps `KafkaTemplate.send()` with Resilience4j `@Retry` + `@CircuitBreaker` (instance name `languagePreferenceProducer`). The circuit breaker's `fallbackMethod` logs and drops to a dead-letter store when open.
- `LanguagePreferenceController` exposes `POST /language-preferences` and returns 202 Accepted immediately (fire-and-forget to the producer).

**Consumer side** (`consumer/`):
- `KafkaConfig` disables auto-commit (`enable.auto.commit=false`) and sets `AckMode.MANUAL_IMMEDIATE` — offset is committed only after `process()` succeeds.
- `LanguagePreferenceConsumer.onMessage()` calls `process()`, then `ack.acknowledge()`. On exception it rethrows without acking; `DefaultErrorHandler` (fixed backoff from `application.yml`) retries, then routes to the dead-letter topic (DLT).
- `process()` carries its own `@Retry` + `@CircuitBreaker` (instance name `languagePreferenceConsumer`) for any downstream call added there.

**Resilience4j config** lives in `application.yml` under `resilience4j.circuitbreaker` and `resilience4j.retry`. Both producer and consumer have named instances.

**Observability**: Actuator exposes `health`, `info`, `prometheus`, `circuitbreakers`, and `retries`. Circuit breaker health is surfaced in `/actuator/health`.

### Security patch pattern

`gradle/libs.versions.toml` holds all transitive dependency pins as `patch-<cve-id>` entries under `[libraries]` and a `security-patches` bundle under `[bundles]`. `build.gradle` applies the bundle as `constraints { }` blocks. `settings.gradle` also pre-loads these onto the buildscript classpath. When adding a new CVE pin, follow this pattern and add it to both `[libraries]` and the `security-patches` bundle.

### Testing

Tests use `@EmbeddedKafka` (no external broker needed). `@DirtiesContext` resets the application context between test classes. Add new tests in the same package under `src/test/`.

CI runs against Java 17, 21, and 25 in parallel.
