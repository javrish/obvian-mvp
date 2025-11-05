# API Surface Map

**Version:** 0.5.0
**Base URL:** `http://localhost:8080`
**Schema Version:** 1.0
**Documentation Generated:** 2025-11-02

---

## Table of Contents

1. [Authentication](#authentication)
2. [Core System Endpoints](#core-system-endpoints)
3. [Petri Net Workflow Pipeline](#petri-net-workflow-pipeline)
4. [Request/Response Schemas](#requestresponse-schemas)
5. [Error Handling](#error-handling)
6. [Code Examples](#code-examples)

---

## Authentication

### Current Status: **Permissive (Development Mode)**

All Petri endpoints (`/api/v1/petri/**`) are currently **permitAll** for development/testing.

### Supported Authentication Methods (When Enabled)

| Method | Header Format | Use Case |
|--------|--------------|----------|
| JWT Token | `Authorization: Bearer <token>` | User authentication |
| API Key | `X-API-Key: <key>` | Service-to-service |
| OAuth2 (Google) | OAuth flow | SSO integration |

### OAuth2 Configuration

- **Provider:** Google Workspace
- **Scopes:** `openid`, `profile`, `email`
- **Redirect URI:** `http://localhost:8080/login/oauth2/code/google`
- **Token Expiration:** 24 hours (86400000ms)

**JWT Claims:**
```json
{
  "iss": "obvian-api",
  "aud": "obvian-clients",
  "sub": "user-id",
  "exp": 1730563200
}
```

---

## Core System Endpoints

### Health & Status

| Method | Route | Description | Auth Required | Response Time |
|--------|-------|-------------|---------------|---------------|
| GET | `/api/v1/health` | System health check | No | <50ms |
| GET | `/api/v1/` | API version info | No | <50ms |
| GET | `/api/v1/petri/health` | Petri service component health | No | <100ms |

#### GET /api/v1/health

**Response (200 OK):**
```json
{
  "status": "UP",
  "service": "Obvian Backend",
  "timestamp": 1730563200000
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/health
```

---

#### GET /api/v1/

**Response (200 OK):**
```json
{
  "message": "Obvian Backend API",
  "version": "0.5.0"
}
```

---

#### GET /api/v1/petri/health

**Response (200 OK):**
```json
{
  "status": "healthy",
  "service": "PetriNetService",
  "timestamp": 1730563200000,
  "schemaVersion": "1.0",
  "components": {
    "promptParser": "healthy",
    "automationGrammar": "healthy",
    "petriNetValidator": "healthy",
    "petriTokenSimulator": "healthy",
    "petriToDagProjector": "healthy"
  }
}
```

**Degraded Status Example:**
```json
{
  "status": "degraded",
  "components": {
    "promptParser": "healthy",
    "petriNetValidator": "unhealthy: Connection timeout"
  }
}
```

---

## Petri Net Workflow Pipeline

The Petri Net API provides a **5-stage pipeline** for converting natural language into executable, formally verified workflows:

```
Natural Language → Intent Spec → Petri Net → Validation → DAG Projection → Simulation
     (PARSE)         (BUILD)      (VALIDATE)     (DAG)       (SIMULATE)
```

### Pipeline Endpoints

| Method | Route | Stage | Input | Output | Avg Latency |
|--------|-------|-------|-------|--------|-------------|
| POST | `/api/v1/petri/parse` | 1. Parse | Natural language | Intent Specification | 200-500ms |
| POST | `/api/v1/petri/build` | 2. Build | Intent Spec | Petri Net | 100-300ms |
| POST | `/api/v1/petri/validate` | 3. Validate | Petri Net | Validation Report | 150-2000ms |
| POST | `/api/v1/petri/dag` | 4. Project | Petri Net | DAG Representation | 50-200ms |
| POST | `/api/v1/petri/simulate` | 5. Simulate | Petri Net + Config | Execution Trace | 100-500ms |
| POST | `/api/v1/petri/execute` | End-to-End | Intent Spec | Execution Result | **501 Not Implemented** |

---

### 1. Parse Natural Language

**POST /api/v1/petri/parse**

Convert natural language workflow descriptions into structured intent specifications using pattern matching and LLM-based parsing.

**Request Schema:**
```json
{
  "text": "string (required, max 10000 chars)",
  "templateHint": "string (optional)",
  "metadata": "object (optional)"
}
```

**Request Example:**
```json
{
  "text": "run tests; if pass deploy to staging; if fail alert slack",
  "templateHint": "devops"
}
```

**Response (200 OK):**
```json
{
  "schemaVersion": "1.0",
  "intent": {
    "modelType": "PetriIntentSpec",
    "name": "ParsedWorkflow",
    "description": "Workflow parsed from: run tests; if pass deploy...",
    "schemaVersion": "1.0",
    "originalPrompt": "run tests; if pass deploy to staging; if fail alert slack",
    "templateId": "devops",
    "metadata": {},
    "steps": [
      {
        "id": "step1",
        "type": "ACTION",
        "description": "Execute send_email",
        "dependencies": [],
        "conditions": {},
        "metadata": {
          "recipient": "user@example.com"
        }
      }
    ]
  },
  "templateUsed": "devops",
  "confidence": 0.85,
  "success": true
}
```

**Error Response (400 Bad Request):**
```json
{
  "schemaVersion": "1.0",
  "error": {
    "code": "PARSE_ERROR",
    "message": "Failed to parse natural language: No matching template found"
  }
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/petri/parse \
  -H "Content-Type: application/json" \
  -d '{
    "text": "warm up, then pass and shoot in parallel, then cool down",
    "templateHint": "football"
  }'
```

**Supported Template Hints:**
- `devops` - CI/CD workflows with conditionals
- `football` - Sequential + parallel task patterns
- `generic-workflow` - Default fallback

---

### 2. Build Petri Net

**POST /api/v1/petri/build**

Transform intent specifications into formal Petri net structures with places, transitions, and arcs.

**Request Schema:**
```json
{
  "intent": {
    "name": "string",
    "description": "string",
    "steps": "array<IntentStep>",
    "originalPrompt": "string",
    "templateId": "string"
  }
}
```

**Request Example:**
```json
{
  "intent": {
    "name": "CI/CD Workflow",
    "description": "Test and deploy pipeline",
    "steps": [
      {
        "id": "run_tests",
        "type": "ACTION",
        "description": "Execute test suite"
      },
      {
        "id": "deploy",
        "type": "ACTION",
        "description": "Deploy to staging",
        "dependencies": ["run_tests"]
      }
    ]
  }
}
```

**Response (200 OK):**
```json
{
  "schemaVersion": "1.0",
  "success": true,
  "petriNet": {
    "name": "CI/CD Workflow",
    "description": "Test and deploy pipeline",
    "places": [
      {
        "id": "p_start",
        "name": "Start",
        "capacity": 1
      },
      {
        "id": "p_run_tests_ready",
        "name": "run_tests Ready",
        "capacity": 1
      },
      {
        "id": "p_run_tests_done",
        "name": "run_tests Done",
        "capacity": 1
      }
    ],
    "transitions": [
      {
        "id": "t_run_tests",
        "name": "run_tests",
        "description": "Execute test suite",
        "metadata": {
          "stepType": "ACTION"
        }
      },
      {
        "id": "t_deploy",
        "name": "deploy",
        "description": "Deploy to staging",
        "metadata": {
          "stepType": "ACTION"
        }
      }
    ],
    "arcs": [
      {
        "from": "p_start",
        "to": "t_run_tests",
        "weight": 1
      },
      {
        "from": "t_run_tests",
        "to": "p_run_tests_done",
        "weight": 1
      }
    ],
    "initialMarking": {
      "p_start": 1
    },
    "workflowSummary": [
      {
        "name": "run_tests",
        "description": "Execute test suite",
        "stepType": "ACTION"
      }
    ]
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "schemaVersion": "1.0",
  "error": {
    "code": "BUILD_ERROR",
    "message": "Failed to build Petri net: Intent must contain at least one step"
  }
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/petri/build \
  -H "Content-Type: application/json" \
  -d @intent.json
```

---

### 3. Validate Petri Net

**POST /api/v1/petri/validate**

Perform formal verification including deadlock detection, reachability analysis, liveness checking, and boundedness verification.

**Request Schema:**
```json
{
  "petriNet": "PetriNet object",
  "config": {
    "kBound": "integer (default: 200)",
    "maxTimeMs": "long (default: 30000)",
    "enabledChecks": "array<string> (optional)"
  }
}
```

**Request Example:**
```json
{
  "petriNet": {
    "name": "Test Workflow",
    "places": [...],
    "transitions": [...],
    "arcs": [...],
    "initialMarking": {"p_start": 1}
  },
  "config": {
    "kBound": 200,
    "maxTimeMs": 30000,
    "enabledChecks": ["DEADLOCK", "REACHABILITY", "LIVENESS", "BOUNDEDNESS"]
  }
}
```

**Response (200 OK - Valid):**
```json
{
  "schemaVersion": "1.0",
  "success": true,
  "validationResult": {
    "petriNetId": "petri_xyz",
    "petriStatus": "SAFE",
    "isValid": true,
    "statesExplored": 42,
    "checkResults": [
      {
        "type": "DEADLOCK",
        "status": "PASS",
        "message": "No deadlocks detected",
        "details": {},
        "executionTimeMs": 85
      },
      {
        "type": "REACHABILITY",
        "status": "PASS",
        "message": "All places reachable",
        "details": {},
        "executionTimeMs": 120
      },
      {
        "type": "LIVENESS",
        "status": "PASS",
        "message": "All transitions are live",
        "details": {},
        "executionTimeMs": 95
      },
      {
        "type": "BOUNDEDNESS",
        "status": "PASS",
        "message": "Net is k-bounded with k=1",
        "details": {
          "bound": 1
        },
        "executionTimeMs": 50
      }
    ],
    "hints": ["Workflow is safe for execution"],
    "suggestions": []
  }
}
```

**Response (200 OK - Failed Validation):**
```json
{
  "schemaVersion": "1.0",
  "success": false,
  "validationResult": {
    "petriStatus": "DEADLOCK",
    "isValid": false,
    "statesExplored": 15,
    "checkResults": [
      {
        "type": "DEADLOCK",
        "status": "FAIL",
        "message": "Deadlock detected at marking {p_wait: 1}",
        "executionTimeMs": 120
      }
    ],
    "counterExample": {
      "description": "Deadlock detected",
      "failingMarking": {"p_wait": 1},
      "enabledTransitions": [],
      "pathToFailure": ["t1", "t2"]
    },
    "hints": [
      "Check for missing transitions from place 'p_wait'",
      "Consider adding alternative execution paths"
    ],
    "suggestions": [
      "Add a transition from p_wait to an exit state"
    ]
  }
}
```

**Error Response (400 Bad Request):**
```json
{
  "schemaVersion": "1.0",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request: Petri net is required"
  }
}
```

**Validation Check Types:**
- `DEADLOCK` - Detects states where no transitions can fire
- `REACHABILITY` - Verifies all places/transitions are reachable
- `LIVENESS` - Checks if transitions can eventually fire
- `BOUNDEDNESS` - Ensures token counts stay within k-bound

**Performance Notes:**
- Default k-bound: 200 states
- Default timeout: 30 seconds
- State space exploration uses breadth-first search
- Optimized for nets with ≤30 places and ≤30 transitions

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/petri/validate \
  -H "Content-Type: application/json" \
  -d @petri_net.json
```

---

### 4. Project to DAG

**POST /api/v1/petri/dag**

Convert Petri net structures into simplified DAG representations using formal transitive reduction algorithm.

**Request Schema:**
```json
{
  "petriNet": "PetriNet object"
}
```

**Response (200 OK):**
```json
{
  "schemaVersion": "1.0",
  "success": true,
  "dag": {
    "id": "dag_abc123",
    "name": "CI/CD Workflow",
    "derivedFromPetriNetId": "petri_xyz",
    "metadata": {},
    "nodes": [
      {
        "id": "t_run_tests",
        "action": "run_tests",
        "type": "ACTION",
        "inputParams": {},
        "metadata": {
          "originalTransitionId": "t_run_tests",
          "petriNetPlaces": ["p_run_tests_ready", "p_run_tests_done"]
        },
        "dependencies": []
      },
      {
        "id": "t_deploy",
        "action": "deploy",
        "type": "ACTION",
        "inputParams": {},
        "metadata": {
          "originalTransitionId": "t_deploy",
          "petriNetPlaces": ["p_deploy_ready", "p_deploy_done"],
          "incomingEdges": [
            {
              "from": "t_run_tests",
              "places": ["p_run_tests_done", "p_deploy_ready"]
            }
          ]
        },
        "dependencies": ["t_run_tests"]
      }
    ],
    "edges": [
      {
        "from": "t_run_tests",
        "to": "t_deploy",
        "places": ["p_run_tests_done", "p_deploy_ready"]
      }
    ],
    "rootNodeId": "t_run_tests"
  },
  "projectionInfo": {
    "algorithm": "transitive-reduction",
    "derivedFrom": "petri_xyz",
    "nodeCount": 2
  }
}
```

**Projection Algorithm:**
- Uses **transitive reduction** for minimal DAG edges
- Preserves workflow semantics from Petri net
- Filters out intermediate dependency connectors
- Includes cross-highlighting metadata for UI visualization

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/petri/dag \
  -H "Content-Type: application/json" \
  -d @petri_net.json
```

---

### 5. Simulate Token Flow

**POST /api/v1/petri/simulate**

Execute step-by-step token simulation with comprehensive trace logging and deadlock detection.

**Request Schema:**
```json
{
  "petriNet": "PetriNet object",
  "config": {
    "seed": "long (optional)",
    "mode": "DETERMINISTIC | INTERACTIVE",
    "maxSteps": "integer (default: 1000)",
    "stepDelayMs": "integer (default: 0)",
    "enableTracing": "boolean (default: true)",
    "enableAnimation": "boolean (default: false)",
    "pauseOnDeadlock": "boolean (default: true)",
    "verbose": "boolean (default: false)"
  }
}
```

**Request Example:**
```json
{
  "petriNet": {...},
  "config": {
    "seed": 42,
    "mode": "DETERMINISTIC",
    "maxSteps": 100,
    "enableTracing": true
  }
}
```

**Response (200 OK - Success):**
```json
{
  "schemaVersion": "1.0",
  "success": true,
  "simulationResult": {
    "success": true,
    "status": "COMPLETED",
    "message": "Simulation completed successfully",
    "stepsExecuted": 5,
    "initialMarking": {"p_start": 1},
    "finalMarking": {"p_end": 1},
    "simulationStartTime": "2025-11-02T10:00:00.000Z",
    "simulationEndTime": "2025-11-02T10:00:00.523Z",
    "trace": [
      {
        "timestamp": "2025-11-02T10:00:00.100Z",
        "sequenceNumber": 1,
        "type": "TRANSITION_FIRED",
        "transitionId": "t_run_tests",
        "transition": "run_tests",
        "markingBefore": {"p_start": 1},
        "markingAfter": {"p_run_tests_done": 1},
        "description": "Fired transition: run_tests"
      },
      {
        "timestamp": "2025-11-02T10:00:00.250Z",
        "sequenceNumber": 2,
        "type": "TRANSITION_FIRED",
        "transitionId": "t_deploy",
        "transition": "deploy",
        "markingBefore": {"p_run_tests_done": 1},
        "markingAfter": {"p_end": 1},
        "description": "Fired transition: deploy"
      }
    ],
    "diagnostics": {
      "totalTransitionsFired": 5,
      "uniqueTransitions": 3,
      "averageStepTime": 104
    }
  }
}
```

**Response (200 OK - Deadlock):**
```json
{
  "schemaVersion": "1.0",
  "success": false,
  "simulationResult": {
    "success": false,
    "status": "DEADLOCK",
    "message": "Simulation halted: deadlock detected",
    "stepsExecuted": 3,
    "finalMarking": {"p_wait": 1},
    "trace": [...],
    "diagnostics": {
      "deadlockDetected": true,
      "deadlockMarking": {"p_wait": 1}
    }
  }
}
```

**Simulation Modes:**
- `DETERMINISTIC` - Reproducible execution with seeded randomness
- `INTERACTIVE` - Manual transition selection (for UI)

**Trace Event Types:**
- `TRANSITION_FIRED` - Transition successfully executed
- `DEADLOCK_DETECTED` - No enabled transitions
- `MAX_STEPS_REACHED` - Simulation limit hit

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/petri/simulate \
  -H "Content-Type: application/json" \
  -d '{
    "petriNet": {...},
    "config": {
      "mode": "DETERMINISTIC",
      "seed": 42
    }
  }'
```

---

### 6. Execute Workflow (NOT IMPLEMENTED)

**POST /api/v1/petri/execute**

End-to-end execution: parse → build → validate → project → execute with plugins.

**Status:** `501 Not Implemented` - Plugin execution layer under development.

**Request Schema:**
```json
{
  "intentSpec": "PetriIntentSpec object",
  "validationEnabled": "boolean",
  "strictValidation": "boolean",
  "kBound": "integer",
  "validationTimeoutMs": "long",
  "simulationEnabled": "boolean",
  "maxSimulationSteps": "integer",
  "simulationSeed": "long",
  "executionEnabled": "boolean"
}
```

**Response (501 Not Implemented):**
```json
{
  "success": false,
  "error": "Plugin execution not yet implemented. Use /build-dag endpoint for visualization only.",
  "message": "This endpoint requires P3NetExecutionService which is currently disabled (WIP)"
}
```

---

## Request/Response Schemas

### Core Data Types

#### PetriIntentSpec

```typescript
{
  "modelType": "PetriIntentSpec",
  "name": "string",
  "description": "string",
  "schemaVersion": "1.0",
  "originalPrompt": "string",
  "templateId": "string",
  "metadata": {
    "key": "value"
  },
  "steps": [
    {
      "id": "string",
      "type": "ACTION | CHOICE | PARALLEL | JOIN",
      "description": "string",
      "dependencies": ["stepId1", "stepId2"],
      "conditions": {
        "key": "value"
      },
      "when": "string (conditional expression)",
      "metadata": {}
    }
  ]
}
```

**StepType Enumeration:**
- `ACTION` - Single executable task
- `CHOICE` - Conditional branch point
- `PARALLEL` - Concurrent execution fork
- `JOIN` - Synchronization point

---

#### PetriNet

```typescript
{
  "name": "string",
  "description": "string",
  "places": [
    {
      "id": "string",
      "name": "string",
      "capacity": "integer (default: MAX_INT)"
    }
  ],
  "transitions": [
    {
      "id": "string",
      "name": "string",
      "description": "string",
      "action": "string (optional)",
      "guard": "string (optional condition)",
      "metadata": {},
      "timeoutMs": "long (optional)",
      "delayMs": "long (optional)",
      "retryPolicy": {},
      "inhibitorConditions": {}
    }
  ],
  "arcs": [
    {
      "from": "string (place or transition ID)",
      "to": "string (transition or place ID)",
      "weight": "integer (default: 1)"
    }
  ],
  "initialMarking": {
    "placeId": "tokenCount"
  },
  "workflowSummary": [
    {
      "name": "string",
      "description": "string",
      "stepType": "string"
    }
  ]
}
```

---

#### DAG

```typescript
{
  "id": "string",
  "name": "string",
  "derivedFromPetriNetId": "string",
  "metadata": {},
  "nodes": [
    {
      "id": "string",
      "action": "string",
      "type": "ACTION",
      "inputParams": {},
      "metadata": {
        "originalTransitionId": "string",
        "petriNetPlaces": ["placeId1", "placeId2"]
      },
      "dependencies": ["nodeId1", "nodeId2"]
    }
  ],
  "edges": [
    {
      "from": "nodeId",
      "to": "nodeId",
      "places": ["placeId1", "placeId2"]
    }
  ],
  "rootNodeId": "string"
}
```

---

### DTO Summary

| DTO Class | Purpose | Key Fields |
|-----------|---------|------------|
| `PetriParseRequest` | Parse input | `text` (max 10k chars), `templateHint` |
| `PetriParseResponse` | Parse output | `intentSpec`, `confidence`, `templateUsed` |
| `PetriBuildRequest` | Build input | `intent` (PetriIntentSpec) |
| `PetriBuildResponse` | Build output | `petriNet`, `buildTime` |
| `PetriValidateRequest` | Validation input | `petriNet`, `config` |
| `PetriDagRequest` | DAG projection input | `petriNet` |
| `PetriDagResponse` | DAG projection output | `dag`, `projectionInfo` |
| `PetriSimulateRequest` | Simulation input | `petriNet`, `config` |
| `PetriExecuteRequest` | Execution input | `intentSpec`, validation/sim flags |
| `PetriErrorResponse` | Error wrapper | `error`, `message`, `errorCode` |

---

## Error Handling

### HTTP Status Codes

| Status Code | Meaning | When Used |
|-------------|---------|-----------|
| 200 OK | Success | Operation completed (check `success` field for validation results) |
| 400 Bad Request | Invalid input | Schema violations, missing required fields, malformed JSON |
| 401 Unauthorized | Auth required | Missing or invalid authentication credentials |
| 403 Forbidden | Insufficient permissions | Valid auth but lacking required role/scope |
| 404 Not Found | Resource not found | Unknown endpoint |
| 409 Conflict | Construction conflict | Unmatched joins, structural Petri net issues |
| 422 Unprocessable Entity | Validation inconclusive | State space bound reached during analysis |
| 500 Internal Server Error | Server error | Unexpected engine errors |
| 501 Not Implemented | Feature unavailable | `/execute` endpoint (plugin layer WIP) |

---

### Error Response Format

All errors follow this structure:

```json
{
  "schemaVersion": "1.0",
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error description"
  }
}
```

**Optional fields:**
```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Failed to validate Petri net: Deadlock detected",
    "details": {
      "failingMarking": {"p_wait": 1},
      "suggestion": "Add exit transition from p_wait"
    }
  }
}
```

---

### Error Code Reference

| Error Code | HTTP Status | Description | Resolution |
|------------|-------------|-------------|------------|
| `PARSE_ERROR` | 400 | Failed to parse natural language | Check input text format, try `templateHint` |
| `INVALID_INPUT` | 400 | Schema validation failed | Review request schema, check required fields |
| `BUILD_ERROR` | 400 | Petri net construction failed | Ensure intent has valid steps with proper dependencies |
| `CONSTRUCTION_CONFLICT` | 409 | Structural issue in Petri net | Check for unmatched JOIN nodes or circular dependencies |
| `VALIDATION_ERROR` | 400 / 500 | Formal verification failed | Review validation report, check for deadlocks |
| `VALIDATION_INCONCLUSIVE` | 422 | State space bound reached | Increase `kBound` or simplify workflow |
| `SIMULATION_ERROR` | 400 / 500 | Simulation execution failed | Verify Petri net validity, check simulation config |
| `DAG_PROJECTION_ERROR` | 400 / 500 | Failed to project to DAG | Ensure Petri net is well-formed |
| `ENGINE_ERROR` | 500 | Internal system error | Contact support, check logs |

---

## Code Examples

### Complete Workflow: Parse → Build → Validate → DAG → Simulate

#### Bash Script

```bash
#!/bin/bash

API_BASE="http://localhost:8080/api/v1/petri"

# Step 1: Parse natural language
echo "Step 1: Parsing natural language..."
PARSE_RESPONSE=$(curl -s -X POST "$API_BASE/parse" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "warm up, then pass and shoot in parallel, then cool down"
  }')

echo "$PARSE_RESPONSE" | jq '.'

# Extract intent from response
INTENT=$(echo "$PARSE_RESPONSE" | jq '.intent')

# Step 2: Build Petri net
echo -e "\nStep 2: Building Petri net..."
BUILD_RESPONSE=$(curl -s -X POST "$API_BASE/build" \
  -H "Content-Type: application/json" \
  -d "{\"intent\": $INTENT}")

echo "$BUILD_RESPONSE" | jq '.'

# Extract Petri net from response
PETRI_NET=$(echo "$BUILD_RESPONSE" | jq '.petriNet')

# Step 3: Validate Petri net
echo -e "\nStep 3: Validating Petri net..."
VALIDATE_RESPONSE=$(curl -s -X POST "$API_BASE/validate" \
  -H "Content-Type: application/json" \
  -d "{
    \"petriNet\": $PETRI_NET,
    \"config\": {
      \"kBound\": 200,
      \"maxTimeMs\": 30000
    }
  }")

echo "$VALIDATE_RESPONSE" | jq '.'

# Step 4: Project to DAG
echo -e "\nStep 4: Projecting to DAG..."
DAG_RESPONSE=$(curl -s -X POST "$API_BASE/dag" \
  -H "Content-Type: application/json" \
  -d "{\"petriNet\": $PETRI_NET}")

echo "$DAG_RESPONSE" | jq '.'

# Step 5: Simulate execution
echo -e "\nStep 5: Simulating execution..."
SIMULATE_RESPONSE=$(curl -s -X POST "$API_BASE/simulate" \
  -H "Content-Type: application/json" \
  -d "{
    \"petriNet\": $PETRI_NET,
    \"config\": {
      \"seed\": 42,
      \"mode\": \"DETERMINISTIC\",
      \"enableTracing\": true
    }
  }")

