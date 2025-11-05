# SYSTEM MAP - Obvian Architecture Analysis

**Generated:** 2025-11-02
**Model:** Claude Sonnet 4.5
**Scope:** Production-grade DAG execution engine with Petri-net formal verification

---

## Executive Summary

Obvian is a **production-grade orchestration platform** combining:
- Natural language workflow parsing
- Petri-net formal verification (deadlock detection, reachability, liveness)
- DAG-based task execution with plugin architecture
- Memory-aware execution context
- Real-time WebSocket monitoring

**Production Readiness Assessment:** **7/10**
- Strong: Formal verification, plugin architecture, observability
- Weak: Execution engine complexity, error handling gaps, concurrency safety

---

## 1. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         API Layer (Spring Boot)                      │
│  ┌─────────────────┐  ┌──────────────────┐  ┌──────────────────┐   │
│  │ PetriController │  │ DagController    │  │ PromptController │   │
│  │ (Line 36)       │  │                  │  │                  │   │
│  └────────┬────────┘  └────────┬─────────┘  └────────┬─────────┘   │
│           │                    │                      │              │
└───────────┼────────────────────┼──────────────────────┼──────────────┘
            │                    │                      │
            ▼                    ▼                      ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Service Layer                                 │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │ DagExecutionService (api/service/DagExecutionService.java)│       │
│  │  - Orchestrates execution workflow                        │       │
│  │  - Manages async execution queue                          │       │
│  │  - Integrates with realtime/explainability services       │       │
│  │  Lines: 35-760                                            │       │
│  └──────────────────┬──────────────────┬────────────────────┘       │
│                     │                  │                             │
└─────────────────────┼──────────────────┼─────────────────────────────┘
                      │                  │
                      ▼                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                          Core Engine                                 │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ StandardDagExecutorService (core/impl/StandardDagExecutorService)│
