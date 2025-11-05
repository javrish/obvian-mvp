# Secrets Configuration Guide

**Status**: ✅ [INFRA-2] Completed
**Date**: 2025-11-05
**Task**: Rotate secrets from hardcoded values to environment variables

---

## Overview

All sensitive configuration values are now loaded from environment variables instead of being hardcoded in source files. This prevents accidental exposure of secrets in version control and enables secure deployment practices.

## Required Environment Variables

### Critical (Application will NOT start without these)

#### `OBVIAN_JWT_SECRET`
- **Purpose**: Signs and validates JWT authentication tokens
- **Generate**: `openssl rand -base64 64`
- **Minimum length**: 32 characters (256 bits for HS256)
- **Example**:
  ```bash
  export OBVIAN_JWT_SECRET="$(openssl rand -base64 64)"
  ```

### Optional (for GitHub Integration)

#### `GITHUB_APP_PRIVATE_KEY`
- **Purpose**: Authenticates with GitHub API for workflow verification
- **Required in**: Production (`prod` profile)
- **Optional in**: Dev/test environments
- **Format**: PEM-encoded RSA private key
- **Obtain from**: GitHub App Settings → Private Key
- **Example**:
  ```bash
  export GITHUB_APP_PRIVATE_KEY="$(cat your-app-private-key.pem)"
  ```

#### `GITHUB_APP_ID`
- **Purpose**: GitHub App identifier
- **Example**: `123456`

#### `GITHUB_APP_INSTALLATION_ID`
- **Purpose**: Installation ID for your GitHub App
- **Example**: `789012`

#### `GITHUB_WEBHOOK_SECRET`
- **Purpose**: Validates GitHub webhook signatures
- **Set in**: GitHub App Settings → Webhook
- **Generate**: `openssl rand -hex 32`

---

## Configuration Files

### `src/main/resources/application.properties`
Main application configuration with environment variable placeholders:
```properties
# JWT Configuration
obvian.jwt.secret=${OBVIAN_JWT_SECRET:}
obvian.jwt.expiration=${OBVIAN_JWT_EXPIRATION:86400000}
obvian.jwt.issuer=${OBVIAN_JWT_ISSUER:obvian-api}
obvian.jwt.audience=${OBVIAN_JWT_AUDIENCE:obvian-client}

# GitHub App Configuration
obvian.github.app.id=${GITHUB_APP_ID:}
obvian.github.app.private-key=${GITHUB_APP_PRIVATE_KEY:}
obvian.github.webhook.secret=${GITHUB_WEBHOOK_SECRET:}
```

### `.env.example`
Template for local development environment variables. Copy to `.env.local` and fill in values:
```bash
cp .env.example .env.local
# Edit .env.local with your actual values
source .env.local
```

### `.gitignore`
Ensures `.env.local` and similar files are never committed:
```
# Environment files with secrets
.env.local
.env.*.local
*.env
```

---

## Startup Validation

### `config/SecretsValidator.java`
Validates required secrets on application startup and fails fast with helpful error messages.

**Validation Rules**:
1. `OBVIAN_JWT_SECRET` must be set (always)
2. `OBVIAN_JWT_SECRET` must be at least 32 characters
3. `GITHUB_APP_PRIVATE_KEY` must be set in production profile
4. Fails with clear instructions if validation fails

**Error Example**:
```
CRITICAL: Application startup failed due to missing required secrets:

  ❌ OBVIAN_JWT_SECRET is not set
     Required for JWT token signing and validation
     Generate: openssl rand -base64 64
     Set: export OBVIAN_JWT_SECRET="your-generated-secret"

For complete setup instructions, see: docs/CONFIGURATION.md
For .env.example, see: .env.example
```

---

## Setup Instructions

### Quick Start (Development)

1. **Generate JWT Secret**:
   ```bash
   openssl rand -base64 64
   ```

2. **Create `.env.local`**:
   ```bash
   cp .env.example .env.local
   ```

3. **Edit `.env.local`** and set `OBVIAN_JWT_SECRET` with generated value