echo "$SIMULATE_RESPONSE" | jq '.'
```

---

#### Python Client

```python
import requests
import json

API_BASE = "http://localhost:8080/api/v1/petri"

class PetriNetClient:
    def __init__(self, base_url=API_BASE):
        self.base_url = base_url
        self.session = requests.Session()
        self.session.headers.update({
            "Content-Type": "application/json"
        })

    def parse(self, text, template_hint=None):
        """Parse natural language into intent specification."""
        payload = {"text": text}
        if template_hint:
            payload["templateHint"] = template_hint

        response = self.session.post(f"{self.base_url}/parse", json=payload)
        response.raise_for_status()
        return response.json()

    def build(self, intent_spec):
        """Build Petri net from intent specification."""
        response = self.session.post(
            f"{self.base_url}/build",
            json={"intent": intent_spec}
        )
        response.raise_for_status()
        return response.json()

    def validate(self, petri_net, config=None):
        """Validate Petri net with formal verification."""
        payload = {"petriNet": petri_net}
        if config:
            payload["config"] = config

        response = self.session.post(f"{self.base_url}/validate", json=payload)
        response.raise_for_status()
        return response.json()

    def project_to_dag(self, petri_net):
        """Project Petri net to DAG representation."""
        response = self.session.post(
            f"{self.base_url}/dag",
            json={"petriNet": petri_net}
        )
        response.raise_for_status()
        return response.json()

    def simulate(self, petri_net, config=None):
        """Simulate token flow through Petri net."""
        payload = {"petriNet": petri_net}
        if config:
            payload["config"] = config

        response = self.session.post(f"{self.base_url}/simulate", json=payload)
        response.raise_for_status()
        return response.json()

    def health_check(self):
        """Check Petri service health."""
        response = self.session.get(f"{self.base_url}/health")
        response.raise_for_status()
        return response.json()

