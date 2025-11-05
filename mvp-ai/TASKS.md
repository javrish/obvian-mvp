# ENGINEERING TASKS: 2-Week Sprint Plan

**Project:** Obvian Verify MVP
**Sprint Duration:** 2 weeks (80 hours)
**Team:** Solo (Rishabh)
**Goal:** Ship functional GitHub Actions verification POC

---

## Sprint Structure

**Week 1:** Foundation (YAML parsing, GitHub webhook, validation integration)
**Week 2:** GitHub App (check runs, inline comments, polish)

---

## Week 1 Tasks (40 hours)

### Monday (Day 1-2): Production Blockers & Setup

**[INFRA-1]** Fix Compilation Issues — **Rishabh** — **4h** — **Dependencies:** None
**Description:**
Fix Checkstyle violations blocking Maven compilation. Currently `mvn compile` fails with 4,025 violations.

**Acceptance Criteria:**
- [ ] `mvn compile` succeeds without `-Dcheckstyle.skip=true`
- [ ] All Java files pass Google Checks style rules
- [ ] CI/CD pipeline runs cleanly

**Tests:**
- N/A (infra change, verified by successful compilation)

**Implementation Notes:**
- Run `mvn spotless:apply` to auto-format code
- Fix remaining violations manually (likely import order, line length)
- Update `.editorconfig` for consistent formatting

---

**[INFRA-2]** Rotate Secrets & Environment Setup — **Rishabh** — **4h** — **Dependencies:** None
**Description:**
Move hardcoded secrets from `application.properties` to environment variables. Fix SECURITY_REVIEW.md critical issues #1 and #6.

**Acceptance Criteria:**
- [ ] `OBVIAN_JWT_SECRET` loaded from env (not hardcoded)
- [ ] `GITHUB_APP_PRIVATE_KEY` loaded from env
- [ ] `.env.example` created with placeholder values
- [ ] `.env.local` removed from git history (if tracked)
- [ ] App fails fast with clear error if secrets missing

**Tests:**
- Create `SecretsValidationTest` to verify startup fails when `OBVIAN_JWT_SECRET` missing
- Test with empty env vars to ensure proper error messages

**Implementation Notes:**
```java
// Add to application.properties
obvian.jwt.secret=${OBVIAN_JWT_SECRET:}
obvian.github.app.private-key=${GITHUB_APP_PRIVATE_KEY:}

// Validate on startup in @PostConstruct
if (jwtSecret == null || jwtSecret.isEmpty()) {
    throw new IllegalStateException("OBVIAN_JWT_SECRET environment variable required");
}
```

---

### Tuesday-Thursday (Day 3-6): YAML Parser & Validation Pipeline

**[PARSER-1]** GitHub Actions YAML Parser — **Rishabh** — **12h** — **Dependencies:** [INFRA-1]
**Description:**
Create parser to convert `.github/workflows/*.yml` files to `PetriIntentSpec` format for formal verification.

**Acceptance Criteria:**
- [ ] Parse jobs, steps, `needs` dependencies, `if` conditions
- [ ] Convert to `PetriIntentSpec` with proper Places/Transitions/Arcs
- [ ] Handle matrix builds (create AND-split for parallel jobs)
- [ ] Parse 10 real-world workflows: Next.js, React, Rust, Vue, Svelte, Kubernetes, Terraform, Django, Rails, Laravel
- [ ] Generate parse errors with YAML line numbers

**Tests:**
- `GitHubActionsParserTest` with 15 test cases:
  - Simple workflow (2 jobs, linear dependency)
  - Complex workflow (5 jobs, matrix build, conditions)
  - Edge cases: empty workflow, missing `needs`, circular deps
  - Error cases: invalid YAML, malformed `needs` array
  - Real workflows from top 10 GitHub repos