│  │  - Topological execution with parallelism                    │  │
│  │  - Optimized thread pool (core/max: cores/2 to cores)       │  │
│  │  - Metrics tracking (nodeExecutionTimes, totalExecutionTime) │  │
│  │  - Execution traces for observability                        │  │
│  │  Lines: 29-661 | Key methods:                                │  │
│  │    - execute() L106: Main sync execution                     │  │
│  │    - executeAsync() L157: Async wrapper                      │  │
│  │    - executeInternal() L342: Level-based parallel exec       │  │
│  │    - buildExecutionLevels() L512: Topological sort           │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ PromptParser (core/PromptParser.java)                         │  │
│  │  - Regex-based NL parsing (EMAIL_PATTERN, FILE_CREATE_PATTERN)│  │
│  │  - Memory-aware resolution (LAST_FILE_PATTERN L68-72)        │  │
│  │  - Compound prompt support (CONJUNCTION_PATTERN L56-59)      │  │
│  │  Lines: 22-647 | Patterns: 12 regex rules                   │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ DagBuilder (core/DagBuilder.java)                             │  │
│  │  - Converts ParsedIntent → DAG with TaskNodes                │  │
│  │  - Dependency detection (hasImplicitDataDependency L218-271) │  │
│  │  - Token substitution (${dependency.task_N.filename} L365)   │  │
│  │  - Memory-aware param resolution (L84-109)                   │  │
│  │  Lines: 9-776 | Key: ACTION_TO_PLUGIN map L22-31            │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ TaskNode (core/TaskNode.java)                                 │  │
│  │  - Node definition: id, action, dependencies, inputParams    │  │
│  │  - Retry config: maxRetries, retryDelayMs, backoffMultiplier │  │
│  │  - Lifecycle hooks: beforeHook, afterHook (Consumer<Context>)│  │
│  │  - Metadata for CADR insights, temporal constraints          │  │
│  │  Lines: 15-285 | Constructor L76-116 with retry config      │  │
│  └──────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                       Petri-Net Subsystem                            │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ PetriNetValidator (core/petri/validation/PetriNetValidator)   │  │
│  │  - Formal verification with bounded state exploration        │  │
│  │  - Deadlock detection (BFS/DFS, L219-288)                    │  │
│  │  - Reachability analysis (terminal state detection L290-350) │  │
│  │  - Liveness check (L352-417): dead transition detection      │  │
│  │  - Boundedness check (L419-483): unbounded place detection   │  │
│  │  Lines: 38-984 | Config: k-bound=200, timeout=30s (L43-44)  │  │
│  │  Key: StateSpaceExplorer L532-859 implements verification    │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ AutomationGrammar (core/petri/grammar)                        │  │
│  │  - Transforms PetriIntentSpec → PetriNet                     │  │
│  │  - Used by PetriController.buildPetriNet() L173              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ PetriToDagProjector (core/petri/projection)                   │  │
│  │  - Projects PetriNet → DAG using transitive reduction        │  │
│  │  - Used by PetriController.projectToDAG() L228               │  │
│  │  - Preserves cross-highlighting metadata for UI              │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ PetriTokenSimulator (core/petri/simulation)                   │  │
│  │  - Token flow simulation with step-by-step tracing           │  │
│  │  - Modes: DETERMINISTIC, RANDOM                              │  │
│  │  - SimulationResult with trace events for visualization      │  │
│  └──────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                         Plugin System                                │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ PluginRouter (interface: plugins/PluginRouter.java)           │  │
│  │  - Dynamic plugin registration & execution                    │  │
│  │  - Health checks (performHealthCheck, performAllHealthChecks) │  │
│  │  - Hot-reload support (loadPluginFromJar, supportsHotReload) │  │
│  │  Lines: 11-87                                                │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  │ Built-in Plugins (mapped in DagBuilder L22-31):              │  │
│  │  - EmailPlugin (send_email)                                  │  │
│  │  - FilePlugin (create_file)                                  │  │
│  │  - SlackPlugin (send_slack)                                  │  │
│  │  - ReminderPlugin (set_reminder)                             │  │
│  │  - LLMPlugin (analyze_text, generate_text, consciousness)    │  │
│  │  - EchoPlugin (generic fallback)                             │  │
└──────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                   State & Concurrency Management                     │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ ExecutionContext (core/ExecutionContext.java)                 │  │
│  │  - ConcurrentHashMap for variables & metadata                │  │
│  │  - MemoryStore reference for cross-execution state           │  │
│  │  - ExecutionId generation (UUID-based)                       │  │
│  │  Lines: 14-106 | Thread-safe: Yes (ConcurrentHashMap)       │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Thread Pools (StandardDagExecutorService L79-106)             │  │
│  │  - executorService: Bounded ThreadPoolExecutor               │  │
│  │    - Core: cores/2, Max: cores                               │  │
│  │    - Queue: LinkedBlockingQueue(100)                         │  │
│  │    - Rejection: CallerRunsPolicy (backpressure)              │  │
│  │  - parallelExecutor: ForkJoinPool.commonPool()               │  │
│  │  - scheduledExecutor: 2 daemon threads for cleanup           │  │
│  │  Backpressure: ✓ CallerRunsPolicy (L502)                    │  │
│  │  Cleanup: Periodic stale execution removal (L606-624)        │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Execution State (StandardDagExecutorService.ExecutionState)   │  │
│  │  - activeExecutions: ConcurrentHashMap                       │  │
│  │  - runningExecutions: ConcurrentHashMap<Future>              │  │
│  │  - executionTraces: ConcurrentHashMap                        │  │
│  │  - Cancellation: volatile boolean flag (L635)                │  │
│  │  Lines: 629-660 | Thread-safe: Partial (volatile progress)  │  │
│  └──────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    Observability & Resilience                        │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ ExecutionTrace (core/explainability/ExecutionTrace)           │  │
│  │  - Decision points with causal reasoning                     │  │
│  │  - DecisionType: EXECUTION_START, NODE_SUCCESS, NODE_FAILURE │  │
│  │  - Stored per execution in executionTraces map               │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Metrics (StandardDagExecutorService L276-309)                 │  │
│  │  - totalExecutions, successfulExecutions, failedExecutions   │  │
│  │  - activeExecutions, totalNodesExecuted                      │  │
│  │  - nodeExecutionTimes: Map<String, Long> (L47)               │  │
│  │  - Top 5 slowest nodes tracking (L297-306)                   │  │
│  │  - Thread pool metrics (pool size, active, queue)            │  │
│  │  - Memory usage (MB) via MemoryMXBean                        │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Error Handling                                                │  │
│  │  - DagExecutionException (L121-150): typed errors            │  │
│  │  - ErrorCode: VALIDATION_FAILED, PLUGIN_EXECUTION_FAILED,    │  │
│  │               CANCELLED, UNKNOWN                              │  │
│  │  - Retry logic: Per-node retry config (TaskNode L39-48)      │  │
│  │  - Fallback: fallbackPluginId (TaskNode L52)                 │  │
│  │  - Circuit breakers: NOT IMPLEMENTED                         │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │ Timeouts                                                      │  │
│  │  - Global timeout: request.getTimeoutMs() (DagExecutionService)│
│  │  - Per-node timeout: NOT ENFORCED in StandardDagExecutorService│
│  │  - Validation timeout: 30s (PetriNetValidator L44)           │  │
│  │  WARNING: No execution timeout enforcement in executor       │  │
│  └──────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 2. Key Classes & Responsibilities

### Core Execution Path

