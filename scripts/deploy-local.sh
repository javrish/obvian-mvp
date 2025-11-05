#!/bin/bash

# Local Development Deployment Script for Obvian Petri Net POC
# This script sets up the complete development environment

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_requirements() {
    log_info "Checking system requirements..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker Desktop."
        exit 1
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose."
        exit 1
    fi

    # Check Java
    if ! command -v java &> /dev/null; then
        log_warning "Java not found. It's recommended for local development."
    else
        java_version=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ $java_version -lt 17 ]]; then
            log_warning "Java 17+ recommended. Current version: $(java -version 2>&1 | head -1)"
        fi
    fi

    # Check Node.js
    if ! command -v node &> /dev/null; then
        log_warning "Node.js not found. It's recommended for local development."
    else
        node_version=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
        if [[ $node_version -lt 18 ]]; then
            log_warning "Node.js 18+ recommended. Current version: $(node -v)"
        fi
    fi

    # Check available disk space (need at least 2GB)
    available_space=$(df -BG "$PROJECT_ROOT" | awk 'NR==2 {print $4}' | sed 's/G//')
    if [[ $available_space -lt 2 ]]; then
        log_warning "Low disk space detected. Need at least 2GB for Docker containers."
    fi

    log_success "System requirements check completed"
}

check_ports() {
    log_info "Checking if required ports are available..."

    ports=(3000 8080 5432 6379)
    for port in "${ports[@]}"; do
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
            log_warning "Port $port is already in use"
            case $port in
                3000) log_warning "  Frontend will conflict. Stop existing React app." ;;
                8080) log_warning "  Backend will conflict. Stop existing Spring Boot app." ;;
                5432) log_warning "  PostgreSQL will conflict. Stop existing PostgreSQL." ;;
                6379) log_warning "  Redis will conflict. Stop existing Redis." ;;
            esac
        else
            log_info "  Port $port is available"
        fi
    done
}

build_backend() {
    log_info "Building backend application..."

    cd "$PROJECT_ROOT"

    # Check if Maven wrapper exists
    if [[ -f "./mvnw" ]]; then
        MAVEN_CMD="./mvnw"
    elif command -v mvn &> /dev/null; then
        MAVEN_CMD="mvn"
    else
        log_error "Maven not found. Please install Maven or use Maven wrapper."
        exit 1
    fi

    # Run tests first
    log_info "Running backend tests..."
    $MAVEN_CMD test -P fast-tests -q

    # Build application
    log_info "Building backend JAR..."
    $MAVEN_CMD clean package -DskipTests -q

    log_success "Backend build completed"
}

build_frontend() {
    log_info "Building frontend application..."

    cd "$PROJECT_ROOT/frontend"

    if [[ ! -f "package.json" ]]; then
        log_error "Frontend package.json not found"
        exit 1
    fi

    # Install dependencies
    if [[ ! -d "node_modules" ]] || [[ "package-lock.json" -nt "node_modules" ]]; then
        log_info "Installing frontend dependencies..."
        npm ci
    fi

    # Run tests
    log_info "Running frontend tests..."
    npm run test:vitest -- --run --silent

    log_success "Frontend build completed"
    cd "$PROJECT_ROOT"
}

setup_environment() {
    log_info "Setting up environment files..."

    # Create .env file if it doesn't exist
    ENV_FILE="$PROJECT_ROOT/.env"
    if [[ ! -f "$ENV_FILE" ]]; then
        cat > "$ENV_FILE" << EOF
# Obvian Petri Net POC Environment Configuration

# Database Configuration
POSTGRES_DB=obvian_poc
POSTGRES_USER=obvian
POSTGRES_PASSWORD=poc_password_2024

# Application Configuration
SPRING_PROFILES_ACTIVE=development
POC_ENVIRONMENT=true
POC_MAX_PLACES=30
POC_MAX_TRANSITIONS=30

# Frontend Configuration
REACT_APP_API_URL=http://localhost:8080
REACT_APP_POC_MODE=true

# Development Settings
JAVA_OPTS=-Xmx1g -Xms512m -XX:+UseG1GC
NODE_ENV=development
EOF
        log_success "Created .env file"
    else
        log_info ".env file already exists"
    fi

    # Create necessary directories
    mkdir -p "$PROJECT_ROOT/logs"
    mkdir -p "$PROJECT_ROOT/data/postgres"
    mkdir -p "$PROJECT_ROOT/data/redis"

    log_success "Environment setup completed"
}

