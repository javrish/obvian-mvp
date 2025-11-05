# Technical Architecture: Petri Net DAG System

## System Overview

The Petri Net DAG system is architected as a modern web application with a clear separation between frontend presentation, backend API services, and core engine logic. The architecture prioritizes modularity, testability, and clear data flow while maintaining the formal rigor required for mathematical workflow validation.

## High-Level Architecture

```mermaid
graph TB
    subgraph "Client Tier"
        Browser[Web Browser]
        UI[React + Vite Frontend]
    end
    
    subgraph "API Tier"
        LB[Load Balancer]
        API[Spring Boot API]
        Swagger[OpenAPI Documentation]
    end
    
    subgraph "Engine Tier"
        Core[Engine Core]
        NLP[NLP Templates]
        Validation[Validation Engine]
        Simulation[Simulation Engine]
    end
    
    subgraph "Data Tier"
        Memory[In-Memory Storage]
        Export[File Export]
        Logs[Trace Logs]
    end
    
    Browser --> UI
    UI --> LB
    LB --> API
    API --> Swagger
    API --> Core
    Core --> NLP
    Core --> Validation
    Core --> Simulation
    Core --> Memory
    Core --> Export
    Simulation --> Logs
    
    style UI fill:#e3f2fd
    style API fill:#f3e5f5
    style Core fill:#e8f5e8
    style Memory fill:#fff3e0
```

## Component Architecture

### Frontend Components (React + Vite)

```mermaid
graph TB
    subgraph "React Application"
        App[App Component]
        
        subgraph "Input Components"
            PromptInput[Prompt Input]
            TemplateSelector[Template Selector]
            ErrorDisplay[Error Display]
        end
        
        subgraph "Visualization Components"
            DualView[Dual View Container]
            PetriCanvas[Petri Net Canvas]
            DagCanvas[DAG Canvas]
            CrossHighlight[Cross Highlight Manager]
        end
        
        subgraph "Control Components"
            ValidationBanner[Validation Banner]
            SimControls[Simulation Controls]
            TraceViewer[Trace Viewer]
            ExportPanel[Export Panel]
        end
        
        subgraph "Services"
            ApiClient[API Client]
            StateManager[State Manager]
            EventBus[Event Bus]
        end
    end
    
    App --> PromptInput
    App --> DualView
    App --> ValidationBanner
    App --> SimControls
    
    DualView --> PetriCanvas
    DualView --> DagCanvas
    DualView --> CrossHighlight
    
    PromptInput --> ApiClient
    SimControls --> ApiClient
    ApiClient --> StateManager
    StateManager --> EventBus
    
    style App fill:#e3f2fd
    style ApiClient fill:#f3e5f5
    style StateManager fill:#e8f5e8
```

### Backend Services (Spring Boot)

```mermaid
graph TB
    subgraph "Spring Boot Application"
        subgraph "Web Layer"
            ParseController[Parse Controller]
            BuildController[Build Controller]
            ValidateController[Validate Controller]
            SimulateController[Simulate Controller]
            DagController[DAG Controller]
        end
        
        subgraph "Service Layer"
            WorkflowService[Workflow Service]
            ValidationService[Validation Service]
            SimulationService[Simulation Service]
            ExportService[Export Service]
        end
        
        subgraph "Integration Layer"
            EngineAdapter[Engine Adapter]
            ErrorHandler[Global Error Handler]
            SchemaValidator[Schema Validator]
        end
    end
    
    ParseController --> WorkflowService
    BuildController --> WorkflowService
    ValidateController --> ValidationService
    SimulateController --> SimulationService
    DagController --> WorkflowService
    
    WorkflowService --> EngineAdapter
    ValidationService --> EngineAdapter
    SimulationService --> EngineAdapter
    
    EngineAdapter --> ErrorHandler
    EngineAdapter --> SchemaValidator
    
    style ParseController fill:#e3f2fd
    style WorkflowService fill:#f3e5f5
    style EngineAdapter fill:#e8f5e8
```

### Core Engine Architecture

