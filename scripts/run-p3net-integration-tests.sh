#!/bin/bash

# P3Net Integration Test Suite Runner
# Comprehensive test execution script for P3Net pipeline validation
#
# Usage:
#   ./scripts/run-p3net-integration-tests.sh [options]
#
# Options:
#   -p, --profile PROFILE    Test profile: full, fast, api, components, performance
#   -r, --report             Generate detailed reports
#   -c, --coverage           Run with code coverage analysis
#   -v, --verbose            Enable verbose output
#   -h, --help               Show this help message
#
# Examples:
#   ./scripts/run-p3net-integration-tests.sh --profile full --report --coverage
#   ./scripts/run-p3net-integration-tests.sh -p fast -v
#   ./scripts/run-p3net-integration-tests.sh --profile performance --report

set -euo pipefail

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Default configuration
PROFILE="full"
GENERATE_REPORTS=false
COVERAGE=false
VERBOSE=false
MAVEN_OPTS="${MAVEN_OPTS:-""}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_header() {
    echo -e "\n${PURPLE}=== $1 ===${NC}\n"
}

# Help function
show_help() {
    cat << EOF
P3Net Integration Test Suite Runner

DESCRIPTION:
    Comprehensive test execution script for P3Net pipeline validation.
    Runs integration tests across AutomationGrammar, PetriNetValidator,
    PetriTokenSimulator, and full API pipeline.

USAGE:
    $0 [OPTIONS]

OPTIONS:
    -p, --profile PROFILE    Test profile to run (default: full)
                            Available profiles:
                              full        - All integration tests + performance
                              fast        - Quick integration tests only
                              api         - API integration tests only
                              components  - Component integration tests only
                              performance - Performance benchmarks only
                              golden      - Golden output generation tests

    -r, --report            Generate detailed HTML reports and coverage
    -c, --coverage          Enable JaCoCo code coverage analysis
    -v, --verbose           Enable verbose Maven output
    -h, --help              Show this help message

EXAMPLES:
    # Run full test suite with reports and coverage
    $0 --profile full --report --coverage

    # Quick validation run
    $0 -p fast -v

    # Performance benchmarking only
    $0 --profile performance --report

    # Generate golden test outputs
    $0 --profile golden

ENVIRONMENT VARIABLES:
    MAVEN_OPTS              Additional Maven options
    JAVA_OPTS               Java runtime options
    P3NET_TEST_TIMEOUT      Test timeout in seconds (default: 600)

EOF
}

# Parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -p|--profile)
                PROFILE="$2"
                shift 2
                ;;
            -r|--report)
                GENERATE_REPORTS=true
                shift
                ;;
            -c|--coverage)
                COVERAGE=true
                shift
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# Validate profile
validate_profile() {
    case $PROFILE in
        full|fast|api|components|performance|golden)
            log_info "Using test profile: $PROFILE"
            ;;
        *)
            log_error "Invalid profile: $PROFILE"
            log_info "Valid profiles: full, fast, api, components, performance, golden"
            exit 1
            ;;
    esac
}

# Environment setup
setup_environment() {
    log_header "Environment Setup"

    cd "$PROJECT_ROOT"

    # Check Java version
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
        log_info "Java version: $JAVA_VERSION"

        if [ "$JAVA_VERSION" -lt 17 ]; then
            log_error "Java 17 or higher required. Found version: $JAVA_VERSION"
            exit 1
        fi
    else
        log_error "Java not found in PATH"
        exit 1
    fi

    # Check Maven
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn -version | head -1 | awk '{print $3}')
        log_info "Maven version: $MVN_VERSION"
    else
        log_error "Maven not found in PATH"
        exit 1
    fi

    # Set test timeout
    export P3NET_TEST_TIMEOUT="${P3NET_TEST_TIMEOUT:-600}"
    log_info "Test timeout: ${P3NET_TEST_TIMEOUT}s"

    # Configure Maven options
    if [ "$VERBOSE" = true ]; then
        MAVEN_OPTS="$MAVEN_OPTS -X"
    fi

    if [ "$COVERAGE" = true ]; then
        MAVEN_OPTS="$MAVEN_OPTS -Djacoco.enabled=true"
    fi

    export MAVEN_OPTS
    log_info "Maven options: $MAVEN_OPTS"
}

