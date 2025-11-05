# Contributing to Obvian Petri Net DAG System

## Overview

Thank you for your interest in contributing to the Obvian Petri Net DAG proof-of-concept system! This guide will help you set up your development environment, understand the codebase structure, and contribute effectively to the project.

## Development Setup

### Prerequisites

- **Java 17+** (OpenJDK recommended)
- **Node.js 18+** with npm
- **Maven 3.8+**
- **Git** for version control
- **IDE** (IntelliJ IDEA, VS Code, or Eclipse)

### Local Development Environment

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd obvian-core
   ```

2. **Backend setup**:
   ```bash
   # Install dependencies and run tests
   mvn clean install
   
   # Start the Spring Boot application
   mvn spring-boot:run
   
   # API will be available at http://localhost:8080
   ```

3. **Frontend setup**:
   ```bash
   cd frontend
   npm install
   npm run dev
   
   # Frontend will be available at http://localhost:5173
   ```

4. **Verify setup**:
   ```bash
   # Test the complete flow
   curl -X POST http://localhost:8080/parse \
     -H "Content-Type: application/json" \
     -d '{"schemaVersion":"1.0","text":"Every time I push code: run tests"}'
   ```

### Docker Development

```bash
# Build and run with Docker Compose
docker-compose up --build

# Access at http://localhost:3000
```

## Project Structure

```
obvian-core/
├── engine-core/                 # Core Petri net engine
│   ├── src/main/java/
│   │   ├── petri/              # Petri net data models
│   │   ├── builder/            # IntentSpec to Petri net conversion
│   │   ├── validation/         # Formal validation algorithms
│   │   ├── simulation/         # Token simulation engine
│   │   └── dag/                # DAG projection
│   └── src/test/java/          # Unit tests
├── nlp-templates/              # Template-based NL parsing
│   ├── src/main/java/
│   │   ├── parser/             # Template parsers
│   │   ├── devops/             # DevOps-specific templates
│   │   └── football/           # Football training templates
│   └── src/test/java/          # Parser tests
├── web-api/                    # Spring Boot REST API
│   ├── src/main/java/
│   │   ├── controller/         # REST controllers
│   │   ├── service/            # Business logic
│   │   ├── dto/                # Data transfer objects
│   │   └── config/             # Configuration classes
│   └── src/test/java/          # Integration tests
├── frontend/                   # React + Vite UI
│   ├── src/
│   │   ├── components/         # React components
│   │   ├── services/           # API clients
│   │   ├── utils/              # Utility functions
│   │   └── styles/             # CSS modules
│   └── tests/                  # Frontend tests
└── docs/                       # Documentation
```

## Code Standards

### Java Code Style

- **Language Level**: Java 17 with modern features
- **Formatting**: Google Java Style Guide
- **Naming**: Clear, descriptive names following Java conventions
- **Documentation**: Javadoc for public APIs, inline comments for complex logic

```java
/**
 * Validates a Petri net for formal properties including deadlock detection,
 * reachability analysis, and liveness checking.
 */
public class PetriNetValidator {
    
    /**
     * Performs comprehensive validation of the given Petri net.
     *
     * @param petriNet the Petri net to validate
     * @param config validation configuration including bounds and timeouts
     * @return validation report with detailed results
     * @throws ValidationException if validation cannot be performed
     */
    public ValidationReport validate(PetriNet petriNet, ValidationConfig config) {
        // Implementation with clear error handling
    }
}
```

### TypeScript/React Code Style

- **Language**: TypeScript with strict mode enabled
- **Components**: Functional components with hooks
- **Styling**: CSS Modules with SCSS
- **State Management**: React Context + useReducer for complex state

```typescript
interface PetriNetCanvasProps {
  petriNet: PetriNet;
  selectedElements: Set<string>;
  onElementSelect: (elementId: string) => void;
  onElementHover: (elementId: string | null) => void;
}

export const PetriNetCanvas: React.FC<PetriNetCanvasProps> = ({
  petriNet,
  selectedElements,
  onElementSelect,
  onElementHover
}) => {
  // Component implementation with proper error boundaries
};
```

### Testing Standards

- **Unit Test Coverage**: ≥80% for engine-core modules
- **Test Naming**: Descriptive test method names following Given-When-Then pattern
- **Test Data**: Use builders and factories for test object creation
- **Assertions**: Clear, specific assertions with meaningful error messages

```java
@Test
void shouldDetectDeadlockWhenNoTransitionsEnabled() {
    // Given
    PetriNet petriNet = PetriNetBuilder.create()
        .withPlace("p1")
        .withTransition("t1")
        .withInitialMarking("p1", 0) // No tokens
        .build();
    
    // When
    ValidationReport report = validator.validate(petriNet, defaultConfig);
    
    // Then
    assertThat(report.getStatus()).isEqualTo(ValidationStatus.FAIL);
    assertThat(report.getChecks().get("deadlock").getResult())
        .isEqualTo(CheckStatus.FAIL);
}
```

## Adding New Template Intents

### 1. Define the Template Pattern

Create a new template class in the appropriate domain package:

```java
@Component
public class BusinessProcessTemplate implements TemplateParser {
    