start_services() {
    log_info "Starting services with Docker Compose..."

    cd "$PROJECT_ROOT"

    # Choose compose command
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    # Pull latest images
    log_info "Pulling Docker images..."
    $COMPOSE_CMD pull --quiet

    # Build and start services
    log_info "Building and starting containers..."
    $COMPOSE_CMD up --build -d

    # Wait for services to be healthy
    log_info "Waiting for services to be ready..."

    # Wait for database
    max_attempts=30
    attempt=0
    while [[ $attempt -lt $max_attempts ]]; do
        if $COMPOSE_CMD exec -T postgres pg_isready -U obvian -d obvian_poc &>/dev/null; then
            log_success "Database is ready"
            break
        fi
        ((attempt++))
        log_info "Waiting for database... ($attempt/$max_attempts)"
        sleep 2
    done

    if [[ $attempt -eq $max_attempts ]]; then
        log_error "Database failed to start within timeout"
        show_logs
        exit 1
    fi

    # Wait for backend
    max_attempts=60
    attempt=0
    while [[ $attempt -lt $max_attempts ]]; do
        if curl -f http://localhost:8080/actuator/health &>/dev/null; then
            log_success "Backend is ready"
            break
        fi
        ((attempt++))
        log_info "Waiting for backend... ($attempt/$max_attempts)"
        sleep 2
    done

    if [[ $attempt -eq $max_attempts ]]; then
        log_error "Backend failed to start within timeout"
        show_logs
        exit 1
    fi

    # Check frontend
    max_attempts=30
    attempt=0
    while [[ $attempt -lt $max_attempts ]]; do
        if curl -f http://localhost:3000 &>/dev/null; then
            log_success "Frontend is ready"
            break
        fi
        ((attempt++))
        log_info "Waiting for frontend... ($attempt/$max_attempts)"
        sleep 2
    done

    log_success "All services are running!"
}

show_status() {
    log_info "Service Status:"

    cd "$PROJECT_ROOT"
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    echo ""
    $COMPOSE_CMD ps
    echo ""

    log_info "Service URLs:"
    echo "  🌐 Frontend:  http://localhost:3000"
    echo "  🚀 Backend:   http://localhost:8080"
    echo "  📊 API Docs:  http://localhost:8080/swagger-ui.html"
    echo "  🏥 Health:    http://localhost:8080/actuator/health"
    echo "  🐘 Database:  localhost:5432 (obvian/poc_password_2024)"
    echo "  🔴 Redis:     localhost:6379"
    echo ""

    # Show POC warnings
    log_warning "POC Environment Limitations:"
    echo "  • Maximum 30 places per Petri net"
    echo "  • Maximum 30 transitions per Petri net"
    echo "  • Development-grade security only"
    echo "  • Performance optimized for demonstration"
    echo ""
}

show_logs() {
    log_info "Recent service logs:"

    cd "$PROJECT_ROOT"
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    echo "=== Backend Logs ==="
    $COMPOSE_CMD logs --tail=20 backend-dev

    echo ""
    echo "=== Frontend Logs ==="
    $COMPOSE_CMD logs --tail=10 frontend-dev

    echo ""
    echo "=== Database Logs ==="
    $COMPOSE_CMD logs --tail=10 postgres
}

stop_services() {
    log_info "Stopping services..."

    cd "$PROJECT_ROOT"
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    $COMPOSE_CMD down
    log_success "Services stopped"
}

cleanup() {
    log_info "Cleaning up containers and volumes..."

    cd "$PROJECT_ROOT"
    if command -v docker-compose &> /dev/null; then
        COMPOSE_CMD="docker-compose"
    else
        COMPOSE_CMD="docker compose"
    fi

    $COMPOSE_CMD down -v --remove-orphans
    docker system prune -f
    log_success "Cleanup completed"
}

show_help() {
    echo "Obvian Petri Net POC - Local Deployment Script"
    echo ""
    echo "Usage: $0 [COMMAND]"
    echo ""
    echo "Commands:"
    echo "  start     Start all services (default)"
    echo "  stop      Stop all services"
    echo "  restart   Restart all services"
    echo "  status    Show service status"
    echo "  logs      Show service logs"
    echo "  build     Build applications without starting"
    echo "  cleanup   Stop services and clean up volumes"
    echo "  help      Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                 # Start all services"
    echo "  $0 start           # Start all services"
    echo "  $0 stop            # Stop all services"
    echo "  $0 logs            # Show recent logs"
    echo ""
}

# Main script logic
main() {
    local command="${1:-start}"

    case $command in
        start)
            log_info "Starting Obvian Petri Net POC..."
            check_requirements
            check_ports
            setup_environment
            build_backend
            build_frontend
            start_services
            show_status
            ;;
        stop)
            stop_services
            ;;
        restart)
            stop_services
            sleep 2
            main start
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs
            ;;
        build)
            check_requirements
            build_backend
            build_frontend
            ;;
        cleanup)
            cleanup
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "Unknown command: $command"
            show_help
            exit 1
            ;;
    esac
}

# Run main function
main "$@"