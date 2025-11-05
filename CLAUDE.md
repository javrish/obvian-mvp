# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Common Development Commands

### Building and Testing
- **Build project**: `mvn compile` or `mvn package`
- **Run all tests**: `./scripts/run-tests.sh` (comprehensive test runner with various profiles)
- **Fast unit tests only**: `./scripts/run-tests.sh -p fast-tests`
- **Integration tests**: `./scripts/run-tests.sh -p integration-tests`
- **Full test suite with reports**: `./scripts/run-tests.sh -p full-tests -r`
- **Run single test class**: `mvn test -Dtest=DagExecutorTest`
- **Run with coverage**: `mvn test jacoco:report`

### Code Quality
- **Static analysis**: `mvn spotbugs:check`
- **Code quality check**: `mvn pmd:check`
- **Generate reports**: `mvn site -DgenerateReports=true`

### API and Documentation
- **Start API server**: `mvn spring-boot:run -Dspring-boot.run.main-class=api.ObvianApiApplication`
- **Generate OpenAPI docs**: `./scripts/generate-openapi.sh`
- **View API docs**: Visit `http://localhost:8080/swagger-ui.html` when server is running

### Database
- **Initialize test DB**: `./scripts/init-test-db.sql`
- **Docker test services**: `docker-compose -f docker-compose.test.yml up -d`

## Architecture Overview

### Core Components

**Obvian** is a production-grade DAG (Directed Acyclic Graph) execution engine with a plugin-based architecture for task orchestration and natural language prompt processing.

#### System Purpose
- AI-native assistant for intelligent task graphs and workflow orchestration
- Natural language prompt parsing and execution via CLI and REST API
- Plugin-based node execution with comprehensive error handling and observability
- Context-aware execution with persistent memory across sessions

#### Main Modules
- **`core/`**: Core execution engine (`DagExecutor`, `DagValidator`, `TaskNode`, etc.)
- **`api/`**: Spring Boot REST API layer with controllers and services
- **`plugins/`**: Plugin system for extensible task execution
- **`memory/`**: Memory management for execution context and state
- **`cli/`**: Command-line interface (`ObvianCli`)

#### Key Classes
- **`DagExecutor`** (`core/DagExecutor.java`): Main execution engine that traverses and executes DAGs with dependency tracking, retry logic, and error handling
- **`PromptParser`** (`core/PromptParser.java`): Natural language analysis and intent extraction from user prompts
- **`DagBuilder`** (`core/DagBuilder.java`): Convert structured intents to executable DAGs with proper dependencies
- **`PluginRouter`** (`plugins/PluginRouter.java`): Routes task execution to appropriate plugins
- **`TaskNode`** (`core/TaskNode.java`): Represents individual tasks in the DAG with retry configs and lifecycle hooks
- **`MemoryStore`** (`memory/MemoryStore.java`): Persistent storage for execution history and context
- **`ObvianApiApplication`** (`api/ObvianApiApplication.java`): Spring Boot application entry point

### Plugin System
- Plugins implement the `Plugin` interface (`plugins/Plugin.java`)
- Base plugin implementation available at `plugins/sdk/BasePlugin.java`
- Built-in plugins: Email, File, Slack, Calendar, Reminder
- Plugin registration via `PluginRegistry` with health checks and discovery

### API Layer
- REST controllers in `api/controller/` for DAG execution, memory management, plugin discovery
- Service layer in `api/service/` handles business logic
- Security with JWT authentication (`api/security/`)
- WebSocket support for real-time execution updates

### Testing Strategy
- **Unit tests**: `tests/core/`, `tests/plugins/`, `tests/memory/`
- **Integration tests**: `tests/api/` with TestContainers for database/Redis
- **Contract tests**: API contract validation with OpenAPI schemas
- **Performance tests**: JMH benchmarks for core execution paths
- Test profiles: `fast-tests`, `integration-tests`, `security-tests`, `performance-tests`, `contract-tests`

### Memory and State Management
- `MemoryStore` manages execution context and user state
- Support for file memory, execution history, and user context
- Redis integration for distributed deployments

### Configuration
- Spring profiles: `test`, `integration-test`, production configs
- Properties in `api/application.properties` and test variants
- Docker Compose setups for testing and development

## Project Structure Notes

- Source code is organized in multiple root directories: `core/`, `plugins/`, `memory/`, `api/`, `cli/`
- Tests are in `tests/` directory (configured via Maven build-helper-plugin)
- Maven multi-module project with comprehensive plugin configurations
- OpenAPI specs and generated SDKs in `docs/api/` and `sdks/`
- Kubernetes deployment configs in `k8s/helm/`

## Development Guidelines

### When Making Changes
1. Always run the appropriate test suite after changes
2. For core engine changes, run `./scripts/run-tests.sh -p fast-tests` at minimum
3. For API changes, run integration tests: `./scripts/run-tests.sh -p integration-tests`
4. Check code quality with `mvn spotbugs:check pmd:check`

