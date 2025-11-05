# Petri Net Workflow API

This document describes the REST API endpoints for processing Petri net workflows from natural language descriptions.

## Overview

The Petri Net API provides comprehensive workflow processing capabilities:

- **Parse**: Convert natural language into structured intent specifications
- **Build**: Transform intent specifications into formal Petri nets
- **Validate**: Perform formal verification with deadlock detection and reachability analysis
- **Simulate**: Execute Petri nets with deterministic or interactive token animation
- **DAG Project**: Generate DAG representations for simplified visualization

All endpoints accept and return JSON with `schemaVersion: "1.0"`.

## Authentication

All endpoints (except `/health`) require authentication with one of:
- JWT token with `USER` or `ADMIN` role
- API key with appropriate authorities (`petri:parse`, `petri:build`, etc.)

## Base URL

```
/api/v1/petri
```

## Endpoints

### POST /parse

Parse natural language workflow descriptions into structured intent specifications.

**Request:**
```json
{
  "schemaVersion": "1.0",
  "text": "Every time I push code: run tests; if pass deploy to staging; if fail alert Slack",
  "templateHint": "devops"
}
```

**Response (200 OK):**
```json
{
  "schemaVersion": "1.0",
  "intent": {
    "modelType": "PETRI",
    "name": "DeployOnGreen",
    "description": "CI/CD workflow with conditional deployment",
    "steps": [
      {
        "id": "run_tests",
        "type": "ACTION",
        "description": "Execute test suite"
      },
      {
        "id": "branch_on_result",
        "type": "CHOICE",
        "paths": ["pass", "fail"]
      },
      {
        "id": "deploy_staging",
        "type": "ACTION",
        "when": "pass",
        "description": "Deploy to staging environment"
      },
      {
        "id": "notify_slack",
        "type": "ACTION",
        "when": "fail",
        "description": "Send failure notification"
      }
    ],
    "schemaVersion": "1.0"
  },
  "templateUsed": "devops-ci-cd",
  "confidence": 0.95
}
```

**Supported Patterns:**
- **DevOps**: `"run tests; if pass deploy; if fail alert"`
- **Football**: `"warm-up, then pass and shoot in parallel, then cooldown"`

### POST /build

Build formal Petri nets from intent specifications.

**Request:**
```json
{
  "schemaVersion": "1.0",
  "intent": { /* PetriIntentSpec from parse response */ },
  "generateDag": false
}
```

**Response (200 OK):**
```json
{
  "schemaVersion": "1.0",
  "petriNet": {
    "id": "petri_abc123def456",
    "name": "Deploy On Green Workflow",
    "places": [
      {"id": "p_code", "name": "Code Ready", "capacity": 1},
      {"id": "p_testing", "name": "Testing In Progress", "capacity": 1},
      {"id": "p_pass", "name": "Tests Passed", "capacity": 1},
      {"id": "p_fail", "name": "Tests Failed", "capacity": 1},
      {"id": "p_done", "name": "Workflow Complete", "capacity": 1}
    ],
    "transitions": [
      {"id": "t_run_tests", "name": "Run Tests"},
      {"id": "t_pass", "name": "Tests Pass"},
      {"id": "t_fail", "name": "Tests Fail"},
      {"id": "t_deploy", "name": "Deploy to Staging"},
      {"id": "t_notify", "name": "Notify Slack"}
    ],
    "arcs": [
      {"from": "p_code", "to": "t_run_tests", "weight": 1},
      {"from": "t_run_tests", "to": "p_testing", "weight": 1},
      {"from": "p_testing", "to": "t_pass", "weight": 1},
      {"from": "p_testing", "to": "t_fail", "weight": 1}
    ],
    "initialMarking": {"p_code": 1},
    "schemaVersion": "1.0"
  }
}
```

### POST /validate

Validate Petri nets using formal methods.

**Request:**
```json
{
  "schemaVersion": "1.0",
  "petriNet": { /* PetriNet from build response */ },
  "config": {
    "kBound": 200,
    "maxMillis": 30000,
    "enableDeadlockCheck": true,
    "enableReachabilityCheck": true,
    "enableLivenessCheck": true,
    "enableBoundednessCheck": true
  }
}
```

**Response (200 OK - Validation Passed):**
```json
{
  "schemaVersion": "1.0",
  "report": {
    "status": "PASS",
    "summaryMessage": "All validation checks passed",
    "checks": {
      "deadlock": "PASS",
      "reachability": "PASS",
      "liveness": "PASS",
      "boundedness": "PASS"
    },
    "statesExplored": 25,
    "executionTimeMs": 150,
    "hints": ["Workflow is safe for execution"]
  }
}
```

**Response (422 Unprocessable Entity - Validation Inconclusive):**
```json
{
  "schemaVersion": "1.0",
  "error": {
    "code": "VALIDATION_INCONCLUSIVE",
    "message": "Bound 200 reached during reachability analysis",
    "details": {
      "statesExplored": 200,
      "suggestion": "Increase kBound or simplify workflow"
    }
  }
}
```

### POST /simulate

Simulate Petri net execution with trace logging.

**Request:**
```json
{
  "schemaVersion": "1.0",
  "petriNet": { /* PetriNet from build response */ },
  "config": {
    "seed": 42,
    "mode": "DETERMINISTIC",
    "maxSteps": 1000,
    "stepDelayMs": 100,
    "enableTrace": true
  }
}
```