```mermaid
graph TB
    subgraph "Engine Core"
        subgraph "Data Models"
            PetriNet[Petri Net Model]
            IntentSpec[Intent Spec Model]
            ValidationReport[Validation Report]
            TraceEvent[Trace Event]
            DAG[DAG Model]
        end
        
        subgraph "Processing Engines"
            TemplateParser[Template Parser]
            PetriBuilder[Petri Net Builder]
            Validator[Formal Validator]
            DagProjector[DAG Projector]
            TokenSimulator[Token Simulator]
        end
        
        subgraph "Algorithms"
            DeadlockDetector[Deadlock Detector]
            ReachabilityAnalyzer[Reachability Analyzer]
            LivenessChecker[Liveness Checker]
            BoundednessChecker[Boundedness Checker]
        end
        
        subgraph "Utilities"
            MarkingUtils[Marking Utils]
            GraphUtils[Graph Utils]
            ExportUtils[Export Utils]
        end
    end
    
    TemplateParser --> IntentSpec
    PetriBuilder --> PetriNet
    Validator --> ValidationReport
    TokenSimulator --> TraceEvent
    DagProjector --> DAG
    
    Validator --> DeadlockDetector
    Validator --> ReachabilityAnalyzer
    Validator --> LivenessChecker
    Validator --> BoundednessChecker
    
    PetriBuilder --> MarkingUtils
    DagProjector --> GraphUtils
    TokenSimulator --> ExportUtils
    
    style PetriNet fill:#e3f2fd
    style Validator fill:#f3e5f5
    style DeadlockDetector fill:#e8f5e8
```

## Data Models and Schemas

### Core Data Structures

#### Petri Net Model
```java
public class PetriNet {
    private String netId;
    private String name;
    private List<Place> places;
    private List<Transition> transitions;
    private List<Arc> arcs;
    private Marking initialMarking;
    private String schemaVersion;
    
    // Derived properties
    public Set<Transition> getEnabledTransitions(Marking marking);
    public Marking fire(Transition transition, Marking marking);
    public boolean isTerminal(Marking marking);
}

public class Place {
    private String id;
    private String name;
    private Integer capacity; // null = unbounded
    private Map<String, Object> metadata;
}

public class Transition {
    private String id;
    private String name;
    private Map<String, Object> guards;
    private Map<String, Object> metadata;
}

public class Arc {
    private String fromId;
    private String toId;
    private int weight;
    private ArcType type; // PLACE_TO_TRANSITION, TRANSITION_TO_PLACE
}
```

#### Intent Specification
```java
public class IntentSpec {
    private String name;
    private String description;
    private List<IntentStep> steps;
    private Map<String, Object> metadata;
    private String schemaVersion;
}

public class IntentStep {
    private String id;
    private StepType type; // ACTION, CHOICE, PARALLEL, SYNC
    private String description;
    private List<String> dependencies;
    private Map<String, Object> conditions;
    private Map<String, Object> metadata;
}
```

#### Validation Report
```java
public class ValidationReport {
    private ValidationStatus status;
    private Map<String, CheckResult> checks;
    private Optional<CounterExample> counterExample;
    private List<String> hints;
    private ValidationConfig config;
    private ValidationMetrics metrics;
    private String schemaVersion;
}

public class CheckResult {
    private String checkName;
    private CheckStatus status;
    private String description;
    private Map<String, Object> details;
    private Optional<String> witness;
}

public class CounterExample {
    private Marking marking;
    private Set<String> enabledTransitions;
    private List<String> pathToReach;
    private String explanation;
}
```

### JSON Schema Definitions

#### IntentSpec Schema
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "schemaVersion": {"type": "string", "const": "1.0"},
    "name": {"type": "string"},
    "description": {"type": "string"},
    "steps": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "id": {"type": "string"},
          "type": {"enum": ["action", "choice", "parallel", "sync"]},
          "description": {"type": "string"},
          "dependencies": {"type": "array", "items": {"type": "string"}},
          "conditions": {"type": "object"},
          "metadata": {"type": "object"}
        },
        "required": ["id", "type"]
      }
    }
  },
  "required": ["schemaVersion", "name", "steps"]
}
```

#### PetriNet Schema
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "properties": {
    "schemaVersion": {"type": "string", "const": "1.0"},
    "netId": {"type": "string"},
    "name": {"type": "string"},
    "places": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "id": {"type": "string"},
          "name": {"type": "string"},
          "capacity": {"type": ["integer", "null"], "minimum": 1}
        },
        "required": ["id"]
      }
    },
    "transitions": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "id": {"type": "string"},
          "name": {"type": "string"}
        },
        "required": ["id"]
      }
    },
    "arcs": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "fromId": {"type": "string"},
          "toId": {"type": "string"},
          "weight": {"type": "integer", "minimum": 1}
        },
        "required": ["fromId", "toId", "weight"]
      }
    },
    "initialMarking": {
      "type": "object",
      "patternProperties": {
        "^p_": {"type": "integer", "minimum": 0}
      }
    }
  },
  "required": ["schemaVersion", "netId", "places", "transitions", "arcs", "initialMarking"]
}
```

## Sequence Diagrams

### Complete Workflow Processing