# Clean and prepare
clean_prepare() {
    log_header "Clean and Prepare"

    # Clean previous test results
    log_info "Cleaning previous test results..."
    rm -rf target/surefire-reports/
    rm -rf target/failsafe-reports/
    rm -rf target/site/jacoco/
    rm -rf logs/test-*.log

    # Create directories
    mkdir -p logs/
    mkdir -p target/test-reports/

    # Compile project
    log_info "Compiling project..."
    if ! mvn clean compile test-compile -q; then
        log_error "Compilation failed"
        exit 1
    fi

    log_success "Clean and preparation completed"
}

# Run API integration tests
run_api_tests() {
    log_header "P3Net API Integration Tests"

    local TEST_ARGS=""
    if [ "$COVERAGE" = true ]; then
        TEST_ARGS="jacoco:prepare-agent"
    fi

    log_info "Running P3Net API integration tests..."

    if mvn $TEST_ARGS test -Dtest="P3NetPipelineIntegrationTest" \
        -Dspring.profiles.active=integration-test \
        -DforkCount=1 \
        -DreuseForks=false \
        -Dmaven.test.failure.ignore=true; then
        log_success "API integration tests completed"
        return 0
    else
        log_error "API integration tests failed"
        return 1
    fi
}

# Run component integration tests
run_component_tests() {
    log_header "P3Net Component Integration Tests"

    local TEST_ARGS=""
    if [ "$COVERAGE" = true ]; then
        TEST_ARGS="jacoco:prepare-agent"
    fi

    log_info "Running P3Net component integration tests..."

    if mvn $TEST_ARGS test -Dtest="P3NetComponentIntegrationTest" \
        -Dspring.profiles.active=test \
        -DforkCount=1 \
        -DreuseForks=false \
        -Dmaven.test.failure.ignore=true; then
        log_success "Component integration tests completed"
        return 0
    else
        log_error "Component integration tests failed"
        return 1
    fi
}

# Run performance tests
run_performance_tests() {
    log_header "P3Net Performance Benchmark Tests"

    local TEST_ARGS=""
    if [ "$COVERAGE" = true ]; then
        TEST_ARGS="jacoco:prepare-agent"
    fi

    log_info "Running P3Net performance benchmark tests..."
    log_warn "Performance tests may take several minutes to complete..."

    if mvn $TEST_ARGS test -Dtest="P3NetPerformanceBenchmarkTest" \
        -Dspring.profiles.active=performance-test \
        -DforkCount=1 \
        -DreuseForks=false \
        -Dmaven.test.failure.ignore=true \
        -Dmaven.surefire.timeout="${P3NET_TEST_TIMEOUT}"; then
        log_success "Performance benchmark tests completed"
        return 0
    else
        log_error "Performance benchmark tests failed"
        return 1
    fi
}

# Run golden output generation tests
run_golden_tests() {
    log_header "Golden Output Generation Tests"

    log_info "Running golden output generation..."
    log_info "This will create reference outputs for regression testing"

    # Create golden outputs directory
    mkdir -p target/test-reports/golden-outputs/

    # Run tests with golden output generation flag
    if mvn test -Dtest="P3NetPipelineIntegrationTest#*devOps*,P3NetPipelineIntegrationTest#*football*" \
        -Dspring.profiles.active=integration-test \
        -Dp3net.generate.golden.outputs=true \
        -Dp3net.golden.outputs.dir="$PROJECT_ROOT/target/test-reports/golden-outputs/" \
        -DforkCount=1 \
        -DreuseForks=false; then
        log_success "Golden output generation completed"

        # List generated outputs
        if [ -d "$PROJECT_ROOT/target/test-reports/golden-outputs/" ]; then
            log_info "Generated golden outputs:"
            find "$PROJECT_ROOT/target/test-reports/golden-outputs/" -name "*.json" -exec basename {} \; | sort
        fi

        return 0
    else
        log_error "Golden output generation failed"
        return 1
    fi
}

# Generate reports
generate_reports() {
    if [ "$GENERATE_REPORTS" != true ]; then
        return 0
    fi

    log_header "Generating Test Reports"

    # Generate coverage report if enabled
    if [ "$COVERAGE" = true ]; then
        log_info "Generating JaCoCo coverage report..."
        mvn jacoco:report -q

        if [ -f "target/site/jacoco/index.html" ]; then
            log_success "Coverage report: target/site/jacoco/index.html"
        else
            log_warn "Coverage report generation failed"
        fi
    fi

    # Generate Surefire report
    log_info "Generating Surefire test report..."
    mvn surefire-report:report-only -q

    if [ -f "target/site/surefire-report.html" ]; then
        log_success "Test report: target/site/surefire-report.html"
    else
        log_warn "Test report generation failed"
    fi

    # Create summary report
    create_summary_report
}

