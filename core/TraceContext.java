package core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents tracing context that can be propagated through plugin execution.
 * Contains correlation IDs, trace IDs, and other distributed tracing metadata.
 */
public class TraceContext {
    
    private final String traceId;
    private final String correlationId;
    private final String parentSpanId;
    private final String spanId;
    private final Map<String, String> baggage;
    private final long startTime;
    
    /**
     * Create a new root trace context
     */
    public TraceContext() {
        this.traceId = generateTraceId();
        this.correlationId = generateCorrelationId();
        this.parentSpanId = null;
        this.spanId = generateSpanId();
        this.baggage = new HashMap<>();
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Create a trace context with specific IDs
     */
    public TraceContext(String traceId, String correlationId, String parentSpanId, String spanId) {
        this.traceId = traceId != null ? traceId : generateTraceId();
        this.correlationId = correlationId != null ? correlationId : generateCorrelationId();
        this.parentSpanId = parentSpanId;
        this.spanId = spanId != null ? spanId : generateSpanId();
        this.baggage = new HashMap<>();
        this.startTime = System.currentTimeMillis();
    }
    
    /**
     * Create a child trace context for a new span
     */
    public TraceContext createChildContext(String newSpanId) {
        TraceContext child = new TraceContext(this.traceId, this.correlationId, this.spanId, newSpanId);
        child.baggage.putAll(this.baggage);
        return child;
    }
    
    /**
     * Create a child trace context with auto-generated span ID
     */
    public TraceContext createChildContext() {
        return createChildContext(generateSpanId());
    }
    
    /**
     * Get the trace ID
     */
    public String getTraceId() {
        return traceId;
    }
    
    /**
     * Get the correlation ID
     */
    public String getCorrelationId() {
        return correlationId;
    }
    
    /**
     * Get the parent span ID
     */
    public String getParentSpanId() {
        return parentSpanId;
    }
    
    /**
     * Get the current span ID
     */
    public String getSpanId() {
        return spanId;
    }
    
    /**
     * Get the start time
     */
    public long getStartTime() {
        return startTime;
    }
    
    /**
     * Add baggage item (key-value pair that propagates with the trace)
     */
    public void addBaggage(String key, String value) {
        if (key != null && value != null) {
            baggage.put(key, value);
        }
    }
    
    /**
     * Get baggage item
     */
    public String getBaggage(String key) {
        return baggage.get(key);
    }
    
    /**
     * Get all baggage items
     */
    public Map<String, String> getAllBaggage() {
        return new HashMap<>(baggage);
    }
    
    /**
     * Convert trace context to map for plugin context
     */
    public Map<String, Object> toContextMap() {
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("traceId", traceId);
        contextMap.put("correlationId", correlationId);
        contextMap.put("spanId", spanId);
        if (parentSpanId != null) {
            contextMap.put("parentSpanId", parentSpanId);
        }
        contextMap.put("startTime", startTime);
        if (!baggage.isEmpty()) {
            contextMap.put("baggage", new HashMap<>(baggage));
        }
        return contextMap;
    }
    
    /**
     * Create trace context from map (for deserialization)
     */
    public static TraceContext fromContextMap(Map<String, Object> contextMap) {
        if (contextMap == null) {
            return new TraceContext();
        }
        
        String traceId = (String) contextMap.get("traceId");
        String correlationId = (String) contextMap.get("correlationId");
        String parentSpanId = (String) contextMap.get("parentSpanId");
        String spanId = (String) contextMap.get("spanId");
        
        TraceContext context = new TraceContext(traceId, correlationId, parentSpanId, spanId);
        
        // Restore baggage if present
        @SuppressWarnings("unchecked")
        Map<String, String> baggageMap = (Map<String, String>) contextMap.get("baggage");
        if (baggageMap != null) {
            context.baggage.putAll(baggageMap);
        }
        
        return context;
    }
    
    /**
     * Generate a new trace ID
     */
    private static String generateTraceId() {
        return "trace_" + UUID.randomUUID().toString().replace("-", "");
    }
    
    /**
     * Generate a new correlation ID
     */
    private static String generateCorrelationId() {
        return "corr_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    /**
     * Generate a new span ID
     */
    private static String generateSpanId() {
        return "span_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
    
    @Override
    public String toString() {
        return "TraceContext{" +
                "traceId='" + traceId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", parentSpanId='" + parentSpanId + '\'' +
                ", spanId='" + spanId + '\'' +
                ", baggage=" + baggage +
                ", startTime=" + startTime +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        TraceContext that = (TraceContext) o;
        
        if (startTime != that.startTime) return false;
        if (!traceId.equals(that.traceId)) return false;
        if (!correlationId.equals(that.correlationId)) return false;
        if (parentSpanId != null ? !parentSpanId.equals(that.parentSpanId) : that.parentSpanId != null)
            return false;
        if (!spanId.equals(that.spanId)) return false;
        return baggage.equals(that.baggage);
    }
    
    @Override
    public int hashCode() {
        int result = traceId.hashCode();
        result = 31 * result + correlationId.hashCode();
        result = 31 * result + (parentSpanId != null ? parentSpanId.hashCode() : 0);
        result = 31 * result + spanId.hashCode();
        result = 31 * result + baggage.hashCode();
        result = 31 * result + (int) (startTime ^ (startTime >>> 32));
        return result;
    }
}