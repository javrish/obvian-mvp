# Petri Net DAG POC - Task 10 Demo Scenarios

This directory contains comprehensive demo scenarios and acceptance tests that validate the complete Petri Net DAG POC implementation, fulfilling the requirements for **Task 10** from `.kiro/specs/petri-net-dag-poc/tasks.md`.

## Overview

The demo scenario test suite provides **professional, stakeholder-ready demonstrations** of the complete Petri Net DAG pipeline, from natural language input to executable workflow graphs. These tests validate all requirements 7.1-7.5 and 9.1-9.5 through realistic, business-relevant scenarios.

## Demo Scenarios

### 1. DevOps CI/CD Pipeline 🔄
- **Workflow**: `"run tests; if pass deploy; if fail alert"`
- **Pattern**: Conditional branching with success/failure paths
- **Validates**: Error handling, conditional logic, deployment workflows
- **File**: `DevOpsCICDDemoTest.java`

### 2. Football Training Session ⚽
- **Workflow**: `"warm-up, then pass and shoot in parallel, then cooldown"`
- **Pattern**: Sequential-parallel-sequential with AND-split/AND-join
- **Validates**: Parallel execution, synchronization, complex workflow orchestration
- **File**: `FootballTrainingDemoTest.java`

### 3. Negative Test Case ❌
- **Workflow**: `"warm-up, then pass and shoot in parallel"` (missing AND-join)
- **Pattern**: Intentionally malformed parallel workflow
- **Validates**: Error detection with diagnostic hints containing "AND-join"
- **File**: `FootballNegativeTestDemo.java`

## Test Infrastructure

### Golden Snapshot Testing
- **Base Class**: `DemoScenarioTestBase.java`
- **Coverage**: ValidationReports, Traces, DAG structures, Mermaid outputs
- **Purpose**: Ensure consistent outputs across test runs and detect regressions

### Performance Validation
- **File**: `PetriNetPerformanceDemoTest.java`
- **Requirements**: Sub-2s end-to-end latency for networks ≤30 places/transitions
- **Coverage**: Load testing, component profiling, memory usage validation

### Comprehensive Acceptance Tests
- **File**: `ComprehensiveAcceptanceTest.java`
- **Coverage**: All requirements 7.1-7.5 and 9.1-9.5
- **Method**: Systematic validation of each requirement with detailed reporting

### Browser Automation
- **File**: `../frontend/tests/demo-scenarios.spec.js`
- **Coverage**: Cross-browser compatibility (Chrome, Firefox, Safari, Edge)
- **Features**: Responsive design testing, UI workflow validation

## Running the Tests

### Quick Start
```bash
# Run complete demo scenario test suite
./scripts/run-demo-tests.sh

# Run only backend tests
./scripts/run-demo-tests.sh backend

# Run only frontend/browser tests
./scripts/run-demo-tests.sh frontend

# Show help
./scripts/run-demo-tests.sh help
```

### Individual Test Execution
```bash
# DevOps demo scenario
mvn test -Dtest=DevOpsCICDDemoTest

# Football training demo
mvn test -Dtest=FootballTrainingDemoTest

# Negative test cases
mvn test -Dtest=FootballNegativeTestDemo

# Performance tests
mvn test -Dtest=PetriNetPerformanceDemoTest

# Complete acceptance tests
mvn test -Dtest=ComprehensiveAcceptanceTest

# Full test suite
mvn test -Dtest=DemoScenarioTestSuite
```

### Frontend Browser Tests
```bash
cd frontend
npx playwright test tests/demo-scenarios.spec.js
```

## Requirements Validation

### Requirements 7.1-7.5 (Core Functionality)
| Requirement | Description | Test Coverage |
|-------------|-------------|---------------|
| **7.1** | Natural language parsing | DevOps + Football scenarios |
| **7.2** | Petri net construction | All demo workflows + edge cases |
| **7.3** | Formal validation | Deadlock detection, reachability analysis |
| **7.4** | Token simulation | Comprehensive trace logging |
| **7.5** | DAG projection | Graph structure with semantic preservation |

### Requirements 9.1-9.5 (Performance & Reliability)
| Requirement | Description | Test Coverage |
|-------------|-------------|---------------|
| **9.1** | End-to-end latency ≤2s | Performance benchmarking across scenarios |
| **9.2** | Network size ≤30 nodes | Structure validation for all workflows |
| **9.3** | Error handling | Negative test scenarios with diagnostic hints |
| **9.4** | Resource efficiency | Golden snapshot testing, memory monitoring |
| **9.5** | Integration reliability | Browser automation, API validation |

## Test Reports

The test suite generates comprehensive reports in `test-results/demo/`:

### Executive Reports
- **`executive-summary.md`**: Stakeholder-ready overview
- **`stakeholder-dashboard.html`**: Interactive visual dashboard
- **`technical-validation-report.md`**: Detailed technical analysis

### Specialized Reports
- **`performance-benchmark-report.md`**: Performance metrics and analysis
- **`test-coverage-report.md`**: Coverage analysis across all components
- **`requirement-*.md`**: Individual requirement validation reports

