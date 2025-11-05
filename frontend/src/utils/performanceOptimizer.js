/**
 * Performance Optimization Utilities for Petri Net POC
 * Optimized for target network sizes (≤30 places, ≤30 transitions)
 */

// POC Performance Limits
export const POC_LIMITS = {
  MAX_PLACES: 30,
  MAX_TRANSITIONS: 30,
  MAX_ARCS: 100,
  MAX_TOKENS_PER_PLACE: 1000,
  MAX_SIMULATION_STEPS: 1000,
  RENDER_THROTTLE_MS: 16, // ~60fps
  ANIMATION_DURATION_MS: 300,
  DEBOUNCE_DELAY_MS: 300
};

// Performance monitoring
let performanceMetrics = {
  renderCount: 0,
  averageRenderTime: 0,
  lastRenderTime: 0,
  memoryUsage: 0,
  networkComplexity: 0
};

/**
 * Validate network size against POC limits
 */
export const validateNetworkSize = (petriNet) => {
  const warnings = [];
  const errors = [];

  if (!petriNet) {
    errors.push('No Petri net provided');
    return { valid: false, warnings, errors };
  }

  const placeCount = petriNet.places?.length || 0;
  const transitionCount = petriNet.transitions?.length || 0;
  const arcCount = petriNet.arcs?.length || 0;

  // Hard limits (errors)
  if (placeCount > POC_LIMITS.MAX_PLACES) {
    errors.push(
      `Network has ${placeCount} places (limit: ${POC_LIMITS.MAX_PLACES} for optimal performance)`
    );
  }

  if (transitionCount > POC_LIMITS.MAX_TRANSITIONS) {
    errors.push(
      `Network has ${transitionCount} transitions (limit: ${POC_LIMITS.MAX_TRANSITIONS} for optimal performance)`
    );
  }

  if (arcCount > POC_LIMITS.MAX_ARCS) {
    errors.push(
      `Network has ${arcCount} arcs (limit: ${POC_LIMITS.MAX_ARCS} for optimal performance)`
    );
  }

  // Soft limits (warnings)
  const totalElements = placeCount + transitionCount;
  if (totalElements > 40) {
    warnings.push(
      `Network has ${totalElements} total elements. Consider simplifying for better performance.`
    );
  }

  // Check token counts
  if (petriNet.initialMarking) {
    Object.entries(petriNet.initialMarking).forEach(([placeId, tokens]) => {
      if (tokens > POC_LIMITS.MAX_TOKENS_PER_PLACE) {
        warnings.push(
          `Place ${placeId} has ${tokens} tokens (consider reducing for better performance)`
        );
      }
    });
  }

  // Calculate network complexity score
  const complexityScore = calculateNetworkComplexity(petriNet);
  performanceMetrics.networkComplexity = complexityScore;

  if (complexityScore > 0.8) {
    warnings.push(
      'Network complexity is high. Consider simplifying the structure for better performance.'
    );
  }

  return {
    valid: errors.length === 0,
    warnings,
    errors,
    metrics: {
      placeCount,
      transitionCount,
      arcCount,
      totalElements,
      complexityScore
    }
  };
};

/**
 * Calculate network complexity score (0-1, higher = more complex)
 */
export const calculateNetworkComplexity = (petriNet) => {
  if (!petriNet || !petriNet.places || !petriNet.transitions) {
    return 0;
  }

  const placeCount = petriNet.places.length;
  const transitionCount = petriNet.transitions.length;
  const arcCount = petriNet.arcs?.length || 0;

  // Normalize against POC limits
  const placeComplexity = placeCount / POC_LIMITS.MAX_PLACES;
  const transitionComplexity = transitionCount / POC_LIMITS.MAX_TRANSITIONS;
  const arcComplexity = arcCount / POC_LIMITS.MAX_ARCS;

  // Calculate average degree (arcs per element)
  const totalElements = placeCount + transitionCount;
  const avgDegree = totalElements > 0 ? arcCount / totalElements : 0;
  const degreeComplexity = Math.min(avgDegree / 5, 1); // Normalize assuming max degree of 5

  // Weighted complexity score
  return Math.min(
    (placeComplexity * 0.3) +
    (transitionComplexity * 0.3) +
    (arcComplexity * 0.2) +
    (degreeComplexity * 0.2),
    1.0
  );
};

/**
 * Optimize rendering performance based on network size
 */