```mermaid
sequenceDiagram
    participant User
    participant UI as Frontend
    participant API as Spring Boot
    participant Parser as Template Parser
    participant Builder as Petri Builder
    participant Validator as Formal Validator
    participant Projector as DAG Projector
    participant Simulator as Token Simulator
    
    User->>UI: Enter "Every time I push code: run tests..."
    UI->>API: POST /parse {"text": "..."}
    API->>Parser: parse(text)
    Parser->>Parser: Match DevOps template
    Parser-->>API: IntentSpec
    API-->>UI: {"intent": {...}}
    
    UI->>API: POST /build {"intent": {...}}
    API->>Builder: build(intentSpec)
    Builder->>Builder: Apply mapping rules
    Builder-->>API: PetriNet
    API-->>UI: {"petriNet": {...}}
    
    UI->>API: POST /validate {"petriNet": {...}}
    API->>Validator: validate(petriNet, config)
    Validator->>Validator: Deadlock detection
    Validator->>Validator: Reachability analysis
    Validator->>Validator: Liveness checking
    Validator-->>API: ValidationReport
    API-->>UI: {"report": {...}}
    
    UI->>API: POST /dag {"petriNet": {...}}
    API->>Projector: project(petriNet)
    Projector->>Projector: Build causality graph
    Projector->>Projector: Transitive reduction
    Projector-->>API: DAG
    API-->>UI: {"dag": {...}}
    
    User->>UI: Click "Simulate"
    UI->>API: POST /simulate {"petriNet": {...}, "config": {...}}
    API->>Simulator: simulate(petriNet, config)
    Simulator->>Simulator: Initialize tokens
    loop For each simulation step
        Simulator->>Simulator: Find enabled transitions
        Simulator->>Simulator: Select transition (deterministic)
        Simulator->>Simulator: Fire transition
        Simulator->>Simulator: Generate trace event
    end
    Simulator-->>API: SimulationResult
    API-->>UI: {"trace": [...], "finalMarking": {...}}
    
    UI->>UI: Animate tokens
    UI->>UI: Update trace viewer
```

### Error Handling Flow

```mermaid
sequenceDiagram
    participant UI as Frontend
    participant API as Spring Boot
    participant Engine as Core Engine
    participant ErrorHandler as Global Error Handler
    
    UI->>API: POST /parse {"text": "unsupported pattern"}
    API->>Engine: parse(text)
    Engine-->>API: ParseException("Unrecognized pattern")
    API->>ErrorHandler: handle(ParseException)
    ErrorHandler->>ErrorHandler: Map to HTTP 400
    ErrorHandler->>ErrorHandler: Create error response
    ErrorHandler-->>API: ErrorResponse
    API-->>UI: 400 {"error": {"code": "PARSE_FAILED", ...}}
    UI->>UI: Display error with suggestions
    
    UI->>API: POST /validate {"petriNet": {...}}
    API->>Engine: validate(petriNet)
    Engine->>Engine: Reachability analysis
    Engine->>Engine: Hit bound limit (k=200)
    Engine-->>API: ValidationReport(INCONCLUSIVE)
    API-->>UI: 422 {"report": {"status": "INCONCLUSIVE", ...}}
    UI->>UI: Show inconclusive banner with explanation
```

## Technology Stack

### Frontend Stack
- **Framework**: React 18 with TypeScript
- **Build Tool**: Vite for fast development and optimized builds
- **Graph Visualization**: Cytoscape.js for interactive network diagrams
- **State Management**: React Context + useReducer for application state
- **HTTP Client**: Axios with request/response interceptors
- **Styling**: CSS Modules with SCSS for component styling
- **Testing**: Vitest + React Testing Library for unit/integration tests

### Backend Stack
- **Framework**: Spring Boot 3.2 with Java 17
- **Web Layer**: Spring MVC with embedded Tomcat
- **Validation**: Bean Validation (JSR-303) with custom validators
- **Documentation**: OpenAPI 3 with Swagger UI
- **Testing**: JUnit 5 + Mockito + TestContainers for integration tests
- **Build Tool**: Maven with multi-module structure
- **Logging**: SLF4J with Logback for structured logging

### Core Engine Stack
- **Language**: Java 17 with modern language features
- **Collections**: Eclipse Collections for high-performance data structures
- **Algorithms**: Custom implementations optimized for Petri net operations
- **Serialization**: Jackson for JSON processing with custom serializers
- **Validation**: Custom formal verification algorithms
- **Testing**: Property-based testing with jqwik for algorithm verification

## Deployment Architecture

### Local Development

```mermaid
graph LR
    subgraph "Developer Machine"
        IDE[IDE/Editor]
        Backend[Spring Boot :8080]
        Frontend[Vite Dev Server :5173]
        Browser[Browser]
    end
    
    IDE --> Backend
    IDE --> Frontend
    Frontend --> Backend
    Browser --> Frontend
    
    style Backend fill:#f3e5f5
    style Frontend fill:#e3f2fd
```

### Docker Deployment

