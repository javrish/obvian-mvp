package api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Plugin analytics data for developer insights
 */
@Schema(description = "Plugin analytics data")
public class PluginAnalytics {

    @Schema(description = "Plugin identifier", example = "plugin-001")
    @JsonProperty("pluginId")
    private String pluginId;

    @Schema(description = "Plugin name", example = "Advanced Analytics Plugin")
    @JsonProperty("pluginName")
    private String pluginName;

    @Schema(description = "Developer identifier", example = "dev-001")
    @JsonProperty("developerId")
    private String developerId;

    @Schema(description = "Total downloads count", example = "1250")
    @JsonProperty("totalDownloads")
    private Integer totalDownloads;

    @Schema(description = "Active installs count", example = "980")
    @JsonProperty("activeInstalls")
    private Integer activeInstalls;

    @Schema(description = "Average user rating", example = "4.3")
    @JsonProperty("averageRating")
    private Double averageRating;

    @Schema(description = "Total number of ratings", example = "87")
    @JsonProperty("totalRatings")
    private Integer totalRatings;

    @Schema(description = "Plugin status", example = "ACTIVE")
    @JsonProperty("status")
    private String status;

    @Schema(description = "Last update timestamp")
    @JsonProperty("lastUpdated")
    private LocalDateTime lastUpdated;

    @Schema(description = "Download trends over time (daily counts for last 30 days)")
    @JsonProperty("downloadTrends")
    private List<Integer> downloadTrends;

    @Schema(description = "Rating distribution (5-star breakdown)")
    @JsonProperty("ratingDistribution")
    private RatingDistribution ratingDistribution;

    @Schema(description = "Geographic distribution of users")
    @JsonProperty("geographicData")
    private Map<String, Integer> geographicData;

    @Schema(description = "User demographics data")
    @JsonProperty("demographics")
    private Demographics demographics;

    @Schema(description = "Performance metrics")
    @JsonProperty("performance")
    private PerformanceMetrics performance;

    @Schema(description = "Usage analytics")
    @JsonProperty("usage")
    private UsageAnalytics usage;

    @Schema(description = "Conversion metrics")
    @JsonProperty("conversions")
    private ConversionMetrics conversions;

    // Nested classes
    @Schema(description = "Rating distribution breakdown")
    public static class RatingDistribution {
        @JsonProperty("fiveStars")
        private Integer fiveStars;

        @JsonProperty("fourStars")
        private Integer fourStars;

        @JsonProperty("threeStars")
        private Integer threeStars;

        @JsonProperty("twoStars")
        private Integer twoStars;

        @JsonProperty("oneStar")
        private Integer oneStar;

        // Constructors
        public RatingDistribution() {}

        // Getters and setters
        public Integer getFiveStars() { return fiveStars; }
        public void setFiveStars(Integer fiveStars) { this.fiveStars = fiveStars; }
        public Integer getFourStars() { return fourStars; }
        public void setFourStars(Integer fourStars) { this.fourStars = fourStars; }
        public Integer getThreeStars() { return threeStars; }
        public void setThreeStars(Integer threeStars) { this.threeStars = threeStars; }
        public Integer getTwoStars() { return twoStars; }
        public void setTwoStars(Integer twoStars) { this.twoStars = twoStars; }
        public Integer getOneStar() { return oneStar; }
        public void setOneStar(Integer oneStar) { this.oneStar = oneStar; }
    }

    @Schema(description = "User demographics data")
    public static class Demographics {
        @JsonProperty("ageGroups")
        private Map<String, Integer> ageGroups;

        @JsonProperty("industries")
        private Map<String, Integer> industries;

        @JsonProperty("companySize")
        private Map<String, Integer> companySize;

        @JsonProperty("userTypes")
        private Map<String, Integer> userTypes;

        // Constructors
        public Demographics() {}

