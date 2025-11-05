# RISK REGISTER: Obvian Verify MVP

**Project:** Obvian Verify – Workflow Correctness SaaS
**Last Updated:** Nov 2, 2025
**Owner:** Rishabh Pathak
**Review Cadence:** Weekly

---

## Risk Scoring Matrix

**Likelihood:** 1 (Rare) → 5 (Certain)
**Impact:** 1 (Negligible) → 5 (Critical)
**Risk Level:** Low (1-5) | Medium (6-12) | High (13-20) | Critical (21-25)

---

## Critical Risks (Score 21-25)

### R01: Hardcoded JWT Secret Compromised
**Category:** Security
**Likelihood:** 4 (Likely if codebase leaked)
**Impact:** 5 (Complete authentication bypass)
**Risk Level:** **20 (CRITICAL)**

**Description:**
JWT secret currently hardcoded in `application.properties:111`. If repository is public or leaked, attackers can generate valid tokens and access all customer data.

**Impact:**
- Complete authentication bypass
- Unauthorized access to all customer workflows
- Data breach / regulatory fines (GDPR: avg €877K)
- Reputation damage

**Mitigation:**
1. **Immediate (Day 1):** Rotate secret to environment variable `OBVIAN_JWT_SECRET`
2. **Week 1:** Remove `.env.local` from git history (if tracked)
3. **Week 2:** Implement token blacklist (Redis) for revocation
4. **Week 3:** Add secret rotation policy (quarterly)

**Contingency:**
If secret is leaked:
- Immediately rotate secret
- Invalidate all existing tokens
- Force password reset for all users
- Notify affected customers within 72 hours (GDPR)

**Owner:** Rishabh
**Status:** 🔴 OPEN (requires immediate action)
**Target Close:** Day 1 (4 hours)

---

### R02: Zero Plugin Sandboxing
**Category:** Security
**Likelihood:** 5 (Certain if malicious plugin uploaded)
**Impact:** 5 (Arbitrary code execution, data exfiltration)
**Risk Level:** **25 (CRITICAL)**

**Description:**
Plugins currently run in same JVM as main application with full filesystem and network access. Malicious plugin can:
- Read `OBVIAN_JWT_SECRET`, `GITHUB_APP_PRIVATE_KEY`
- Access customer data from database
- Exfiltrate data to attacker-controlled server
- Fork bomb / DoS attack
- Modify other plugins / compromise system

**Impact:**
- Complete system compromise
- Customer data breach
- Regulatory fines (SOC 2 non-compliance)
- Service disruption

**Mitigation:**
1. **Week 2:** Implement Docker container isolation per plugin
2. **Week 3:** Set resource limits (512MB RAM, 1 CPU, 10s timeout)
3. **Week 4:** Add plugin signature verification (only run signed plugins)
4. **Week 5:** Implement plugin permission system (request network/filesystem access)

**Contingency:**
If malicious plugin detected:
- Immediately kill all plugin executions
- Quarantine affected customer data
- Rotate all secrets
- Audit logs for data exfiltration
- Notify affected customers

**Owner:** Rishabh
**Status:** 🔴 OPEN (blocks production deployment)
**Target Close:** Week 2 (8 hours)

---

## High Risks (Score 13-20)

### R03: GitHub API Rate Limits
**Category:** Technical
**Likelihood:** 4 (Likely at 100+ customers)
**Impact:** 4 (Service degradation, customer impact)
**Risk Level:** **16 (HIGH)**

**Description:**
GitHub API limits: 5,000 requests/hour for authenticated requests. At 100 PRs/hour * 5 API calls per PR = 500 requests/hour. Headroom exists but growth will hit limits.

**Impact:**
- Verification delays (>1 minute)
- Failed verifications (rate limit errors)
- Customer complaints
- Churn

**Mitigation:**
1. **Week 2:** Implement 5-minute cache for workflow YAML
2. **Week 3:** Use conditional requests (`If-None-Match` headers) to reduce API calls
3. **Week 4:** Add Redis cache for verification results
4. **Week 5:** Monitor rate limit headers, scale API usage horizontally