| Class | Path | Purpose | Lines | Runtime Characteristics |
|-------|------|---------|-------|------------------------|
| **PromptParser** | `core/PromptParser.java` | NL → ParsedIntent via regex | 22-647 | **Stateless**, CPU-bound (regex matching) |
| **DagBuilder** | `core/DagBuilder.java` | ParsedIntent → DAG | 9-776 | **Stateless**, builds object graph in-memory |
| **StandardDagExecutorService** | `core/impl/StandardDagExecutorService.java` | DAG execution orchestrator | 29-661 | **Stateful** (activeExecutions), **Multi-threaded** |
| **PluginRouter** | `plugins/PluginRouter.java` | Plugin dispatch | 11-87 | **Stateless** (interface), implementations may vary |
| **ExecutionContext** | `core/ExecutionContext.java` | Execution state container | 14-106 | **Thread-safe** (ConcurrentHashMap) |

### Petri-Net Verification

| Class | Path | Purpose | Lines | Algorithm |
|-------|------|---------|-------|-----------|
| **PetriNetValidator** | `core/petri/validation/PetriNetValidator.java` | Formal verification | 38-984 | BFS state exploration, k-bound=200 |
| **StateSpaceExplorer** | `core/petri/validation/PetriNetValidator.java` | State graph traversal | 532-859 | BFS with visited set, timeout=30s |
| **AutomationGrammar** | `core/petri/grammar/AutomationGrammar.java` | Intent → PetriNet | N/A | Grammar-based transformation |
| **PetriToDagProjector** | `core/petri/projection/PetriToDagProjector.java` | PetriNet → DAG | N/A | Transitive reduction |

### Service Layer

| Class | Path | Purpose | Lines | Dependencies |
|-------|------|---------|-------|--------------|
| **DagExecutionService** | `api/service/DagExecutionService.java` | API orchestration | 35-760 | DagExecutorService, PluginRouter, MemoryStore, Redis |
| **PetriController** | `api/controller/PetriController.java` | REST endpoints | 36-1113 | PromptParser, AutomationGrammar, PetriNetValidator |

---

## 3. Data Flow: Prompt → Execution

```
┌─────────────────────────────────────────────────────────────────────┐
│ 1. Natural Language Input                                           │
│    "Create file report.txt with 'Status', then email it to team@co" │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 2. PromptParser.parseCompoundPrompt()                               │
│    Input: String prompt                                             │
│    Output: CompoundParseResult {                                    │
│      intents: [                                                     │
│        ParsedIntent(action="create_file",                           │
│                    params={filename="report.txt", content="Status"}),│
│        ParsedIntent(action="send_email",                            │
│                    params={recipient="team@co", memoryRef="last_file"})│
│      ],                                                             │
│      isCompound: true                                               │
│    }                                                                │
│    Logic:                                                           │
│      - Regex pattern matching (FILE_CREATE_PATTERN L45-48)         │
│      - Conjunction detection (CONJUNCTION_PATTERN L56-59)           │
│      - Memory reference detection (detectMemoryReference L275-292)  │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 3. DagBuilder.buildFromCompoundPrompt()                             │
│    Input: CompoundParseResult                                       │
│    Output: DAG {                                                    │
│      nodes: [                                                       │
│        TaskNode(id="task_0", action="FilePlugin",                   │
│                inputParams={filename="report.txt", content="Status"},│
│                dependencies=[]),                                    │
│        TaskNode(id="task_1", action="EmailPlugin",                  │
│                inputParams={recipient="team@co",                    │
│                           attachment="${dependency.task_0.filename}",│
│                           body="${dependency.task_0.fileContent}"},  │
│                dependencies=["task_0"])                             │
│      ],                                                             │
│      rootNode: task_0                                               │
│    }                                                                │
│    Logic:                                                           │
│      - Dependency detection (detectDependencies L179-210)           │
│      - Token substitution (createTaskNodeForActionWithTokens L338)  │
│      - Memory resolution (resolveMemoryReference L642-673)          │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 4. StandardDagExecutorService.execute()                             │
│    Input: DAG, ExecutionContext                                     │
│    Logic:                                                           │
│      a) Validate DAG (hasCycles L447-479)                           │
│      b) Build execution levels (buildExecutionLevels L512-538)      │
│         - Topological sort                                          │
│         - Level 0: [task_0]  (no dependencies)                      │
│         - Level 1: [task_1]  (depends on task_0)                    │
│      c) Execute nodes level-by-level:                               │
│         - Sequential if 1 node per level                            │
│         - Parallel if >1 nodes (CompletableFuture + ForkJoinPool)   │
│      d) For each node:                                              │
│         - executeNodeWithMetrics() L543-585:                        │
│           - Start trace (ExecutionTrace.addDecision L392)           │
│           - Call PluginRouter.executeAction() L406                  │
│           - Track execution time (nodeExecutionTimes L418)          │
│           - Record NodeExecutionResult L554-563                     │
│      e) Update progress callback after each level L370-372          │
│      f) Store result in MemoryStore L137                            │
│    Output: DagExecutionResult {                                     │
│      success: true,                                                 │
│      executionId: "exec-1730553600-42",                             │
│      results: {                                                     │
│        "task_0": NodeExecutionResult(status=SUCCESS, data="report.txt"),│
│        "task_1": NodeExecutionResult(status=SUCCESS, data="email_sent") │
│      }                                                              │
│    }                                                                │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 5. PluginRouter.executeAction() (per node)                          │
│    Example for task_0 (FilePlugin):                                 │
│      Input: action="FilePlugin", context={filename="report.txt", ...}│
│      Process:                                                       │
│        - Resolve plugin from registry                               │
│        - Execute plugin.execute(context)                            │
│        - Return ExecutionResult                                     │
│      Output: ExecutionResult {                                      │
│        success: true,                                               │
│        data: {filename: "report.txt", content: "Status"}            │
│      }                                                              │
│                                                                      │
│    Example for task_1 (EmailPlugin):                                │
│      Input: action="EmailPlugin", context={recipient="team@co",     │
│              attachment="${dependency.task_0.filename}"}            │
│      Process:                                                       │
│        - Token substitution (not shown in code - gap!)              │
│        - Execute plugin.execute(context)                            │
│      Output: ExecutionResult {success: true, data: "email_sent"}    │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 6. Response Assembly (DagExecutionService L467)                     │
│    - Convert DagExecutionResult → DagExecutionResponse              │
│    - Attach ExecutionTrace if trace=true                            │
│    - Store in Redis for async retrieval                             │
│    - Broadcast updates via WebSocket (RealtimeExecutionService)     │
└─────────────────────────────────────────────────────────────────────┘
```