**Implementation Notes:**
```java
package core.petri.grammar;

import org.yaml.snakeyaml.Yaml;
import core.petri.PetriIntentSpec;

public class GitHubActionsParser {
    public PetriIntentSpec parse(String yamlContent) {
        Yaml yaml = new Yaml();
        Map<String, Object> workflow = yaml.load(yamlContent);

        PetriIntentSpec.Builder spec = PetriIntentSpec.builder();
        Map<String, Object> jobs = (Map) workflow.get("jobs");

        for (Map.Entry<String, Object> job : jobs.entrySet()) {
            String jobId = job.getKey();
            Map<String, Object> jobDef = (Map) job.getValue();

            // Add task node
            spec.addTask(jobId, "github_action", extractJobParams(jobDef));

            // Add dependencies
            List<String> needs = (List) jobDef.get("needs");
            if (needs != null) {
                for (String dep : needs) {
                    spec.addDependency(dep, jobId);
                }
            }

            // Handle matrix builds (AND-split)
            Map<String, Object> strategy = (Map) jobDef.get("strategy");
            if (strategy != null && strategy.containsKey("matrix")) {
                spec.markAsParallel(jobId);
            }
        }

        return spec.build();
    }
}
```

**DoD (Definition of Done):**
- [ ] Code merged to `main`
- [ ] Tests pass: `mvn test -Dtest=GitHubActionsParserTest`
- [ ] Code coverage >80% for parser class
- [ ] Documentation added to README with example usage

---

**[PARSER-2]** Error Messages with Line Numbers — **Rishabh** — **4h** — **Dependencies:** [PARSER-1]
**Description:**
Track YAML line numbers during parsing to provide precise error messages pointing to exact failure locations.

**Acceptance Criteria:**
- [ ] Parse errors show YAML line number
- [ ] Validation errors mapped back to YAML lines
- [ ] Fix suggestions generated: "Line 42: Add `needs: [test]` before `deploy`"
- [ ] Error message includes snippet of surrounding YAML lines (context)

**Tests:**
- `GitHubActionsParserErrorTest` with 8 error scenarios:
  - Missing `needs` dependency
  - Circular dependency detected
  - Invalid job name (reserved keyword)
  - Malformed YAML syntax
  - Unknown workflow keys

**Implementation Notes:**
- Use SnakeYAML's `Mark` object to track line/column during parsing
- Store line numbers in `PetriIntentSpec` metadata
- When validation fails, map Petri-net node back to YAML line
```java
// Track line numbers
Map<String, Integer> nodeLineNumbers = new HashMap<>();
Mark mark = ((Node) jobNode).getStartMark();
nodeLineNumbers.put(jobId, mark.getLine());

// Generate error with line number
throw new ParseException(
    "Line " + nodeLineNumbers.get(jobId) + ": Missing dependency",
    nodeLineNumbers.get(jobId)
);
```

---

**[API-1]** Webhook Endpoint — **Rishabh** — **8h** — **Dependencies:** [PARSER-1]
**Description:**
Create REST endpoint to receive GitHub webhook events for `pull_request.opened` and trigger workflow verification.

**Acceptance Criteria:**
- [ ] `POST /api/v1/github/webhooks/pull_request` endpoint created
- [ ] Webhook signature verification (HMAC SHA-256)
- [ ] Extract owner, repo, SHA from payload
- [ ] Fetch workflow YAML from GitHub API
- [ ] Trigger verification pipeline
- [ ] Return 200 OK immediately (async processing)

**Tests:**
- `WorkflowWebhookControllerTest` with mocked GitHub API:
  - Valid webhook signature → verification triggered
  - Invalid signature → 401 Unauthorized
  - Missing workflow file → 404 error logged
  - GitHub API timeout → retry with exponential backoff
  - Webhook replay attack → reject duplicate delivery IDs

**Implementation Notes:**
```java
@RestController
@RequestMapping("/api/v1/github/webhooks")
public class WorkflowWebhookController {

    @PostMapping("/pull_request")
    public ResponseEntity<Void> handlePullRequest(
            @RequestBody String rawPayload,
            @RequestHeader("X-Hub-Signature-256") String signature,
            @RequestHeader("X-GitHub-Delivery") String deliveryId) {

        // 1. Verify signature
        String computed = "sha256=" + Hmac.hmacSha256(rawPayload, webhookSecret);
        if (!MessageDigest.isEqual(computed.getBytes(), signature.getBytes())) {
            logger.warn("Invalid webhook signature for delivery {}", deliveryId);
            return ResponseEntity.status(401).build();
        }

        // 2. Parse payload
        GitHubWebhookPayload payload = objectMapper.readValue(rawPayload, GitHubWebhookPayload.class);
        String owner = payload.getRepository().getOwner().getLogin();
        String repo = payload.getRepository().getName();
        String sha = payload.getPullRequest().getHead().getSha();

        // 3. Async verification
        executor.submit(() -> verifyWorkflow(owner, repo, sha));

        return ResponseEntity.ok().build();
    }
}
```

