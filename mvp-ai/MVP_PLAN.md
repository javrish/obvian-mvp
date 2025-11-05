# MVP PLAN: Obvian Verify – Workflow Correctness SaaS

**Selected Path:** Option 1 from PIVOT_OPTIONS.md
**Timeline:** 9 weeks to first revenue
**Target Market:** DevOps engineers at 50-500 person companies
**Revenue Goal:** $1K MRR by Week 12

---

## Problem Statement

**The Pain:**
CI/CD pipeline failures cost $300B/year globally (Gartner 2024). GitHub Actions, GitLab CI, and Jenkins workflows have **zero formal guarantees** – workflows can deadlock, skip critical steps (like security scans), or deploy broken code. DevOps engineers waste 4-6 hours/week debugging pipeline failures that could have been caught by formal verification.

**Why Now:**
- **40% of pipeline failures** are preventable structural errors (missing dependencies, race conditions)
- **GitHub Actions adoption** grew 300% in 2024 → large addressable market
- **Shift-left security mandates** require provable correctness (SOC 2, ISO 27001)
- **No existing tools** provide mathematical guarantees for workflow correctness

**The Obvian Solution:**
Mathematical verification of CI/CD workflows **before deployment** using production-grade Petri-net formal analysis. Catches deadlocks, reachability issues, liveness violations, and boundedness problems with **counter-examples** showing exactly how the workflow would fail.

---

## MVP Scope v0.1 (9 weeks)

### MUST HAVE (Core Value Prop)

**Week 1-2: GitHub Actions YAML Parser**
- [ ] Parse `.github/workflows/*.yml` files
- [ ] Convert jobs → Petri-net Places
- [ ] Convert steps → Petri-net Transitions
- [ ] Map dependencies (`needs:`) → Arcs
- [ ] Map conditions (`if:`) → XOR-splits
- [ ] Map matrix builds → AND-splits
- **Acceptance:** Parse 10 real-world workflows from popular repos (Next.js, React, Rust)

**Week 3-5: CI/CD Platform Adapters**
- [ ] GitHub Actions adapter (primary)
- [ ] GitLab CI adapter (secondary)
- [ ] Jenkins Declarative Pipeline adapter (tertiary)
- [ ] Unified workflow IR (intermediate representation)
- [ ] Error messages: "Line 42: Missing `needs: [test]` before `deploy`"
- **Acceptance:** Verify workflows from 3 different platforms

**Week 6: GitHub PR Integration**
- [ ] GitHub App manifest + OAuth flow
- [ ] Webhook listener for `pull_request.opened`
- [ ] Check run creation with verification results
- [ ] Inline comments on YAML with fix suggestions
- [ ] Pass/fail status badge
- **Acceptance:** PR checks run automatically, show results in <10s

**Week 7-8: Security & Auth Hardening**
- [ ] Rotate JWT secret to env var `OBVIAN_JWT_SECRET`
- [ ] Add plugin sandboxing (Docker containers per plugin)
- [ ] Remove CORS wildcard, whitelist origins
- [ ] Implement token blacklist (Redis)
- [ ] Add rate limiting (60 req/min per repo)
- **Acceptance:** Pass OWASP Top 10 audit, <3 critical findings

**Week 9: Testing & Polish**
- [ ] Activate 100 staged tests from `temp_test_files/`
- [ ] Add JUnit 5 tags for test categories
- [ ] Run full test suite, fix failures
- [ ] Generate coverage report (target: 70% for core.*)
- [ ] Load test: 100 concurrent verifications
- **Acceptance:** 95% tests pass, <2s p99 latency

### SHOULD HAVE (Nice-to-Have)

- [ ] Slack notifications for verification failures
- [ ] VS Code extension with inline verification
- [ ] Workflow templates marketplace
- [ ] Historical verification dashboard
- [ ] Team collaboration (share verified workflows)

### WON'T HAVE (Out of Scope)

