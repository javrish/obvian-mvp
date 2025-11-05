# P3Net Production Monitoring & Error Recovery Runbook

**Version:** 1.0
**Last Updated:** 2024-01-29
**Owner:** P3Net Operations Team

## Overview

This runbook provides comprehensive operational procedures for monitoring and recovering the P3Net processing pipeline in production environments. It covers incident detection, diagnosis, recovery procedures, and preventive maintenance.

## System Architecture

### Core Components
- **P3Net Parser** - Natural language to PetriIntentSpec conversion
- **Petri Builder** - Intent specification to Petri net transformation
- **Petri Validator** - Formal validation using model checking
- **Petri Simulator** - Token-based execution simulation
- **DAG Projector** - Petri net to DAG conversion

### Monitoring Infrastructure
- **Metrics Collector** - Micrometer/Prometheus metrics
- **Health Check Service** - Deep component validation
- **Circuit Breaker Manager** - Resilience patterns
- **Alerting Service** - Error detection and notification
- **Recovery Service** - Automated self-healing

---

## Incident Response Matrix

### Severity Levels

| Level | Response Time | Escalation | Description |
|-------|---------------|------------|-------------|
| **P0 - Critical** | 5 minutes | Immediate | System down, critical functionality unavailable |
| **P1 - High** | 15 minutes | 30 minutes | Performance severely degraded, circuit breakers open |
| **P2 - Medium** | 1 hour | 4 hours | Elevated error rates, some degraded functionality |
| **P3 - Low** | 4 hours | 24 hours | Minor issues, monitoring alerts |

### Alert Types and Response

#### High Error Rate Alerts

**Alert:** `p3net_high_error_rate`
**Threshold:** >10% error rate for any operation
**Response Time:** 15 minutes

**Immediate Actions:**
1. Check operational dashboard: `GET /api/v1/monitoring/dashboard`
2. Identify affected operation from metrics
3. Check circuit breaker status: `GET /api/v1/monitoring/circuit-breakers`
4. Review recent logs for error patterns

**Investigation Steps:**
```bash
# Check current error rates
curl -X GET http://localhost:8080/api/v1/monitoring/metrics/summary

# Get detailed health status
curl -X GET http://localhost:8080/api/v1/monitoring/health

# Check active alerts
curl -X GET http://localhost:8080/api/v1/monitoring/alerts
```

**Recovery Actions:**
1. If error rate >20%, trigger graceful degradation:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=affected_operation&strategy=GRACEFUL_DEGRADATION"
   ```
2. If circuit breakers are open, attempt reset:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/circuit-breakers/{name}/reset
   ```
3. Monitor recovery progress via dashboard

#### Memory Leak Detection

**Alert:** `p3net_memory_leak_detected`
**Threshold:** >90% memory usage or increasing trend
**Response Time:** 10 minutes

**Immediate Actions:**
1. Check JVM memory metrics:
   ```bash
   curl -X GET http://localhost:8080/api/v1/monitoring/status | jq '.jvm'
   ```
2. Force garbage collection (if safe):
   ```bash
   curl -X POST http://localhost:8080/actuator/gc
   ```
3. Check for memory leak patterns in logs

**Recovery Actions:**
1. Trigger cache warming to free memory:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=memory&strategy=CACHE_WARM"
   ```
2. If memory usage remains high, consider component restart:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=memory&strategy=COMPONENT_RESTART"
   ```
3. If critical, trigger scale-out:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=memory&strategy=SCALE_OUT"
   ```

#### Circuit Breaker Open

**Alert:** `p3net_circuit_breaker_open`
**Threshold:** Circuit breaker transitions to OPEN state
**Response Time:** 5 minutes

**Immediate Actions:**
1. Identify which circuit breaker is open:
   ```bash
   curl -X GET http://localhost:8080/api/v1/monitoring/circuit-breakers
   ```
2. Check failure rate and recent failures:
   ```bash
   curl -X GET http://localhost:8080/api/v1/monitoring/circuit-breakers/{name}
   ```
3. Verify if upstream dependencies are healthy

**Recovery Actions:**
1. If upstream is healthy, attempt reset:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/circuit-breakers/{name}/reset
   ```