---

**[VALIDATION-1]** End-to-End Verification Pipeline — **Rishabh** — **4h** — **Dependencies:** [PARSER-1], [API-1]
**Description:**
Wire up full pipeline: YAML → PetriIntentSpec → PetriNet → Validation → Result.

**Acceptance Criteria:**
- [ ] Fetch workflow YAML from GitHub API
- [ ] Parse to `PetriIntentSpec`
- [ ] Build `PetriNet` using existing `PetriNetBuilder`
- [ ] Validate using existing `PetriNetValidator` (983 LOC, no changes)
- [ ] Return `PetriNetValidationResult` with pass/fail status
- [ ] Log full trace to console

**Tests:**
- `E2EVerificationPipelineTest` integration test:
  - Mock GitHub API responses
  - Inject test YAML with known bug (missing dependency)
  - Assert validation fails with expected error message
  - Test pass case with valid workflow

**Implementation Notes:**
- Reuse existing services: `PetriNetBuilder`, `PetriNetValidator`
- No changes needed to validator (already production-ready)
- Focus on integration glue code

---

### Friday (Day 7): Testing & Documentation

**[TEST-1]** Activate Staged Tests — **Rishabh** — **4h** — **Dependencies:** None
**Description:**
Move 15-20 high-value tests from `temp_test_files/` to `tests/` and add JUnit 5 tags for profile-based execution.

**Acceptance Criteria:**
- [ ] Move `PetriNetValidatorTest`, `PetriTokenSimulatorTest`, `PetriToDagProjectorTest` to `tests/core/petri/`
- [ ] Add `@Tag("unit")` to fast tests
- [ ] Add `@Tag("integration")` to integration tests
- [ ] Run `mvn test -Dgroups=unit` → all tests pass
- [ ] Generate coverage report: `mvn jacoco:report`

**Tests:**
- Verify test migration: `mvn test -Dtest=PetriNetValidatorTest`
- Check coverage: Target >70% for `core.petri.*` packages

**Implementation Notes:**
- Start with core Petri-net tests (highest value)
- Fix any test failures before moving to next test
- Update `pom.xml` test profiles if needed

---

**[DOC-1]** README & Quick Start — **Rishabh** — **4h** — **Dependencies:** [API-1]
**Description:**
Update README with GitHub Actions verification use case and installation instructions.

**Acceptance Criteria:**
- [ ] Add "Workflow Verification" section to README
- [ ] Include example: Parse → Validate → Fix suggestions
- [ ] Document webhook setup instructions
- [ ] Add curl examples for manual testing
- [ ] Update architecture diagram with new components

**Tests:**
- N/A (documentation)
- Manual verification: Follow instructions on fresh machine

**Implementation Notes:**
```markdown
## Workflow Verification

Obvian provides **mathematical guarantees** for CI/CD workflow correctness.

### Quick Start

1. Install webhook:
   \`\`\`bash
   curl -X POST https://api.github.com/repos/owner/repo/hooks \
     -H "Authorization: token $GITHUB_TOKEN" \
     -d '{"config":{"url":"https://verify.obvian.com/api/v1/github/webhooks/pull_request"}}'
   \`\`\`

2. Create PR with workflow change

3. See verification results in PR checks
```

---

## Week 2 Tasks (40 hours)

### Monday-Wednesday (Day 8-11): GitHub App Integration

**[GITHUB-1]** GitHub App Manifest & Setup — **Rishabh** — **6h** — **Dependencies:** None
**Description:**
Create GitHub App with proper permissions and webhook configuration for production deployment.

**Acceptance Criteria:**
- [ ] GitHub App created with name "Obvian Workflow Verify"
- [ ] Permissions: `checks:write`, `contents:read`, `pull_requests:read`
- [ ] Webhook URL: `https://verify.obvian.com/api/v1/github/webhooks/pull_request`
- [ ] Generate private key for JWT authentication
- [ ] Test installation on personal repo
- [ ] Document installation steps in README