**Contingency:**
If rate limit hit:
- Queue verifications with exponential backoff
- Prioritize paying customers (rate limit free tier first)
- Notify affected customers with ETA
- Consider GitHub Enterprise license ($21/user/month for higher limits)

**Owner:** Rishabh
**Status:** 🟡 OPEN (monitor, mitigate in Week 2-3)
**Target Close:** Week 3

---

### R04: Users Don't Trust Formal Verification
**Category:** Go-to-Market
**Likelihood:** 3 (Possible if messaging wrong)
**Impact:** 5 (Zero adoption, product failure)
**Risk Level:** **15 (HIGH)**

**Description:**
Developers may perceive formal verification as "academic" or "too complex" and prefer manual reviews / traditional CI/CD tools. "Petri-net" terminology may alienate target users.

**Impact:**
- <10 GitHub App installations after launch
- 0% free → paid conversion
- Product failure, wasted 9 weeks

**Mitigation:**
1. **Week 1 (Validation):** Post to HackerNews "Show HN: We built formal verification for GitHub Actions"
   - Target: >100 upvotes (validates interest)
   - If <50 upvotes → PIVOT to Option 5 (open source library)
2. **Week 2:** Avoid jargon in messaging ("Petri-net" → "workflow graph")
3. **Week 3:** Show concrete example: "Caught a bug in React's CI workflow"
4. **Week 4:** Provide counter-examples with **exact failure scenarios** (not abstract theory)

**Contingency:**
If Week 1 validation fails (<10 upvotes on HN, 0 positive LinkedIn responses):
- **PIVOT to Option 5:** Open source library (Obvian Labs)
  - Same codebase, different packaging
  - Target: Academic credibility + enterprise support contracts
  - Revenue model: $10K/year support instead of $99/month SaaS

**Owner:** Rishabh
**Status:** 🟡 OPEN (Week 1 validation checkpoint)
**Target Close:** Week 1 (kill criteria defined)

---

### R05: YAML Parsing Edge Cases
**Category:** Technical
**Likelihood:** 4 (Likely given GitHub Actions has 50+ features)
**Impact:** 3 (False positives erode trust, but not fatal)
**Risk Level:** **12 (MEDIUM-HIGH)**

**Description:**
GitHub Actions YAML has complex features: matrix builds, reusable workflows, composite actions, environment variables, secrets, `if` conditions with complex expressions. Parser may misinterpret edge cases and produce false positives/negatives.

**Impact:**
- False positives: Workflow flagged as broken when it's correct → customer frustration
- False negatives: Workflow approved but has bug → trust erosion
- Customer churn (>20% monthly)

**Mitigation:**
1. **Week 1:** Test against 100 real-world workflows from top GitHub repos
2. **Week 2:** Add "skip verification" annotation: `# obvian-skip: complex-matrix-build`
3. **Week 3:** Provide detailed error messages with YAML line numbers
4. **Week 4:** Add "strict" vs "lenient" validation modes (lenient = fewer false positives)

**Contingency:**
If >30% false positive rate:
- Add workflow complexity score: "This workflow is too complex for verification (score: 87/100)"
- Suggest manual review for complex workflows
- Improve parser iteratively based on customer feedback

**Owner:** Rishabh
**Status:** 🟡 OPEN (mitigate in Week 1-3)
**Target Close:** Week 3

---

## Medium Risks (Score 6-12)

### R06: Performance Bottleneck (>10s Verification)
**Category:** Technical
**Likelihood:** 2 (Unlikely if k-bound=200)
**Impact:** 3 (Customer experience degradation, but not fatal)
**Risk Level:** **6 (MEDIUM)**

**Description:**
PetriNetValidator uses bounded state space exploration (k-bound=200 states). Complex workflows with many parallel branches may exceed this limit, causing timeouts or inconclusive results.