2. Monitor for successful transitions to CLOSED
3. If reset fails, trigger component recovery:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component={breaker_name}&strategy=CIRCUIT_BREAKER_RESET"
   ```

#### Performance Degradation

**Alert:** `p3net_performance_degradation`
**Threshold:** Response times >1s for critical operations
**Response Time:** 15 minutes

**Investigation Steps:**
```bash
# Check current performance metrics
curl -X GET http://localhost:8080/api/v1/monitoring/dashboard/real-time

# Review error rates by operation
curl -X GET http://localhost:8080/api/v1/monitoring/metrics/summary | jq '.performance'

# Check resource utilization
curl -X GET http://localhost:8080/api/v1/monitoring/status | jq '.jvm'
```

**Recovery Actions:**
1. Enable graceful degradation mode:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=p3net-pipeline&strategy=GRACEFUL_DEGRADATION"
   ```
2. If degradation persists, consider scaling:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=p3net-pipeline&strategy=SCALE_OUT"
   ```

#### Cascading Failure

**Alert:** `p3net_cascading_failure_detected`
**Threshold:** >3 circuit breakers open simultaneously
**Response Time:** 2 minutes

**Immediate Actions:**
1. **STOP** - Do not attempt individual component recovery
2. Check overall system health:
   ```bash
   curl -X GET http://localhost:8080/api/v1/monitoring/status
   ```
3. Activate incident response protocol
4. Notify on-call engineer immediately

**Recovery Actions:**
1. Trigger full system failover:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=p3net-pipeline&strategy=FAILOVER"
   ```
2. Scale out additional capacity:
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=p3net-pipeline&strategy=SCALE_OUT"
   ```
3. Monitor recovery across all components

---

## Monitoring Procedures

### Daily Health Checks

**Schedule:** Every day at 9:00 AM UTC
**Duration:** 15 minutes
**Owner:** Operations Team

```bash
#!/bin/bash
# Daily P3Net health check script

echo "=== P3Net Daily Health Check $(date) ==="

# 1. Overall system health
echo "1. System Health Status:"
curl -s http://localhost:8080/api/v1/monitoring/status | jq '{
  healthy,
  overallHealth,
  activeAlerts,
  criticalAlerts
}'

# 2. Component health details
echo "2. Component Health:"
curl -s http://localhost:8080/api/v1/monitoring/health | jq '.components | keys[]'

# 3. Circuit breaker status
echo "3. Circuit Breaker Status:"
curl -s http://localhost:8080/api/v1/monitoring/circuit-breakers | \
  jq 'to_entries[] | {name: .key, state: .value.state, failures: .value.failureCount}'

# 4. Performance metrics
echo "4. Performance Summary:"
curl -s http://localhost:8080/api/v1/monitoring/metrics/summary | jq '.performance'

# 5. Memory and resource usage
echo "5. Resource Usage:"
curl -s http://localhost:8080/api/v1/monitoring/status | jq '.jvm | {
  memoryUsedMB: (.memoryUsed / 1024 / 1024 | floor),
  memoryTotalMB: (.memoryTotal / 1024 / 1024 | floor),
  memoryUsagePercent: ((.memoryUsed / .memoryTotal * 100) | floor)
}'

# 6. Active recovery operations
echo "6. Recovery Operations:"
curl -s http://localhost:8080/api/v1/monitoring/recovery | jq '{
  activeRecoveries,
  deadLetterQueueSize,
  healthy
}'

# 7. Alert summary
echo "7. Alert Summary:"
curl -s http://localhost:8080/api/v1/monitoring/alerts/summary | jq '{
  totalActive,
  healthy,
  bySeverity
}'

echo "=== Health Check Complete ==="
```

### Weekly Performance Review

**Schedule:** Every Monday at 10:00 AM UTC
**Duration:** 30 minutes
**Owner:** Engineering Team

```bash
#!/bin/bash
# Weekly P3Net performance review script

echo "=== P3Net Weekly Performance Review $(date) ==="

