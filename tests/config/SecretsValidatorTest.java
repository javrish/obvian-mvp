
package tests.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import config.SecretsValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for SecretsValidator to ensure proper validation of required environment variables.
 *
 * <p>These tests verify that the application fails fast with clear error messages when critical
 * secrets are missing or invalid.
 */
class SecretsValidatorTest {

  private SecretsValidator validator;

  @BeforeEach
  void setUp() {
    validator = new SecretsValidator();
    // Default to dev profile for most tests
    ReflectionTestUtils.setField(validator, "activeProfile", "dev");
  }

  @Test
  @DisplayName("Should fail when JWT secret is missing")
  void shouldFailWhenJwtSecretMissing() {
    // Given: No JWT secret set
    ReflectionTestUtils.setField(validator, "jwtSecret", "");

    // When/Then: Validation should fail with clear error message
    assertThatThrownBy(() -> validator.validateSecrets())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OBVIAN_JWT_SECRET is not set")
        .hasMessageContaining("openssl rand -base64 64");
  }

  @Test
  @DisplayName("Should fail when JWT secret is too short")
  void shouldFailWhenJwtSecretTooShort() {
    // Given: JWT secret with less than 32 characters
    ReflectionTestUtils.setField(validator, "jwtSecret", "short-secret-123");

    // When/Then: Validation should fail
    assertThatThrownBy(() -> validator.validateSecrets())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OBVIAN_JWT_SECRET is too short")
        .hasMessageContaining("min 32 characters");
  }

  @Test
  @DisplayName("Should pass when JWT secret is valid in dev profile")
  void shouldPassWhenJwtSecretValidInDev() {
    // Given: Valid JWT secret (32+ characters)
    String validSecret = "a-very-long-and-secure-jwt-secret-key-that-is-at-least-32-chars-long";
    ReflectionTestUtils.setField(validator, "jwtSecret", validSecret);
    ReflectionTestUtils.setField(validator, "githubAppPrivateKey", ""); // Not required in dev

    // When/Then: Validation should pass without exceptions
    validator.validateSecrets();
  }

  @Test
  @DisplayName("Should warn but not fail when GitHub key missing in dev")
  void shouldWarnButNotFailWhenGitHubKeyMissingInDev() {
    // Given: Valid JWT secret but no GitHub key in dev profile
    String validSecret = "a-very-long-and-secure-jwt-secret-key-that-is-at-least-32-chars-long";
    ReflectionTestUtils.setField(validator, "jwtSecret", validSecret);
    ReflectionTestUtils.setField(validator, "githubAppPrivateKey", "");
    ReflectionTestUtils.setField(validator, "activeProfile", "dev");

    // When/Then: Should pass (GitHub key not required in dev)
    validator.validateSecrets();
  }

  @Test
  @DisplayName("Should fail when GitHub key missing in production")
  void shouldFailWhenGitHubKeyMissingInProduction() {
    // Given: Valid JWT secret but no GitHub key in production
    String validSecret = "a-very-long-and-secure-jwt-secret-key-that-is-at-least-32-chars-long";
    ReflectionTestUtils.setField(validator, "jwtSecret", validSecret);
    ReflectionTestUtils.setField(validator, "githubAppPrivateKey", "");
    ReflectionTestUtils.setField(validator, "activeProfile", "prod");

    // When/Then: Should fail (GitHub key required in production)
    assertThatThrownBy(() -> validator.validateSecrets())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("GITHUB_APP_PRIVATE_KEY is not set")
        .hasMessageContaining("required in production");
  }

  @Test
  @DisplayName("Should pass when all secrets valid in production")
  void shouldPassWhenAllSecretsValidInProduction() {
    // Given: All required secrets for production
    String validSecret = "a-very-long-and-secure-jwt-secret-key-that-is-at-least-32-chars-long";
    String validPrivateKey =
        "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA...\n-----END RSA PRIVATE KEY-----";

    ReflectionTestUtils.setField(validator, "jwtSecret", validSecret);
    ReflectionTestUtils.setField(validator, "githubAppPrivateKey", validPrivateKey);
    ReflectionTestUtils.setField(validator, "activeProfile", "prod");

    // When/Then: Validation should pass
    validator.validateSecrets();
  }

  @Test
  @DisplayName("Should provide helpful error messages with setup instructions")
  void shouldProvideHelpfulErrorMessages() {
    // Given: Missing JWT secret
    ReflectionTestUtils.setField(validator, "jwtSecret", "");

    // When/Then: Error message should include setup instructions
    assertThatThrownBy(() -> validator.validateSecrets())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("CRITICAL: Application startup failed")
        .hasMessageContaining("docs/CONFIGURATION.md")
        .hasMessageContaining(".env.example");
  }

  @Test
  @DisplayName("Should validate JWT secret exactly 32 characters")
  void shouldValidateJwtSecretExactly32Characters() {
    // Given: JWT secret with exactly 32 characters (minimum)
    String minSecret = "12345678901234567890123456789012"; // Exactly 32 chars
    ReflectionTestUtils.setField(validator, "jwtSecret", minSecret);
    ReflectionTestUtils.setField(validator, "githubAppPrivateKey", "");

    // When/Then: Should pass (32 is minimum)
    validator.validateSecrets();
  }

  @Test
  @DisplayName("Should trim whitespace from secrets before validation")
  void shouldTrimWhitespaceFromSecrets() {
    // Given: Empty string with whitespace
    ReflectionTestUtils.setField(validator, "jwtSecret", "   ");

    // When/Then: Should treat as empty and fail
    assertThatThrownBy(() -> validator.validateSecrets())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("OBVIAN_JWT_SECRET is not set");
  }
}