### Critical Observation: Token Substitution Gap

**Line 365 in DagBuilder** defines token patterns like `${dependency.task_0.filename}`, but **StandardDagExecutorService does NOT implement token resolution before plugin execution**. This is a **critical gap** - tokens are passed as-is to plugins, which likely don't handle them.

---

## 4. Runtime Characteristics

### Concurrency Model

| Component | Threading Model | Parallelism | Notes |
|-----------|----------------|-------------|-------|
| **PromptParser** | Single-threaded | None | Regex parsing (CPU-bound) |
| **DagBuilder** | Single-threaded | None | Object construction |
| **StandardDagExecutorService** | Multi-threaded | Level-based | Uses ForkJoinPool for parallel node execution within levels |
| **StateSpaceExplorer** | Single-threaded | None | BFS traversal (state exploration) |
| **PluginRouter** | Depends on impl | Potentially parallel | Interface doesn't specify threading contract |

#### Thread Pool Configuration (StandardDagExecutorService L488-507)

```java
ThreadPoolExecutor {
  corePoolSize: max(2, cores/2),
  maxPoolSize: max(4, cores),
  keepAliveTime: 60s,
  workQueue: LinkedBlockingQueue(100),
  rejectionPolicy: CallerRunsPolicy  // Backpressure: caller executes if queue full
}
```

**Backpressure:** ✓ Implemented via `CallerRunsPolicy`
**Thread Safety:** ✓ ConcurrentHashMap for state, volatile for cancellation
**Resource Cleanup:** ✓ Periodic cleanup every 5 minutes (L98)

### Execution Flow (Level-Based Parallelism)

```
DAG: task_0 → task_1
               ↘
                task_2

Level 0: [task_0]              → Sequential execution
Level 1: [task_1, task_2]      → Parallel execution (CompletableFuture.allOf)
```

**Code Reference:** `StandardDagExecutorService.executeInternal()` L350-372

```java
for (int level = 0; level < executionLevels.size(); level++) {
    List<TaskNode> nodesAtLevel = executionLevels.get(level);

    if (nodesAtLevel.size() == 1) {
        // Single node - execute synchronously
        executeNodeWithMetrics(...);
    } else {
        // Multiple nodes - execute in parallel
        List<CompletableFuture<Void>> futures = nodesAtLevel.stream()
            .map(node -> CompletableFuture.runAsync(..., parallelExecutor))
            .collect(Collectors.toList());
        CompletableFuture.allOf(futures.toArray(...)).join();
    }
}
```

---

## 5. Error Handling & Retry Logic

### Error Handling Strategy

| Layer | Mechanism | Retries | Fallback |
|-------|-----------|---------|----------|
| **API Layer** | Try-catch → DagExecutionResponse.failure() | No | N/A |
| **Service Layer** | DagExecutionException with error codes | No | Async queue re-submission (not shown) |
| **Executor Layer** | DagExecutionException propagation | **Per-node** (TaskNode.maxRetries) | TaskNode.fallbackPluginId |
| **Plugin Layer** | Plugin-specific exceptions | Plugin-defined | N/A |

### Retry Configuration (TaskNode L39-48)

