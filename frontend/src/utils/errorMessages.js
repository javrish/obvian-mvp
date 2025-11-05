/**
 * Comprehensive error messaging utility for Obvian Petri Net POC
 * Provides actionable suggestions for common issues
 */

export const ErrorTypes = {
  NETWORK_ERROR: 'NETWORK_ERROR',
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  PETRI_NET_ERROR: 'PETRI_NET_ERROR',
  SIMULATION_ERROR: 'SIMULATION_ERROR',
  PERFORMANCE_ERROR: 'PERFORMANCE_ERROR',
  ACCESSIBILITY_ERROR: 'ACCESSIBILITY_ERROR',
  UNKNOWN_ERROR: 'UNKNOWN_ERROR'
};

export const ErrorMessages = {
  [ErrorTypes.NETWORK_ERROR]: {
    title: 'Network Connection Error',
    message: 'Unable to connect to the server. Please check your internet connection.',
    suggestions: [
      'Check your internet connection',
      'Verify the server is running on http://localhost:8080',
      'Try refreshing the page',
      'Contact support if the problem persists'
    ],
    technicalDetails: 'Network request failed or timed out'
  },

  [ErrorTypes.VALIDATION_ERROR]: {
    title: 'Input Validation Error',
    message: 'The provided input contains errors or is incomplete.',
    suggestions: [
      'Check all required fields are filled',
      'Ensure numeric values are within valid ranges',
      'Verify the Petri net structure is valid',
      'Review the input format requirements'
    ],
    technicalDetails: 'Input failed validation rules'
  },

  [ErrorTypes.PETRI_NET_ERROR]: {
    title: 'Petri Net Structure Error',
    message: 'The Petri net contains structural issues that prevent processing.',
    suggestions: [
      'Ensure all places have unique IDs',
      'Verify all transitions have unique IDs',
      'Check that arcs connect valid places and transitions',
      'Confirm the network is properly connected',
      'Validate token counts are non-negative integers'
    ],
    technicalDetails: 'Petri net failed structural validation'
  },

  [ErrorTypes.SIMULATION_ERROR]: {
    title: 'Simulation Execution Error',
    message: 'An error occurred during Petri net simulation.',
    suggestions: [
      'Check if any transitions are enabled',
      'Verify the initial marking is valid',
      'Ensure the network is not deadlocked',
      'Try reducing the simulation speed',
      'Check for infinite loops in the network'
    ],
    technicalDetails: 'Simulation engine encountered an error'
  },

  [ErrorTypes.PERFORMANCE_ERROR]: {
    title: 'Performance Limit Exceeded',
    message: 'The operation exceeded performance thresholds for this POC.',
    suggestions: [
      'Reduce the number of places (target: ≤30)',
      'Reduce the number of transitions (target: ≤30)',
      'Simplify the network structure',
      'Consider using a smaller initial marking',
      'Try disabling animations for better performance'
    ],
    technicalDetails: 'Network size or complexity exceeds POC limits'
  },

  [ErrorTypes.ACCESSIBILITY_ERROR]: {
    title: 'Accessibility Feature Error',
    message: 'An error occurred with accessibility features.',
    suggestions: [
      'Try refreshing the page',
      'Check if browser supports required accessibility APIs',
      'Try disabling browser extensions that might interfere',
      'Use latest version of a modern browser',
      'Report issue with browser and operating system details'
    ],
    technicalDetails: 'Accessibility enhancement failed to initialize'
  },

  [ErrorTypes.UNKNOWN_ERROR]: {
    title: 'Unexpected Error',
    message: 'An unexpected error occurred. This is a technical issue.',
    suggestions: [
      'Try refreshing the page',
      'Clear browser cache and cookies',
      'Try using a different browser',
      'Report this issue with steps to reproduce',
      'Include browser console logs when reporting'
    ],
    technicalDetails: 'Unhandled exception or error'
  }
};

/**
 * Performance validation for POC limits
 */
export const validatePOCLimits = (petriNet) => {
  const errors = [];

  if (!petriNet) {
    return [{ type: ErrorTypes.PETRI_NET_ERROR, message: 'No Petri net provided' }];
  }

  // Check place count limit
  const placeCount = petriNet.places?.length || 0;
  if (placeCount > 30) {
    errors.push({
      type: ErrorTypes.PERFORMANCE_ERROR,
      message: `Network has ${placeCount} places (limit: 30 for optimal performance)`
    });
  }

  // Check transition count limit
  const transitionCount = petriNet.transitions?.length || 0;
  if (transitionCount > 30) {
    errors.push({
      type: ErrorTypes.PERFORMANCE_ERROR,
      message: `Network has ${transitionCount} transitions (limit: 30 for optimal performance)`
    });
  }

  // Check for extremely large token counts
  if (petriNet.initialMarking) {
    const maxTokens = Math.max(...Object.values(petriNet.initialMarking));
    if (maxTokens > 1000) {
      errors.push({
        type: ErrorTypes.PERFORMANCE_ERROR,
        message: `Initial marking contains ${maxTokens} tokens in a single place (consider reducing for better performance)`
      });
    }
  }

  return errors;
};

/**
 * Get user-friendly error message with actionable suggestions
 */
export const getErrorMessage = (errorType, context = {}) => {
  const errorInfo = ErrorMessages[errorType] || ErrorMessages[ErrorTypes.UNKNOWN_ERROR];

  return {
    ...errorInfo,
    context,
    timestamp: new Date().toISOString(),
    errorId: generateErrorId()
  };
};

/**
 * Generate unique error ID for tracking/reporting
 */
const generateErrorId = () => {
  return `err_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
};

/**
 * Common error patterns and their classifications
 */
export const classifyError = (error) => {
  if (!error) return ErrorTypes.UNKNOWN_ERROR;

  const message = error.message?.toLowerCase() || '';

  if (message.includes('network') || message.includes('fetch') || message.includes('connection')) {
    return ErrorTypes.NETWORK_ERROR;
  }

  if (message.includes('validation') || message.includes('invalid') || message.includes('required')) {
    return ErrorTypes.VALIDATION_ERROR;
  }

  if (message.includes('petri') || message.includes('place') || message.includes('transition') || message.includes('arc')) {
    return ErrorTypes.PETRI_NET_ERROR;
  }

  if (message.includes('simulation') || message.includes('token') || message.includes('marking')) {
    return ErrorTypes.SIMULATION_ERROR;
  }

  if (message.includes('performance') || message.includes('timeout') || message.includes('limit')) {
    return ErrorTypes.PERFORMANCE_ERROR;
  }

  if (message.includes('accessibility') || message.includes('aria') || message.includes('screen reader')) {
    return ErrorTypes.ACCESSIBILITY_ERROR;
  }

  return ErrorTypes.UNKNOWN_ERROR;
};

/**
 * Format error for display in UI components
 */
export const formatErrorForDisplay = (error, includeDetails = false) => {
  const errorType = classifyError(error);
  const errorMessage = getErrorMessage(errorType, { originalError: error });

  return {
    title: errorMessage.title,
    message: errorMessage.message,
    suggestions: errorMessage.suggestions,
    ...(includeDetails && {
      technicalDetails: errorMessage.technicalDetails,
      originalMessage: error?.message,
      stack: error?.stack
    })
  };
};

export default {
  ErrorTypes,
  ErrorMessages,
  validatePOCLimits,
  getErrorMessage,
  classifyError,
  formatErrorForDisplay
};