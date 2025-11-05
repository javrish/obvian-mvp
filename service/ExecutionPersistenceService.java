package api.service;

import api.model.DagExecutionRequest;
import api.model.DagExecutionResponse;
import api.model.PromptExecutionRequest;
import api.model.PromptExecutionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service for persisting execution results and analytics to database
 */
@Service
public class ExecutionPersistenceService {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final Environment environment;
    
    @Autowired
    public ExecutionPersistenceService(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // For LocalDateTime support
    }
    
    @PostConstruct
    public void initializeDatabase() {
        createTables();
    }
    
    /**
     * Create database tables if they don't exist
     */
    private void createTables() {
        try {
            // Check if we're using PostgreSQL or H2
            String driverClass = environment.getProperty("spring.datasource.driverClassName", 
                environment.getProperty("spring.datasource.driver-class-name", ""));
            String dbUrl = environment.getProperty("spring.datasource.url", "");
            boolean isPostgres = driverClass.contains("postgresql") || dbUrl.contains("postgresql");
            
            // Execution results table with database-specific syntax
            String idColumn = isPostgres ? 
                "id BIGSERIAL PRIMARY KEY" : 
                "id BIGINT AUTO_INCREMENT PRIMARY KEY";
            
            String timestampDefault = isPostgres ? 
                "DEFAULT CURRENT_TIMESTAMP" : 
                "DEFAULT CURRENT_TIMESTAMP()";
            
            jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS execution_results (
                    %s,
                    execution_id VARCHAR(255) NOT NULL UNIQUE,
                    execution_type VARCHAR(50) NOT NULL,
                    priority VARCHAR(20),
                    status VARCHAR(50) NOT NULL,
                    success BOOLEAN NOT NULL,
                    start_time TIMESTAMP NOT NULL,
                    end_time TIMESTAMP,
                    duration_ms BIGINT,
                    request_data TEXT,
                    response_data TEXT,
                    error_message TEXT,
                    error_type VARCHAR(100),
                    user_context TEXT,
                    node_count INTEGER,
                    nodes_succeeded INTEGER,
                    nodes_failed INTEGER,
                    nodes_skipped INTEGER,
                    created_at TIMESTAMP %s,
                    updated_at TIMESTAMP %s
                )
            """, idColumn, timestampDefault, timestampDefault));
            
            // Create indexes for execution_results table (H2-compatible)
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_execution_id ON execution_results (execution_id)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_execution_type ON execution_results (execution_type)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_status ON execution_results (status)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_start_time ON execution_results (start_time)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_success ON execution_results (success)"); } catch (Exception e) { /* Index might already exist */ }
            
            // Node execution details table with database-specific syntax
            jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS node_execution_details (
                    %s,
                    execution_id VARCHAR(255) NOT NULL,
                    node_id VARCHAR(255) NOT NULL,
                    plugin_name VARCHAR(255),
                    status VARCHAR(50) NOT NULL,
                    success BOOLEAN NOT NULL,
                    start_time TIMESTAMP,
                    end_time TIMESTAMP,
                    duration_ms BIGINT,
                    retry_count INTEGER DEFAULT 0,
                    used_fallback BOOLEAN DEFAULT FALSE,
                    input_params TEXT,
                    output_data TEXT,
                    error_message TEXT,
                    error_type VARCHAR(100),
                    created_at TIMESTAMP %s
                )
            """, idColumn, timestampDefault));
            
            // Create indexes for node_execution_details table (H2-compatible)
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_node_execution_id ON node_execution_details (execution_id)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_node_id ON node_execution_details (node_id)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_node_status ON node_execution_details (status)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_plugin_name ON node_execution_details (plugin_name)"); } catch (Exception e) { /* Index might already exist */ }
            
