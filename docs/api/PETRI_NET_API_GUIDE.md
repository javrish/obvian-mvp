# Petri Net API Developer Guide

Complete reference for the Obvian Petri Net API with interactive examples, code samples, and best practices.

## 🚀 Getting Started

### Authentication

The Petri Net API supports multiple authentication methods:

**JWT Bearer Token (Recommended for users):**
```bash
curl -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

**API Key (Service-to-service):**
```bash
curl -H "X-API-Key: your-api-key-here"
```

### Base URLs

- **Production**: `https://api.obvian.com`
- **Staging**: `https://staging-api.obvian.com`
- **Local Development**: `http://localhost:8080`

### Schema Version

All requests must include `schemaVersion: "1.0"` in the request body for API versioning.

## 📝 API Endpoints Overview

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/petri/parse` | POST | Parse natural language to intent specification |
| `/api/v1/petri/build` | POST | Build Petri net from intent specification |
| `/api/v1/petri/validate` | POST | Validate Petri net using formal methods |
| `/api/v1/petri/simulate` | POST | Simulate Petri net execution with traces |
| `/api/v1/petri/dag` | POST | Project Petri net to DAG representation |
| `/api/v1/petri/health` | GET | Health check for Petri net services |

## 🔍 Detailed API Reference

### 1. Parse Natural Language

Convert natural language workflow descriptions into structured intent specifications.

**Endpoint:** `POST /api/v1/petri/parse`

**Request Schema:**
```json
{
  "text": "string (required) - Natural language workflow description",
  "templateHint": "string (optional) - Template hint: 'devops' or 'football'",
  "schemaVersion": "string (required) - API version '1.0'"
}
```

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/petri/parse" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "text": "Every time I push code: run tests; if pass deploy; if fail alert team",
    "templateHint": "devops",
    "schemaVersion": "1.0"
  }'
```

**Success Response (200):**
```json
{
  "schemaVersion": "1.0",
  "intent": {
    "name": "DeployOnGreen",
    "steps": [
      {
        "id": "run_tests",
        "type": "task",
        "name": "Run Tests"
      },
      {
        "id": "deploy_or_alert",
        "type": "choice",
        "condition": "tests_passed",
        "branches": [
          {
            "condition": "true",
            "target": "deploy"
          },
          {
            "condition": "false",
            "target": "alert_team"
          }
        ]
      }
    ]
  },
  "templateUsed": "devops-ci-cd",
  "confidence": 0.95
}
```

**Error Responses:**
- **400 Bad Request**: Unrecognized pattern or invalid input
- **401 Unauthorized**: Missing or invalid authentication
- **500 Internal Server Error**: Parsing engine failure

### 2. Build Petri Net

Convert structured intent specifications into formal Petri net representations.

**Endpoint:** `POST /api/v1/petri/build`

**Request Schema:**
```json
{
  "intent": "PetriIntentSpec (required) - Structured workflow specification",
  "generateDag": "boolean (optional) - Also generate DAG representation",
  "schemaVersion": "string (required) - API version '1.0'"
}
```

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/petri/build" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "intent": {
      "name": "DeployOnGreen",
      "steps": [
        {
          "id": "run_tests",
          "type": "task",
          "name": "Run Tests"
        },
        {
          "id": "deploy_or_alert",
          "type": "choice",
          "condition": "tests_passed"
        }
      ]
    },
    "generateDag": true,
    "schemaVersion": "1.0"
  }'