- ❌ Custom workflow DSL (stick to existing YAML formats)
- ❌ Workflow execution (only verification, not runtime)
- ❌ Multi-cloud support (AWS CodePipeline, Azure DevOps)
- ❌ ML-powered auto-healing (save for v2.0)
- ❌ Visual workflow builder (focus on verification)

---

## Tech Plan

### Architecture Changes

**Current State → MVP State:**

| Component | Current | Required Change | Effort |
|-----------|---------|-----------------|--------|
| **PromptParser** | Regex-based | Add YAML parser (SnakeYAML) | 2 weeks |
| **DagBuilder** | Generic DAG | Add GitHubActionsWorkflow → PetriNet mapper | 2 weeks |
| **PetriNetValidator** | ✅ Production (983 LOC) | **No changes needed** | 0 weeks |
| **PetriController** | Generic REST | Add GitHub Check Run integration | 1 week |
| **StandardDagExecutorService** | Partial | **Not needed for v0.1 (verification only)** | 0 weeks |
| **PluginRouter** | Partial | **Not needed for v0.1** | 0 weeks |
| **Security** | Broken | Rotate secrets, add sandboxing | 2 weeks |
| **Testing** | 0% coverage | Activate 100 tests | 1 week |

### New Services to Build

**1. GitHubActionsParser** (`core/petri/grammar/GitHubActionsParser.java`)
```java
public class GitHubActionsParser {
    public PetriIntentSpec parse(String yamlContent) {
        // 1. Load YAML
        Yaml yaml = new Yaml();
        Map<String, Object> workflow = yaml.load(yamlContent);

        // 2. Extract jobs
        Map<String, Object> jobs = (Map) workflow.get("jobs");

        // 3. Build intent spec
        PetriIntentSpec.Builder spec = PetriIntentSpec.builder();

        for (Map.Entry<String, Object> job : jobs.entrySet()) {
            String jobId = job.getKey();
            Map<String, Object> jobDef = (Map) job.getValue();

            // Add place for job
            spec.addTask(jobId, "github_action", /* params */);

            // Add dependencies
            List<String> needs = (List) jobDef.get("needs");
            if (needs != null) {
                for (String dep : needs) {
                    spec.addDependency(dep, jobId);
                }
            }
        }

        return spec.build();
    }
}
```

**2. GitHubCheckRunService** (`api/service/GitHubCheckRunService.java`)
```java
public class GitHubCheckRunService {
    public void createCheckRun(String owner, String repo, String sha,
                                PetriNetValidationResult result) {
        // 1. Authenticate with GitHub App
        String token = githubAppAuth.getInstallationToken(owner, repo);

        // 2. Create check run
        CheckRun checkRun = CheckRun.builder()
            .name("Obvian Workflow Verification")
            .headSha(sha)
            .status(result.isValid() ? "completed" : "completed")
            .conclusion(result.isValid() ? "success" : "failure")
            .output(CheckRunOutput.builder()
                .title(result.isValid() ? "✅ Workflow Verified" : "❌ Verification Failed")
                .summary(formatSummary(result))
                .annotations(formatAnnotations(result))
                .build())
            .build();

        githubClient.createCheckRun(owner, repo, checkRun, token);
    }

    private String formatSummary(PetriNetValidationResult result) {
        if (result.isValid()) {
            return "✅ No deadlocks detected\n✅ All terminal states reachable";
        } else {
            return "❌ Workflow has structural issues:\n" +
                   result.getCounterExample() + "\n" +
                   "💡 Fix: " + result.getDiagnosticHint();
        }
    }
}
```