            // Execution analytics table
            jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS execution_analytics (
                    %s,
                    metric_name VARCHAR(255) NOT NULL,
                    metric_value DOUBLE PRECISION NOT NULL,
                    metric_type VARCHAR(50) NOT NULL,
                    execution_type VARCHAR(50),
                    time_bucket TIMESTAMP NOT NULL,
                    metadata TEXT,
                    created_at TIMESTAMP %s
                )
            """, idColumn, timestampDefault));
            
            // Create indexes for execution_analytics table (H2-compatible)
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_metric_name ON execution_analytics (metric_name)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_time_bucket ON execution_analytics (time_bucket)"); } catch (Exception e) { /* Index might already exist */ }
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_analytics_execution_type ON execution_analytics (execution_type)"); } catch (Exception e) { /* Index might already exist */ }
            
            // Queue metrics table
            jdbcTemplate.execute(String.format("""
                CREATE TABLE IF NOT EXISTS queue_metrics (
                    %s,
                    timestamp TIMESTAMP NOT NULL,
                    queue_size INTEGER NOT NULL,
                    active_workers INTEGER NOT NULL,
                    total_executions BIGINT NOT NULL,
                    completed_executions BIGINT NOT NULL,
                    failed_executions BIGINT NOT NULL,
                    cancelled_executions BIGINT NOT NULL,
                    dead_letter_queue_size INTEGER NOT NULL,
                    success_rate DOUBLE PRECISION,
                    avg_execution_time_ms DOUBLE PRECISION,
                    created_at TIMESTAMP %s
                )
            """, idColumn, timestampDefault));
            
            // Create indexes for queue_metrics table (H2-compatible)
            try { jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_queue_timestamp ON queue_metrics (timestamp)"); } catch (Exception e) { /* Index might already exist */ }
            
            System.out.println("Database tables initialized successfully");
            
        } catch (DataAccessException e) {
            System.err.println("Failed to initialize database tables: " + e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    /**
     * Persist DAG execution result
     */
    @Transactional
    public void persistDagExecution(String executionId, DagExecutionRequest request, DagExecutionResponse response) {
        try {
            // Serialize request and response
            String requestJson = objectMapper.writeValueAsString(request);
            String responseJson = objectMapper.writeValueAsString(response);
            String userContextJson = request.getContext() != null ? 
                objectMapper.writeValueAsString(request.getContext()) : null;
            
            // Calculate metrics
            LocalDateTime startTime = response.getTimestamp() != null ? response.getTimestamp() : LocalDateTime.now();
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            // Extract node metrics
            int nodeCount = 0;
            int nodesSucceeded = 0;
            int nodesFailed = 0;
            int nodesSkipped = 0;
            
            if (response.getNodeResults() != null) {
                nodeCount = response.getNodeResults().size();
                for (var nodeResult : response.getNodeResults().values()) {
                    if (nodeResult.isSuccess()) {
                        nodesSucceeded++;
                    } else if ("SKIPPED".equals(nodeResult.getStatus())) {
                        nodesSkipped++;
                    } else {
                        nodesFailed++;
                    }
                }
            }
            
            // Insert execution result
            jdbcTemplate.update("""
                INSERT INTO execution_results (
                    execution_id, execution_type, priority, status, success, start_time, end_time, 
                    duration_ms, request_data, response_data, error_message, error_type, user_context,
                    node_count, nodes_succeeded, nodes_failed, nodes_skipped
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                executionId, "DAG", "NORMAL", response.isSuccess() ? "COMPLETED" : "FAILED", 
                response.isSuccess(), Timestamp.valueOf(startTime), Timestamp.valueOf(endTime),
                durationMs, requestJson, responseJson, response.getMessage(), response.getErrorType(),
                userContextJson, nodeCount, nodesSucceeded, nodesFailed, nodesSkipped
            );
            
            // Insert node execution details
            if (response.getNodeResults() != null) {
                for (var entry : response.getNodeResults().entrySet()) {
                    persistNodeExecution(executionId, entry.getKey(), entry.getValue());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to persist DAG execution: " + e.getMessage());
            // Don't throw exception to avoid breaking the execution flow
        }
    }
    