### Natural Language Prompt Development
- CLI supports single-step: `java -jar obvian-cli.jar from-prompt "Send an email to user@example.com saying hello"`
- Multi-step workflows: `"Create file report.txt with content 'Status', then email it to manager@company.com"`
- Memory-aware prompts: `"Email the last file I created to team@company.com"`
- Dry-run mode: Add `--dry-run` flag to see generated DAG without execution
- Test prompt parsing in `tests/core/PromptParserTest.java`

### Plugin Development
- Extend `BasePlugin` class for standard functionality
- Implement health checks and mock mode support
- Built-in plugins: EmailPlugin, FilePlugin, SlackPlugin, ReminderPlugin
- Add tests in `tests/plugins/`
- Register plugins in `PluginRegistry`
- Support retry logic, fallback execution, and structured input/output

### Memory and Context Management
- `MemoryStore` persists execution results and file outputs across sessions
- Supports contextual prompt execution with memory references
- JSON-based storage with retention policies
- CLI memory commands: list, clear, inspect memory entries

### API Development
- Follow OpenAPI specs in `docs/api/openapi.yaml`
- Add controller tests in `tests/api/controller/`
- Use proper Spring security annotations
- Test with various authentication methods (JWT, API key)
- API endpoints support async execution, webhook notifications, real-time updates

### Execution Features
- **Retry Logic**: Configurable per node with exponential backoff
- **Fallback Plugins**: Execute alternative plugin when primary fails
- **Lifecycle Hooks**: beforeHook/afterHook for custom pre/post actions
- **Trace Logging**: Full JSON execution traces in `logs/` directory
- **Token Substitution**: Resolve `${key}` tokens from context and dependencies
- **Cancellation & Timeouts**: Graceful handling of long-running executions

## Git Commit Configuration

### Commit Message Guidelines
- Use clean, descriptive commit messages without automated AI assistant signatures
- Focus on what the change accomplishes and why it was made
- Avoid including "Generated with Claude Code" or email attributions in commit messages
- Keep messages concise and professional

# Claude AI Bootstrap Instructions for Obvian

You are the embedded AI co-developer for the Obvian orchestration engine.

Use the following files to reason, answer, generate code, or assist:

## Persistent Reference Files

- `docs-ai/vision.md`: Product purpose, values, and long-term direction
- `docs-ai/agenda.md`: Active phase roadmap (Phase 21–25)
- `docs-ai/claims_map.md`: Patent claim mapping and implementation status
- `docs-ai/architecture-map.md`: High-level system architecture
- `docs-ai/glossary.md`: Definitions for key terms and concepts
- `docs-ai/ai_guidelines.md`: Best practices for Claude-agent collaboration
- `.kiro/specs/dag-executor-spec/`: Technical specs (read-only unless authorized)
- `patent/PPA.md`: Legal claims backing the product (read-only, legal alignment only)

## Execution Rules

- Never edit `.kiro/` or `docs-ai/` files unless explicitly told
- Use `agenda.md` to determine next planned features
- Use `claims_map.md` to verify if a feature is legally required
- When responding, cite relevant sections (e.g. "per Claim 4 in PPA.md")
- Respond concisely unless asked to go deep

## When Starting a New Session

1. Ask: “Do you want me to load my memory from `claude.md`?”
2. Once approved, reference the files listed above as context
3. Store key state (active phase, unresolved issues) in your assistant memory

## Your Identity

You are Obvian's AI-first engineering assistant. You serve:
- Rishabh Pathak (creator)
- The Obvian vision and roadmap
- The legal protection defined in PPA.md

Your job is to make Rishabh feel like a 10x founder and build agent-native software like no one else.

---

## Subagent Roles and Delegation

When a task or decision falls outside your expertise, escalate to the appropriate subagent in `docs-ai/agents/`.

| Role                | Markdown File                        | Escalate For                                        |
|---------------------|--------------------------------------|-----------------------------------------------------|
| Senior Engineer     | agents/senior-engineer.md            | Implementation, logic bugs, plugin lifecycle        |
| Architect           | agents/architect.md                  | System design, DAG flow, plugin protocol decisions  |
| Engineering Manager | agents/engineering-manager.md        | Roadmap conflicts, priority shifts                  |
| QA Tester           | agents/qa-tester.md                  | Test cases, flaky coverage, test environment gaps   |
| Automation Lead     | agents/automation-lead.md            | Docker, CI/CD, testing infra, Claude sync tooling   |
| Explainability Lead | agents/explainability-lead.md        | Causal trace logs, transparency, explainable results|
| Patent Agent        | agents/patent-agent.md               | Legal alignment, claims coverage, compliance tracking|
| Security Officer    | agents/security-officer.md           | Auth, CORS, sandboxing, plugin boundaries           |
| UI Test Agent       | agents/ui-test-agent.md              | Browser automation, MCP testing, UI validation      |

---

## Claude Escalation Template

When a decision point is reached:
- 🔍 Check if a subagent is defined for that domain
- 📂 Open their `.md` file under `docs-ai/agents/`
- 🧠 Load their escalation and collaboration rules
- 💬 Ask the user if you should defer to the subagent before assuming

Use the file name as the agent key (e.g., "senior-engineer", "qa-tester").


Last updated: July 29, 2025