# 1. Generate performance report
curl -X POST http://localhost:8080/api/v1/monitoring/metrics/log

# 2. Circuit breaker statistics
echo "Circuit Breaker Performance (Past Week):"
curl -s http://localhost:8080/api/v1/monitoring/circuit-breakers | \
  jq 'to_entries[] | {
    name: .key,
    state: .value.state,
    totalFailures: .value.failureCount,
    failureRate: (.value.failureRate * 100 | floor),
    lastFailure: .value.lastFailureTime
  }'

# 3. Recovery system performance
echo "Recovery System Statistics:"
curl -s http://localhost:8080/api/v1/monitoring/recovery/failures | \
  jq 'to_entries[] | {component: .key, failures: .value}'

# 4. Alert trend analysis
curl -X POST http://localhost:8080/api/v1/monitoring/alerts/log-summary

echo "=== Performance Review Complete ==="
```

---

## Recovery Procedures

### Automatic Recovery Strategies

The P3Net system implements several automatic recovery strategies:

#### 1. Retry with Exponential Backoff
- **Trigger:** Transient failures
- **Parameters:** 3 attempts, 1s initial delay, 2x backoff
- **Use Case:** Network timeouts, temporary resource unavailability

#### 2. Circuit Breaker Reset
- **Trigger:** Circuit breaker open for >5 minutes
- **Action:** Attempt graceful reset and health verification
- **Use Case:** Dependency recovery, network issues resolved

#### 3. Cache Warming
- **Trigger:** Memory usage >90% or leak detection
- **Action:** Force GC, preload common patterns
- **Use Case:** Memory pressure, performance optimization

#### 4. Graceful Degradation
- **Trigger:** Error rate >20% or performance degradation
- **Action:** Reduce functionality, simplified processing
- **Use Case:** High load, partial system failure

#### 5. Component Restart
- **Trigger:** Systematic failures (>10 failures per component)
- **Action:** Reinitialize component state
- **Use Case:** Memory leaks, corrupted state

#### 6. Scale Out
- **Trigger:** Sustained high load or performance issues
- **Action:** Add processing capacity
- **Use Case:** Traffic spikes, resource exhaustion

#### 7. Failover
- **Trigger:** Cascading failures or critical system failure
- **Action:** Route traffic to backup systems
- **Use Case:** Regional outages, major system failures

### Manual Recovery Procedures

#### Emergency System Restart

**When to Use:** System unresponsive, multiple critical failures
**Impact:** 2-5 minutes downtime
**Authorization:** Incident Commander approval required

```bash
# 1. Graceful shutdown
curl -X POST http://localhost:8080/actuator/shutdown

# 2. Wait for graceful shutdown (max 30 seconds)
sleep 30

# 3. Force kill if necessary
pkill -f "p3net"

# 4. Clear temporary state (if safe)
rm -rf /tmp/p3net-*

# 5. Restart service
systemctl restart p3net-service

# 6. Verify startup
timeout 60 bash -c 'until curl -f http://localhost:8080/api/v1/monitoring/health; do sleep 5; done'

# 7. Confirm all components healthy
curl -X GET http://localhost:8080/api/v1/monitoring/status
```

#### Database Recovery

**When to Use:** Database connectivity issues, data corruption
**Impact:** 5-15 minutes degraded performance

```bash
# 1. Check database connectivity
curl -X GET http://localhost:8080/api/v1/monitoring/circuit-breakers/database

# 2. If database circuit breaker is open, investigate
# Check database logs, connectivity, resource usage

# 3. Reset database circuit breaker once resolved
curl -X POST http://localhost:8080/api/v1/monitoring/circuit-breakers/database/reset

# 4. Trigger cache warm to rebuild state
curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
  -d "component=database&strategy=CACHE_WARM"

# 5. Monitor recovery progress
watch curl -s http://localhost:8080/api/v1/monitoring/circuit-breakers/database
```

#### Memory Leak Recovery

**When to Use:** Sustained high memory usage, OOM errors
**Impact:** 1-2 minutes performance degradation

```bash
# 1. Check current memory status
curl -X GET http://localhost:8080/api/v1/monitoring/status | jq '.jvm'