**Response (200 OK):**
```json
{
  "schemaVersion": "1.0",
  "trace": [
    {
      "timestamp": "2025-09-21T10:00:00.123Z",
      "sequenceNumber": 1,
      "transition": "t_run_tests",
      "fromPlaces": ["p_code"],
      "toPlaces": ["p_testing"],
      "markingBefore": {"p_code": 1},
      "markingAfter": {"p_testing": 1}
    }
  ],
  "finalMarking": {"p_done": 1},
  "status": "COMPLETED",
  "stepsExecuted": 5,
  "executionTimeMs": 250
}
```

**Simulation Modes:**
- `DETERMINISTIC`: Reproducible execution with seeded random selection
- `INTERACTIVE`: Manual transition selection (for UI integration)

### POST /dag

Generate DAG representation from Petri net.

**Request:**
```json
{
  "schemaVersion": "1.0",
  "petriNet": { /* PetriNet from build response */ },
  "includeMetadata": true
}
```

**Response (200 OK):**
```json
{
  "schemaVersion": "1.0",
  "dag": {
    "nodes": [
      {"id": "t_run_tests", "name": "Run Tests", "type": "TRANSITION"},
      {"id": "t_deploy", "name": "Deploy", "type": "TRANSITION"}
    ],
    "edges": [
      {"from": "t_run_tests", "to": "t_deploy", "condition": "pass"}
    ]
  },
  "derivedFromPetriNetId": "petri_abc123def456",
  "projectionNotes": [
    "Petri Net is source of truth; DAG simplifies ordering",
    "Transitions mapped to DAG nodes; places inform edge dependencies"
  ]
}
```

### GET /health

Health check for Petri net service.

**Response (200 OK):**
```json
{
  "status": "healthy",
  "service": "PetriNetService",
  "schemaVersion": "1.0",
  "timestamp": 1632844800000,
  "components": {
    "templateRegistry": "healthy",
    "petriNetBuilder": "healthy",
    "petriNetValidator": "healthy",
    "tokenSimulator": "healthy"
  }
}
```

## Error Handling

All endpoints return structured error responses with appropriate HTTP status codes:

### HTTP Status Codes

- **200 OK**: Successful operation
- **400 Bad Request**: Invalid input (schema violations, malformed JSON)
- **401 Unauthorized**: Authentication required
- **403 Forbidden**: Insufficient permissions
- **409 Conflict**: Construction conflicts (unmatched joins, structural issues)
- **422 Unprocessable Entity**: Validation inconclusive (bound reached)
- **500 Internal Server Error**: Unexpected engine errors

### Error Response Format

```json
{
  "schemaVersion": "1.0",
  "error": {
    "code": "VALIDATION_INCONCLUSIVE",
    "message": "Human-readable error message",
    "details": {
      "additionalInfo": "value",
      "suggestion": "How to resolve the issue"
    }
  }
}
```

### Common Error Codes

- `PARSE_ERROR`: No matching template found for input pattern
- `INVALID_INPUT`: Schema violations or missing required fields
- `BUILD_ERROR`: Failed to construct Petri net from intent
- `CONSTRUCTION_CONFLICT`: Structural issues like unmatched synchronization points
- `VALIDATION_ERROR`: Formal validation failed (deadlocks, unreachable states)
- `VALIDATION_INCONCLUSIVE`: State space bound reached during analysis
- `SIMULATION_ERROR`: Execution simulation failed
- `DAG_PROJECTION_ERROR`: Failed to project Petri net to DAG
- `ENGINE_ERROR`: Unexpected internal errors

## Complete Workflow Example

Here's a complete end-to-end example using curl:

```bash
# 1. Parse natural language
curl -X POST "/api/v1/petri/parse" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "schemaVersion": "1.0",
    "text": "run tests; if pass deploy; if fail alert"
  }'

# 2. Build Petri net (using intent from step 1)
curl -X POST "/api/v1/petri/build" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "schemaVersion": "1.0",
    "intent": { /* intent from parse response */ }
  }'

# 3. Validate Petri net (using petriNet from step 2)
curl -X POST "/api/v1/petri/validate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "schemaVersion": "1.0",
    "petriNet": { /* petriNet from build response */ }
  }'

# 4. Simulate execution (using petriNet from step 2)
curl -X POST "/api/v1/petri/simulate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "schemaVersion": "1.0",
    "petriNet": { /* petriNet from build response */ },
    "config": {"seed": 42, "mode": "DETERMINISTIC"}
  }'

# 5. Generate DAG view (using petriNet from step 2)
curl -X POST "/api/v1/petri/dag" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "schemaVersion": "1.0",
    "petriNet": { /* petriNet from build response */ }
  }'
```

## Performance Considerations

- **State Space Exploration**: Validation bounded to k=200 states by default (configurable)
- **Network Size**: Optimized for ≤30 places and ≤30 transitions
- **Response Time**: Target <2s for parse→validate→render flow
- **Simulation**: Step latency <100ms for real-time animation

## OpenAPI Specification

Complete OpenAPI 3.0 specification available at `/swagger-ui.html` when the service is running.

## Integration Notes

- All components integrate with existing Obvian infrastructure
- Memory store integration for execution context
- Audit logging for all operations
- Security integration with JWT and role-based access control
- Compatible with existing DAG execution infrastructure