```

**Success Response (200):**
```json
{
  "schemaVersion": "1.0",
  "petriNet": {
    "id": "petri_abc123def456",
    "name": "Deploy On Green Workflow",
    "places": [
      {
        "id": "p_start",
        "name": "Start Place",
        "capacity": null
      },
      {
        "id": "p_after_tests",
        "name": "After Tests",
        "capacity": null
      },
      {
        "id": "p_done",
        "name": "Completion",
        "capacity": null
      }
    ],
    "transitions": [
      {
        "id": "t_run_tests",
        "name": "Run Tests",
        "guard": null
      },
      {
        "id": "t_deploy",
        "name": "Deploy",
        "guard": "tests_passed == true"
      },
      {
        "id": "t_alert",
        "name": "Alert Team",
        "guard": "tests_passed == false"
      }
    ],
    "arcs": [
      {
        "id": "arc_1",
        "source": "p_start",
        "target": "t_run_tests",
        "weight": 1
      },
      {
        "id": "arc_2",
        "source": "t_run_tests",
        "target": "p_after_tests",
        "weight": 1
      },
      {
        "id": "arc_3",
        "source": "p_after_tests",
        "target": "t_deploy",
        "weight": 1
      },
      {
        "id": "arc_4",
        "source": "p_after_tests",
        "target": "t_alert",
        "weight": 1
      },
      {
        "id": "arc_5",
        "source": "t_deploy",
        "target": "p_done",
        "weight": 1
      },
      {
        "id": "arc_6",
        "source": "t_alert",
        "target": "p_done",
        "weight": 1
      }
    ],
    "initialMarking": {
      "p_start": 1,
      "p_after_tests": 0,
      "p_done": 0
    }
  },
  "dag": {
    "nodes": [
      {
        "id": "t_run_tests",
        "name": "Run Tests",
        "type": "TRANSITION"
      },
      {
        "id": "t_deploy",
        "name": "Deploy",
        "type": "TRANSITION"
      },
      {
        "id": "t_alert",
        "name": "Alert Team",
        "type": "TRANSITION"
      }
    ],
    "edges": [
      {
        "source": "t_run_tests",
        "target": "t_deploy",
        "metadata": {
          "places": ["p_after_tests"]
        }
      },
      {
        "source": "t_run_tests",
        "target": "t_alert",
        "metadata": {
          "places": ["p_after_tests"]
        }
      }
    ]
  }
}
```

**Error Responses:**
- **400 Bad Request**: Invalid intent specification
- **409 Conflict**: Construction conflicts (unmatched synchronization)
- **500 Internal Server Error**: Build engine failure

### 3. Validate Petri Net

Perform comprehensive formal validation including deadlock detection, reachability analysis, liveness checking, and boundedness verification.

**Endpoint:** `POST /api/v1/petri/validate`

**Request Schema:**
```json
{
  "petriNet": "PetriNet (required) - Petri net to validate",
  "config": {
    "kBound": "integer (optional) - State exploration bound (default: 200)",
    "maxMillis": "integer (optional) - Timeout in milliseconds (default: 30000)",
    "enableDeadlockCheck": "boolean (optional) - Enable deadlock detection (default: true)",
    "enableReachabilityCheck": "boolean (optional) - Enable reachability analysis (default: true)",
    "enableLivenessCheck": "boolean (optional) - Enable liveness checking (default: true)",
    "enableBoundednessCheck": "boolean (optional) - Enable boundedness verification (default: true)"
  },
  "schemaVersion": "string (required) - API version '1.0'"
}
```

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/petri/validate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "petriNet": {
      "id": "petri_abc123",
      "places": [...],
      "transitions": [...],
      "arcs": [...]
    },
    "config": {
      "kBound": 200,
      "maxMillis": 30000,
      "enableDeadlockCheck": true,
      "enableReachabilityCheck": true
    },
    "schemaVersion": "1.0"
  }'
```

**Success Response (200) - PASS:**
```json
{
  "schemaVersion": "1.0",
  "report": {
    "status": "PASS",
    "checks": {
      "deadlock": "PASS",
      "reachability": "PASS",
      "liveness": "PASS",
      "boundedness": "PASS"
    },
    "statesExplored": 45,
    "executionTimeMs": 1250,
    "witness": null,
    "hints": []
  }
}
```

**Success Response (200) - FAIL:**
```json
{
  "schemaVersion": "1.0",
  "report": {
    "status": "FAIL",
    "checks": {
      "deadlock": "FAIL",
      "reachability": "PASS",
      "liveness": "FAIL",
      "boundedness": "PASS"
    },
    "statesExplored": 123,
    "executionTimeMs": 3456,
    "witness": {
      "type": "DEADLOCK",
      "marking": {
        "p_blocked": 1,
        "p_waiting": 1
      },
      "path": ["t_start", "t_split", "t_task1"],
      "description": "Deadlock reached: no enabled transitions from marking {p_blocked: 1, p_waiting: 1}"
    },
    "hints": [
      "Consider adding an AND-join transition before t_cooldown",
      "Check for missing synchronization points in parallel branches"
    ]
  }
}
```

