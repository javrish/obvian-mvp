package api.config;

import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Map;

/**
 * Stripe Configuration for Payment Processing
 * 
 * Centralizes all Stripe-related configuration with:
 * - API keys and authentication settings
 * - Webhook configuration and security
 * - Payment processing limits and rules
 * - Currency and region support
 * - Development vs production settings
 * - Security and compliance configurations
 */
@Configuration
@ConfigurationProperties(prefix = "obvian.payment.stripe")
@Validated
public class StripeConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(StripeConfiguration.class);
    
    // Core Stripe API Configuration
    @NotBlank(message = "Stripe publishable key is required")
    private String publicKey = "pk_test_51234567890abcdef"; // Placeholder - replace with real key
    
    @NotBlank(message = "Stripe secret key is required")
    private String secretKey = "sk_test_51234567890abcdef"; // Placeholder - replace with real key
    
    @NotBlank(message = "Stripe API version is required")
    private String apiVersion = "2023-10-16";
    
    private boolean enabled = true;
    
    // Webhook Configuration
    @NotBlank(message = "Webhook endpoint secret is required")
    private String webhookSecret = "whsec_1234567890abcdef"; // Placeholder - replace with real secret
    
    private String webhookUrl = "/api/marketplace/payments/webhook";
    
    private List<String> webhookEvents = List.of(
            "payment_intent.succeeded",
            "payment_intent.payment_failed",
            "customer.subscription.created",
            "customer.subscription.updated",
            "customer.subscription.deleted",
            "invoice.payment_succeeded",
            "invoice.payment_failed",
            "charge.dispute.created"
    );
    
    // Payment Processing Configuration
    private PaymentSettings paymentSettings = new PaymentSettings();
    
    // Subscription Configuration
    private SubscriptionSettings subscriptionSettings = new SubscriptionSettings();
    
    // Security and Compliance Configuration
    private SecuritySettings securitySettings = new SecuritySettings();
    
    // Development and Testing Configuration
    private TestSettings testSettings = new TestSettings();
    
    // Regional Configuration
    private RegionalSettings regionalSettings = new RegionalSettings();
    
    // Nested Configuration Classes
    
    public static class PaymentSettings {
        private String defaultCurrency = "USD";
        private String minimumAmount = "0.50";
        private String maximumAmount = "10000.00";
        private List<String> supportedCurrencies = List.of("USD", "EUR", "GBP", "CAD");
        private boolean capturePayments = true;
        private String paymentMethodTypes = "card";
        private int paymentIntentTimeout = 3600; // seconds
        private boolean enableSavePaymentMethods = true;
        private String receiptEmail = "auto"; // "auto", "always", "never"
        
        // Getters and Setters
        public String getDefaultCurrency() { return defaultCurrency; }
        public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
        
        public String getMinimumAmount() { return minimumAmount; }
        public void setMinimumAmount(String minimumAmount) { this.minimumAmount = minimumAmount; }
        
        public String getMaximumAmount() { return maximumAmount; }
        public void setMaximumAmount(String maximumAmount) { this.maximumAmount = maximumAmount; }
        
        public List<String> getSupportedCurrencies() { return supportedCurrencies; }
        public void setSupportedCurrencies(List<String> supportedCurrencies) { this.supportedCurrencies = supportedCurrencies; }
        
        public boolean isCapturePayments() { return capturePayments; }
        public void setCapturePayments(boolean capturePayments) { this.capturePayments = capturePayments; }
        
        public String getPaymentMethodTypes() { return paymentMethodTypes; }
        public void setPaymentMethodTypes(String paymentMethodTypes) { this.paymentMethodTypes = paymentMethodTypes; }
        
        public int getPaymentIntentTimeout() { return paymentIntentTimeout; }
        public void setPaymentIntentTimeout(int paymentIntentTimeout) { this.paymentIntentTimeout = paymentIntentTimeout; }
        
        public boolean isEnableSavePaymentMethods() { return enableSavePaymentMethods; }
        public void setEnableSavePaymentMethods(boolean enableSavePaymentMethods) { this.enableSavePaymentMethods = enableSavePaymentMethods; }
        
        public String getReceiptEmail() { return receiptEmail; }
        public void setReceiptEmail(String receiptEmail) { this.receiptEmail = receiptEmail; }
    }
    
    public static class SubscriptionSettings {
        private boolean enableTrials = true;
        private int defaultTrialDays = 14;
        private String defaultBillingCycleAnchor = "now";
        private boolean prorationBehavior = true;
        private String collectionMethod = "charge_automatically";
        private int invoicePaymentRetryLimit = 3;
        private String incompleteSubscriptionBehavior = "cancel";
        private boolean enableCancellationFeedback = true;
        
        // Getters and Setters
        public boolean isEnableTrials() { return enableTrials; }
        public void setEnableTrials(boolean enableTrials) { this.enableTrials = enableTrials; }
        
        public int getDefaultTrialDays() { return defaultTrialDays; }
        public void setDefaultTrialDays(int defaultTrialDays) { this.defaultTrialDays = defaultTrialDays; }
        
        public String getDefaultBillingCycleAnchor() { return defaultBillingCycleAnchor; }
        public void setDefaultBillingCycleAnchor(String defaultBillingCycleAnchor) { this.defaultBillingCycleAnchor = defaultBillingCycleAnchor; }
        
        public boolean isProrationBehavior() { return prorationBehavior; }
        public void setProrationBehavior(boolean prorationBehavior) { this.prorationBehavior = prorationBehavior; }
        
        public String getCollectionMethod() { return collectionMethod; }
        public void setCollectionMethod(String collectionMethod) { this.collectionMethod = collectionMethod; }
        
        public int getInvoicePaymentRetryLimit() { return invoicePaymentRetryLimit; }
        public void setInvoicePaymentRetryLimit(int invoicePaymentRetryLimit) { this.invoicePaymentRetryLimit = invoicePaymentRetryLimit; }
        
        public String getIncompleteSubscriptionBehavior() { return incompleteSubscriptionBehavior; }
        public void setIncompleteSubscriptionBehavior(String incompleteSubscriptionBehavior) { this.incompleteSubscriptionBehavior = incompleteSubscriptionBehavior; }
        
        public boolean isEnableCancellationFeedback() { return enableCancellationFeedback; }
        public void setEnableCancellationFeedback(boolean enableCancellationFeedback) { this.enableCancellationFeedback = enableCancellationFeedback; }
    }
    
    public static class SecuritySettings {
        private boolean enableWebhookSignatureVerification = true;
        private int webhookToleranceSeconds = 300;
        private boolean enableApiKeyRotation = true;
        private int apiKeyRotationDays = 90;
        private boolean enableRateLimiting = true;
        private int maxRequestsPerMinute = 100;
        private boolean enableIpWhitelisting = false;
        private List<String> whitelistedIps = List.of();
        private boolean enableTlsVerification = true;
        private String minimumTlsVersion = "1.2";
        private boolean logSecurityEvents = true;
        
        // Getters and Setters
        public boolean isEnableWebhookSignatureVerification() { return enableWebhookSignatureVerification; }
        public void setEnableWebhookSignatureVerification(boolean enableWebhookSignatureVerification) { this.enableWebhookSignatureVerification = enableWebhookSignatureVerification; }
        
        public int getWebhookToleranceSeconds() { return webhookToleranceSeconds; }
        public void setWebhookToleranceSeconds(int webhookToleranceSeconds) { this.webhookToleranceSeconds = webhookToleranceSeconds; }
        
        public boolean isEnableRateLimiting() { return enableRateLimiting; }
        public void setEnableRateLimiting(boolean enableRateLimiting) { this.enableRateLimiting = enableRateLimiting; }
        
        public int getMaxRequestsPerMinute() { return maxRequestsPerMinute; }
        public void setMaxRequestsPerMinute(int maxRequestsPerMinute) { this.maxRequestsPerMinute = maxRequestsPerMinute; }
    }
    
    public static class TestSettings {
        private boolean mockMode = false;
        private boolean logApiCalls = true;
        private String testClockId = null;
        private Map<String, String> testPaymentMethods = Map.of(
                "success", "pm_card_visa",
                "decline", "pm_card_chargeCustomerFail",
                "timeout", "pm_timeout_test",
                "3ds", "pm_card_threeDSecure2Required"
        );
        private boolean enableWebhookTesting = true;
        private String webhookTestEndpoint = "/api/marketplace/payments/webhook/test";
        
        // Getters and Setters
        public boolean isMockMode() { return mockMode; }
        public void setMockMode(boolean mockMode) { this.mockMode = mockMode; }
        
        public boolean isLogApiCalls() { return logApiCalls; }
        public void setLogApiCalls(boolean logApiCalls) { this.logApiCalls = logApiCalls; }
        
        public Map<String, String> getTestPaymentMethods() { return testPaymentMethods; }
        public void setTestPaymentMethods(Map<String, String> testPaymentMethods) { this.testPaymentMethods = testPaymentMethods; }
    }
    
    public static class RegionalSettings {
        private String defaultCountry = "US";
        private List<String> supportedCountries = List.of("US", "CA", "GB", "DE", "FR", "AU");
        private boolean enableLocalPaymentMethods = true;
        private Map<String, List<String>> countryPaymentMethods = Map.of(
                "US", List.of("card", "us_bank_account"),
                "GB", List.of("card", "bacs_debit"),
                "DE", List.of("card", "sepa_debit"),
                "CA", List.of("card", "acss_debit")
        );
        private boolean enableTaxCalculation = true;
        private String taxProvider = "stripe_tax";
        
        // Getters and Setters
        public String getDefaultCountry() { return defaultCountry; }
        public void setDefaultCountry(String defaultCountry) { this.defaultCountry = defaultCountry; }
        
        public List<String> getSupportedCountries() { return supportedCountries; }
        public void setSupportedCountries(List<String> supportedCountries) { this.supportedCountries = supportedCountries; }
        
        public Map<String, List<String>> getCountryPaymentMethods() { return countryPaymentMethods; }
        public void setCountryPaymentMethods(Map<String, List<String>> countryPaymentMethods) { this.countryPaymentMethods = countryPaymentMethods; }
    }
    
    // Constructor and Initialization
    public StripeConfiguration() {
        logger.info("Initializing Stripe configuration");
    }
    
    // Validation and Utility Methods
    
    /**
     * Check if the current configuration is for testing
     */
    public boolean isTestMode() {
        return publicKey != null && publicKey.contains("_test_") && 
               secretKey != null && secretKey.contains("_test_");
    }
    
    /**
     * Get the appropriate API base URL based on test/live mode
     */
    public String getApiBaseUrl() {
        return "https://api.stripe.com";
    }
    
    /**
     * Check if a currency is supported
     */
    public boolean isCurrencySupported(String currency) {
        return paymentSettings.supportedCurrencies.contains(currency.toUpperCase());
    }
    
    /**
     * Mask sensitive configuration values for logging
     */
    public String maskSensitiveValue(String value) {
        if (value == null || value.length() < 8) {
            return "[REDACTED]";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
    
    // Primary Getters and Setters
    
    public String getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
        logger.debug("Stripe publishable key updated: {}", maskSensitiveValue(publicKey));
    }
    
    public String getSecretKey() {
        return secretKey;
    }
    
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
        logger.debug("Stripe secret key updated: {}", maskSensitiveValue(secretKey));
    }
    
    public String getApiVersion() {
        return apiVersion;
    }
    
    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
        logger.debug("Stripe API version set to: {}", apiVersion);
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("Stripe integration {}", enabled ? "enabled" : "disabled");
    }
    
    public String getWebhookSecret() {
        return webhookSecret;
    }
    
    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
        logger.debug("Stripe webhook secret updated: {}", maskSensitiveValue(webhookSecret));
    }
    
    public String getWebhookUrl() {
        return webhookUrl;
    }
    
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
    
    public List<String> getWebhookEvents() {
        return webhookEvents;
    }
    
    public void setWebhookEvents(List<String> webhookEvents) {
        this.webhookEvents = webhookEvents;
    }
    
    public PaymentSettings getPaymentSettings() {
        return paymentSettings;
    }
    
    public void setPaymentSettings(PaymentSettings paymentSettings) {
        this.paymentSettings = paymentSettings;
    }
    
    public SubscriptionSettings getSubscriptionSettings() {
        return subscriptionSettings;
    }
    
    public void setSubscriptionSettings(SubscriptionSettings subscriptionSettings) {
        this.subscriptionSettings = subscriptionSettings;
    }
    
    public SecuritySettings getSecuritySettings() {
        return securitySettings;
    }
    
    public void setSecuritySettings(SecuritySettings securitySettings) {
        this.securitySettings = securitySettings;
    }
    
    public TestSettings getTestSettings() {
        return testSettings;
    }
    
    public void setTestSettings(TestSettings testSettings) {
        this.testSettings = testSettings;
    }
    
    public RegionalSettings getRegionalSettings() {
        return regionalSettings;
    }
    
    public void setRegionalSettings(RegionalSettings regionalSettings) {
        this.regionalSettings = regionalSettings;
    }
    
    @Override
    public String toString() {
        return "StripeConfiguration{" +
                "enabled=" + enabled +
                ", apiVersion='" + apiVersion + '\'' +
                ", publicKey='" + maskSensitiveValue(publicKey) + '\'' +
                ", secretKey='" + maskSensitiveValue(secretKey) + '\'' +
                ", webhookSecret='" + maskSensitiveValue(webhookSecret) + '\'' +
                ", testMode=" + isTestMode() +
                '}';
    }
}