```java
public class TaskNode {
    int maxRetries;           // Default: 0 (no retry)
    int retryDelayMs;         // Default: 1000ms
    double backoffMultiplier; // Default: 1.0 (constant delay)
    String fallbackPluginId;  // Alternative plugin on failure
}
```

**Problem:** Retry logic is **defined but NOT executed** in `StandardDagExecutorService`. No code in `executeNodeWithMetrics()` (L543-585) implements retry loops.

### Timeout Handling

| Component | Timeout | Enforcement |
|-----------|---------|-------------|
| **DAG Execution** | `request.getTimeoutMs()` | **NOT ENFORCED** (read but not used) |
| **Node Execution** | N/A | **NOT IMPLEMENTED** |
| **Petri Validation** | 30s | ✓ Enforced (L520: `isTimeoutReached()`) |
| **Thread Pool** | 60s keepAlive | ✓ Enforced (L493) |

**Critical Gap:** Global and per-node timeouts are **not enforced** in StandardDagExecutorService.

---

## 6. State Management

### Execution State Lifecycle

```
┌──────────────┐
│ DAG Created  │
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ ExecutionState created                   │
│  - executionId generated                 │
│  - Added to activeExecutions             │
│  - ExecutionTrace initialized            │
└──────┬───────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ Level-by-level execution                 │
│  - Progress tracked: completed/total     │
│  - NodeExecutionResults stored in map    │
│  - Trace events appended                 │
└──────┬───────────────────────────────────┘
       │
       ▼
┌──────────────────────────────────────────┐
│ Execution complete                       │
│  - Removed from activeExecutions         │
│  - Stored in MemoryStore                 │
│  - Trace kept in executionTraces (max 1000)│
└──────────────────────────────────────────┘
```

### Stateful Components

| Component | State Storage | Cleanup Strategy |
|-----------|--------------|------------------|
| **activeExecutions** | `ConcurrentHashMap<String, ExecutionState>` | Remove on completion |
| **executionTraces** | `ConcurrentHashMap<String, ExecutionTrace>` | Keep last 1000 (L616-623) |
| **runningExecutions** | `ConcurrentHashMap<String, Future>` (DagExecutionService) | Remove on completion |
| **MemoryStore** | Persistent (file-based or Redis) | Retention policy (not shown) |

**Memory Leak Risk:** `executionTraces` can grow unbounded until cleanup runs (every 5 minutes). If execution rate > 200/min, cleanup may lag.

---

## 7. Observability

### Metrics (StandardDagExecutorService.getMetrics() L278-309)

```json
{
  "total_executions": 1234,
  "successful_executions": 1180,
  "failed_executions": 52,
  "cancelled_executions": 2,
  "active_executions": 5,
  "cached_traces": 247,
  "is_healthy": true,
  "total_nodes_executed": 8432,
  "average_execution_time_ms": 124,
  "thread_pool_size": 8,
  "thread_pool_active": 3,
  "thread_pool_queue_size": 12,
  "memory_usage_mb": 512,
  "top_slow_nodes": {
    "LLMPlugin": 2340,
    "EmailPlugin": 890,
    "SlackPlugin": 670
  }
}
```

### Trace Logging

**ExecutionTrace** captures:
- Decision points (DecisionType: EXECUTION_START, NODE_SUCCESS, NODE_FAILURE)
- Node-level timing (L392-421)
- Causal relationships (not detailed in code)

**Storage:** In-memory (executionTraces map) + MemoryStore persistence

**Limitations:**
- No distributed tracing (no trace IDs propagated to plugins)
- No correlation with external systems (e.g., OpenTelemetry)

---

## 8. Resilience Analysis

### Circuit Breakers
**Status:** ❌ **NOT IMPLEMENTED**

No circuit breaker pattern found in:
- PluginRouter
- StandardDagExecutorService
- DagExecutionService

**Recommendation:** Implement per-plugin circuit breakers to prevent cascading failures.

### Rate Limiting
**Status:** ❌ **NOT IMPLEMENTED** in execution layer

Thread pool queue (100 items) acts as implicit rate limiter, but no explicit rate limiting per:
- User
- Plugin type
- DAG complexity

### Backpressure
**Status:** ✓ **IMPLEMENTED**

- Thread pool: `CallerRunsPolicy` forces caller to execute if queue full (L502)
- Execution queue: Uses Spring's async queue (ExecutionQueueService, details not shown)

### Failure Handling

| Failure Mode | Detection | Recovery |
|--------------|-----------|----------|
| **Plugin failure** | Exception in executeNode() L564-584 | Propagate as DagExecutionException |
| **Node timeout** | NOT DETECTED | N/A |
| **DAG cycle** | Validation (hasCycles L447) | Reject before execution |
| **Memory exhaustion** | JVM OOM | No graceful degradation |
| **Thread pool exhaustion** | CallerRunsPolicy | Caller blocks and executes |