**Error Responses:**
- **400 Bad Request**: Invalid Petri net structure
- **422 Unprocessable Entity**: Validation inconclusive (bound/timeout reached)
- **500 Internal Server Error**: Validation engine failure

### 4. Simulate Petri Net

Execute Petri net simulation with token movement tracking and trace generation.

**Endpoint:** `POST /api/v1/petri/simulate`

**Request Schema:**
```json
{
  "petriNet": "PetriNet (required) - Petri net to simulate",
  "config": {
    "seed": "integer (optional) - Random seed for deterministic simulation",
    "mode": "string (optional) - Simulation mode: 'DETERMINISTIC' or 'INTERACTIVE'",
    "maxSteps": "integer (optional) - Maximum simulation steps (default: 1000)",
    "stepDelayMs": "integer (optional) - Delay between steps for animation (default: 0)",
    "enableTrace": "boolean (optional) - Generate execution trace (default: true)"
  },
  "schemaVersion": "string (required) - API version '1.0'"
}
```

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/petri/simulate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "petriNet": {
      "id": "petri_abc123",
      "places": [...],
      "transitions": [...],
      "arcs": [...]
    },
    "config": {
      "seed": 42,
      "mode": "DETERMINISTIC",
      "maxSteps": 100,
      "enableTrace": true
    },
    "schemaVersion": "1.0"
  }'
```

**Success Response (200):**
```json
{
  "schemaVersion": "1.0",
  "trace": [
    {
      "step": 0,
      "transition": "t_run_tests",
      "timestamp": 1640995200000,
      "markingBefore": {
        "p_start": 1,
        "p_after_tests": 0,
        "p_done": 0
      },
      "markingAfter": {
        "p_start": 0,
        "p_after_tests": 1,
        "p_done": 0
      },
      "enabledTransitions": ["t_run_tests"],
      "tokenId": "token_001",
      "seed": 42
    },
    {
      "step": 1,
      "transition": "t_deploy",
      "timestamp": 1640995201000,
      "markingBefore": {
        "p_start": 0,
        "p_after_tests": 1,
        "p_done": 0
      },
      "markingAfter": {
        "p_start": 0,
        "p_after_tests": 0,
        "p_done": 1
      },
      "enabledTransitions": ["t_deploy", "t_alert"],
      "tokenId": "token_001",
      "seed": 42
    }
  ],
  "finalMarking": {
    "p_start": 0,
    "p_after_tests": 0,
    "p_done": 1
  },
  "status": "COMPLETED",
  "stepsExecuted": 2,
  "executionTimeMs": 125
}
```

**Error Responses:**
- **400 Bad Request**: Invalid Petri net or simulation configuration
- **500 Internal Server Error**: Simulation engine failure

### 5. Project to DAG

Generate simplified DAG representation from validated Petri net for visualization and execution.

**Endpoint:** `POST /api/v1/petri/dag`

**Request Schema:**
```json
{
  "petriNet": "PetriNet (required) - Petri net to project",
  "includeMetadata": "boolean (optional) - Include projection notes (default: false)",
  "schemaVersion": "string (required) - API version '1.0'"
}
```

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/v1/petri/dag" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "petriNet": {
      "id": "petri_abc123",
      "places": [...],
      "transitions": [...],
      "arcs": [...]
    },
    "includeMetadata": true,
    "schemaVersion": "1.0"
  }'
```

**Success Response (200):**
```json
{
  "schemaVersion": "1.0",
  "dag": {
    "nodes": [
      {
        "id": "t_run_tests",
        "name": "Run Tests",
        "type": "TRANSITION",
        "metadata": {}
      },
      {
        "id": "t_deploy",
        "name": "Deploy",
        "type": "TRANSITION",
        "metadata": {}
      },
      {
        "id": "t_alert",
        "name": "Alert Team",
        "type": "TRANSITION",
        "metadata": {}
      }
    ],
    "edges": [
      {
        "source": "t_run_tests",
        "target": "t_deploy",
        "metadata": {
          "places": ["p_after_tests"]
        }
      },
      {
        "source": "t_run_tests",
        "target": "t_alert",
        "metadata": {
          "places": ["p_after_tests"]
        }
      }
    ]
  },
  "derivedFromPetriNetId": "petri_abc123",
  "projectionNotes": [
    "Petri Net is source of truth; DAG simplifies ordering",
    "Transitions mapped to DAG nodes; places inform edge dependencies",
    "Partial order derived using topological sort with lexicographic tiebreaking"
  ]
}
```