    private static final Pattern APPROVAL_PATTERN = Pattern.compile(
        "(?i)submit\\s+(.+?)\\s+for\\s+approval.*?if\\s+approved\\s+(.+?)\\s+if\\s+rejected\\s+(.+)"
    );
    
    @Override
    public ParseResult parse(String text) {
        Matcher matcher = APPROVAL_PATTERN.matcher(text);
        if (matcher.find()) {
            return ParseResult.success(buildApprovalIntent(matcher));
        }
        return ParseResult.failure("Pattern not recognized as business approval process");
    }
    
    private IntentSpec buildApprovalIntent(Matcher matcher) {
        return IntentSpec.builder()
            .name("Business Approval Process")
            .addStep(IntentStep.action("submit", matcher.group(1)))
            .addStep(IntentStep.choice("approval_decision", "approved", "rejected"))
            .addStep(IntentStep.action("approve_action", matcher.group(2)).when("approved"))
            .addStep(IntentStep.action("reject_action", matcher.group(3)).when("rejected"))
            .build();
    }
}
```

### 2. Add Template Tests

Create comprehensive tests for the new template:

```java
class BusinessProcessTemplateTest {
    
    @Test
    void shouldParseApprovalWorkflow() {
        // Given
        String input = "Submit expense report for approval; if approved process payment; if rejected notify submitter";
        
        // When
        ParseResult result = template.parse(input);
        
        // Then
        assertThat(result.isSuccess()).isTrue();
        IntentSpec intent = result.getIntentSpec();
        assertThat(intent.getSteps()).hasSize(4);
        assertThat(intent.getSteps().get(0).getType()).isEqualTo(StepType.ACTION);
        assertThat(intent.getSteps().get(1).getType()).isEqualTo(StepType.CHOICE);
    }
    
    @Test
    void shouldProvideHelpfulErrorForUnsupportedPattern() {
        // Given
        String input = "Do something complex without clear structure";
        
        // When
        ParseResult result = template.parse(input);
        
        // Then
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrors()).contains("Pattern not recognized");
        assertThat(result.getSuggestions()).isNotEmpty();
    }
}
```

### 3. Register the Template

Add the template to the parser registry:

```java
@Configuration
public class TemplateConfiguration {
    
    @Bean
    public TemplateRegistry templateRegistry(
            DevOpsTemplate devOpsTemplate,
            FootballTemplate footballTemplate,
            BusinessProcessTemplate businessProcessTemplate) {
        
        return TemplateRegistry.builder()
            .register("devops", devOpsTemplate)
            .register("football", footballTemplate)
            .register("business", businessProcessTemplate)
            .build();
    }
}
```

### 4. Add UI Examples

Update the frontend with example text for the new template:

```typescript
const TEMPLATE_EXAMPLES = {
  devops: "Every time I push code: run tests; if pass deploy to staging; if fail alert Slack",
  football: "Warm-up, then pass and shoot in parallel, then cooldown",
  business: "Submit expense report for approval; if approved process payment; if rejected notify submitter"
};
```

### 5. Create Golden Tests

Add golden file tests for the complete flow:

```java
@Test
void shouldProcessBusinessApprovalEndToEnd() {
    // Given
    String input = "Submit expense report for approval; if approved process payment; if rejected notify submitter";
    
    // When - Complete flow
    ParseResult parseResult = parser.parse(input);
    BuildResult buildResult = builder.build(parseResult.getIntentSpec());
    ValidationReport validationReport = validator.validate(buildResult.getPetriNet(), defaultConfig);
    SimulationResult simulationResult = simulator.simulate(buildResult.getPetriNet(), deterministicConfig);
    
    // Then - Compare with golden files
    assertThat(parseResult).matchesGoldenFile("business-approval-parse.json");
    assertThat(buildResult.getPetriNet()).matchesGoldenFile("business-approval-petri.json");
    assertThat(validationReport).matchesGoldenFile("business-approval-validation.json");
    assertThat(simulationResult.getTrace()).matchesGoldenFile("business-approval-trace.json");
}
```

## Testing Guidelines

### Unit Tests

- **Scope**: Test individual classes and methods in isolation
- **Mocking**: Use Mockito for dependencies, avoid over-mocking
- **Data**: Use test builders for complex objects
- **Coverage**: Aim for ≥80% line coverage, 100% for critical algorithms

### Integration Tests

- **Scope**: Test component interactions and API contracts
- **Database**: Use TestContainers for database-dependent tests
- **Web Layer**: Use MockMvc for controller testing
- **External Services**: Mock external dependencies

### End-to-End Tests

- **Scope**: Test complete user workflows
- **Tools**: Cypress or Playwright for frontend E2E tests
- **Data**: Use realistic test scenarios based on demo scripts
- **Environment**: Run against Docker-composed application

### Property-Based Tests

Use jqwik for testing algorithmic properties:

```java
@Property
void tokenConservationDuringSimulation(@ForAll("validPetriNets") PetriNet petriNet) {
    // Given
    int initialTokens = petriNet.getInitialMarking().getTotalTokens();
    
    // When
    SimulationResult result = simulator.simulate(petriNet, deterministicConfig);
    
    // Then - Token count should be conserved throughout execution
    for (TraceEvent event : result.getTrace()) {
        int tokensAfter = event.getMarkingAfter().getTotalTokens();
        assertThat(tokensAfter).isEqualTo(initialTokens);
    }
}
```

## Performance Guidelines

### Backend Performance

- **Response Times**: Target <2s for end-to-end workflow processing
- **Memory Usage**: Monitor heap usage, avoid memory leaks
- **Algorithm Complexity**: Document time/space complexity for core algorithms
- **Profiling**: Use JProfiler or similar tools for performance analysis

### Frontend Performance

- **Bundle Size**: Keep JavaScript bundles under 1MB
- **Rendering**: Use React.memo for expensive components
- **Network**: Minimize API calls, implement proper caching
- **Animation**: Use CSS transforms for smooth token animation

## Documentation Standards

### Code Documentation

- **Public APIs**: Comprehensive Javadoc with examples
- **Complex Algorithms**: Inline comments explaining the approach
- **Configuration**: Document all configuration options
- **Error Handling**: Document expected exceptions and recovery strategies

### User Documentation

- **README**: Keep updated with current setup instructions
- **API Documentation**: OpenAPI specifications with examples
- **Architecture**: Maintain architecture diagrams and decision records
- **Troubleshooting**: Common issues and solutions

## Commit Guidelines

### Commit Message Format

```
type(scope): brief description