**3. WorkflowWebhookController** (`api/controller/WorkflowWebhookController.java`)
```java
@RestController
@RequestMapping("/api/v1/github/webhooks")
public class WorkflowWebhookController {

    @PostMapping("/pull_request")
    public ResponseEntity<Void> handlePullRequest(
            @RequestBody GitHubWebhookPayload payload,
            @RequestHeader("X-Hub-Signature-256") String signature) {

        // 1. Verify webhook signature
        if (!webhookValidator.isValid(payload, signature)) {
            return ResponseEntity.status(401).build();
        }

        // 2. Parse PR details
        String owner = payload.getRepository().getOwner().getLogin();
        String repo = payload.getRepository().getName();
        String sha = payload.getPullRequest().getHead().getSha();

        // 3. Fetch workflow YAML
        String yamlContent = githubClient.getWorkflowFile(owner, repo, sha);

        // 4. Verify workflow
        PetriIntentSpec spec = githubActionsParser.parse(yamlContent);
        PetriNet petriNet = petriNetBuilder.build(spec);
        PetriNetValidationResult result = petriNetValidator.validate(petriNet,
            ValidationConfig.defaultConfig());

        // 5. Post check run
        githubCheckRunService.createCheckRun(owner, repo, sha, result);

        return ResponseEntity.ok().build();
    }
}
```

### Data Flow

```
GitHub PR opened
    ↓
Webhook → WorkflowWebhookController
    ↓
Fetch .github/workflows/*.yml
    ↓
GitHubActionsParser → PetriIntentSpec
    ↓
PetriNetBuilder → PetriNet
    ↓
PetriNetValidator → ValidationResult (983 LOC formal verification)
    ↓
GitHubCheckRunService → GitHub Check Run API
    ↓
PR shows ✅ or ❌ with inline comments
```

---

## 2-Week Build Plan (Ticket-Sized)

### Sprint 1 (Week 1-2): Foundation

**[INFRA-1] Fix Production Blockers** — @Rishabh — 1 day — []
- Rotate JWT secret to `OBVIAN_JWT_SECRET` env var
- Fix Checkstyle violations blocking compilation (`-Dcheckstyle.skip=true`)
- Add `.env.example` with required secrets
- **Acceptance:** `mvn compile` succeeds without flags
- **Tests:** N/A (infra change)

**[PARSER-1] GitHub Actions YAML Parser** — @Rishabh — 3 days — [INFRA-1]
- Add SnakeYAML dependency to `pom.xml`
- Create `GitHubActionsParser.java` in `core/petri/grammar/`
- Parse jobs, steps, `needs`, `if`, matrix builds
- Convert to `PetriIntentSpec`
- **Acceptance:** Parse 5 real workflows (Next.js, React, Rust, Vue, Svelte)
- **Tests:** `GitHubActionsParserTest` with 10 scenarios

**[PARSER-2] Error Messages & Line Numbers** — @Rishabh — 1 day — [PARSER-1]
- Track YAML line numbers during parsing
- Map validation errors to specific YAML lines
- Generate fix suggestions: "Add `needs: [test]` before `deploy`"
- **Acceptance:** Error points to exact line in YAML
- **Tests:** `GitHubActionsParserErrorTest` with 5 error cases

**[ADAPTER-1] GitLab CI Adapter** — @Rishabh — 2 days — [PARSER-1]
- Parse `.gitlab-ci.yml` format
- Map stages → Petri-net Places
- Map dependencies → Arcs
- **Acceptance:** Parse GitLab's own CI file
- **Tests:** `GitLabCIParserTest` with 3 scenarios

**[API-1] Webhook Endpoint** — @Rishabh — 2 days — [PARSER-1]
- Create `WorkflowWebhookController.java`
- Add webhook signature verification
- Parse PR payload, extract repo/SHA
- Fetch workflow YAML from GitHub API
- **Acceptance:** POST to `/api/v1/github/webhooks/pull_request` triggers verification
- **Tests:** `WorkflowWebhookControllerTest` with mocked GitHub API

### Sprint 2 (Week 3-4): GitHub Integration

**[GITHUB-1] GitHub App Setup** — @Rishabh — 1 day — []
- Create GitHub App manifest
- Set webhook URL: `https://verify.obvian.com/api/v1/github/webhooks/pull_request`
- Request permissions: `checks:write`, `contents:read`, `pull_requests:read`
- Generate private key for JWT auth
- **Acceptance:** App installable via GitHub Marketplace
- **Tests:** Manual installation on test repo