        // Getters and setters
        public Map<String, Integer> getAgeGroups() { return ageGroups; }
        public void setAgeGroups(Map<String, Integer> ageGroups) { this.ageGroups = ageGroups; }
        public Map<String, Integer> getIndustries() { return industries; }
        public void setIndustries(Map<String, Integer> industries) { this.industries = industries; }
        public Map<String, Integer> getCompanySize() { return companySize; }
        public void setCompanySize(Map<String, Integer> companySize) { this.companySize = companySize; }
        public Map<String, Integer> getUserTypes() { return userTypes; }
        public void setUserTypes(Map<String, Integer> userTypes) { this.userTypes = userTypes; }
    }

    @Schema(description = "Performance metrics")
    public static class PerformanceMetrics {
        @JsonProperty("averageResponseTime")
        private Double averageResponseTime;

        @JsonProperty("errorRate")
        private Double errorRate;

        @JsonProperty("uptime")
        private Double uptime;

        @JsonProperty("memoryUsage")
        private Double memoryUsage;

        @JsonProperty("cpuUsage")
        private Double cpuUsage;

        @JsonProperty("crashRate")
        private Double crashRate;

        // Constructors
        public PerformanceMetrics() {}

        // Getters and setters
        public Double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(Double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public Double getErrorRate() { return errorRate; }
        public void setErrorRate(Double errorRate) { this.errorRate = errorRate; }
        public Double getUptime() { return uptime; }
        public void setUptime(Double uptime) { this.uptime = uptime; }
        public Double getMemoryUsage() { return memoryUsage; }
        public void setMemoryUsage(Double memoryUsage) { this.memoryUsage = memoryUsage; }
        public Double getCpuUsage() { return cpuUsage; }
        public void setCpuUsage(Double cpuUsage) { this.cpuUsage = cpuUsage; }
        public Double getCrashRate() { return crashRate; }
        public void setCrashRate(Double crashRate) { this.crashRate = crashRate; }
    }

    @Schema(description = "Usage analytics")
    public static class UsageAnalytics {
        @JsonProperty("dailyActiveUsers")
        private Integer dailyActiveUsers;

        @JsonProperty("weeklyActiveUsers")
        private Integer weeklyActiveUsers;

        @JsonProperty("monthlyActiveUsers")
        private Integer monthlyActiveUsers;

        @JsonProperty("averageSessionDuration")
        private Double averageSessionDuration;

        @JsonProperty("featuresUsed")
        private Map<String, Integer> featuresUsed;

        @JsonProperty("retentionRate")
        private Double retentionRate;

        // Constructors
        public UsageAnalytics() {}

        // Getters and setters
        public Integer getDailyActiveUsers() { return dailyActiveUsers; }
        public void setDailyActiveUsers(Integer dailyActiveUsers) { this.dailyActiveUsers = dailyActiveUsers; }
        public Integer getWeeklyActiveUsers() { return weeklyActiveUsers; }
        public void setWeeklyActiveUsers(Integer weeklyActiveUsers) { this.weeklyActiveUsers = weeklyActiveUsers; }
        public Integer getMonthlyActiveUsers() { return monthlyActiveUsers; }
        public void setMonthlyActiveUsers(Integer monthlyActiveUsers) { this.monthlyActiveUsers = monthlyActiveUsers; }
        public Double getAverageSessionDuration() { return averageSessionDuration; }
        public void setAverageSessionDuration(Double averageSessionDuration) { this.averageSessionDuration = averageSessionDuration; }
        public Map<String, Integer> getFeaturesUsed() { return featuresUsed; }
        public void setFeaturesUsed(Map<String, Integer> featuresUsed) { this.featuresUsed = featuresUsed; }
        public Double getRetentionRate() { return retentionRate; }
        public void setRetentionRate(Double retentionRate) { this.retentionRate = retentionRate; }
    }

    @Schema(description = "Conversion metrics")
    public static class ConversionMetrics {
        @JsonProperty("viewToDownloadRate")
        private Double viewToDownloadRate;

        @JsonProperty("downloadToInstallRate")
        private Double downloadToInstallRate;