export const getOptimizedRenderConfig = (petriNet) => {
  const validation = validateNetworkSize(petriNet);
  const complexity = validation.metrics?.complexityScore || 0;

  // Base configuration
  let config = {
    enableAnimations: true,
    animationDuration: POC_LIMITS.ANIMATION_DURATION_MS,
    renderQuality: 'high',
    enableLabels: true,
    enableTokenDisplay: true,
    maxFPS: 60,
    throttleUpdates: false
  };

  // Adjust based on complexity
  if (complexity > 0.7) {
    // High complexity - reduce features for performance
    config = {
      ...config,
      enableAnimations: false,
      renderQuality: 'medium',
      maxFPS: 30,
      throttleUpdates: true
    };
  } else if (complexity > 0.5) {
    // Medium complexity - some optimizations
    config = {
      ...config,
      animationDuration: POC_LIMITS.ANIMATION_DURATION_MS * 0.7,
      renderQuality: 'medium',
      maxFPS: 45
    };
  }

  return config;
};

/**
 * Throttle function calls for performance
 */
export const throttle = (func, delay = POC_LIMITS.RENDER_THROTTLE_MS) => {
  let timeoutId;
  let lastExecTime = 0;

  return function (...args) {
    const currentTime = Date.now();

    if (currentTime - lastExecTime > delay) {
      func.apply(this, args);
      lastExecTime = currentTime;
    } else {
      clearTimeout(timeoutId);
      timeoutId = setTimeout(() => {
        func.apply(this, args);
        lastExecTime = Date.now();
      }, delay - (currentTime - lastExecTime));
    }
  };
};

/**
 * Debounce function calls
 */
export const debounce = (func, delay = POC_LIMITS.DEBOUNCE_DELAY_MS) => {
  let timeoutId;

  return function (...args) {
    clearTimeout(timeoutId);
    timeoutId = setTimeout(() => func.apply(this, args), delay);
  };
};

/**
 * Performance monitoring utilities
 */
export const performanceMonitor = {
  startRender: () => {
    performanceMetrics.lastRenderTime = performance.now();
  },

  endRender: () => {
    const endTime = performance.now();
    const renderTime = endTime - performanceMetrics.lastRenderTime;

    performanceMetrics.renderCount++;
    performanceMetrics.averageRenderTime =
      (performanceMetrics.averageRenderTime * (performanceMetrics.renderCount - 1) + renderTime) /
      performanceMetrics.renderCount;

    return renderTime;
  },

  getMetrics: () => ({ ...performanceMetrics }),

  updateMemoryUsage: () => {
    if (performance.memory) {
      performanceMetrics.memoryUsage = performance.memory.usedJSHeapSize / 1024 / 1024; // MB
    }
  },

  reset: () => {
    performanceMetrics = {
      renderCount: 0,
      averageRenderTime: 0,
      lastRenderTime: 0,
      memoryUsage: 0,
      networkComplexity: 0
    };
  }
};

/**
 * Memory optimization for large data sets
 */
export const optimizeDataForRendering = (petriNet) => {
  if (!petriNet) return null;

  // Create optimized copy
  const optimized = JSON.parse(JSON.stringify(petriNet));

  // Truncate long labels for performance
  if (optimized.places) {
    optimized.places.forEach(place => {
      if (place.name && place.name.length > 20) {
        place.displayName = place.name.substring(0, 17) + '...';
      }
    });
  }

  if (optimized.transitions) {
    optimized.transitions.forEach(transition => {
      if (transition.name && transition.name.length > 15) {
        transition.displayName = transition.name.substring(0, 12) + '...';
      }
    });
  }

  return optimized;
};

/**
 * Check if reduced motion is preferred
 */
export const prefersReducedMotion = () => {
  return window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
};

/**
 * Adaptive performance configuration based on device capabilities
 */
export const getAdaptiveConfig = () => {
  const isLowEndDevice = () => {
    // Heuristic for low-end device detection
    const hardwareConcurrency = navigator.hardwareConcurrency || 2;
    const memory = navigator.deviceMemory || 2; // GB

    return hardwareConcurrency <= 2 || memory <= 2;
  };

  const isMobile = () => {
    return /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
  };

  let config = {
    maxFPS: 60,
    enableAnimations: true,
    renderQuality: 'high',
    enableEffects: true
  };

  if (isLowEndDevice() || isMobile()) {
    config = {
      maxFPS: 30,
      enableAnimations: !prefersReducedMotion(),
      renderQuality: 'medium',
      enableEffects: false
    };
  }

  if (prefersReducedMotion()) {
    config.enableAnimations = false;
    config.enableEffects = false;
  }

  return config;
};

/**
 * Export performance utilities as default
 */
export default {
  POC_LIMITS,
  validateNetworkSize,
  calculateNetworkComplexity,
  getOptimizedRenderConfig,
  throttle,
  debounce,
  performanceMonitor,
  optimizeDataForRendering,
  prefersReducedMotion,
  getAdaptiveConfig
};