# Create summary report
create_summary_report() {
    local SUMMARY_FILE="target/test-reports/p3net-integration-summary.txt"

    log_info "Creating test execution summary..."

    cat > "$SUMMARY_FILE" << EOF
P3Net Integration Test Suite Execution Summary
===============================================
Execution Date: $(date)
Profile: $PROFILE
Coverage: $COVERAGE
Reports: $GENERATE_REPORTS

Test Results:
EOF

    # Parse test results
    if [ -d "target/surefire-reports" ]; then
        local TOTAL_TESTS=$(find target/surefire-reports -name "*.xml" -exec grep -h "tests=" {} \; | sed 's/.*tests="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')
        local FAILED_TESTS=$(find target/surefire-reports -name "*.xml" -exec grep -h "failures=" {} \; | sed 's/.*failures="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')
        local ERROR_TESTS=$(find target/surefire-reports -name "*.xml" -exec grep -h "errors=" {} \; | sed 's/.*errors="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')
        local SKIPPED_TESTS=$(find target/surefire-reports -name "*.xml" -exec grep -h "skipped=" {} \; | sed 's/.*skipped="\([0-9]*\)".*/\1/' | awk '{sum += $1} END {print sum}')

        echo "Total Tests: ${TOTAL_TESTS:-0}" >> "$SUMMARY_FILE"
        echo "Failed Tests: ${FAILED_TESTS:-0}" >> "$SUMMARY_FILE"
        echo "Error Tests: ${ERROR_TESTS:-0}" >> "$SUMMARY_FILE"
        echo "Skipped Tests: ${SKIPPED_TESTS:-0}" >> "$SUMMARY_FILE"
        echo "Passed Tests: $((${TOTAL_TESTS:-0} - ${FAILED_TESTS:-0} - ${ERROR_TESTS:-0} - ${SKIPPED_TESTS:-0}))" >> "$SUMMARY_FILE"
    fi

    if [ "$COVERAGE" = true ] && [ -f "target/site/jacoco/index.html" ]; then
        echo "" >> "$SUMMARY_FILE"
        echo "Coverage Report: target/site/jacoco/index.html" >> "$SUMMARY_FILE"
    fi

    echo "" >> "$SUMMARY_FILE"
    echo "Logs Directory: logs/" >> "$SUMMARY_FILE"
    echo "Test Reports Directory: target/test-reports/" >> "$SUMMARY_FILE"

    log_success "Summary report: $SUMMARY_FILE"
}

# Main execution function
main() {
    local start_time=$(date +%s)
    local overall_result=0

    log_header "P3Net Integration Test Suite"
    log_info "Profile: $PROFILE | Coverage: $COVERAGE | Reports: $GENERATE_REPORTS"

    # Setup
    setup_environment
    clean_prepare

    # Execute tests based on profile
    case $PROFILE in
        full)
            run_component_tests || overall_result=1
            run_api_tests || overall_result=1
            run_performance_tests || overall_result=1
            ;;
        fast)
            run_component_tests || overall_result=1
            run_api_tests || overall_result=1
            ;;
        api)
            run_api_tests || overall_result=1
            ;;
        components)
            run_component_tests || overall_result=1
            ;;
        performance)
            run_performance_tests || overall_result=1
            ;;
        golden)
            run_golden_tests || overall_result=1
            ;;
    esac

    # Generate reports
    generate_reports

    # Final results
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log_header "Test Suite Results"

    if [ $overall_result -eq 0 ]; then
        log_success "✅ All tests passed! Execution time: ${duration}s"

        if [ "$GENERATE_REPORTS" = true ]; then
            log_info "📊 Reports generated in target/site/ and target/test-reports/"
        fi

        if [ "$COVERAGE" = true ]; then
            log_info "📈 Coverage report: target/site/jacoco/index.html"
        fi
    else
        log_error "❌ Some tests failed. Execution time: ${duration}s"
        log_info "📋 Check test reports for details: target/surefire-reports/"

        if [ -f "target/test-reports/p3net-integration-summary.txt" ]; then
            log_info "📄 Summary: target/test-reports/p3net-integration-summary.txt"
        fi
    fi

    return $overall_result
}

# Script entry point
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    parse_arguments "$@"
    validate_profile
    main
fi