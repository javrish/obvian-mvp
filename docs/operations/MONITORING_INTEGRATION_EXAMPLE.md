# P3Net Monitoring Integration Example

This document demonstrates how the P3Net monitoring system integrates with the processing pipeline to provide comprehensive observability and error recovery.

## Complete Implementation

Here's how to integrate monitoring into a typical P3Net controller endpoint:

```java
@PostMapping("/parse")
public ResponseEntity<?> parseNaturalLanguage(@Valid @RequestBody PetriParseRequest request) {
    logger.info("Parsing natural language: {}",
        request.getText().substring(0, Math.min(request.getText().length(), 100)));

    // Start monitoring metrics
    Timer.Sample sample = metricsCollector.startParseTimer();
    String operationId = "parse_" + System.currentTimeMillis();

    try {
        // Execute with circuit breaker protection and fallback
        return circuitBreakerManager.executeWithCircuitBreaker(
            CircuitBreakerManager.PARSER_CB,

            // Primary operation
            () -> {
                // Parse natural language using real PromptParser
                PromptParser.CompoundParseResult parseResult =
                    promptParser.parseCompoundPrompt(request.getText());
                List<PromptParser.ParsedIntent> intents = parseResult.getIntents();

                // Convert to PetriIntentSpec
                PetriIntentSpec intentSpec = buildIntentSpec(parseResult, request);

                // Convert to response format
                Map<String, Object> response = convertToResponse(intentSpec, parseResult);

                // Record successful workflow processing
                metricsCollector.recordWorkflowProcessed("parse");
                metricsCollector.recordParseTime(sample, true);

                logger.info("Successfully parsed input into {} intents", intents.size());
                return ResponseEntity.ok(response);
            },

            // Fallback operation (graceful degradation)
            () -> {
                logger.warn("Parser circuit breaker open, using fallback parsing");

                // Simple fallback parsing logic
                Map<String, Object> fallbackResponse = createFallbackResponse(request.getText());

                metricsCollector.recordParseTime(sample, false);

                return ResponseEntity.ok(fallbackResponse);
            }
        );

    } catch (CircuitBreakerManager.CircuitBreakerException e) {
        // Circuit breaker is open, report to recovery service
        recoveryService.reportFailedOperation(operationId, "parser", e,
            Map.of("input_length", request.getText().length()));

        metricsCollector.recordParseTime(sample, false);

        logger.error("Parser circuit breaker failed", e);
        return ResponseEntity.status(503).body(createErrorResponse("CIRCUIT_BREAKER_OPEN",
            "Parser service temporarily unavailable, please try again later"));

    } catch (Exception e) {
        // Report failure for potential recovery
        recoveryService.reportFailedOperation(operationId, "parser", e,
            Map.of("input_text", request.getText().substring(0, Math.min(100, request.getText().length()))));

        metricsCollector.recordParseTime(sample, false);

        logger.error("Error during parsing", e);
        return ResponseEntity.badRequest().body(createErrorResponse("PARSE_ERROR",
            "Failed to parse natural language: " + e.getMessage()));
    }
}
```

## Monitoring Flow Visualization

```
┌─────────────────┐
│   API Request   │
└─────────┬───────┘
          │
    ┌─────▼─────┐
    │  Metrics  │ ◄── Start timer, increment counters
    │ Collection│
    └─────┬─────┘
          │
   ┌──────▼──────┐
   │Circuit      │ ◄── Check if service is healthy
   │Breaker      │
   │Check        │
   └──────┬──────┘
          │
    ┌─────▼─────┐     Success     ┌─────────────┐
    │  Execute  ├─────────────────►│  Record     │
    │ Primary   │                 │  Success    │
    │ Operation │                 │  Metrics    │
    └─────┬─────┘                 └─────────────┘
          │
       Failure
          │
    ┌─────▼─────┐
    │  Circuit  │ ◄── Update failure count
    │  Breaker  │
    │  Update   │
    └─────┬─────┘
          │
    ┌─────▼─────┐
    │  Execute  │ ◄── Graceful degradation
    │ Fallback  │
    │ Operation │
    └─────┬─────┘
          │
    ┌─────▼─────┐
    │  Recovery │ ◄── Report failure for analysis
    │  Service  │
    │  Report   │
    └───────────┘
```