        @JsonProperty("freeToProRate")
        private Double freeToProRate;

        @JsonProperty("trialToSubscriptionRate")
        private Double trialToSubscriptionRate;

        @JsonProperty("churnRate")
        private Double churnRate;

        // Constructors
        public ConversionMetrics() {}

        // Getters and setters
        public Double getViewToDownloadRate() { return viewToDownloadRate; }
        public void setViewToDownloadRate(Double viewToDownloadRate) { this.viewToDownloadRate = viewToDownloadRate; }
        public Double getDownloadToInstallRate() { return downloadToInstallRate; }
        public void setDownloadToInstallRate(Double downloadToInstallRate) { this.downloadToInstallRate = downloadToInstallRate; }
        public Double getFreeToProRate() { return freeToProRate; }
        public void setFreeToProRate(Double freeToProRate) { this.freeToProRate = freeToProRate; }
        public Double getTrialToSubscriptionRate() { return trialToSubscriptionRate; }
        public void setTrialToSubscriptionRate(Double trialToSubscriptionRate) { this.trialToSubscriptionRate = trialToSubscriptionRate; }
        public Double getChurnRate() { return churnRate; }
        public void setChurnRate(Double churnRate) { this.churnRate = churnRate; }
    }

    // Constructors
    public PluginAnalytics() {}

    // Utility methods
    public void updateRating(Double newRating) {
        if (totalRatings == null) totalRatings = 0;
        if (averageRating == null) averageRating = 0.0;
        
        double totalScore = averageRating * totalRatings;
        totalRatings++;
        averageRating = (totalScore + newRating) / totalRatings;
    }

    public void updateDownloadTrend() {
        // Add today's download count to trends
        if (downloadTrends != null && !downloadTrends.isEmpty()) {
            // Increment last entry (today's downloads)
            int lastIndex = downloadTrends.size() - 1;
            downloadTrends.set(lastIndex, downloadTrends.get(lastIndex) + 1);
        }
    }

    // Getters and setters
    public String getPluginId() { return pluginId; }
    public void setPluginId(String pluginId) { this.pluginId = pluginId; }

    public String getPluginName() { return pluginName; }
    public void setPluginName(String pluginName) { this.pluginName = pluginName; }

    public String getDeveloperId() { return developerId; }
    public void setDeveloperId(String developerId) { this.developerId = developerId; }

    public Integer getTotalDownloads() { return totalDownloads; }
    public void setTotalDownloads(Integer totalDownloads) { this.totalDownloads = totalDownloads; }

    public Integer getActiveInstalls() { return activeInstalls; }
    public void setActiveInstalls(Integer activeInstalls) { this.activeInstalls = activeInstalls; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getTotalRatings() { return totalRatings; }
    public void setTotalRatings(Integer totalRatings) { this.totalRatings = totalRatings; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public List<Integer> getDownloadTrends() { return downloadTrends; }
    public void setDownloadTrends(List<Integer> downloadTrends) { this.downloadTrends = downloadTrends; }

    public RatingDistribution getRatingDistribution() { return ratingDistribution; }
    public void setRatingDistribution(RatingDistribution ratingDistribution) { this.ratingDistribution = ratingDistribution; }

    public Map<String, Integer> getGeographicData() { return geographicData; }
    public void setGeographicData(Map<String, Integer> geographicData) { this.geographicData = geographicData; }

    public Demographics getDemographics() { return demographics; }
    public void setDemographics(Demographics demographics) { this.demographics = demographics; }

    public PerformanceMetrics getPerformance() { return performance; }
    public void setPerformance(PerformanceMetrics performance) { this.performance = performance; }

    public UsageAnalytics getUsage() { return usage; }
    public void setUsage(UsageAnalytics usage) { this.usage = usage; }

    public ConversionMetrics getConversions() { return conversions; }
    public void setConversions(ConversionMetrics conversions) { this.conversions = conversions; }
}