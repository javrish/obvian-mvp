package config;

import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Validates that all required secrets are properly configured at application startup.
 * Fails fast with clear error messages if critical environment variables are missing.
 *
 * <p>This prevents the application from starting with insecure default values or missing secrets.
 */
@Component
public class SecretsValidator {

  private static final Logger logger = LoggerFactory.getLogger(SecretsValidator.class);

  @Value("${obvian.jwt.secret:}")
  private String jwtSecret;

  @Value("${obvian.github.app.private-key:}")
  private String githubAppPrivateKey;

  @Value("${spring.profiles.active:dev}")
  private String activeProfile;

  /**
   * Validates required secrets on application startup.
   *
   * @throws IllegalStateException if critical secrets are missing
   */
  @PostConstruct
  public void validateSecrets() {
    logger.info("Validating application secrets for profile: {}", activeProfile);

    boolean hasErrors = false;
    StringBuilder errorMessage =
        new StringBuilder(
            "CRITICAL: Application startup failed due to missing required secrets:\n\n");

    // Validate JWT Secret (always required)
    if (isNullOrEmpty(jwtSecret)) {
      hasErrors = true;
      errorMessage.append("  ❌ OBVIAN_JWT_SECRET is not set\n");
      errorMessage.append("     Required for JWT token signing and validation\n");
      errorMessage.append("     Generate: openssl rand -base64 64\n");
      errorMessage.append("     Set: export OBVIAN_JWT_SECRET=\"your-generated-secret\"\n\n");
    } else if (jwtSecret.length() < 32) {
      hasErrors = true;
      errorMessage.append("  ❌ OBVIAN_JWT_SECRET is too short (min 32 characters)\n");
      errorMessage.append("     Current length: ").append(jwtSecret.length()).append("\n");
      errorMessage.append("     Generate: openssl rand -base64 64\n\n");
    } else {
      logger.info("✓ JWT secret validated (length: {} characters)", jwtSecret.length());
    }

    // Validate GitHub App Private Key (required for production and GitHub features)
    if ("prod".equals(activeProfile) || "production".equals(activeProfile)) {
      if (isNullOrEmpty(githubAppPrivateKey)) {
        hasErrors = true;
        errorMessage.append("  ❌ GITHUB_APP_PRIVATE_KEY is not set (required in production)\n");
        errorMessage.append("     Required for GitHub Actions verification\n");
        errorMessage.append("     Obtain from: GitHub App Settings → Private Key\n");
        errorMessage.append(
            "     Set: export GITHUB_APP_PRIVATE_KEY=\"$(cat your-private-key.pem)\"\n\n");
      } else {
        logger.info("✓ GitHub App private key validated");
      }
    } else {
      // Dev/test environments: warn but don't fail
      if (isNullOrEmpty(githubAppPrivateKey)) {
        logger.warn(
            "⚠️  GITHUB_APP_PRIVATE_KEY not set (GitHub features will be unavailable)");
      } else {
        logger.info("✓ GitHub App private key configured");
      }
    }

    if (hasErrors) {
      errorMessage.append("\n");
      errorMessage.append("For complete setup instructions, see: docs/CONFIGURATION.md\n");
      errorMessage.append("For .env.example, see: .env.example\n");

      logger.error("{}", errorMessage);
      throw new IllegalStateException(errorMessage.toString());
    }

    logger.info("✓ All required secrets validated successfully");
  }

  private boolean isNullOrEmpty(String value) {
    return value == null || value.trim().isEmpty();
  }
}