### Visual Artifacts
- **Screenshots**: Complete workflow screenshots for each scenario
- **Performance Graphs**: Latency and resource usage visualizations
- **Network Diagrams**: Generated Petri net and DAG visualizations

## Golden Snapshot Testing

Golden snapshots ensure consistent outputs across test runs:

```java
// Example usage in test
GoldenSnapshot snapshot = executeEndToEndWorkflow(scenarioName, workflow, templateHint);
compareWithGoldenMaster(snapshot); // Validates against stored golden master
```

### Golden Master Files
- Stored in `tests/demo/golden/`
- Include complete pipeline outputs for each scenario
- Automatically updated on first run, validated on subsequent runs
- Enable regression detection for any pipeline changes

## Performance Benchmarks

### Target Performance Metrics
- **End-to-end latency**: ≤ 2000ms
- **Parse time**: < 800ms
- **Build time**: < 1200ms
- **Validation time**: < 1200ms
- **Simulation time**: < 600ms
- **DAG projection**: < 300ms

### Network Size Constraints
- **Places**: ≤ 30
- **Transitions**: ≤ 30
- **Total network nodes**: Scales appropriately with workflow complexity

### Load Testing
- **Concurrent users**: 5 simultaneous executions
- **Consistency testing**: 10 repeated runs per scenario
- **Memory usage**: Baseline vs. final consumption monitoring

## Browser Automation Features

### Cross-Browser Testing
- **Chrome**: Latest stable version
- **Firefox**: Latest stable version
- **Safari**: WebKit engine testing
- **Edge**: Chromium-based testing

### Responsive Design Validation
- **Mobile**: 375px width (iPhone 12)
- **Tablet**: 768px width (iPad)
- **Desktop**: 1280px+ width
- **Large Desktop**: 1920px+ width

### UI Workflow Testing
- Complete end-to-end user workflows
- Real-time feedback and progress indicators
- Error message presentation and user guidance
- Performance validation for UI interactions

## Integration with CI/CD

### GitHub Actions Integration
```yaml
- name: Run Demo Scenario Tests
  run: ./scripts/run-demo-tests.sh all

- name: Upload Test Reports
  uses: actions/upload-artifact@v3
  with:
    name: demo-test-reports
    path: test-results/demo/
```

### Test Environment Setup
- Automated test database initialization
- Docker Compose for test services
- Environment variable configuration
- Cleanup and teardown procedures

## Stakeholder Demonstration

### Demo Flow
1. **Setup**: Show clean test environment and prerequisites
2. **DevOps Scenario**: Demonstrate conditional branching workflow
3. **Football Scenario**: Showcase parallel execution patterns
4. **Error Handling**: Show diagnostic capabilities with malformed input
5. **Performance**: Highlight sub-2s latency achievements
6. **Reports**: Present comprehensive validation dashboard

### Key Demo Points
- **Natural Language Interface**: Easy workflow specification
- **Visual Feedback**: Real-time Petri net and DAG generation
- **Formal Validation**: Mathematical correctness guarantees
- **Performance**: Production-ready response times
- **Error Handling**: Professional diagnostic messages
- **Cross-Platform**: Works across all major browsers

## Development Guidelines

### Adding New Demo Scenarios
1. Extend `DemoScenarioTestBase` for infrastructure
2. Follow naming convention: `[ScenarioName]DemoTest.java`
3. Include golden snapshot testing
4. Add performance validation
5. Update browser automation tests
6. Generate comprehensive reports

### Maintaining Golden Masters
- Run tests with `UPDATE_GOLDEN=true` to regenerate
- Review changes carefully before committing
- Include justification for golden master updates
- Validate performance hasn't regressed

### Performance Optimization
- Monitor component-level performance metrics
- Use profiling for bottleneck identification
- Implement caching strategies for repeated patterns
- Consider parallel processing for validation steps

## Architecture Integration

### Core Components Tested
- **Natural Language Parser**: Template-based pattern recognition
- **Petri Net Builder**: Graph construction with validation
- **Formal Validator**: Deadlock detection and reachability analysis
- **Token Simulator**: Execution with comprehensive trace logging
- **DAG Projector**: Semantic-preserving graph transformation

### API Integration
- REST endpoint validation for all operations
- Authentication and authorization testing
- Error response formatting and HTTP status codes
- Request/response serialization verification

### Frontend Integration
- Complete user workflow simulation
- Component interaction testing
- Visual regression detection
- Accessibility and usability validation

## Conclusion

The Task 10 demo scenario test suite provides **comprehensive validation** of the Petri Net DAG POC through:

✅ **Professional demo scenarios** ready for stakeholder presentation
✅ **Complete requirements coverage** for 7.1-7.5 and 9.1-9.5
✅ **Performance validation** meeting sub-2s latency requirements
✅ **Golden snapshot testing** ensuring consistent outputs
✅ **Cross-browser compatibility** for production deployment
✅ **Comprehensive reporting** with visual documentation

The implementation is **production-ready** with robust error handling, comprehensive test coverage, and stakeholder-ready demonstration materials.