# Usage Example
if __name__ == "__main__":
    client = PetriNetClient()

    # Check health
    health = client.health_check()
    print("Service Status:", health["status"])

    # Parse workflow
    parse_result = client.parse(
        "run tests; if pass deploy to staging; if fail alert slack",
        template_hint="devops"
    )
    intent = parse_result["intent"]
    print(f"Parsed with confidence: {parse_result['confidence']}")

    # Build Petri net
    build_result = client.build(intent)
    petri_net = build_result["petriNet"]
    print(f"Built Petri net with {len(petri_net['places'])} places")

    # Validate
    validation = client.validate(petri_net, config={"kBound": 200})
    is_valid = validation["validationResult"]["isValid"]
    print(f"Validation: {'PASS' if is_valid else 'FAIL'}")

    if is_valid:
        # Project to DAG
        dag_result = client.project_to_dag(petri_net)
        dag = dag_result["dag"]
        print(f"Projected to DAG with {len(dag['nodes'])} nodes")

        # Simulate
        simulation = client.simulate(petri_net, config={
            "seed": 42,
            "mode": "DETERMINISTIC",
            "enableTracing": True
        })
        print(f"Simulation: {simulation['simulationResult']['status']}")
        print(f"Steps executed: {simulation['simulationResult']['stepsExecuted']}")