# 2. Force garbage collection
curl -X POST http://localhost:8080/actuator/gc

# 3. Wait and check again
sleep 30
curl -X GET http://localhost:8080/api/v1/monitoring/status | jq '.jvm'

# 4. If memory still high, trigger cache warming
curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
  -d "component=memory&strategy=CACHE_WARM"

# 5. If critical, prepare for restart
if memory_usage > 95%; then
  # Notify team of impending restart
  # Trigger graceful restart procedure
fi
```

---

## Escalation Procedures

### Level 1: Automated Recovery
- **Duration:** 0-15 minutes
- **Actions:** Automatic recovery strategies execute
- **Notification:** Dashboard alerts, log entries
- **Escalation Trigger:** Recovery fails or P1/P0 alert

### Level 2: Operations Team
- **Duration:** 15-60 minutes
- **Contact:** ops-team@company.com, Slack #p3net-ops
- **Actions:** Manual diagnosis and recovery procedures
- **Escalation Trigger:** Multiple recovery failures, P0 alert

### Level 3: Engineering Team
- **Duration:** 1-4 hours
- **Contact:** eng-team@company.com, Slack #p3net-eng
- **Actions:** Code-level diagnosis, hotfixes
- **Escalation Trigger:** System-level issues, design problems

### Level 4: Incident Commander
- **Duration:** Immediate for P0, 4+ hours for others
- **Contact:** incident-commander@company.com
- **Actions:** Cross-team coordination, external communication
- **Escalation Trigger:** Business impact, customer-facing issues

---

## Maintenance Windows

### Weekly Maintenance
**Schedule:** Sundays 2:00-4:00 AM UTC
**Duration:** 2 hours maximum

**Standard Procedures:**
1. **Pre-maintenance health check**
   ```bash
   curl -X GET http://localhost:8080/api/v1/monitoring/dashboard > pre_maintenance_status.json
   ```

2. **Enable maintenance mode** (if available)
   ```bash
   curl -X POST http://localhost:8080/api/v1/admin/maintenance-mode?enabled=true
   ```

3. **System updates and patches**
4. **Configuration updates**
5. **Cache warming**
   ```bash
   curl -X POST http://localhost:8080/api/v1/monitoring/recovery/trigger \
     -d "component=cache&strategy=CACHE_WARM"
   ```

6. **Post-maintenance verification**
   ```bash
   curl -X GET http://localhost:8080/api/v1/monitoring/health
   curl -X GET http://localhost:8080/api/v1/monitoring/dashboard
   ```

7. **Disable maintenance mode**
   ```bash
   curl -X POST http://localhost:8080/api/v1/admin/maintenance-mode?enabled=false
   ```

### Emergency Maintenance

**Authorization:** Incident Commander + Engineering Lead
**Notification:** 30 minutes advance notice minimum

**Procedures:**
1. **Assess impact and risk**
2. **Notify stakeholders**
3. **Execute minimal necessary changes**
4. **Comprehensive post-change testing**

---

## Metrics and SLI/SLO Targets

### Service Level Indicators (SLIs)

| SLI | Target | Measurement |
|-----|--------|-------------|
| **Availability** | 99.9% | System responding to health checks |
| **Parse Success Rate** | 99.5% | Successful natural language parsing |
| **Validation Success Rate** | 99.0% | Successful Petri net validation |
| **Parse Latency** | <500ms (95th percentile) | Time to parse to intent spec |
| **Validation Latency** | <2000ms (95th percentile) | Time to complete validation |
| **Simulation Latency** | <5000ms (95th percentile) | Time to complete simulation |
| **Error Recovery Time** | <30s (automated) | Time to recover from transient errors |

### Service Level Objectives (SLOs)

| SLO | Target | Action Threshold |
|-----|--------|------------------|
| **Monthly Availability** | 99.9% | <99.5% = Incident Review |
| **Error Rate** | <0.5% | >1% = Alert + Investigation |
| **P95 Response Time** | <1000ms | >2000ms = Performance Review |
| **Circuit Breaker Uptime** | 95% closed | >5% open = Architecture Review |
| **Memory Usage** | <80% average | >90% = Scale/Optimize |

---

## Troubleshooting Guide

### Common Issues

#### Issue: High Parse Error Rate
**Symptoms:**
- Parse error rate >5%
- Increased "PARSE_ERROR" alerts
- User reports of failed workflow creation

**Diagnosis:**
```bash
# Check recent parse errors
curl -X GET http://localhost:8080/api/v1/monitoring/metrics/summary | jq '.performance.parse'