**Tests:**
- Manual: Install app on test repo
- Verify webhook fires when PR created
- Check app permissions in GitHub settings

**Implementation Notes:**
- Create app at https://github.com/settings/apps/new
- Download private key, store as `GITHUB_APP_PRIVATE_KEY` env var
- Note app ID, store as `GITHUB_APP_ID` env var

---

**[GITHUB-2]** JWT Authentication Service — **Rishabh** — **6h** — **Dependencies:** [GITHUB-1]
**Description:**
Implement GitHub App JWT authentication to create installation access tokens for API calls.

**Acceptance Criteria:**
- [ ] Generate JWT signed with private key (RS256)
- [ ] Get installation token for specific repo
- [ ] Handle token expiration (60 minutes)
- [ ] Cache tokens (don't regenerate for every API call)
- [ ] Log authentication flow for debugging

**Tests:**
- `GitHubAppAuthTest`:
  - Generate JWT with mock private key
  - Verify JWT structure (header, payload, signature)
  - Test token refresh when expired
  - Test token caching (don't call GitHub API twice)

**Implementation Notes:**
```java
public class GitHubAppAuthService {
    public String getInstallationToken(String owner, String repo) {
        // 1. Generate JWT
        String jwt = Jwts.builder()
            .setIssuer(appId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + 10 * 60 * 1000))
            .signWith(privateKey, SignatureAlgorithm.RS256)
            .compact();

        // 2. Get installation ID
        Installation installation = githubClient.getInstallation(owner, repo, jwt);

        // 3. Create access token
        AccessToken token = githubClient.createInstallationToken(installation.getId(), jwt);
        return token.getToken();
    }
}
```

---

**[GITHUB-3]** Check Run Creation Service — **Rishabh** — **10h** — **Dependencies:** [GITHUB-2]
**Description:**
Create GitHub Check Run with verification results, including pass/fail status, summary, and inline annotations on YAML.

**Acceptance Criteria:**
- [ ] Create check run with name "Obvian Workflow Verification"
- [ ] Set status: `completed`
- [ ] Set conclusion: `success` or `failure`
- [ ] Add summary with verification results
- [ ] Add inline annotations on YAML lines with issues
- [ ] Support markdown formatting in summary
- [ ] Link to detailed verification report

**Tests:**
- `GitHubCheckRunServiceTest` with mocked GitHub API:
  - Success case: No validation errors → ✅ check run
  - Failure case: Deadlock detected → ❌ with annotation
  - Test annotation format (path, line, message)
  - Verify summary markdown rendering

**Implementation Notes:**
```java
public class GitHubCheckRunService {
    public void createCheckRun(String owner, String repo, String sha,
                                PetriNetValidationResult result) {
        String token = authService.getInstallationToken(owner, repo);

        CheckRun checkRun = CheckRun.builder()
            .name("Obvian Workflow Verification")
            .headSha(sha)
            .status("completed")
            .conclusion(result.isValid() ? "success" : "failure")
            .output(CheckRunOutput.builder()
                .title(result.isValid() ? "✅ Workflow Verified" : "❌ Verification Failed")
                .summary(formatSummary(result))
                .annotations(formatAnnotations(result))
                .build())
            .build();

        githubClient.post("/repos/" + owner + "/" + repo + "/check-runs", checkRun, token);
    }

    private List<Annotation> formatAnnotations(PetriNetValidationResult result) {
        if (result.isValid()) return Collections.emptyList();

        return List.of(Annotation.builder()
            .path(".github/workflows/deploy.yml")
            .startLine(result.getErrorLineNumber())
            .endLine(result.getErrorLineNumber())
            .annotationLevel("failure")
            .message(result.getDiagnosticHint())
            .title("Missing dependency")
            .build());
    }
}
```

---

### Thursday-Friday (Day 12-14): Security & Polish

**[SECURITY-1]** Plugin Sandboxing (Phase 1) — **Rishabh** — **8h** — **Dependencies:** None
**Description:**
Add Docker container isolation for plugin execution to prevent malicious plugins from accessing secrets or filesystem.

**Acceptance Criteria:**
- [ ] Each plugin runs in isolated Docker container
- [ ] Resource limits: 512MB RAM, 1 CPU core, 10s timeout
- [ ] No filesystem access outside `/tmp`
- [ ] No network access except whitelisted domains
- [ ] Kill container after execution (no persistent state)

**Tests:**
- `PluginSandboxTest`:
  - Malicious plugin tries to read `/etc/passwd` → blocked
  - Plugin tries to access `OBVIAN_JWT_SECRET` → blocked
  - Plugin exceeds 512MB RAM → killed
  - Plugin exceeds 10s timeout → killed
  - Plugin tries to fork bomb → blocked

**Implementation Notes:**
```java
public class SandboxedPluginExecutor {
    public PluginResult execute(Plugin plugin, Map<String, Object> input) {
        String containerId = dockerClient.createContainer(
            ContainerConfig.builder()
                .image("obvian-plugin-runner:latest")
                .cmd(List.of("java", "-jar", plugin.getJarPath()))
                .hostConfig(HostConfig.builder()
                    .memoryLimit(512 * 1024 * 1024) // 512MB
                    .cpuQuota(100000) // 1 CPU
                    .readonlyRootfs(true)
                    .networkMode("none")
                    .build())
                .build()
        );

        dockerClient.startContainer(containerId);
        dockerClient.waitContainer(containerId, 10); // 10s timeout

        String output = dockerClient.logs(containerId);
        dockerClient.removeContainer(containerId, true);

        return parsePluginResult(output);
    }
}
```

---

**[SECURITY-2]** CORS & Rate Limiting — **Rishabh** — **4h** — **Dependencies:** None
**Description:**
Fix CORS wildcard and add rate limiting to prevent abuse.

**Acceptance Criteria:**
- [ ] CORS: Whitelist only `https://github.com`, `https://verify.obvian.com`
- [ ] Rate limit: 60 requests/minute per repo
- [ ] Rate limit: 10 requests/minute per IP (webhook endpoint)
- [ ] Return `429 Too Many Requests` with `Retry-After` header
- [ ] Log rate limit violations for monitoring

**Tests:**
- `RateLimitTest`:
  - 61st request in 1 minute → 429 error
  - Wait 60 seconds → requests allowed again
  - Different repos → separate rate limits

**Implementation Notes:**
```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "https://github.com",
            "https://verify.obvian.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

**[TEST-2]** Integration Test Suite — **Rishabh** — **6h** — **Dependencies:** [GITHUB-3]
**Description:**
Create end-to-end integration tests covering full webhook → verification → check run flow.

**Acceptance Criteria:**
- [ ] Mock GitHub API responses
- [ ] Simulate webhook POST to `/api/v1/github/webhooks/pull_request`
- [ ] Assert check run created with correct status
- [ ] Test error cases: invalid signature, missing workflow, timeout
- [ ] Test real workflows from top 5 GitHub repos

**Tests:**
- `E2EGitHubIntegrationTest`:
  - Happy path: Valid workflow → ✅ check run
  - Failure path: Deadlock detected → ❌ with annotation
  - Error path: Invalid webhook signature → 401
  - Timeout path: GitHub API timeout → retry logic
  - Edge case: Empty workflow file → parse error

**Implementation Notes:**
- Use WireMock to mock GitHub API
- Use TestRestTemplate to call webhook endpoint
- Assert on check run payload sent to GitHub API

---

**[POLISH-1]** Performance Optimization — **Rishabh** — **4h** — **Dependencies:** None
**Description:**
Optimize verification latency to <3s p95 (currently ~5-10s for complex workflows).

**Acceptance Criteria:**
- [ ] Cache workflow YAML for 5 minutes (reduce GitHub API calls)
- [ ] Cache validation results for identical workflows
- [ ] Add Redis cache for verification results
- [ ] Measure latency: p50 <1s, p95 <3s, p99 <10s
- [ ] Load test: 100 concurrent verifications

**Tests:**
- `PerformanceTest`:
  - Verify caching reduces latency by 50%
  - Test cache invalidation when workflow changes
  - Load test: 100 concurrent requests → all complete in <10s

**Implementation Notes:**
- Use Caffeine cache for in-memory caching
- Use Redis for distributed caching
```java
@Cacheable(value = "workflows", key = "#owner + ':' + #repo + ':' + #sha")
public String fetchWorkflowYaml(String owner, String repo, String sha) {
    return githubClient.getFileContent(owner, repo, ".github/workflows/deploy.yml", sha);
}
```

---

**[POLISH-2]** Monitoring & Alerting — **Rishabh** — **2h** — **Dependencies:** None
**Description:**
Add Prometheus metrics and logging for production monitoring.

**Acceptance Criteria:**
- [ ] Metrics: verification latency histogram
- [ ] Metrics: verification pass/fail counters
- [ ] Metrics: GitHub API error rate
- [ ] Metrics: webhook processing queue depth
- [ ] Logs: Structured JSON logs with trace IDs

**Tests:**
- `MetricsTest`:
  - Verify metrics incremented on verification
  - Test histogram buckets (100ms, 500ms, 1s, 5s, 10s)

**Implementation Notes:**
```java
@Component
public class VerificationMetrics {
    private final Counter verificationsTotal;
    private final Histogram verificationLatency;

    public void recordVerification(boolean passed, Duration latency) {
        verificationsTotal.labels(passed ? "pass" : "fail").inc();
        verificationLatency.observe(latency.toMillis() / 1000.0);
    }
}
```

---

## Task Dependencies Graph

```
[INFRA-1] ────┐
              ├──> [PARSER-1] ──> [PARSER-2]
[INFRA-2] ────┘        │
                       ├──> [API-1] ──> [VALIDATION-1]
                       │
                       └──> [TEST-1]

[GITHUB-1] ──> [GITHUB-2] ──> [GITHUB-3]
                                   │
                                   └──> [TEST-2]

[SECURITY-1] ──┐
               ├──> [POLISH-1] ──> [POLISH-2]
[SECURITY-2] ──┘
```

---

## Sprint Velocity Tracking

**Week 1 Target:** 40 story points
- Day 1-2: 8 points ([INFRA-1, INFRA-2])
- Day 3-6: 24 points ([PARSER-1, PARSER-2, API-1, VALIDATION-1])
- Day 7: 8 points ([TEST-1, DOC-1])

**Week 2 Target:** 40 story points
- Day 8-11: 22 points ([GITHUB-1, GITHUB-2, GITHUB-3])
- Day 12-14: 18 points ([SECURITY-1, SECURITY-2, TEST-2, POLISH-1, POLISH-2])

**Total Sprint:** 80 story points

---

## Definition of Done (DoD)

Every task must meet these criteria before marking as complete:

- [ ] Code merged to `main` branch
- [ ] All tests pass: `mvn test`
- [ ] Code coverage >70% for new code (check JaCoCo report)
- [ ] No new Checkstyle violations
- [ ] No new SpotBugs warnings
- [ ] Documentation updated (JavaDoc + README)
- [ ] Reviewed by: Self (solo project, but review own PR before merge)
- [ ] Acceptance criteria met (all checkboxes ticked)

---

## Daily Standup Format (Self-Check)

**Every morning, ask:**
1. What did I complete yesterday?
2. What am I working on today?
3. Any blockers or risks?

**Every evening, record:**
- Story points completed
- Actual vs estimated hours
- Lessons learned / surprises

---

## Risk Mitigation

**If Week 1 runs behind:**
- ✂️ Cut [PARSER-2] (error messages) → defer to Week 2
- ✂️ Cut [DOC-1] (documentation) → ship without polish

**If Week 2 runs behind:**
- ✂️ Cut [SECURITY-1] (sandboxing) → **CRITICAL, only cut if <2 days behind**
- ✂️ Cut [POLISH-1, POLISH-2] → ship without caching/metrics
- ⚠️ **DO NOT CUT:** [GITHUB-3] (check runs) = core value prop

---

## Success Criteria

**Week 1 Exit:** Can verify workflow YAML end-to-end via API call
**Week 2 Exit:** Can install GitHub App, see check runs on PRs

**MVP Launch:** Ship to 10 beta testers, collect feedback, iterate
