# TenantResourceIntegrationTest - Comprehensive API Testing

## Overview
This integration test suite provides comprehensive coverage for the TenantResource API endpoints in the Obvian system. The test follows Spring Boot testing best practices and covers all required scenarios for multi-tenant resource management.

## Test Coverage

### API Endpoints Tested
1. **GET /api/v1/tenant/{tenantId}/resources** - Resource usage dashboard
2. **POST /api/v1/tenant/{tenantId}/resources/allocate** - Resource allocation
3. **DELETE /api/v1/tenant/{tenantId}/resources/release** - Resource release
4. **POST /api/v1/tenant/{tenantId}/resources/reserve** - Enterprise reservations
5. **GET /api/v1/tenant/{tenantId}/resources/analytics** - Usage analytics
6. **PUT /api/v1/tenant/{tenantId}/resources/quota** - Quota overrides (Admin only)

### Tenant Tiers Tested
- **BASIC** - Limited resources (100MB memory, 10 concurrent operations)
- **STANDARD** - Moderate resources (500MB memory, 50 concurrent operations)
- **ENTERPRISE** - High resources (2GB memory, 200 concurrent operations) + reservations

### Test Scenarios

#### Authentication & Authorization
- ✅ Requires authentication for all endpoints
- ✅ Tenant access permission enforcement
- ✅ Admin-only access for quota overrides
- ✅ Role-based access control (USER vs ADMIN)

#### Resource Usage Dashboard
- ✅ Basic dashboard retrieval
- ✅ Detailed dashboard with history and trends
- ✅ Invalid tenant handling
- ✅ Error response handling

#### Resource Allocation
- ✅ Successful allocation for all tier types
- ✅ Quota exceeded scenarios for BASIC tier
- ✅ Request parameter validation
- ✅ Priority handling (LOW, NORMAL, HIGH)
- ✅ Resource type validation (MEMORY, CPU, OPERATIONS)

#### Resource Release
- ✅ Successful resource release
- ✅ Parameter validation (resourceType, amount)
- ✅ Release failure handling
- ✅ Edge cases (zero amount, missing parameters)

#### Enterprise Reservations
- ✅ Successful reservation creation for ENTERPRISE tenants
- ✅ Reservation denial for non-ENTERPRISE tenants
- ✅ Request validation (duration, amount, justification)
- ✅ Insufficient resources handling
- ✅ Guaranteed vs non-guaranteed reservations

#### Usage Analytics
- ✅ Analytics retrieval with configurable time windows
- ✅ Prediction inclusion/exclusion
- ✅ Time window validation (1 hour to 1 year)
- ✅ Invalid parameter handling

#### Quota Overrides
- ✅ Admin-only quota override application
- ✅ Non-admin access denial
- ✅ Request validation (reason, validUntil, approvedBy)
- ✅ Override expiration handling

#### Concurrent Operations
- ✅ Concurrent allocation requests
- ✅ Thread safety verification
- ✅ Resource conflict resolution

#### Error Handling
- ✅ Service layer exception handling
- ✅ Malformed JSON request handling
- ✅ Unsupported HTTP method handling
- ✅ Graceful error responses with proper status codes

## Technical Features

### Spring Boot Integration
- Uses `@SpringBootTest` with random port for full integration testing
- Integrates with Spring Security for authentication testing
- Uses MockMvc for precise HTTP request/response testing
- Supports multiple Spring profiles (integration-test)

### Mocking Strategy
- **@SpyBean** for TenantResourceService to allow partial mocking
- **@MockBean** for core dependencies (ResourceArbitrator, ResourceManager, etc.)
- Comprehensive mock setup for realistic service responses
- Sample data creation for consistent test scenarios

### Test Organization
- **@TestMethodOrder** with explicit ordering for logical test flow
- **@Tag** annotations for test categorization (integration, api)
- **@DisplayName** for clear test documentation
- Parallel execution support with **@Execution(ExecutionMode.CONCURRENT)**

### Data Management
- Sample data factories for all resource-related entities
- Realistic tenant contexts for all three tiers
- Mock resource metrics, availability, and usage history
- Proper cleanup between tests

## Usage

### Running the Tests
```bash
# Run all integration tests
mvn test -Dtest=TenantResourceIntegrationTest

# Run with specific Spring profile
mvn test -Dtest=TenantResourceIntegrationTest -Dspring.profiles.active=integration-test

# Run specific test method
mvn test -Dtest=TenantResourceIntegrationTest#shouldAllocateResourceForEnterpriseTier
```

### Test Configuration
The test uses the following configuration:
- **Port**: Random port for isolation
- **Profile**: integration-test
- **Security**: Enabled with mock users
- **Caching**: Disabled for test consistency
- **Rate Limiting**: Disabled for test performance

### Mock Data
- **Basic Tenant**: basic-tenant-123
- **Standard Tenant**: standard-tenant-456  
- **Enterprise Tenant**: enterprise-tenant-789
- **Invalid Tenant**: invalid-tenant-999

## Maintenance Notes

### Adding New Tests
1. Follow the existing naming convention: `should[Action][Condition]`
2. Use appropriate `@Order` annotation for logical sequence
3. Include both happy path and error scenarios
4. Update mock setup methods as needed

### Extending Mock Data
1. Add new sample data methods following the `createSample*()` pattern
2. Update `setupMockResponses()` to include new scenarios
3. Maintain consistency with actual service implementations

### Performance Considerations
- Tests run concurrently where possible
- Caching is disabled to prevent test interference
- Mock responses are created once per test class
- Cleanup is performed automatically between tests

## Dependencies
- Spring Boot 3.x
- Spring Security 6.x
- JUnit 5
- Mockito
- Jackson (JSON processing)
- AssertJ (fluent assertions)

## Integration with CI/CD
This test suite is designed to run in automated pipelines:
- No external dependencies (uses mocks)
- Fast execution (parallel where possible)
- Clear failure reporting with descriptive test names
- Proper resource cleanup

The test provides comprehensive coverage of all TenantResource API functionality and serves as both regression protection and living documentation of the API behavior.