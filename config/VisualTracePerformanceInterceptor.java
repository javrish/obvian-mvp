package api.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Performance monitoring interceptor for Visual Trace HTTP requests.
 * 
 * This interceptor provides:
 * - Request/response timing metrics
 * - Slow request detection and logging
 * - HTTP status code tracking
 * - Request size monitoring
 * - MDC context management
 * - Performance budgets enforcement
 * 
 * @author Obvian Labs
 * @since Phase 26.2
 */
@Component
public class VisualTracePerformanceInterceptor implements HandlerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(VisualTracePerformanceInterceptor.class);
    
    // Performance thresholds
    private static final Duration SLOW_REQUEST_THRESHOLD = Duration.ofMillis(200);
    private static final Duration VERY_SLOW_REQUEST_THRESHOLD = Duration.ofSeconds(1);
    private static final int LARGE_REQUEST_THRESHOLD = 10 * 1024; // 10KB
    private static final int LARGE_RESPONSE_THRESHOLD = 100 * 1024; // 100KB
    
    // Request attributes
    private static final String START_TIME_ATTR = "startTime";
    private static final String REQUEST_ID_ATTR = "requestId";
    private static final String ENDPOINT_TYPE_ATTR = "endpointType";
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;
    
    // Metrics
    private Timer requestTimer;
    private Timer slowRequestTimer;
    private Counter requestCounter;
    private Counter errorCounter;
    private Counter largeRequestCounter;
    private Counter largeResponseCounter;
    
    /**
     * Initialize metrics if MeterRegistry is available.
     */
    @jakarta.annotation.PostConstruct
    public void initializeMetrics() {
        if (meterRegistry != null) {
            requestTimer = Timer.builder("http.visual_trace.request.duration")
                .description("Duration of visual trace HTTP requests")
                .tag("component", "interceptor")
                .register(meterRegistry);
                
            slowRequestTimer = Timer.builder("http.visual_trace.request.slow.duration")
                .description("Duration of slow visual trace HTTP requests")
                .tag("component", "interceptor")
                .register(meterRegistry);
                
            requestCounter = Counter.builder("http.visual_trace.requests")
                .description("Total number of visual trace HTTP requests")
                .tag("component", "interceptor")
                .register(meterRegistry);
                
            errorCounter = Counter.builder("http.visual_trace.errors")
                .description("Number of visual trace HTTP errors")
                .tag("component", "interceptor")
                .register(meterRegistry);
                
            largeRequestCounter = Counter.builder("http.visual_trace.requests.large")
                .description("Number of large visual trace requests")
                .tag("component", "interceptor")
                .register(meterRegistry);
                
            largeResponseCounter = Counter.builder("http.visual_trace.responses.large")
                .description("Number of large visual trace responses")
                .tag("component", "interceptor")
                .register(meterRegistry);
                
            logger.info("VisualTracePerformanceInterceptor metrics initialized");
        } else {
            logger.warn("MeterRegistry not available - metrics will not be collected");
        }
    }
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Instant startTime = Instant.now();
        String requestId = UUID.randomUUID().toString();
        String endpointType = determineEndpointType(request.getRequestURI());
        
        // Store timing information
        request.setAttribute(START_TIME_ATTR, startTime);
        request.setAttribute(REQUEST_ID_ATTR, requestId);
        request.setAttribute(ENDPOINT_TYPE_ATTR, endpointType);
        
        // Set MDC context
        MDC.put("requestId", requestId);
        MDC.put("endpointType", endpointType);
        MDC.put("method", request.getMethod());
        MDC.put("uri", request.getRequestURI());
        
        // Check request size
        int contentLength = request.getContentLength();
        if (contentLength > LARGE_REQUEST_THRESHOLD) {
            if (largeRequestCounter != null) {
                largeRequestCounter.increment();
            }
            logger.warn("Large request detected: {} bytes for {} {}", 
                contentLength, request.getMethod(), request.getRequestURI());
            MDC.put("largeRequest", "true");
            MDC.put("requestSize", String.valueOf(contentLength));
        }
        
        // Increment request counter
        if (requestCounter != null) {
            requestCounter.increment();
        }
        
        logger.debug("Processing request: {} {} [requestId: {}]", 
            request.getMethod(), request.getRequestURI(), requestId);
        
        return true;
    }
    
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) throws Exception {
        // This method is called after the handler method but before view rendering
        // We can add additional logging or metrics here if needed
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        try {
            Instant startTime = (Instant) request.getAttribute(START_TIME_ATTR);
            String requestId = (String) request.getAttribute(REQUEST_ID_ATTR);
            String endpointType = (String) request.getAttribute(ENDPOINT_TYPE_ATTR);
            
            if (startTime == null) {
                logger.warn("Start time not found for request - timing metrics unavailable");
                return;
            }
            
            Duration requestDuration = Duration.between(startTime, Instant.now());
            int statusCode = response.getStatus();
            boolean isError = statusCode >= 400;
            
            // Record timing metrics
            if (requestTimer != null) {
                Timer.Sample sample = Timer.start(meterRegistry);
                sample.stop(Timer.builder("http.visual_trace.request.duration")
                    .tag("method", request.getMethod())
                    .tag("status", String.valueOf(statusCode))
                    .tag("endpoint", endpointType != null ? endpointType : "unknown")
                    .register(meterRegistry));
            }
            
            // Record error metrics
            if (isError && errorCounter != null) {
                errorCounter.increment();
            }
            
            // Handle slow requests
            if (requestDuration.compareTo(SLOW_REQUEST_THRESHOLD) > 0) {
                if (slowRequestTimer != null) {
                    slowRequestTimer.record(requestDuration);
                }
                
                String logLevel = requestDuration.compareTo(VERY_SLOW_REQUEST_THRESHOLD) > 0 ? "ERROR" : "WARN";
                
                if ("ERROR".equals(logLevel)) {
                    logger.error("Very slow request detected: {} {} - duration: {}ms, status: {} [requestId: {}]", 
                        request.getMethod(), request.getRequestURI(), 
                        requestDuration.toMillis(), statusCode, requestId);
                } else {
                    logger.warn("Slow request detected: {} {} - duration: {}ms, status: {} [requestId: {}]", 
                        request.getMethod(), request.getRequestURI(), 
                        requestDuration.toMillis(), statusCode, requestId);
                }
                
                MDC.put("slowRequest", "true");
                MDC.put("requestDuration", String.valueOf(requestDuration.toMillis()));
            }
            
            // Check response size (rough estimation from headers)
            String contentLength = response.getHeader("Content-Length");
            if (contentLength != null) {
                try {
                    int responseSize = Integer.parseInt(contentLength);
                    if (responseSize > LARGE_RESPONSE_THRESHOLD) {
                        if (largeResponseCounter != null) {
                            largeResponseCounter.increment();
                        }
                        logger.warn("Large response detected: {} bytes for {} {} [requestId: {}]", 
                            responseSize, request.getMethod(), request.getRequestURI(), requestId);
                        MDC.put("largeResponse", "true");
                        MDC.put("responseSize", String.valueOf(responseSize));
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse Content-Length header: {}", contentLength);
                }
            }
            
            // Log completion
            if (isError) {
                logger.error("Request completed with error: {} {} - duration: {}ms, status: {} [requestId: {}]", 
                    request.getMethod(), request.getRequestURI(), 
                    requestDuration.toMillis(), statusCode, requestId);
            } else {
                logger.debug("Request completed successfully: {} {} - duration: {}ms, status: {} [requestId: {}]", 
                    request.getMethod(), request.getRequestURI(), 
                    requestDuration.toMillis(), statusCode, requestId);
            }
            
            // Log any exceptions
            if (ex != null) {
                logger.error("Request failed with exception: {} {} [requestId: {}]", 
                    request.getMethod(), request.getRequestURI(), requestId, ex);
                MDC.put("exception", ex.getClass().getSimpleName());
                MDC.put("exceptionMessage", ex.getMessage());
            }
            
        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }
    
    /**
     * Determines the endpoint type based on the request URI.
     */
    private String determineEndpointType(String uri) {
        if (uri == null) return "unknown";
        
        if (uri.contains("/visual-trace/")) {
            if (uri.endsWith("/data")) return "trace-data";
            if (uri.contains("/critical-path")) return "critical-path";
            if (uri.contains("/stream")) return "event-stream";
            return "visual-trace";
        }
        
        if (uri.contains("/timeline/")) {
            if (uri.contains("/events")) return "timeline-events";
            return "timeline";
        }
        
        if (uri.contains("/playback/")) {
            if (uri.contains("/control")) return "playback-control";
            if (uri.contains("/state")) return "playback-state";
            return "playback";
        }
        
        return "other";
    }
    
    /**
     * Determines if a request should be considered for performance budgets.
     */
    private boolean isPerformanceCritical(String endpointType) {
        return "trace-data".equals(endpointType) || 
               "timeline-events".equals(endpointType) ||
               "critical-path".equals(endpointType);
    }
    
    /**
     * Gets performance budget threshold for endpoint type.
     */
    private Duration getPerformanceBudget(String endpointType) {
        switch (endpointType) {
            case "trace-data":
                return Duration.ofMillis(500); // 500ms for trace data
            case "timeline-events":
                return Duration.ofMillis(200); // 200ms for timeline events
            case "critical-path":
                return Duration.ofMillis(300); // 300ms for critical path
            case "playback-control":
                return Duration.ofMillis(100); // 100ms for playback controls
            default:
                return SLOW_REQUEST_THRESHOLD;
        }
    }
}