    /**
     * Persist prompt execution result
     */
    @Transactional
    public void persistPromptExecution(String executionId, PromptExecutionRequest request, PromptExecutionResponse response) {
        try {
            // Serialize request and response
            String requestJson = objectMapper.writeValueAsString(request);
            String responseJson = objectMapper.writeValueAsString(response);
            String userContextJson = request.getContext() != null ? 
                objectMapper.writeValueAsString(request.getContext()) : null;
            
            // Calculate metrics
            LocalDateTime startTime = response.getTimestamp() != null ? response.getTimestamp() : LocalDateTime.now();
            LocalDateTime endTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(startTime, endTime).toMillis();
            
            // Extract node metrics
            int nodeCount = 0;
            int nodesSucceeded = 0;
            int nodesFailed = 0;
            int nodesSkipped = 0;
            
            if (response.getNodeResults() != null) {
                nodeCount = response.getNodeResults().size();
                for (var nodeResult : response.getNodeResults().values()) {
                    if (nodeResult.isSuccess()) {
                        nodesSucceeded++;
                    } else if ("SKIPPED".equals(nodeResult.getStatus())) {
                        nodesSkipped++;
                    } else {
                        nodesFailed++;
                    }
                }
            }
            
            // Insert execution result
            jdbcTemplate.update("""
                INSERT INTO execution_results (
                    execution_id, execution_type, priority, status, success, start_time, end_time, 
                    duration_ms, request_data, response_data, error_message, error_type, user_context,
                    node_count, nodes_succeeded, nodes_failed, nodes_skipped
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                executionId, "PROMPT", "NORMAL", response.isSuccess() ? "COMPLETED" : "FAILED", 
                response.isSuccess(), Timestamp.valueOf(startTime), Timestamp.valueOf(endTime),
                durationMs, requestJson, responseJson, response.getMessage(), response.getErrorType(),
                userContextJson, nodeCount, nodesSucceeded, nodesFailed, nodesSkipped
            );
            
            // Insert node execution details
            if (response.getNodeResults() != null) {
                for (var entry : response.getNodeResults().entrySet()) {
                    persistNodeExecution(executionId, entry.getKey(), entry.getValue());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to persist prompt execution: " + e.getMessage());
            // Don't throw exception to avoid breaking the execution flow
        }
    }
    
    /**
     * Persist failed execution
     */
    @Transactional
    public void persistFailedExecution(String executionId, Object request, Exception error) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            String executionType = request instanceof DagExecutionRequest ? "DAG" : "PROMPT";
            
            jdbcTemplate.update("""
                INSERT INTO execution_results (
                    execution_id, execution_type, priority, status, success, start_time, 
                    request_data, error_message, error_type
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                executionId, executionType, "NORMAL", "FAILED", false, 
                Timestamp.valueOf(LocalDateTime.now()), requestJson, 
                error.getMessage(), error.getClass().getSimpleName()
            );
            
        } catch (Exception e) {
            System.err.println("Failed to persist failed execution: " + e.getMessage());
        }
    }
    
    /**
     * Persist individual node execution details
     */
    private void persistNodeExecution(String executionId, String nodeId, Object nodeResult) {
        try {
            // This would need to be adapted based on the actual NodeExecutionResult structure
            // For now, we'll store basic information
            String outputJson = objectMapper.writeValueAsString(nodeResult);
            
            jdbcTemplate.update("""
                INSERT INTO node_execution_details (
                    execution_id, node_id, status, success, output_data
                ) VALUES (?, ?, ?, ?, ?)
            """,
                executionId, nodeId, "COMPLETED", true, outputJson
            );
            
        } catch (Exception e) {
            System.err.println("Failed to persist node execution: " + e.getMessage());
        }
    }
    
    /**
     * Update analytics metrics
     */
    @Transactional
    public void updateAnalytics(Map<String, Object> metrics) {
        try {
            LocalDateTime timeBucket = LocalDateTime.now().withSecond(0).withNano(0);
            
            // Store queue metrics
            jdbcTemplate.update("""
                INSERT INTO queue_metrics (
                    timestamp, queue_size, active_workers, total_executions, completed_executions,
                    failed_executions, cancelled_executions, dead_letter_queue_size, success_rate
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
                Timestamp.valueOf(timeBucket),
                (Integer) metrics.get("queueSize"),
                (Integer) metrics.get("activeWorkers"),
                ((Number) metrics.get("totalExecutions")).longValue(),
                ((Number) metrics.get("completedExecutions")).longValue(),
                ((Number) metrics.get("failedExecutions")).longValue(),
                ((Number) metrics.get("cancelledExecutions")).longValue(),
                (Integer) metrics.getOrDefault("deadLetterQueueSize", 0),
                (Double) metrics.getOrDefault("successRate", 0.0)
            );
            
            // Store individual metrics
            for (Map.Entry<String, Object> entry : metrics.entrySet()) {
                if (entry.getValue() instanceof Number) {
                    jdbcTemplate.update("""
                        INSERT INTO execution_analytics (
                            metric_name, metric_value, metric_type, time_bucket
                        ) VALUES (?, ?, ?, ?)
                    """,
                        entry.getKey(),
                        ((Number) entry.getValue()).doubleValue(),
                        "QUEUE_METRIC",
                        Timestamp.valueOf(timeBucket)
                    );
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to update analytics: " + e.getMessage());
        }
    }
    
    /**
     * Get execution analytics with filtering
     */
    public Map<String, Object> getExecutionAnalytics(Map<String, Object> filters) {
        Map<String, Object> analytics = new HashMap<>();
        
        try {
            // Build WHERE clause based on filters
            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            List<Object> params = new ArrayList<>();
            
            if (filters.containsKey("executionType")) {
                whereClause.append(" AND execution_type = ?");
                params.add(filters.get("executionType"));
            }
            
            if (filters.containsKey("startDate")) {
                whereClause.append(" AND start_time >= ?");
                params.add(Timestamp.valueOf((LocalDateTime) filters.get("startDate")));
            }
            
            if (filters.containsKey("endDate")) {
                whereClause.append(" AND start_time <= ?");
                params.add(Timestamp.valueOf((LocalDateTime) filters.get("endDate")));
            }
            
            // Execution summary
            String summaryQuery = """
                SELECT 
                    COUNT(*) as total_executions,
                    SUM(CASE WHEN success = true THEN 1 ELSE 0 END) as successful_executions,
                    SUM(CASE WHEN success = false THEN 1 ELSE 0 END) as failed_executions,
                    AVG(duration_ms) as avg_duration_ms,
                    MIN(duration_ms) as min_duration_ms,
                    MAX(duration_ms) as max_duration_ms,
                    AVG(node_count) as avg_node_count
                FROM execution_results 
            """ + whereClause;
            
            Map<String, Object> summary = jdbcTemplate.queryForMap(summaryQuery, params.toArray());
            analytics.put("summary", summary);
            
            // Execution trends by hour
            String trendsQuery = """
                SELECT 
                    DATE_FORMAT(start_time, '%Y-%m-%d %H:00:00') as hour,
                    COUNT(*) as executions,
                    SUM(CASE WHEN success = true THEN 1 ELSE 0 END) as successful,
                    AVG(duration_ms) as avg_duration
                FROM execution_results 
            """ + whereClause + """
                GROUP BY DATE_FORMAT(start_time, '%Y-%m-%d %H:00:00')
                ORDER BY hour DESC
                LIMIT 24
            """;
            
            List<Map<String, Object>> trends = jdbcTemplate.queryForList(trendsQuery, params.toArray());
            analytics.put("trends", trends);
            
            // Error analysis
            String errorsQuery = """
                SELECT 
                    error_type,
                    COUNT(*) as count,
                    COUNT(*) * 100.0 / (SELECT COUNT(*) FROM execution_results WHERE success = false) as percentage
                FROM execution_results 
            """ + whereClause + " AND success = false" + """
                GROUP BY error_type
                ORDER BY count DESC
                LIMIT 10
            """;
            
            List<Map<String, Object>> errors = jdbcTemplate.queryForList(errorsQuery, params.toArray());
            analytics.put("errors", errors);
            
            // Performance percentiles
            String percentilesQuery = """
                SELECT 
                    execution_type,
                    COUNT(*) as count,
                    AVG(duration_ms) as avg_duration,
                    MIN(duration_ms) as min_duration,
                    MAX(duration_ms) as max_duration
                FROM execution_results 
            """ + whereClause + """
                GROUP BY execution_type
            """;
            
            List<Map<String, Object>> performance = jdbcTemplate.queryForList(percentilesQuery, params.toArray());
            analytics.put("performance", performance);
            
        } catch (Exception e) {
            System.err.println("Failed to get execution analytics: " + e.getMessage());
            analytics.put("error", "Failed to retrieve analytics: " + e.getMessage());
        }
        
        return analytics;
    }
    
    /**
     * Get execution history with pagination
     */
    public Map<String, Object> getExecutionHistory(int offset, int limit, Map<String, Object> filters) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Build WHERE clause
            StringBuilder whereClause = new StringBuilder("WHERE 1=1");
            List<Object> params = new ArrayList<>();
            
            if (filters.containsKey("executionType")) {
                whereClause.append(" AND execution_type = ?");
                params.add(filters.get("executionType"));
            }
            
            if (filters.containsKey("status")) {
                whereClause.append(" AND status = ?");
                params.add(filters.get("status"));
            }
            
            if (filters.containsKey("success")) {
                whereClause.append(" AND success = ?");
                params.add(filters.get("success"));
            }
            
            // Get total count
            String countQuery = "SELECT COUNT(*) FROM execution_results " + whereClause;
            int totalCount = jdbcTemplate.queryForObject(countQuery, Integer.class, params.toArray());
            
            // Get paginated results
            String dataQuery = """
                SELECT execution_id, execution_type, status, success, start_time, end_time, 
                       duration_ms, node_count, nodes_succeeded, nodes_failed, error_message
                FROM execution_results 
            """ + whereClause + """
                ORDER BY start_time DESC
                LIMIT ? OFFSET ?
            """;
            
            params.add(limit);
            params.add(offset);
            
            List<Map<String, Object>> executions = jdbcTemplate.queryForList(dataQuery, params.toArray());
            
            result.put("executions", executions);
            result.put("pagination", Map.of(
                "total", totalCount,
                "offset", offset,
                "limit", limit,
                "hasMore", offset + limit < totalCount
            ));
            
        } catch (Exception e) {
            System.err.println("Failed to get execution history: " + e.getMessage());
            result.put("error", "Failed to retrieve execution history: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Get queue metrics history
     */
    public List<Map<String, Object>> getQueueMetricsHistory(int hours) {
        try {
            LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
            
            return jdbcTemplate.queryForList("""
                SELECT timestamp, queue_size, active_workers, total_executions, 
                       completed_executions, failed_executions, success_rate
                FROM queue_metrics
                WHERE timestamp >= ?
                ORDER BY timestamp DESC
            """, Timestamp.valueOf(startTime));
            
        } catch (Exception e) {
            System.err.println("Failed to get queue metrics history: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Clean up old execution data
     */
    @Transactional
    public void cleanupOldData(int daysToKeep) {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(daysToKeep);
            
            // Clean up execution results (cascades to node details)
            int deletedExecutions = jdbcTemplate.update("""
                DELETE FROM execution_results 
                WHERE created_at < ?
            """, Timestamp.valueOf(cutoffTime));
            
            // Clean up analytics data
            int deletedAnalytics = jdbcTemplate.update("""
                DELETE FROM execution_analytics 
                WHERE created_at < ?
            """, Timestamp.valueOf(cutoffTime));
            
            // Clean up queue metrics
            int deletedMetrics = jdbcTemplate.update("""
                DELETE FROM queue_metrics 
                WHERE created_at < ?
            """, Timestamp.valueOf(cutoffTime));
            
            System.out.println("Cleaned up old data: " + deletedExecutions + " executions, " + 
                             deletedAnalytics + " analytics, " + deletedMetrics + " metrics");
            
        } catch (Exception e) {
            System.err.println("Failed to clean up old data: " + e.getMessage());
        }
    }
    
    /**
     * Get database health status
     */
    public Map<String, Object> getDatabaseHealth() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Test basic connectivity
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            health.put("connectivity", "healthy");
            
            // Get table sizes
            health.put("executionResults", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM execution_results", Long.class));
            health.put("nodeExecutionDetails", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM node_execution_details", Long.class));
            health.put("executionAnalytics", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM execution_analytics", Long.class));
            health.put("queueMetrics", jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue_metrics", Long.class));
            
            health.put("status", "healthy");
            
        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}