4. **Load environment**:
   ```bash
   source .env.local
   ```

5. **Start application**:
   ```bash
   mvn spring-boot:run
   ```

### Production Deployment

1. **Use Secrets Manager**:
   - AWS Secrets Manager
   - HashiCorp Vault
   - Azure Key Vault
   - Google Secret Manager

2. **Set environment variables** via your deployment platform:
   - Kubernetes: ConfigMap (non-sensitive) + Secrets (sensitive)
   - Docker: Docker secrets or `--env-file`
   - Heroku: `heroku config:set`
   - AWS ECS: Task definition environment variables
   - Cloud Run: Secret Manager integration

3. **Rotate secrets regularly** (recommended: every 90 days)

4. **Never log secrets** in error messages or application logs

---

## Security Best Practices

### ✅ DO:
- Use environment variables for all secrets
- Generate cryptographically secure random secrets
- Rotate secrets regularly (90 days)
- Use secrets managers in production
- Use asymmetric keys (RS256) for enhanced security
- Fail fast on startup if secrets are missing
- Document all required environment variables

### ❌ DON'T:
- Hardcode secrets in source code
- Commit `.env.local` or production secrets to git
- Use default/example values in production
- Share secrets via email, chat, or documents
- Log secrets in application logs
- Reuse secrets across environments

---

## Testing

### Unit Tests
**Location**: `tests/config/SecretsValidatorTest.java`

**Test Coverage**:
- ✅ Fails when JWT secret is missing
- ✅ Fails when JWT secret is too short
- ✅ Passes when JWT secret is valid in dev
- ✅ Warns but doesn't fail when GitHub key missing in dev
- ✅ Fails when GitHub key missing in production
- ✅ Passes when all secrets valid in production
- ✅ Provides helpful error messages

**Run Tests**:
```bash
mvn test -Dtest=SecretsValidatorTest
```

---

## Troubleshooting

### Application won't start - "OBVIAN_JWT_SECRET is not set"
**Solution**: Generate and set the JWT secret:
```bash
export OBVIAN_JWT_SECRET="$(openssl rand -base64 64)"
mvn spring-boot:run
```

### JWT secret too short error
**Problem**: Secret must be at least 32 characters (256 bits)
**Solution**: Use `openssl rand -base64 64` to generate a longer secret

### GitHub features not working
**Check**: Is `GITHUB_APP_PRIVATE_KEY` set?
```bash
echo $GITHUB_APP_PRIVATE_KEY | head -1
```

**Solution**: If empty, either:
1. Set the variable (for GitHub features)
2. Run in dev mode (GitHub features optional)

---

## Verification

**Verify secrets are properly configured**:
```bash
# Check JWT secret is set (don't print value!)
[ -n "$OBVIAN_JWT_SECRET" ] && echo "✓ JWT secret configured" || echo "✗ JWT secret missing"

# Check JWT secret length
echo -n "$OBVIAN_JWT_SECRET" | wc -c

# Start application and check logs
mvn spring-boot:run | grep "secrets validated"
```

**Expected output**:
```
✓ JWT secret validated (length: 88 characters)
✓ All required secrets validated successfully
```

---

## Related Files

- `src/main/resources/application.properties` - Main configuration
- `.env.example` - Environment variable template
- `.gitignore` - Prevents committing secrets
- `config/SecretsValidator.java` - Startup validation logic
- `security/JwtService.java` - Uses JWT secret for token operations
- `tests/config/SecretsValidatorTest.java` - Validation tests

---

## Acceptance Criteria (From TASKS.md)

✅ `OBVIAN_JWT_SECRET` loaded from env (not hardcoded)
✅ `GITHUB_APP_PRIVATE_KEY` loaded from env
✅ `.env.example` created with placeholder values
✅ `.env.local` removed from git history (never tracked)
✅ App fails fast with clear error if secrets missing
✅ SecretsValidationTest created with 9 test cases

---

**Next Task**: [PARSER-1] GitHub Actions YAML Parser (12 hours)