## Key Monitoring Integration Points

### 1. Metrics Collection

```java
// Timer metrics for response time tracking
Timer.Sample sample = metricsCollector.startParseTimer();
// ... operation execution ...
metricsCollector.recordParseTime(sample, success);

// Business metrics
metricsCollector.recordWorkflowProcessed("parse");
metricsCollector.recordWorkflowProcessed(workflowType);

// Custom metrics for specific events
metricsCollector.recordCustomCounter("complex_parsing", 1,
    "complexity", "high", "token_count", String.valueOf(tokenCount));
```

### 2. Circuit Breaker Integration

```java
// Execute with automatic circuit breaker protection
return circuitBreakerManager.executeWithCircuitBreaker(
    CircuitBreakerManager.PARSER_CB,
    primaryOperation,
    fallbackOperation
);

// Check circuit breaker state
CircuitBreakerManager.CircuitBreakerStatus status =
    circuitBreakerManager.getStatus(CircuitBreakerManager.PARSER_CB);

if (status.getState() == CircuitBreakerManager.State.OPEN) {
    // Handle open circuit breaker scenario
}
```

### 3. Error Recovery Reporting

```java
// Report failures for automated recovery
try {
    // ... operation ...
} catch (Exception e) {
    recoveryService.reportFailedOperation(
        operationId,
        componentName,
        e,
        contextMap // Additional context for recovery
    );

    // Recovery service will:
    // 1. Add to failure counters
    // 2. Queue for retry if appropriate
    // 3. Trigger recovery strategies if thresholds exceeded
}
```

### 4. Health Check Integration

The health check service automatically monitors component health:

```java
// Automatic health checks run every 30 seconds
@Scheduled(fixedRate = 30000)
public void performHealthChecks() {
    checkPromptParserHealth();
    checkPetriNetValidatorHealth();
    // ... other components
}

// Manual health check trigger
@GetMapping("/health/check/{component}")
public ResponseEntity<?> checkComponentHealth(@PathVariable String component) {
    // Trigger specific component health check
    // Return detailed health status
}
```

## Monitoring Data Flow

### Real-time Metrics Pipeline

1. **Request Processing**:
   - Timer starts when request arrives
   - Circuit breaker checks component health
   - Primary operation executes with monitoring

2. **Success Path**:
   - Timer records successful response time
   - Business metrics increment (workflows processed)
   - Success counters update
   - Circuit breaker resets failure count

3. **Failure Path**:
   - Timer records failure response time
   - Error counters increment
   - Circuit breaker increments failure count
   - Recovery service receives failure report
   - Fallback operation executes if available

4. **Circuit Breaker Actions**:
   - If failure threshold exceeded → Open circuit
   - If circuit open → Route to fallback
   - If timeout elapsed → Test with half-open state

5. **Recovery Actions**:
   - Failure patterns analyzed
   - Automatic retry with exponential backoff
   - Component restart if systematic failures
   - Alerting if recovery attempts fail

## Dashboard Integration

The operational dashboard provides real-time visibility:

### Key Metrics Displayed

- **System Health**: Overall status and component health
- **Performance**: Response times, throughput, error rates
- **Circuit Breakers**: State, failure counts, recent state changes
- **Alerts**: Active alerts by severity, recent alert history
- **Recovery**: Active recovery operations, success/failure rates

### API Endpoints for Dashboard