```mermaid
graph TB
    subgraph "Docker Environment"
        subgraph "Frontend Container"
            Nginx[Nginx + Static Files]
        end
        
        subgraph "Backend Container"
            SpringBoot[Spring Boot Application]
            JVM[OpenJDK 17]
        end
        
        subgraph "Shared Volumes"
            Exports[Export Files]
            Logs[Application Logs]
        end
    end
    
    Nginx --> SpringBoot
    SpringBoot --> JVM
    SpringBoot --> Exports
    SpringBoot --> Logs
    
    style Nginx fill:#e3f2fd
    style SpringBoot fill:#f3e5f5
    style JVM fill:#e8f5e8
```

## Security Architecture

### POC Security Model
- **Authentication**: None (POC warning displayed prominently)
- **Authorization**: Single-user, local deployment only
- **Input Validation**: Strict schema validation for all API inputs
- **Output Sanitization**: No user-generated content in responses
- **Data Privacy**: No PII collection, local-only trace storage

### Security Headers
```http
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Content-Security-Policy: default-src 'self'
POC-Warning: This is a proof-of-concept. Do not expose publicly.
```

## Performance Architecture

### Frontend Performance
- **Code Splitting**: Lazy loading of visualization components
- **Memoization**: React.memo for expensive graph rendering
- **Virtual Scrolling**: For large trace event lists
- **Debounced Input**: Prevent excessive API calls during typing
- **Canvas Optimization**: Efficient redraw strategies for token animation

### Backend Performance
- **Connection Pooling**: Optimized for single-user load
- **Caching**: In-memory caching of validation results
- **Async Processing**: Non-blocking simulation execution
- **Resource Limits**: Bounded algorithms prevent resource exhaustion
- **Monitoring**: JVM metrics and response time tracking

### Algorithm Performance
- **Bounded Exploration**: Configurable limits prevent infinite loops
- **Early Termination**: Stop validation on first failure/success
- **Efficient Data Structures**: Optimized for graph operations
- **Memory Management**: Careful object lifecycle management
- **Profiling**: Built-in performance metrics collection

## Monitoring and Observability

### Application Metrics
- **Response Times**: P50, P95, P99 for all API endpoints
- **Error Rates**: 4xx and 5xx response tracking
- **Validation Performance**: States explored, time taken
- **Simulation Metrics**: Steps executed, trace size
- **Memory Usage**: JVM heap and garbage collection metrics

### Logging Strategy
```java
// Structured logging with correlation IDs
log.info("Validation started", 
    kv("netId", petriNet.getNetId()),
    kv("kBound", config.getKBound()),
    kv("correlationId", correlationId));

log.info("Validation completed",
    kv("netId", petriNet.getNetId()),
    kv("status", report.getStatus()),
    kv("statesExplored", report.getMetrics().getStatesExplored()),
    kv("durationMs", duration),
    kv("correlationId", correlationId));
```

### Health Checks
- **API Health**: Basic connectivity and response validation
- **Engine Health**: Core algorithm functionality verification
- **Memory Health**: JVM memory usage and GC pressure
- **Performance Health**: Response time degradation detection

## Testing Architecture

### Test Pyramid

```mermaid
graph TB
    subgraph "Test Pyramid"
        E2E[End-to-End Tests<br/>Cypress/Playwright]
        Integration[Integration Tests<br/>TestContainers + MockMvc]
        Unit[Unit Tests<br/>JUnit 5 + Mockito]
        Property[Property Tests<br/>jqwik for algorithms]
    end
    
    E2E --> Integration
    Integration --> Unit
    Unit --> Property
    
    style E2E fill:#ffebee
    style Integration fill:#f3e5f5
    style Unit fill:#e8f5e8
    style Property fill:#e3f2fd
```

### Test Data Management
- **Golden Files**: Reference outputs for validation reports and traces
- **Test Fixtures**: Reusable Petri net and IntentSpec objects
- **Random Generation**: Property-based testing with controlled randomness
- **Snapshot Testing**: Automated comparison of complex output structures

## Future Architecture Considerations

### Scalability Enhancements
- **Microservices**: Split parsing, validation, and simulation into separate services
- **Event Sourcing**: Track all workflow modifications and validations
- **CQRS**: Separate read/write models for complex workflow queries
- **Caching Layer**: Redis for shared validation results and user sessions

### Advanced Features
- **Plugin Architecture**: Dynamic loading of domain-specific templates
- **Workflow Versioning**: Track changes and enable rollback capabilities
- **Collaborative Editing**: Real-time multi-user workflow design
- **AI Integration**: LLM-powered natural language understanding

### Production Readiness
- **Authentication**: OAuth2/OIDC integration with role-based access
- **Multi-tenancy**: Isolated workspaces with resource quotas
- **Audit Logging**: Comprehensive tracking of all user actions
- **Backup/Recovery**: Persistent storage with automated backups

This architecture provides a solid foundation for the POC while maintaining clear extension points for future production deployment and feature enhancement.