# Review error logs
tail -f /var/log/p3net/parser.log | grep ERROR

# Test with known good input
curl -X POST http://localhost:8080/api/v1/petri/parse \
  -H "Content-Type: application/json" \
  -d '{"text": "create a simple workflow with two steps"}'
```

**Resolution:**
1. Check for pattern recognition issues
2. Verify PromptParser initialization
3. Reset parser circuit breaker if needed
4. Consider graceful degradation

#### Issue: Memory Leak
**Symptoms:**
- Steadily increasing memory usage
- Frequent GC activity
- OutOfMemoryError in logs

**Diagnosis:**
```bash
# Monitor memory trend
watch curl -s http://localhost:8080/api/v1/monitoring/status | jq '.jvm'

# Check for memory leak alerts
curl -X GET http://localhost:8080/api/v1/monitoring/alerts | jq '.[] | select(.type == "MEMORY_LEAK_DETECTED")'

# Generate heap dump (if tools available)
jcmd <pid> GC.run_finalization
jcmd <pid> VM.gc
```

**Resolution:**
1. Force garbage collection
2. Trigger cache warming
3. Restart component if critical
4. Scale out additional capacity

#### Issue: Circuit Breaker Stuck Open
**Symptoms:**
- Circuit breaker remains OPEN
- Dependent operations fail fast
- Reset attempts fail

**Diagnosis:**
```bash
# Check circuit breaker status and history
curl -X GET http://localhost:8080/api/v1/monitoring/circuit-breakers/{name}

# Verify upstream dependency health
# Test connectivity to external services
```

**Resolution:**
1. Verify upstream service recovery
2. Attempt manual reset
3. Force circuit breaker to HALF_OPEN for testing
4. Consider failover if dependency unavailable

### Log Analysis

#### Key Log Locations
- **Application Logs:** `/var/log/p3net/application.log`
- **Parser Logs:** `/var/log/p3net/parser.log`
- **Validator Logs:** `/var/log/p3net/validator.log`
- **Simulator Logs:** `/var/log/p3net/simulator.log`
- **Monitoring Logs:** `/var/log/p3net/monitoring.log`

#### Important Log Patterns
```bash
# Error rate spike detection
grep -E "ERROR|EXCEPTION" /var/log/p3net/*.log | wc -l

# Circuit breaker state changes
grep "Circuit breaker.*transitioning" /var/log/p3net/monitoring.log

# Memory warnings
grep -i "memory\|heap\|gc" /var/log/p3net/application.log

# Recovery operations
grep "Recovery.*initiated\|Recovery.*completed" /var/log/p3net/monitoring.log

# Alert notifications
grep "Alert.*created\|Alert.*resolved" /var/log/p3net/monitoring.log
```

---

## Contact Information

### On-Call Rotation
- **Primary:** ops-primary@company.com
- **Secondary:** ops-secondary@company.com
- **Escalation:** incident-commander@company.com

### Team Contacts
- **Operations Team:** ops-team@company.com, Slack #p3net-ops
- **Engineering Team:** eng-team@company.com, Slack #p3net-eng
- **Product Team:** product-team@company.com, Slack #p3net-product

### External Vendors
- **Infrastructure Provider:** support@cloudprovider.com
- **Monitoring Vendor:** support@monitoringvendor.com
- **Database Support:** support@databasevendor.com

---

## Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-01-29 | Operations Team | Initial comprehensive runbook |

---

*This runbook is a living document. Please update it as procedures evolve and new issues are discovered.*