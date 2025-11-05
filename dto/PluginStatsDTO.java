package api.dto;

import java.util.Map;

/**
 * DTO for plugin statistics.
 */
public class PluginStatsDTO {
    private String pluginId;
    private int totalExecutions;
    private int successfulExecutions;
    private int failedExecutions;
    private double averageExecutionTime;
    private String lastExecutionDate;
    private Map<String, Integer> executionsByDay;
    private int totalPlugins;
    private int systemPlugins;
    private int customPlugins;
    private int activePlugins;
    private int totalTests;
    private int passedTests;
    private int failedTests;

    // Getters and setters
    public String getPluginId() { return pluginId; }
    public void setPluginId(String pluginId) { this.pluginId = pluginId; }
    
    public int getTotalExecutions() { return totalExecutions; }
    public void setTotalExecutions(int totalExecutions) { 
        this.totalExecutions = totalExecutions; 
    }

    public int getSuccessfulExecutions() { return successfulExecutions; }
    public void setSuccessfulExecutions(int successfulExecutions) { 
        this.successfulExecutions = successfulExecutions; 
    }

    public int getFailedExecutions() { return failedExecutions; }
    public void setFailedExecutions(int failedExecutions) { 
        this.failedExecutions = failedExecutions; 
    }

    public double getAverageExecutionTime() { return averageExecutionTime; }
    public void setAverageExecutionTime(double averageExecutionTime) { 
        this.averageExecutionTime = averageExecutionTime; 
    }

    public String getLastExecutionDate() { return lastExecutionDate; }
    public void setLastExecutionDate(String lastExecutionDate) { 
        this.lastExecutionDate = lastExecutionDate; 
    }

    public Map<String, Integer> getExecutionsByDay() { return executionsByDay; }
    public void setExecutionsByDay(Map<String, Integer> executionsByDay) { 
        this.executionsByDay = executionsByDay; 
    }
    
    public int getTotalPlugins() { return totalPlugins; }
    public void setTotalPlugins(int totalPlugins) { this.totalPlugins = totalPlugins; }
    
    public int getSystemPlugins() { return systemPlugins; }
    public void setSystemPlugins(int systemPlugins) { this.systemPlugins = systemPlugins; }
    
    public int getCustomPlugins() { return customPlugins; }
    public void setCustomPlugins(int customPlugins) { this.customPlugins = customPlugins; }
    
    public int getActivePlugins() { return activePlugins; }
    public void setActivePlugins(int activePlugins) { this.activePlugins = activePlugins; }
    
    public int getTotalTests() { return totalTests; }
    public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
    
    public int getPassedTests() { return passedTests; }
    public void setPassedTests(int passedTests) { this.passedTests = passedTests; }
    
    public int getFailedTests() { return failedTests; }
    public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
}