Detailed explanation of the change, including:
- Why the change was made
- What was changed
- Any breaking changes or migration notes

Closes #123
```

### Commit Types

- **feat**: New feature implementation
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, etc.)
- **refactor**: Code refactoring without functional changes
- **test**: Adding or updating tests
- **chore**: Build process or auxiliary tool changes

### Examples

```
feat(validation): add boundedness checking algorithm

Implement bounded place analysis to detect potential token
accumulation issues. The algorithm explores the reachability
graph up to a configurable bound and reports places that
exceed their capacity constraints.

- Add BoundednessChecker class with configurable limits
- Integrate with ValidationReport for comprehensive results
- Include performance optimizations for large state spaces

Closes #45
```

## Pull Request Process

### Before Submitting

1. **Run Tests**: Ensure all tests pass locally
   ```bash
   mvn clean test
   cd frontend && npm test
   ```

2. **Check Code Quality**: Run static analysis tools
   ```bash
   mvn spotbugs:check pmd:check checkstyle:check
   ```

3. **Update Documentation**: Include relevant documentation updates

4. **Test Coverage**: Verify coverage meets requirements
   ```bash
   mvn jacoco:report
   ```

### PR Description Template

```markdown
## Description
Brief description of the changes and their purpose.

## Type of Change
- [ ] Bug fix (non-breaking change which fixes an issue)
- [ ] New feature (non-breaking change which adds functionality)
- [ ] Breaking change (fix or feature that would cause existing functionality to not work as expected)
- [ ] Documentation update

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed
- [ ] Performance impact assessed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] Tests added for new functionality
- [ ] All tests pass
- [ ] No new warnings introduced
```

### Review Process

1. **Automated Checks**: CI pipeline must pass
2. **Code Review**: At least one approving review required
3. **Testing**: Reviewer should test the changes locally
4. **Documentation**: Verify documentation is complete and accurate

## Getting Help

### Resources

- **Architecture Documentation**: [docs/architecture.md](docs/architecture.md)
- **API Documentation**: http://localhost:8080/swagger-ui.html (when running locally)
- **Demo Scripts**: [docs/demo/script.md](docs/demo/script.md)
- **Issue Tracker**: GitHub Issues for bug reports and feature requests

### Communication

- **Questions**: Open a GitHub Discussion for general questions
- **Bug Reports**: Use GitHub Issues with the bug template
- **Feature Requests**: Use GitHub Issues with the feature template
- **Security Issues**: Email security@obvian.com (do not use public issues)

### Development Tips

- **IDE Setup**: Import code style configurations from `.editorconfig`
- **Debugging**: Use remote debugging for Spring Boot application
- **Hot Reload**: Frontend supports hot module replacement for rapid development
- **Database**: Use H2 console at http://localhost:8080/h2-console for debugging

## License

By contributing to this project, you agree that your contributions will be licensed under the same license as the project. See [LICENSE.md](LICENSE.md) for details.

---

Thank you for contributing to Obvian! Your efforts help build reliable, traceable workflow automation for everyone.