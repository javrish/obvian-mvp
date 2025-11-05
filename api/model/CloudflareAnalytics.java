package tests.api.model;

/**
 * Test model for Cloudflare analytics data
 */
public class CloudflareAnalytics {
    private String pluginId;
    private int days;
    private long totalRequests;
    private long uniqueVisitors;
    private long bandwidth;
    private double cacheHitRate;
    
    public String getPluginId() {
        return pluginId;
    }
    
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }
    
    public int getDays() {
        return days;
    }
    
    public void setDays(int days) {
        this.days = days;
    }
    
    public long getTotalRequests() {
        return totalRequests;
    }
    
    public void setTotalRequests(long totalRequests) {
        this.totalRequests = totalRequests;
    }
    
    public long getUniqueVisitors() {
        return uniqueVisitors;
    }
    
    public void setUniqueVisitors(long uniqueVisitors) {
        this.uniqueVisitors = uniqueVisitors;
    }
    
    public long getBandwidth() {
        return bandwidth;
    }
    
    public void setBandwidth(long bandwidth) {
        this.bandwidth = bandwidth;
    }
    
    public double getCacheHitRate() {
        return cacheHitRate;
    }
    
    public void setCacheHitRate(double cacheHitRate) {
        this.cacheHitRate = cacheHitRate;
    }
    
    private java.time.LocalDateTime generatedAt;
    private long totalBandwidth;
    
    public java.time.LocalDateTime getGeneratedAt() {
        return generatedAt;
    }
    
    public void setGeneratedAt(java.time.LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
    
    public long getTotalBandwidth() {
        return totalBandwidth;
    }
    
    public void setTotalBandwidth(long totalBandwidth) {
        this.totalBandwidth = totalBandwidth;
    }
}