## 🔗 End-to-End Workflow Examples

### DevOps CI/CD Pipeline

Complete workflow from natural language to execution:

**Step 1: Parse Natural Language**
```bash
curl -X POST "$BASE_URL/api/v1/petri/parse" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "text": "Every time I push code: run tests; if pass deploy; if fail alert team",
    "templateHint": "devops",
    "schemaVersion": "1.0"
  }' > intent.json
```

**Step 2: Build Petri Net**
```bash
curl -X POST "$BASE_URL/api/v1/petri/build" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d @intent.json > petri.json
```

**Step 3: Validate Workflow**
```bash
curl -X POST "$BASE_URL/api/v1/petri/validate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d @petri.json > validation.json
```

**Step 4: Simulate Execution**
```bash
curl -X POST "$BASE_URL/api/v1/petri/simulate" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "petriNet": '$(cat petri.json | jq .petriNet)',
    "config": {
      "seed": 42,
      "mode": "DETERMINISTIC"
    },
    "schemaVersion": "1.0"
  }' > simulation.json
```

**Step 5: Generate DAG for Execution**
```bash
curl -X POST "$BASE_URL/api/v1/petri/dag" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d @petri.json > dag.json
```

### Football Training Session

Complete parallel workflow example:

**Natural Language Input:**
```
"Start with warm-up, then pass and shoot in parallel, finish with cooldown"
```

**Expected Validation Result:** PASS (proper AND-split/join synchronization)

**Expected Simulation:** 4 steps (warm-up → pass || shoot → cooldown)

**Expected DAG:** 4 nodes with proper dependencies

## 🛠️ SDKs and Code Examples

### JavaScript/Node.js SDK

```javascript
const ObvianPetriClient = require('@obvian/petri-sdk');

const client = new ObvianPetriClient({
  baseURL: 'https://api.obvian.com',
  apiKey: 'your-api-key'
});

// Parse natural language workflow
async function parseWorkflow(text, templateHint = null) {
  try {
    const result = await client.parse({
      text,
      templateHint,
      schemaVersion: '1.0'
    });

    console.log(`Parsed with confidence: ${result.confidence}`);
    console.log(`Template used: ${result.templateUsed}`);
    return result.intent;

  } catch (error) {
    console.error('Parse failed:', error.message);
    throw error;
  }
}

// Build and validate Petri net
async function buildAndValidatePetriNet(intent) {
  try {
    // Build Petri net
    const buildResult = await client.build({
      intent,
      generateDag: true,
      schemaVersion: '1.0'
    });

    // Validate Petri net
    const validationResult = await client.validate({
      petriNet: buildResult.petriNet,
      config: {
        kBound: 200,
        maxMillis: 30000
      },
      schemaVersion: '1.0'
    });

    if (validationResult.report.status === 'PASS') {
      console.log('✅ Petri net validation passed');
    } else if (validationResult.report.status === 'FAIL') {
      console.log('❌ Petri net validation failed');
      console.log('Hints:', validationResult.report.hints);
    }

    return {
      petriNet: buildResult.petriNet,
      validation: validationResult.report,
      dag: buildResult.dag
    };

  } catch (error) {
    console.error('Build/validation failed:', error.message);
    throw error;
  }
}

// Complete workflow
async function processWorkflow(text) {
  const intent = await parseWorkflow(text, 'devops');
  const result = await buildAndValidatePetriNet(intent);

  // Simulate execution
  const simulation = await client.simulate({
    petriNet: result.petriNet,
    config: {
      seed: 42,
      mode: 'DETERMINISTIC'
    },
    schemaVersion: '1.0'
  });

  console.log(`Simulation completed in ${simulation.stepsExecuted} steps`);
  return {
    ...result,
    simulation
  };
}
```

### Python SDK