**Critical Gaps:**
1. No timeout enforcement for stuck plugins
2. No memory-based backpressure (heap usage not checked)
3. No per-plugin failure quotas

---

## 9. Data Flow: Petri-Net Verification Path

```
┌─────────────────────────────────────────────────────────────────────┐
│ 1. PetriController.parseNaturalLanguage()                           │
│    POST /api/v1/petri/parse                                         │
│    Input: {"text": "Create workflow with 3 steps: ..."}            │
│    Output: PetriIntentSpec (intent specification)                   │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 2. PetriController.buildPetriNet()                                  │
│    POST /api/v1/petri/build                                         │
│    Logic: AutomationGrammar.transform(intentSpec) → PetriNet        │
│    Output: PetriNet {                                               │
│      places: [p_start, p_step1_done, p_step2_done, p_done],        │
│      transitions: [t_step1, t_step2, t_step3],                     │
│      arcs: [(p_start, t_step1), (t_step1, p_step1_done), ...],     │
│      initialMarking: {p_start: 1}                                   │
│    }                                                                │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 3. PetriController.validatePetriNet()                               │
│    POST /api/v1/petri/validate                                      │
│    Logic: PetriNetValidator.validate(petriNet, config)              │
│      a) Structural validation (L136-217):                           │
│         - Check places/transitions exist                            │
│         - Check arcs reference valid elements                       │
│         - Check arc weights > 0                                     │
│      b) Deadlock detection (L223-288):                              │
│         - BFS state exploration (StateSpaceExplorer L551-618)       │
│         - Check for non-terminal markings with no enabled transitions│
│      c) Reachability analysis (L295-350):                           │
│         - BFS to find terminal states (places with "p_done" prefix) │
│      d) Liveness check (L356-417):                                  │
│         - Track which transitions are enabled in any reachable marking│
│         - Report dead transitions (never enabled)                   │
│      e) Boundedness check (L423-483):                               │
│         - Track max tokens per place during exploration             │
│         - Detect unbounded growth (heuristic: >k-bound/10)          │
│    Output: PetriNetValidationResult {                               │
│      petriStatus: PASS | FAIL | INCONCLUSIVE_TIMEOUT | INCONCLUSIVE_BOUND,│
│      checks: [                                                      │
│        {type: DEADLOCK_DETECTION, status: PASS, message: "No deadlocks"},│
│        {type: REACHABILITY_ANALYSIS, status: PASS, statesExplored: 42}│
│      ],                                                             │
│      counterExample: null,  // or {failingMarking, pathToFailure}   │
│      hints: ["Add transitions to avoid deadlock at..."]             │
│    }                                                                │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 4. PetriController.simulatePetriNet()                               │
│    POST /api/v1/petri/simulate                                      │
│    Logic: PetriTokenSimulator.simulate(petriNet, config)            │
│      - Step-by-step token flow simulation                           │
│      - Generate trace events for visualization                      │
│    Output: SimulationResult {                                       │
│      success: true,                                                 │
│      status: COMPLETED,                                             │
│      trace: [                                                       │
│        {transitionId: "t_step1", markingBefore: {p_start: 1}, ...},│
│        {transitionId: "t_step2", markingBefore: {p_step1_done: 1}, ...}│
│      ]                                                              │
│    }                                                                │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 5. PetriController.projectToDAG()                                   │
│    POST /api/v1/petri/dag                                           │
│    Logic: PetriToDagProjector.projectToDAG(petriNet)                │
│      - Apply transitive reduction algorithm                         │
│      - Convert Petri-net structure to DAG (transitions → nodes)     │
│    Output: DAG {                                                    │
│      nodes: [task_0, task_1, task_2],  // from transitions         │
│      edges: [{from: task_0, to: task_1}, ...],                     │
│      derivedFromPetriNetId: "petri_123",                            │
│      metadata: {projectionAlgorithm: "transitive-reduction"}        │
│    }                                                                │
└──────────────────────┬──────────────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────────────┐
│ 6. Execute DAG (via StandardDagExecutorService)                     │
│    (same as standard execution flow from Section 3)                 │
└─────────────────────────────────────────────────────────────────────┘
```

### Petri-Net Validation Algorithm (StateSpaceExplorer)

**BFS Exploration (L551-618):**

```java
void detectDeadlocks() {
    Queue<Marking> toExplore = new LinkedList<>();
    Set<Marking> visited = new HashSet<>();

    toExplore.add(initialMarking);

    while (!toExplore.isEmpty() && !timeout && !boundReached) {
        Marking current = toExplore.poll();
        List<Transition> enabled = petriNet.getEnabledTransitions(current);

        // Deadlock check
        if (enabled.isEmpty() && !isTerminal(current)) {
            return new DeadlockDetectionResult(hasDeadlock=true, ...);
        }

        // Explore successors
        for (Transition t : enabled) {
            Marking successor = petriNet.fireTransition(t, current);
            if (!visited.contains(successor)) {
                visited.add(successor);
                toExplore.add(successor);
            }
        }
    }

    return new DeadlockDetectionResult(hasDeadlock=false, ...);
}
```