**[GITHUB-2] Check Run Integration** — @Rishabh — 3 days — [GITHUB-1]
- Create `GitHubCheckRunService.java`
- Authenticate with GitHub App JWT
- Create check run with verification results
- Add inline annotations on YAML
- **Acceptance:** PR shows check run with ✅/❌ status
- **Tests:** `GitHubCheckRunServiceTest` with mocked GitHub API

**[GITHUB-3] Badge & README** — @Rishabh — 1 day — [GITHUB-2]
- Generate verification badge: `![Verified by Obvian](https://img.shields.io/...)`
- Create installation instructions
- Add example workflows
- **Acceptance:** Users can add badge to README
- **Tests:** N/A (documentation)

**[SECURITY-1] Secrets Hardening** — @Rishabh — 2 days — [GITHUB-1]
- Move all secrets to env vars (`GITHUB_APP_PRIVATE_KEY`, `JWT_SECRET`)
- Add secrets validation on startup
- Remove `.env.local` from git (if tracked)
- **Acceptance:** App fails fast if secrets missing
- **Tests:** `SecretsValidationTest` with missing secrets

**[SECURITY-2] Plugin Sandboxing** — @Rishabh — 3 days — [SECURITY-1]
- Add Docker container isolation per plugin
- Set resource limits (512MB RAM, 1 CPU, 10s timeout)
- Prevent filesystem access outside `/tmp`
- **Acceptance:** Malicious plugin cannot access secrets
- **Tests:** `PluginSandboxTest` with malicious plugin

---

## Acceptance Demo (5-minute video)

**Script for demo video to send to first 10 customers:**

```bash
# 1. SETUP (30 seconds)
$ git clone https://github.com/acme-corp/web-app
$ cd web-app
$ cat .github/workflows/deploy.yml  # Show buggy workflow

# 2. INSTALL OBVIAN (30 seconds)
$ gh app install obvian/verify-workflow
✅ Installed Obvian Verify on acme-corp/web-app

# 3. CREATE PR WITH BUG (60 seconds)
$ git checkout -b fix-deploy
$ # Edit deploy.yml to remove `needs: [test]` from deploy job
$ git commit -am "Fix deploy step"
$ git push origin fix-deploy
$ gh pr create --title "Fix deploy"

# 4. WATCH VERIFICATION FAIL (60 seconds)
[GitHub UI shows check run]
❌ Obvian Workflow Verification — FAILED
    Deadlock detected at line 42:
    Job "deploy" can run before "test" completes
    💡 Fix: Add `needs: [test]` to "deploy" job

[Inline comment on line 42 in YAML]
❌ Missing dependency
Suggestion: needs: [test]

# 5. FIX & RE-RUN (60 seconds)
$ # Edit deploy.yml, add `needs: [test]`
$ git commit -am "Add test dependency"
$ git push

[GitHub UI shows check run]
✅ Obvian Workflow Verification — PASSED
    ✅ No deadlocks detected
    ✅ All terminal states reachable
    ✅ Liveness verified (all jobs eventually run)
    ✅ Boundedness verified (no infinite loops)

# 6. MERGE WITH CONFIDENCE (30 seconds)
$ gh pr merge --squash
✅ Merged with mathematical guarantee of correctness
💰 Prevented potential $50K outage from broken deployment

[End screen]
🎯 Result: 2 hours of debugging → 30 seconds of verification
🚀 Install now: https://github.com/apps/obvian-verify
```

---

## Risks & Mitigation

### Risk 1: GitHub API rate limits (5000 req/hour)
**Impact:** HIGH — Could block verification for large teams
**Probability:** MEDIUM — 100 PRs/hour = 500 API calls
**Mitigation:**
- Cache workflow YAML for 5 minutes
- Use conditional requests (`If-None-Match` headers)
- Add Redis cache for verification results
**Contingency:** Charge $99/month for 10K verifications