```python
from obvian_petri import PetriClient
import json

client = PetriClient(
    base_url='https://api.obvian.com',
    api_key='your-api-key'
)

def process_workflow(text: str, template_hint: str = None):
    """Complete workflow processing pipeline"""

    # Parse natural language
    parse_result = client.parse(
        text=text,
        template_hint=template_hint,
        schema_version='1.0'
    )

    print(f"Parsed with confidence: {parse_result['confidence']}")

    # Build Petri net
    build_result = client.build(
        intent=parse_result['intent'],
        generate_dag=True,
        schema_version='1.0'
    )

    # Validate Petri net
    validation_result = client.validate(
        petri_net=build_result['petriNet'],
        config={
            'kBound': 200,
            'maxMillis': 30000
        },
        schema_version='1.0'
    )

    print(f"Validation status: {validation_result['report']['status']}")

    if validation_result['report']['status'] == 'FAIL':
        print("Validation hints:")
        for hint in validation_result['report']['hints']:
            print(f"  - {hint}")
        return None

    # Simulate execution
    simulation_result = client.simulate(
        petri_net=build_result['petriNet'],
        config={
            'seed': 42,
            'mode': 'DETERMINISTIC'
        },
        schema_version='1.0'
    )

    print(f"Simulation completed: {simulation_result['stepsExecuted']} steps")

    return {
        'intent': parse_result['intent'],
        'petri_net': build_result['petriNet'],
        'validation': validation_result['report'],
        'simulation': simulation_result,
        'dag': build_result.get('dag')
    }

# Example usage
devops_workflow = process_workflow(
    "run tests; if pass deploy; if fail alert team",
    template_hint="devops"
)

football_workflow = process_workflow(
    "warm-up, then pass and shoot in parallel, then cooldown",
    template_hint="football"
)
```

## 🚨 Error Handling Best Practices

### Retry Logic

```javascript
async function withRetry(operation, maxRetries = 3, backoffMs = 1000) {
  let lastError;

  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      return await operation();
    } catch (error) {
      lastError = error;

      // Don't retry client errors (4xx)
      if (error.status >= 400 && error.status < 500) {
        throw error;
      }

      if (attempt < maxRetries) {
        await new Promise(resolve =>
          setTimeout(resolve, backoffMs * Math.pow(2, attempt - 1))
        );
      }
    }
  }

  throw lastError;
}

// Usage
const result = await withRetry(() => client.validate(request));
```

### Graceful Degradation

```javascript
async function parseWithFallback(text) {
  try {
    // Try with specific template hint
    return await client.parse({
      text,
      templateHint: 'devops',
      schemaVersion: '1.0'
    });
  } catch (error) {
    if (error.code === 'PARSE_ERROR') {
      // Retry without template hint
      try {
        return await client.parse({
          text,
          schemaVersion: '1.0'
        });
      } catch (fallbackError) {
        // Return structured error for UI handling
        return {
          success: false,
          error: 'Unable to parse workflow. Please check syntax.',
          suggestions: [
            'Try using more explicit language',
            'Include conditional keywords like "if", "then"',
            'Specify parallel operations clearly'
          ]
        };
      }
    }
    throw error;
  }
}
```

## 📊 Rate Limiting

### Rate Limits

| User Type | Requests/Hour | Burst Limit |
|-----------|---------------|-------------|
| Authenticated User | 1000 | 50/minute |
| Unauthenticated | 100 | 10/minute |
| API Key | 5000 | 100/minute |

### Handling Rate Limits

```javascript
class RateLimitHandler {
  constructor() {
    this.requestQueue = [];
    this.isProcessing = false;
  }

  async makeRequest(requestFn) {
    return new Promise((resolve, reject) => {
      this.requestQueue.push({ requestFn, resolve, reject });
      this.processQueue();
    });
  }

  async processQueue() {
    if (this.isProcessing || this.requestQueue.length === 0) return;

    this.isProcessing = true;

    while (this.requestQueue.length > 0) {
      const { requestFn, resolve, reject } = this.requestQueue.shift();

      try {
        const result = await requestFn();
        resolve(result);
      } catch (error) {
        if (error.status === 429) {
          // Rate limited - put request back and wait
          this.requestQueue.unshift({ requestFn, resolve, reject });

          const retryAfter = error.headers['retry-after'] || 60;
          await new Promise(r => setTimeout(r, retryAfter * 1000));
          continue;
        }
        reject(error);
      }

      // Small delay between requests
      await new Promise(r => setTimeout(r, 100));
    }

    this.isProcessing = false;
  }
}
```

## 🔍 Debugging and Monitoring

### Request Tracing

All requests include a `X-Request-ID` header for tracking:

```bash
curl -X POST "$BASE_URL/api/v1/petri/parse" \
  -H "X-Request-ID: req-123456789" \
  -H "Content-Type: application/json" \
  -d '{ ... }'
```