**Impact:**
- Verification takes >10s (developers won't wait)
- "Verification inconclusive" error (frustration)
- Some workflows cannot be verified

**Mitigation:**
1. **Week 2:** Set default timeout: 5s (balance completeness vs speed)
2. **Week 3:** Add timeout warnings: "Verification inconclusive (timeout after 5s)"
3. **Week 4:** Optimize state space exploration with memoization
4. **Week 5:** Provide async verification option (Slack notification when done)

**Contingency:**
If >10% workflows timeout:
- Increase k-bound to 500 (may slow down verification)
- Add heuristic-based pruning (explore most likely paths first)
- Offer "fast" vs "thorough" verification modes

**Owner:** Rishabh
**Status:** 🟢 OPEN (low priority, monitor in production)
**Target Close:** N/A (ongoing monitoring)

---

### R07: Competitors Copy Formal Verification
**Category:** Market
**Likelihood:** 2 (Unlikely in 6 months, but possible long-term)
**Impact:** 4 (Revenue loss, moat erosion)
**Risk Level:** **8 (MEDIUM)**

**Description:**
AWS CodePipeline, GitHub, or GitLab may add built-in formal verification to their platforms, eliminating need for Obvian Verify. Formal methods expertise is rare but not impossible to acquire.

**Impact:**
- Revenue loss (customers migrate to free built-in solution)
- First-mover advantage eroded
- Pivot required

**Mitigation:**
1. **Week 4:** Open source core validator (build credibility moat)
   - Package as `com.obvian:workflow-verifier` on Maven Central
   - Apache 2.0 license (permissive, encourages adoption)
2. **Week 6:** Publish academic paper at PLDI/POPL (lock in credibility)
3. **Week 8:** Lock in customers with GitHub Marketplace presence (network effects)
4. **Week 10:** File patent on "YAML → Petri-net → Verification" pipeline

**Contingency:**
If competitor ships formal verification:
- **PIVOT to Option 5:** Focus on enterprise support ($10K/year contracts)
- Position as "advanced" verification vs built-in "basic" verification
- Add features competitors won't build: custom rules, compliance reports

**Owner:** Rishabh
**Status:** 🟢 OPEN (monitor, mitigate in Week 4-10)
**Target Close:** N/A (ongoing monitoring)

---

### R08: Development Overruns (>9 Weeks)
**Category:** Execution
**Likelihood:** 3 (Possible if YAML parser harder than expected)
**Impact:** 3 (Delayed launch, increased burn rate)
**Risk Level:** **9 (MEDIUM)**

**Description:**
MVP plan assumes 9 weeks to launch. Risk factors:
- YAML parser more complex than estimated (50+ GitHub Actions features)
- GitHub App OAuth flow tricky (JWT authentication has edge cases)
- Plugin sandboxing harder than expected (Docker networking issues)

**Impact:**
- Launch delayed to Week 12-14
- Miss market window (competitor ships first)
- Increased burn rate ($0 revenue for longer)

**Mitigation:**
1. **Week 1:** Timebox each task (if >20% over estimate, cut scope)
2. **Week 2:** Cut nice-to-haves: [PARSER-2] error messages, [DOC-1] documentation
3. **Week 3:** Ship with 80% complete (defer [SECURITY-1] sandboxing if >2 days behind)
4. **Weekly:** Review velocity (target: 40 story points/week)

**Contingency:**
If Week 4 behind schedule:
- Cut GitLab CI adapter → focus on GitHub Actions only
- Ship without inline YAML annotations → defer to v1.1
- Accept technical debt (refactor post-launch)

**Owner:** Rishabh
**Status:** 🟢 OPEN (monitor weekly)
**Target Close:** N/A (ongoing monitoring)

---

## Low Risks (Score 1-5)

### R09: Infrastructure Costs Exceed Budget
**Category:** Financial
**Likelihood:** 1 (Rare at <100 customers)
**Impact:** 2 (Manageable cost overrun)
**Risk Level:** **2 (LOW)**

**Description:**
Infrastructure costs may exceed $500/month budget if customer growth faster than expected or Docker container costs high.

**Impact:**
- Monthly burn increases from $500 to $1,000
- Delayed profitability

**Mitigation:**
- Use DigitalOcean ($40/month for 4GB Droplet)
- Use Redis Cloud free tier (30MB)
- Use PostgreSQL Heroku free tier (10K rows)
- Monitor costs weekly

**Contingency:**
If costs >$1,000/month → increase pricing to $149/month (from $99)

**Owner:** Rishabh
**Status:** 🟢 OPEN (monitor monthly)

---

### R10: Zero Organic Growth (No Viral Loop)
**Category:** Go-to-Market
**Likelihood:** 2 (Unlikely if badge works)
**Impact:** 3 (Growth stalls, but paid marketing can compensate)
**Risk Level:** **6 (MEDIUM)**

**Description:**
MVP relies on viral loop: "✅ Verified by Obvian" badge in README drives discovery. If badge doesn't drive organic installs, growth stalls.

**Impact:**
- Growth limited to paid acquisition ($500 CAC)
- Slower path to $10K MRR

**Mitigation:**
1. **Week 3:** Make badge visible ("Verified by Obvian" → link to verification report)
2. **Week 4:** Add social proof ("Join 100+ teams using Obvian")
3. **Week 5:** Create template gallery (pre-verified workflows for common use cases)

**Contingency:**
If 0% organic growth after 3 months:
- Invest in paid acquisition (Google Ads, LinkedIn)
- Partner with GitHub (featured in Marketplace)
- Content marketing (blog posts on CI/CD best practices)

**Owner:** Rishabh
**Status:** 🟢 OPEN (monitor post-launch)

---

## Risk Mitigation Timeline

**Week 1:**
- 🔴 **R01:** Rotate JWT secret (CRITICAL)
- 🟡 **R04:** Validate market demand (HackerNews post)
- 🟡 **R05:** Test YAML parser against 100 real workflows

**Week 2:**
- 🔴 **R02:** Implement plugin sandboxing (CRITICAL)
- 🟡 **R03:** Add workflow YAML caching

**Week 3:**
- 🟡 **R03:** Implement conditional requests, Redis cache
- 🟡 **R05:** Add error messages with line numbers

**Week 4:**
- 🟡 **R07:** Open source core validator (Maven Central)

**Post-Launch:**
- 🟢 **R06, R08, R09, R10:** Monitor and adjust

---

## Risk Review Cadence

**Daily (during sprint):**
- Review open CRITICAL risks (R01, R02)
- Update risk status if new information emerges

**Weekly (during build):**
- Review all HIGH risks (R03, R04, R05)
- Adjust mitigation plans based on progress

**Monthly (post-launch):**
- Review all risks
- Add new risks based on customer feedback
- Close resolved risks

---

## Kill Criteria (When to Shut Down / Pivot)

**Week 1 Validation Fails:**
- <10 GitHub stars on POC
- <5 positive responses from 20 LinkedIn outreach
- <50 HackerNews upvotes
- **ACTION:** PIVOT to Option 5 (Obvian Labs open source)

**Week 9 Launch Fails:**
- <10 GitHub App installations
- 0 paying customers
- >50% verification false positives
- **ACTION:** Shut down, salvage IP as open source library

**Month 3 Traction Fails:**
- <$500 MRR (5 paying customers)
- >20% monthly churn
- 0% organic growth
- **ACTION:** Shut down, move on to next idea

---

## Risk Escalation Path

**If risk becomes CRITICAL (score >20):**
1. Stop all other work
2. Notify stakeholders (investors, co-founders if applicable)
3. Implement emergency mitigation within 24 hours
4. Document incident for post-mortem

**If multiple HIGH risks open simultaneously:**
1. Reassess MVP scope (cut features)
2. Extend timeline (9 weeks → 12 weeks)
3. Consider hiring contractor for specific risk area

---

**Last Review:** Nov 2, 2025
**Next Review:** Nov 9, 2025
**Owner:** Rishabh Pathak