```javascript
// Real-time dashboard data
GET /api/v1/monitoring/dashboard/real-time
{
  "healthy": true,
  "activeAlerts": 0,
  "criticalAlerts": 0,
  "activeRecoveries": 1,
  "errorRates": {
    "parse": 0.003,
    "build": 0.001,
    "validate": 0.005,
    "simulate": 0.002
  },
  "circuitBreakerStates": {
    "p3net-parser": "CLOSED",
    "p3net-validator": "CLOSED",
    "external-api": "OPEN"
  },
  "memoryUsageRatio": 0.72,
  "timestamp": 1640995200000
}

// Comprehensive dashboard data
GET /api/v1/monitoring/dashboard
{
  "overview": { "healthy": true, "overallHealth": "HEALTHY" },
  "componentHealth": { /* detailed component status */ },
  "circuitBreakers": { /* circuit breaker details */ },
  "alerts": { "totalActive": 0, "critical": 0, "healthy": true },
  "recovery": { "activeRecoveries": 0, "healthy": true },
  "performance": { /* error rates by operation */ },
  "jvm": { /* memory and resource usage */ }
}
```

## Alerting Integration

### Automatic Alert Generation

```java
// High error rate detection
if (errorRate > HIGH_ERROR_RATE_THRESHOLD) {
    alertingService.createAlert(
        AlertType.HIGH_ERROR_RATE,
        AlertSeverity.CRITICAL,
        componentName,
        String.format("High error rate detected: %.2f%%", errorRate * 100),
        Map.of("error_rate", errorRate, "threshold", HIGH_ERROR_RATE_THRESHOLD)
    );
}

// Circuit breaker state changes
if (circuitBreakerState == State.OPEN) {
    alertingService.createAlert(
        AlertType.CIRCUIT_BREAKER_OPEN,
        AlertSeverity.ERROR,
        circuitBreakerName,
        "Circuit breaker opened due to failures",
        Map.of("failure_count", failureCount, "failure_rate", failureRate)
    );
}
```

### Alert Resolution

```java
// Automatic resolution when conditions improve
@Scheduled(fixedRate = 30000)
public void autoResolveAlerts() {
    activeAlerts.values().stream()
        .filter(alert -> !alert.isResolved())
        .forEach(alert -> {
            if (shouldAutoResolve(alert)) {
                resolveAlert(alert.getId());
            }
        });
}

// Manual resolution via API
POST /api/v1/monitoring/alerts/{alertId}/resolve
```

## Recovery Strategy Integration

### Automatic Recovery Triggers

1. **Circuit Breaker Open** → Reset after timeout
2. **High Error Rate** → Graceful degradation mode
3. **Memory Pressure** → Cache warming and GC
4. **Systematic Failures** → Component restart
5. **Cascading Failures** → Full system failover

### Recovery Escalation

```java
// Recovery strategy escalation chain
RETRY_WITH_BACKOFF → GRACEFUL_DEGRADATION → CIRCUIT_BREAKER_RESET →
COMPONENT_RESTART → SCALE_OUT → FAILOVER
```

Each strategy is attempted with monitoring and escalation to the next level if recovery fails.

## Production Deployment

### Configuration

```yaml
# application-production.yml
p3net:
  monitoring:
    enabled: true
    metrics:
      prefix: p3net
    alerting:
      enabled: true
      webhook:
        url: https://alerts.company.com/webhook
    recovery:
      enabled: true
      max-retry-attempts: 3
      retry-backoff-multiplier: 2.0
    circuit-breakers:
      parse:
        failure-threshold: 5
        timeout: 30s
        retry-timeout: 60s
      validate:
        failure-threshold: 10
        timeout: 60s
        retry-timeout: 120s
```

### Health Check Endpoints

```bash
# Overall system health
curl http://localhost:8080/api/v1/monitoring/health

# Component-specific health
curl http://localhost:8080/api/v1/monitoring/status

# Actuator health (Spring Boot integration)
curl http://localhost:8080/actuator/health
```

### Metrics Export

```bash
# Prometheus metrics endpoint
curl http://localhost:8080/actuator/prometheus

# Custom P3Net metrics summary
curl http://localhost:8080/api/v1/monitoring/metrics/summary
```

This comprehensive monitoring integration ensures that the P3Net processing pipeline operates with full observability, automatic error recovery, and production-grade reliability.