### Performance Monitoring

```javascript
class PetriClientWithMetrics extends PetriClient {
  async request(method, path, data) {
    const startTime = Date.now();
    const requestId = `req-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;

    try {
      const result = await super.request(method, path, data, {
        headers: { 'X-Request-ID': requestId }
      });

      const duration = Date.now() - startTime;
      console.log(`[${requestId}] ${method} ${path} - ${duration}ms - SUCCESS`);

      return result;
    } catch (error) {
      const duration = Date.now() - startTime;
      console.error(`[${requestId}] ${method} ${path} - ${duration}ms - ERROR: ${error.message}`);
      throw error;
    }
  }
}
```

## 🔧 Testing Your Integration

### Unit Test Examples

```javascript
describe('Petri Net API Integration', () => {
  let client;

  beforeEach(() => {
    client = new ObvianPetriClient({
      baseURL: 'http://localhost:8080',
      apiKey: 'test-api-key'
    });
  });

  test('should parse DevOps workflow successfully', async () => {
    const result = await client.parse({
      text: 'run tests; if pass deploy; if fail alert',
      templateHint: 'devops',
      schemaVersion: '1.0'
    });

    expect(result.confidence).toBeGreaterThan(0.8);
    expect(result.templateUsed).toBe('devops-ci-cd');
    expect(result.intent.steps).toHaveLength(2);
  });

  test('should handle validation failures gracefully', async () => {
    // Create intentionally invalid Petri net
    const invalidPetriNet = {
      id: 'invalid',
      places: [],
      transitions: [{ id: 't1', name: 'Isolated' }],
      arcs: []
    };

    const result = await client.validate({
      petriNet: invalidPetriNet,
      schemaVersion: '1.0'
    });

    expect(result.report.status).toBe('FAIL');
    expect(result.report.hints).toBeDefined();
  });
});
```

### Load Testing

```bash
# Artillery.js load test configuration
# petri-load-test.yml
config:
  target: 'http://localhost:8080'
  phases:
    - duration: 60
      arrivalRate: 10
  defaults:
    headers:
      Authorization: 'Bearer test-token'
      Content-Type: 'application/json'

scenarios:
  - name: 'Parse and Build Workflow'
    weight: 100
    flow:
      - post:
          url: '/api/v1/petri/parse'
          json:
            text: 'run tests; if pass deploy; if fail alert'
            templateHint: 'devops'
            schemaVersion: '1.0'
          capture:
            json: '$.intent'
            as: 'intent'
      - post:
          url: '/api/v1/petri/build'
          json:
            intent: '{{ intent }}'
            schemaVersion: '1.0'

# Run: artillery run petri-load-test.yml
```

## 📋 Postman Collection

Import our comprehensive Postman collection for interactive API testing:

**Collection URL:** `https://api.obvian.com/docs/postman/petri-net-collection.json`

**Pre-request Scripts:**
```javascript
// Set base URL and auth token
pm.globals.set('baseURL', 'http://localhost:8080');
pm.globals.set('authToken', 'your-jwt-token-here');

// Generate request ID for tracing
pm.globals.set('requestId', 'req-' + Date.now());
```

**Test Scripts:**
```javascript
// Validate response schema
pm.test('Response has valid schema version', function () {
    const jsonData = pm.response.json();
    pm.expect(jsonData).to.have.property('schemaVersion');
    pm.expect(jsonData.schemaVersion).to.equal('1.0');
});

// Performance test
pm.test('Response time is less than 5 seconds', function () {
    pm.expect(pm.response.responseTime).to.be.below(5000);
});

// Store results for next request
if (pm.response.code === 200) {
    const jsonData = pm.response.json();
    if (jsonData.petriNet) {
        pm.globals.set('petriNet', JSON.stringify(jsonData.petriNet));
    }
}
```

---

## 🎯 Next Steps

1. **Try the Interactive Documentation**: Visit `/swagger-ui.html` for hands-on API exploration
2. **Download SDKs**: Get language-specific client libraries from our GitHub releases
3. **Join the Community**: Connect with other developers in our Discord server
4. **Read Advanced Guides**: Explore performance optimization and scaling best practices

For additional support, visit our [developer portal](https://obvian.com/developers) or reach out to our team at api-support@obvian.com.