**Complexity:** O(|S| + |T|) where |S| = reachable states, |T| = transitions
**Bounds:** k-bound=200 states, timeout=30s
**Completeness:** Incomplete for large state spaces (returns INCONCLUSIVE)

---

## 10. Identified Weaknesses & Tech Debt

### Critical Issues (Production Blockers)

1. **Token Substitution Not Implemented** (Severity: HIGH)
   - **Location:** StandardDagExecutorService.executeNode() L390-422
   - **Problem:** Tokens like `${dependency.task_0.filename}` defined in DagBuilder L365 are NOT resolved before plugin execution
   - **Impact:** Inter-node data passing broken for complex workflows
   - **Fix:** Implement token resolution in executeNode() before plugin call

2. **Timeout Not Enforced** (Severity: HIGH)
   - **Location:** StandardDagExecutorService.execute() L107-154
   - **Problem:** `request.getTimeoutMs()` read but never enforced via `CompletableFuture.orTimeout()` or `Future.get(timeout)`
   - **Impact:** Runaway executions can hang indefinitely
   - **Fix:** Wrap execution in `CompletableFuture.orTimeout(timeoutMs)` (Java 9+)

3. **Retry Logic Not Executed** (Severity: MEDIUM)
   - **Location:** StandardDagExecutorService.executeNodeWithMetrics() L543-585
   - **Problem:** TaskNode.maxRetries defined but no retry loop implemented
   - **Impact:** Node failures not retried despite configuration
   - **Fix:** Add retry loop with exponential backoff in executeNode()

4. **No Circuit Breakers** (Severity: MEDIUM)
   - **Location:** PluginRouter implementations
   - **Problem:** Failing plugins can cascade failures across all executions
   - **Impact:** Reduced system resilience
   - **Fix:** Integrate Resilience4j circuit breakers per plugin

### Performance Concerns

5. **ExecutionTrace Memory Leak** (Severity: MEDIUM)
   - **Location:** StandardDagExecutorService L98, L616-623
   - **Problem:** executionTraces map cleaned every 5 min, but if rate > 200 exec/min, can OOM
   - **Impact:** Heap exhaustion on high throughput
   - **Fix:** Use bounded cache (e.g., Caffeine with max size)

6. **Blocking Join in Parallel Execution** (Severity: LOW)
   - **Location:** StandardDagExecutorService.executeInternal() L366
   - **Problem:** `CompletableFuture.allOf(...).join()` blocks executor thread
   - **Impact:** Reduced throughput under high concurrency
   - **Fix:** Use async continuation instead of join()

### Architectural Gaps

7. **No Distributed Tracing** (Severity: LOW)
   - **Problem:** ExecutionTrace not propagated to plugins as trace context
   - **Impact:** Hard to debug cross-service failures
   - **Fix:** Add OpenTelemetry instrumentation

8. **ExecutionContext Not Thread-Safe for Nested Execution** (Severity: MEDIUM)
   - **Location:** ExecutionContext L14-106
   - **Problem:** ConcurrentHashMap only, but no transaction semantics for multi-node updates
   - **Impact:** Race conditions if plugins mutate shared context
   - **Fix:** Add copy-on-write or versioning for context snapshots

9. **Petri-Net Validation Incompleteness** (Severity: LOW)
   - **Location:** PetriNetValidator L54-129
   - **Problem:** k-bound=200 may miss deadlocks in large workflows
   - **Impact:** False negatives (reports PASS but has deadlocks)
   - **Fix:** Warn user if bound reached, suggest manual review

10. **Plugin Health Checks Not Used** (Severity: LOW)
    - **Problem:** PluginRouter.performHealthCheck() defined but not called before execution
    - **Impact:** Executions fail late instead of failing fast
    - **Fix:** Pre-flight health check in StandardDagExecutorService.validate()

---

## 11. Production Readiness Scorecard

| Category | Score | Rationale |
|----------|-------|-----------|
| **Architecture** | 8/10 | Clean separation of concerns, interface-based design. Minor: tight coupling between DagBuilder and PluginRouter |
| **Concurrency** | 6/10 | Good: Level-based parallelism, backpressure. Poor: No timeout enforcement, blocking join() |
| **Error Handling** | 5/10 | Good: Typed exceptions. Poor: Retry/fallback not executed, no circuit breakers |
| **Observability** | 7/10 | Good: Metrics, traces. Poor: No distributed tracing, limited error context |
| **Resilience** | 4/10 | Poor: No timeouts, no circuit breakers, no rate limiting |
| **Testing** | N/A | Not analyzed (would need to review test coverage) |
| **Documentation** | 6/10 | Good inline comments. Poor: No architecture doc (until now) |
| **Performance** | 7/10 | Good: Optimized thread pools, parallel execution. Poor: Memory leak risk, blocking operations |