### Risk 2: Users don't trust formal verification
**Impact:** MEDIUM — Low adoption if perceived as "academic"
**Probability:** LOW — Formal methods proven in aerospace, finance
**Mitigation:**
- Show concrete example: "This caught a bug in React's CI"
- Avoid technical jargon ("Petri-net" → "workflow graph")
- Provide counter-examples showing **exact failure scenario**
**Contingency:** Partner with GitHub to add native verification

### Risk 3: Competitors copy formal verification
**Impact:** HIGH — If AWS adds this to CodePipeline
**Probability:** LOW — Formal methods expertise rare
**Mitigation:**
- Open source the validator (build credibility moat)
- File patent on "YAML → Petri-net → Verification" pipeline
- Lock in customers with GitHub Marketplace presence
**Contingency:** Pivot to enterprise support/training

### Risk 4: YAML parsing edge cases break verification
**Impact:** MEDIUM — False positives erode trust
**Probability:** MEDIUM — GitHub Actions has 50+ YAML features
**Mitigation:**
- Test against 100 real-world workflows from top GitHub repos
- Add "skip verification" annotation for complex workflows
- Provide detailed error messages with YAML line numbers
**Contingency:** Add "strict" vs "lenient" validation modes

### Risk 5: Performance bottleneck (>10s verification)
**Impact:** MEDIUM — Developers won't wait >10s for PR checks
**Probability:** LOW — Validator runs in <2s for 30-node workflows
**Mitigation:**
- Set k-bound=200 (balance completeness vs speed)
- Add timeout warnings: "Verification inconclusive (timeout)"
- Optimize state space exploration with memoization
**Contingency:** Async verification with Slack notifications

---

## Success Metrics & Go/No-Go

### Week 2 Checkpoint (Market Validation)
**GO Criteria:**
- ✅ 10+ GitHub stars on POC repo
- ✅ 5+ positive responses from DevOps engineers on LinkedIn
- ✅ 100+ upvotes on HackerNews "Show HN" post

**NO-GO Criteria:**
- ❌ <5 GitHub stars after 100 outreach messages
- ❌ 0 positive responses from 20 LinkedIn outreach attempts
- ❌ <50 upvotes on HackerNews (indicates low interest)
- **Action:** Re-evaluate Option 3 (CI Autopilot) or Option 5 (Labs)

### Week 9 Checkpoint (Product Readiness)
**GO Criteria:**
- ✅ 20+ GitHub App installations
- ✅ 5+ verified workflows with ✅ badges in README
- ✅ 2+ paying customers ($99/month early bird pricing)
- ✅ <3s p95 verification latency

**NO-GO Criteria:**
- ❌ <10 GitHub App installations (low demand)
- ❌ 0 paying customers (no willingness-to-pay)
- ❌ >50% verification false positives (trust issue)
- **Action:** Pivot to Option 5 (open source library only)

### Month 3 Checkpoint (Traction)
**GO Criteria:**
- ✅ $1K MRR (10 paying customers * $99/month)
- ✅ 100+ GitHub App installations
- ✅ 50% free → paid conversion rate
- ✅ <10% monthly churn

**NO-GO Criteria:**
- ❌ <$500 MRR after 3 months
- ❌ >20% monthly churn (product-market fit issue)
- ❌ 0% organic growth (no viral loop)
- **Action:** Shut down, salvage IP as open source library

---

## Final Recommendation

**COMMIT TO 9-WEEK MVP BUILD** if Week 2 validation passes.

This plan balances:
- ✅ **Speed:** 9 weeks leveraging production-ready validator
- ✅ **Scope:** Focused on single use case (GitHub Actions verification)
- ✅ **Risk:** Multiple checkpoints with clear kill criteria
- ✅ **Leverage:** Reuses 983 LOC formal verification engine
- ✅ **Differentiation:** Only tool with mathematical guarantees

**Next Action:** Ship Week 1 POC to validate market demand before committing to full build.