```

---

#### JavaScript/TypeScript Client

```typescript
// petri-client.ts
interface PetriNetClient {
  parse(text: string, templateHint?: string): Promise<ParseResponse>;
  build(intent: PetriIntentSpec): Promise<BuildResponse>;
  validate(petriNet: PetriNet, config?: ValidationConfig): Promise<ValidationResponse>;
  projectToDag(petriNet: PetriNet): Promise<DagResponse>;
  simulate(petriNet: PetriNet, config?: SimulationConfig): Promise<SimulationResponse>;
}

class PetriNetAPIClient implements PetriNetClient {
  private baseUrl: string;

  constructor(baseUrl = "http://localhost:8080/api/v1/petri") {
    this.baseUrl = baseUrl;
  }

  async parse(text: string, templateHint?: string): Promise<ParseResponse> {
    const response = await fetch(`${this.baseUrl}/parse`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ text, templateHint })
    });

    if (!response.ok) {
      throw new Error(`Parse failed: ${response.statusText}`);
    }

    return response.json();
  }

  async build(intent: PetriIntentSpec): Promise<BuildResponse> {
    const response = await fetch(`${this.baseUrl}/build`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ intent })
    });

    if (!response.ok) {
      throw new Error(`Build failed: ${response.statusText}`);
    }

    return response.json();
  }

  async validate(petriNet: PetriNet, config?: ValidationConfig): Promise<ValidationResponse> {
    const response = await fetch(`${this.baseUrl}/validate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ petriNet, config })
    });

    if (!response.ok) {
      throw new Error(`Validation failed: ${response.statusText}`);
    }

    return response.json();
  }

  async projectToDag(petriNet: PetriNet): Promise<DagResponse> {
    const response = await fetch(`${this.baseUrl}/dag`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ petriNet })
    });

    if (!response.ok) {
      throw new Error(`DAG projection failed: ${response.statusText}`);
    }

    return response.json();
  }

  async simulate(petriNet: PetriNet, config?: SimulationConfig): Promise<SimulationResponse> {
    const response = await fetch(`${this.baseUrl}/simulate`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ petriNet, config })
    });

    if (!response.ok) {
      throw new Error(`Simulation failed: ${response.statusText}`);
    }

    return response.json();
  }
}

// Usage
(async () => {
  const client = new PetriNetAPIClient();

  const parseResult = await client.parse("warm up, then pass and shoot in parallel");
  const buildResult = await client.build(parseResult.intent);
  const validationResult = await client.validate(buildResult.petriNet);

  if (validationResult.validationResult.isValid) {
    const dagResult = await client.projectToDag(buildResult.petriNet);
    const simResult = await client.simulate(buildResult.petriNet, {
      mode: "DETERMINISTIC",
      seed: 42
    });

    console.log(`Simulation completed in ${simResult.simulationResult.stepsExecuted} steps`);
  }
})();
```

---

## Performance Benchmarks

| Endpoint | Avg Latency | P95 Latency | Max Throughput |
|----------|-------------|-------------|----------------|
| `/parse` | 200-500ms | 800ms | ~50 req/sec |
| `/build` | 100-300ms | 500ms | ~100 req/sec |
| `/validate` | 150-2000ms | 3000ms | ~20 req/sec |
| `/dag` | 50-200ms | 400ms | ~150 req/sec |
| `/simulate` | 100-500ms | 1000ms | ~50 req/sec |

**Performance Notes:**
- Validation latency depends on state space size (k-bound)
- Simulation performance scales with maxSteps configuration
- Optimized for Petri nets with ≤30 places and ≤30 transitions
- Target response time for parse→validate→render: <2 seconds

---

## Rate Limiting

**Current Status:** Not implemented (development mode)

**Planned Configuration:**
```properties
# Rate limiting (when enabled)
rate-limit.enabled=false
rate-limit.requests-per-minute=60
rate-limit.burst-size=10
```

**Response Headers (when enabled):**
```
X-RateLimit-Limit: 60
X-RateLimit-Remaining: 45
X-RateLimit-Reset: 1730563860
```

---

## CORS Configuration

**Allowed Origins:**
- `http://localhost:3000` (React frontend)
- `http://localhost:8080` (API server)

**Allowed Methods:**
- GET, POST, PUT, DELETE, OPTIONS, PATCH

**Exposed Headers:**
- `X-Total-Count`
- `X-Correlation-ID`
- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset`

**Allow Credentials:** Yes
**Max Age:** 3600 seconds

---

## WebSocket Endpoints

**Current Status:** Not yet implemented for Petri net workflows

**Planned WebSocket Routes:**
```
ws://localhost:8080/ws/petri/simulation/{sessionId}
```

**Event Types (Planned):**
- `SIMULATION_STARTED`
- `TRANSITION_FIRED`
- `MARKING_CHANGED`
- `DEADLOCK_DETECTED`
- `SIMULATION_COMPLETED`

---

## OpenAPI Specification

**Status:** Temporarily disabled due to SpringDoc version conflicts

**Planned Access:**
- OpenAPI JSON: `/api-docs`
- Swagger UI: `/swagger-ui.html`

**Fallback Documentation:**
- `/Users/rishabhpathak/base/Obvian-repos/obvian-mvp/resources/openapi-fallback.yaml`

---

## Monitoring & Observability

### Health Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Overall system health |
| `/actuator/health/ready` | Readiness probe (Kubernetes) |
| `/actuator/health/live` | Liveness probe (Kubernetes) |

### Metrics

**Prometheus Metrics Endpoint:** `/actuator/prometheus`

**Key Metrics:**
- `http_server_requests_seconds` - Request latency histogram
- `jvm_memory_used_bytes` - JVM memory usage
- `petri_validation_duration_seconds` - Validation performance
- `petri_simulation_steps_total` - Simulation step counter

**Percentiles Tracked:** p50, p95, p99

### Distributed Tracing

**OpenTelemetry Integration:**
- Service Name: `obvian-visual-trace`
- OTLP Endpoint: `http://localhost:4317`
- Sampling Ratio: 10% (configurable)
- Exporters: OTLP (primary), Jaeger (fallback), Zipkin (secondary)

**Trace Attributes:**
```
service.name=obvian-visual-trace
service.version=0.5.0
deployment.environment=development
```

---

## Security Notes

### Development Mode Security

Current Petri endpoints have **CSRF disabled** and **permitAll** access for testing. This is **NOT production-ready**.

### Production Recommendations

1. **Enable Authentication:**
   ```java
   .requestMatchers("/api/v1/petri/**").hasRole("USER")
   ```

2. **Enable CSRF Protection:**
   ```java
   .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
   ```

3. **Add Rate Limiting:**
   - Implement token bucket algorithm
   - 60 requests/minute per user
   - Burst capacity: 10 requests

4. **Input Validation:**
   - Already implemented via `jakarta.validation` annotations
   - Max text length: 10,000 characters
   - Petri net size limits enforced during build

5. **API Key Management:**
   - Rotate keys every 90 days
   - Store in environment variables
   - Use different keys per environment

---

## Migration & Versioning

### API Versioning Strategy

**Current Version:** `v1`
**Schema Version:** `1.0`

All responses include:
```json
{
  "schemaVersion": "1.0"
}
```

### Future Version Changes

When introducing breaking changes:
1. New version route: `/api/v2/petri/...`
2. Maintain `v1` endpoints for 6 months
3. Add deprecation warnings in responses
4. Update schema version in responses

### Deprecation Headers (Planned)

```
Deprecation: true
Sunset: Sat, 01 Jun 2026 00:00:00 GMT
Link: <https://docs.obvian.com/api/v2/migration>; rel="deprecation"
```

---

## SDK Generation

### Planned Client SDKs

- **JavaScript/TypeScript** - Generate from OpenAPI spec
- **Python** - Using `openapi-generator`
- **Java** - Using `openapi-generator`
- **Go** - Using `openapi-generator`

**Generation Command (when OpenAPI is restored):**
```bash
./scripts/generate-openapi.sh
openapi-generator-cli generate -i docs/api/openapi.yaml -g typescript-fetch -o sdks/typescript
```

---

## Support & Contact

**Documentation Repository:** `/Users/rishabhpathak/base/Obvian-repos/obvian-mvp/docs/api/`

**Related Documentation:**
- `PETRI_API.md` - Detailed Petri net API guide
- `PETRI_NET_API_GUIDE.md` - Implementation summary
- `PETRI_IMPLEMENTATION_SUMMARY.md` - Technical architecture

**Testing:**
```bash
# Start server
mvn spring-boot:run -Dspring-boot.run.main-class=api.ObvianApplication

# Run integration tests
./scripts/run-tests.sh -p integration-tests

# Test Petri endpoints
curl http://localhost:8080/api/v1/petri/health
```

---

**Last Updated:** 2025-11-02
**API Version:** 0.5.0
**Schema Version:** 1.0