**Overall: 7/10** - Solid foundation, but **not production-ready** without addressing timeout enforcement and token substitution.

---

## 12. Recommendations

### Immediate (Week 1)

1. **Implement Token Substitution**
   - Add `TokenResolver` class to resolve `${dependency.X.Y}` patterns
   - Call in `executeNode()` before plugin dispatch
   - **File:** `core/impl/StandardDagExecutorService.java` L390-422

2. **Enforce Timeouts**
   - Wrap execution in `CompletableFuture.orTimeout()`
   - Add per-node timeout using `Future.get(timeout)`
   - **File:** `core/impl/StandardDagExecutorService.java` L107-154

3. **Execute Retry Logic**
   - Implement retry loop with exponential backoff in `executeNode()`
   - **File:** `core/impl/StandardDagExecutorService.java` L390-422

### Short-Term (Month 1)

4. **Add Circuit Breakers**
   - Integrate Resilience4j for per-plugin circuit breakers
   - **Files:** `plugins/PluginRouter.java` implementations

5. **Fix ExecutionTrace Memory Leak**
   - Replace `ConcurrentHashMap` with Caffeine cache (max 1000 entries)
   - **File:** `core/impl/StandardDagExecutorService.java` L44

6. **Add Pre-Flight Health Checks**
   - Call `performHealthCheck()` in `validate()`
   - **File:** `core/impl/StandardDagExecutorService.java` L173-222

### Medium-Term (Quarter 1)

7. **Distributed Tracing**
   - Integrate OpenTelemetry for trace propagation
   - **Files:** `core/impl/StandardDagExecutorService.java`, plugin implementations

8. **Rate Limiting**
   - Add per-user, per-plugin rate limits using Bucket4j
   - **File:** `api/service/DagExecutionService.java`

9. **Async Execution Improvements**
   - Replace `join()` with `thenCompose()` for non-blocking execution
   - **File:** `core/impl/StandardDagExecutorService.java` L366

10. **Enhanced Petri-Net Validation**
    - Warn users when k-bound reached
    - Add heuristic-based deadlock detection for large workflows
    - **File:** `core/petri/validation/PetriNetValidator.java`

---

## 13. File Manifest (Key Files Only)

```
/Users/rishabhpathak/base/Obvian-repos/obvian-mvp/
├── core/
│   ├── PromptParser.java              (L22-647)   NL parsing with regex
│   ├── DagBuilder.java                (L9-776)    Intent → DAG conversion
│   ├── TaskNode.java                  (L15-285)   Node definition with retry config
│   ├── ExecutionContext.java          (L14-106)   Execution state container
│   ├── impl/
│   │   └── StandardDagExecutorService.java (L29-661) Main executor with parallelism
│   └── petri/
│       ├── validation/
│       │   └── PetriNetValidator.java (L38-984)  Formal verification engine
│       ├── grammar/
│       │   └── AutomationGrammar.java            Intent → PetriNet transformer
│       ├── projection/
│       │   └── PetriToDagProjector.java          PetriNet → DAG projector
│       └── simulation/
│           └── PetriTokenSimulator.java          Token flow simulator
├── api/
│   ├── controller/
│   │   └── PetriController.java       (L36-1113) REST endpoints for Petri workflows
│   └── service/
│       └── DagExecutionService.java   (L35-760)  API orchestration layer
└── plugins/
    ├── PluginRouter.java              (L11-87)   Plugin dispatch interface
    └── PluginRegistry.java                       Plugin registration/discovery
```

---

## 14. Conclusion

Obvian demonstrates **strong architectural fundamentals** with formal verification, plugin extensibility, and observability. However, **critical gaps** in timeout enforcement, token substitution, and retry execution prevent production deployment without immediate fixes.

**Key Strengths:**
- Formal Petri-net verification (unique capability)
- Level-based parallel execution with backpressure
- Comprehensive metrics and tracing
- Clean interface-based architecture

**Key Weaknesses:**
- Token substitution defined but not executed (data flow broken)
- Timeouts configured but not enforced (runaway risk)
- Retry/fallback logic defined but not executed
- No circuit breakers or rate limiting

**Next Steps:**
1. Implement token resolution (1-2 days)
2. Enforce timeouts (1 day)
3. Execute retry logic (2 days)
4. Add circuit breakers (1 week)
5. Performance testing under load (1 week)

**Estimated Time to Production:** **2-3 weeks** with focused effort on critical gaps.

---

**Document Version:** 1.0
**Last Updated:** 2025-11-02
**Reviewer:** Backend Architect (Claude Sonnet 4.5)
