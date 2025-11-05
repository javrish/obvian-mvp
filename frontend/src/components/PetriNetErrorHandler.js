import React, { useState, useCallback, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  AlertTriangle, XCircle, CheckCircle, Info, X, Copy,
  ChevronDown, ChevronRight, RefreshCw, ExternalLink,
  BookOpen, Code, Bug, Lightbulb, MessageSquare
} from 'lucide-react';
import toast from 'react-hot-toast';

// Error categories and severity levels
const ERROR_CATEGORIES = {
  PARSING: 'parsing',
  VALIDATION: 'validation',
  SIMULATION: 'simulation',
  NETWORK: 'network',
  SYSTEM: 'system'
};

const SEVERITY_LEVELS = {
  INFO: 'info',
  WARNING: 'warning',
  ERROR: 'error',
  CRITICAL: 'critical'
};

const PetriNetErrorHandler = ({
  errors = [],
  onErrorDismiss,
  onErrorRetry,
  onErrorReport,
  className = "",
  showDetailedFeedback = true,
  enableAutoSuggestions = true
}) => {
  const [expandedErrors, setExpandedErrors] = useState(new Set());
  const [filterCategory, setFilterCategory] = useState('all');
  const [filterSeverity, setFilterSeverity] = useState('all');
  const [showSuggestions, setShowSuggestions] = useState(true);

  // Error statistics
  const errorStats = {
    total: errors.length,
    critical: errors.filter(e => e.severity === SEVERITY_LEVELS.CRITICAL).length,
    errors: errors.filter(e => e.severity === SEVERITY_LEVELS.ERROR).length,
    warnings: errors.filter(e => e.severity === SEVERITY_LEVELS.WARNING).length,
    info: errors.filter(e => e.severity === SEVERITY_LEVELS.INFO).length
  };

  // Filter errors based on category and severity
  const filteredErrors = errors.filter(error => {
    const categoryMatch = filterCategory === 'all' || error.category === filterCategory;
    const severityMatch = filterSeverity === 'all' || error.severity === filterSeverity;
    return categoryMatch && severityMatch;
  });

  // Toggle error expansion
  const toggleErrorExpansion = useCallback((errorId) => {
    const newExpanded = new Set(expandedErrors);
    if (expandedErrors.has(errorId)) {
      newExpanded.delete(errorId);
    } else {
      newExpanded.add(errorId);
    }
    setExpandedErrors(newExpanded);
  }, [expandedErrors]);

  // Copy error details to clipboard
  const copyErrorDetails = useCallback((error) => {
    const errorText = `
P3Net Error Report
=================
ID: ${error.id}
Category: ${error.category}
Severity: ${error.severity}
Message: ${error.message}
Timestamp: ${new Date(error.timestamp).toISOString()}
${error.details ? `\nDetails: ${JSON.stringify(error.details, null, 2)}` : ''}
${error.stackTrace ? `\nStack Trace:\n${error.stackTrace}` : ''}
    `.trim();

    navigator.clipboard.writeText(errorText).then(() => {
      toast.success('Error details copied to clipboard');
    }).catch(() => {
      toast.error('Failed to copy error details');
    });
  }, []);

  // Get error suggestions
  const getErrorSuggestions = useCallback((error) => {
    const suggestions = [];

    switch (error.category) {
      case ERROR_CATEGORIES.PARSING:
        suggestions.push({
          type: 'fix',
          title: 'Check Natural Language Input',
          description: 'Ensure your workflow description is clear and follows supported patterns',
          action: 'review_input'
        });
        if (error.code === 'UNRECOGNIZED_PATTERN') {
          suggestions.push({
            type: 'template',
            title: 'Use Template',
            description: 'Try starting with a pre-built workflow template',
            action: 'use_template'
          });
        }
        break;

      case ERROR_CATEGORIES.VALIDATION:
        if (error.code === 'DEADLOCK_DETECTED') {
          suggestions.push({
            type: 'fix',
            title: 'Fix Deadlock',
            description: 'Add alternative execution paths or check token distribution',
            action: 'fix_deadlock'
          });
        }
        if (error.code === 'UNREACHABLE_STATE') {
          suggestions.push({
            type: 'fix',
            title: 'Check Reachability',
            description: 'Verify all transitions can be reached from the initial state',
            action: 'check_reachability'
          });
        }
        suggestions.push({
          type: 'analyze',
          title: 'Run Detailed Analysis',
          description: 'Get a comprehensive validation report with suggested fixes',
          action: 'detailed_analysis'
        });
        break;

      case ERROR_CATEGORIES.SIMULATION:
        if (error.code === 'INFINITE_LOOP') {
          suggestions.push({
            type: 'fix',
            title: 'Add Termination Condition',
            description: 'Set maximum steps or add completion conditions',
            action: 'add_termination'
          });
        }
        suggestions.push({
          type: 'debug',
          title: 'Debug Step-by-Step',
          description: 'Use interactive mode to trace execution manually',
          action: 'debug_simulation'
        });
        break;

      case ERROR_CATEGORIES.NETWORK:
        suggestions.push({
          type: 'retry',
          title: 'Retry Operation',
          description: 'Network errors are often temporary',
          action: 'retry'
        });
        if (error.code === 'WEBSOCKET_DISCONNECTED') {
          suggestions.push({
            type: 'fallback',
            title: 'Use Local Mode',
            description: 'Continue with local simulation while connection is restored',
            action: 'local_mode'
          });
        }
        break;

      case ERROR_CATEGORIES.SYSTEM:
        suggestions.push({
          type: 'info',
          title: 'System Resources',
          description: 'Check system performance and available memory',
          action: 'check_resources'
        });
        break;
    }

    return suggestions;
  }, []);

  // Handle suggestion actions
  const handleSuggestionAction = useCallback((error, suggestion) => {
    switch (suggestion.action) {
      case 'retry':
        if (onErrorRetry) {
          onErrorRetry(error);
        }
        break;
      case 'review_input':
        toast.info('Please review your natural language input for clarity');
        break;
      case 'use_template':
        toast.info('Consider starting with a workflow template');
        break;
      case 'detailed_analysis':
        toast.info('Starting detailed validation analysis...');
        break;
      case 'debug_simulation':
        toast.info('Switch to step-by-step debug mode');
        break;
      case 'local_mode':
        toast.info('Switching to local simulation mode');
        break;
      default:
        toast.info(`Suggestion: ${suggestion.description}`);
    }
  }, [onErrorRetry]);

  // Get icon for error category
  const getCategoryIcon = (category) => {
    switch (category) {
      case ERROR_CATEGORIES.PARSING:
        return Code;
      case ERROR_CATEGORIES.VALIDATION:
        return CheckCircle;
      case ERROR_CATEGORIES.SIMULATION:
        return RefreshCw;
      case ERROR_CATEGORIES.NETWORK:
        return ExternalLink;
      case ERROR_CATEGORIES.SYSTEM:
        return Bug;
      default:
        return AlertTriangle;
    }
  };

  // Get color scheme for severity
  const getSeverityColors = (severity) => {
    switch (severity) {
      case SEVERITY_LEVELS.CRITICAL:
        return {
          bg: 'bg-red-50',
          border: 'border-red-200',
          text: 'text-red-800',
          icon: 'text-red-600',
          badge: 'bg-red-100 text-red-800'
        };
      case SEVERITY_LEVELS.ERROR:
        return {
          bg: 'bg-red-50',
          border: 'border-red-200',
          text: 'text-red-700',
          icon: 'text-red-500',
          badge: 'bg-red-100 text-red-700'
        };
      case SEVERITY_LEVELS.WARNING:
        return {
          bg: 'bg-yellow-50',
          border: 'border-yellow-200',
          text: 'text-yellow-800',
          icon: 'text-yellow-600',
          badge: 'bg-yellow-100 text-yellow-800'
        };
      case SEVERITY_LEVELS.INFO:
        return {
          bg: 'bg-blue-50',
          border: 'border-blue-200',
          text: 'text-blue-800',
          icon: 'text-blue-600',
          badge: 'bg-blue-100 text-blue-800'
        };
      default:
        return {
          bg: 'bg-gray-50',
          border: 'border-gray-200',
          text: 'text-gray-800',
          icon: 'text-gray-600',
          badge: 'bg-gray-100 text-gray-800'
        };
    }
  };

  // Get severity icon
  const getSeverityIcon = (severity) => {
    switch (severity) {
      case SEVERITY_LEVELS.CRITICAL:
      case SEVERITY_LEVELS.ERROR:
        return XCircle;
      case SEVERITY_LEVELS.WARNING:
        return AlertTriangle;
      case SEVERITY_LEVELS.INFO:
        return Info;
      default:
        return AlertTriangle;
    }
  };

  if (filteredErrors.length === 0) {
    return null;
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={`bg-white rounded-xl border border-gray-200 shadow-lg ${className}`}
    >
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-100">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <AlertTriangle className="h-6 w-6 text-red-600" />
            <div>
              <h2 className="text-lg font-semibold text-gray-900">
                P3Net Issues & Suggestions
              </h2>
              <p className="text-sm text-gray-600">
                {errorStats.total} issue{errorStats.total !== 1 ? 's' : ''} detected
                {errorStats.critical > 0 && (
                  <span className="ml-2 text-red-600 font-medium">
                    {errorStats.critical} critical
                  </span>
                )}
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            {/* Error Statistics */}
            <div className="flex items-center space-x-1">
              {errorStats.critical > 0 && (
                <span className="px-2 py-1 bg-red-100 text-red-800 text-xs font-medium rounded-full">
                  {errorStats.critical} Critical
                </span>
              )}
              {errorStats.errors > 0 && (
                <span className="px-2 py-1 bg-red-100 text-red-700 text-xs font-medium rounded-full">
                  {errorStats.errors} Error{errorStats.errors !== 1 ? 's' : ''}
                </span>
              )}
              {errorStats.warnings > 0 && (
                <span className="px-2 py-1 bg-yellow-100 text-yellow-800 text-xs font-medium rounded-full">
                  {errorStats.warnings} Warning{errorStats.warnings !== 1 ? 's' : ''}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Filters */}
        <div className="mt-4 flex items-center space-x-4">
          <div className="flex items-center space-x-2">
            <label className="text-sm font-medium text-gray-700">Category:</label>
            <select
              value={filterCategory}
              onChange={(e) => setFilterCategory(e.target.value)}
              className="text-sm border border-gray-300 rounded-lg px-2 py-1 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All</option>
              <option value={ERROR_CATEGORIES.PARSING}>Parsing</option>
              <option value={ERROR_CATEGORIES.VALIDATION}>Validation</option>
              <option value={ERROR_CATEGORIES.SIMULATION}>Simulation</option>
              <option value={ERROR_CATEGORIES.NETWORK}>Network</option>
              <option value={ERROR_CATEGORIES.SYSTEM}>System</option>
            </select>
          </div>

          <div className="flex items-center space-x-2">
            <label className="text-sm font-medium text-gray-700">Severity:</label>
            <select
              value={filterSeverity}
              onChange={(e) => setFilterSeverity(e.target.value)}
              className="text-sm border border-gray-300 rounded-lg px-2 py-1 focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="all">All</option>
              <option value={SEVERITY_LEVELS.CRITICAL}>Critical</option>
              <option value={SEVERITY_LEVELS.ERROR}>Error</option>
              <option value={SEVERITY_LEVELS.WARNING}>Warning</option>
              <option value={SEVERITY_LEVELS.INFO}>Info</option>
            </select>
          </div>
        </div>
      </div>

      {/* Error List */}
      <div className="max-h-96 overflow-y-auto">
        <AnimatePresence>
          {filteredErrors.map((error) => {
            const isExpanded = expandedErrors.has(error.id);
            const colors = getSeverityColors(error.severity);
            const SeverityIcon = getSeverityIcon(error.severity);
            const CategoryIcon = getCategoryIcon(error.category);
            const suggestions = enableAutoSuggestions ? getErrorSuggestions(error) : [];

            return (
              <motion.div
                key={error.id}
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: 'auto' }}
                exit={{ opacity: 0, height: 0 }}
                className={`border-l-4 ${colors.border} ${colors.bg} mb-2 mx-4 rounded-lg`}
              >
                <div className="p-4">
                  {/* Error Header */}
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-3 flex-1">
                      <SeverityIcon className={`h-5 w-5 ${colors.icon} flex-shrink-0 mt-0.5`} />
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center space-x-2 mb-1">
                          <h4 className={`font-medium ${colors.text}`}>
                            {error.title || 'P3Net Error'}
                          </h4>
                          <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${colors.badge}`}>
                            {error.severity}
                          </span>
                          <span className="px-2 py-0.5 bg-gray-100 text-gray-700 rounded-full text-xs font-medium flex items-center space-x-1">
                            <CategoryIcon className="h-3 w-3" />
                            <span>{error.category}</span>
                          </span>
                        </div>
                        <p className={`text-sm ${colors.text} leading-relaxed`}>
                          {error.message}
                        </p>
                        <div className="flex items-center space-x-4 mt-2 text-xs text-gray-500">
                          <span>ID: {error.id}</span>
                          {error.timestamp && (
                            <span>
                              {new Date(error.timestamp).toLocaleString()}
                            </span>
                          )}
                          {error.code && (
                            <span>Code: {error.code}</span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center space-x-1 ml-4">
                      <motion.button
                        onClick={() => toggleErrorExpansion(error.id)}
                        className="p-1 text-gray-400 hover:text-gray-600 rounded"
                        whileHover={{ scale: 1.1 }}
                        whileTap={{ scale: 0.9 }}
                        title="Toggle details"
                      >
                        {isExpanded ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )}
                      </motion.button>

                      <motion.button
                        onClick={() => copyErrorDetails(error)}
                        className="p-1 text-gray-400 hover:text-gray-600 rounded"
                        whileHover={{ scale: 1.1 }}
                        whileTap={{ scale: 0.9 }}
                        title="Copy error details"
                      >
                        <Copy className="h-4 w-4" />
                      </motion.button>

                      {onErrorDismiss && (
                        <motion.button
                          onClick={() => onErrorDismiss(error.id)}
                          className="p-1 text-gray-400 hover:text-gray-600 rounded"
                          whileHover={{ scale: 1.1 }}
                          whileTap={{ scale: 0.9 }}
                          title="Dismiss error"
                        >
                          <X className="h-4 w-4" />
                        </motion.button>
                      )}
                    </div>
                  </div>

                  {/* Expanded Details */}
                  <AnimatePresence>
                    {isExpanded && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="mt-4 space-y-4"
                      >
                        {/* Error Details */}
                        {(error.details || error.stackTrace) && (
                          <div>
                            <h5 className="text-sm font-medium text-gray-900 mb-2">Details</h5>
                            <div className="bg-white border border-gray-200 rounded-lg p-3">
                              {error.details && (
                                <div className="mb-2">
                                  <pre className="text-xs text-gray-600 whitespace-pre-wrap">
                                    {typeof error.details === 'string'
                                      ? error.details
                                      : JSON.stringify(error.details, null, 2)}
                                  </pre>
                                </div>
                              )}
                              {error.stackTrace && (
                                <div>
                                  <h6 className="text-xs font-medium text-gray-700 mb-1">Stack Trace:</h6>
                                  <pre className="text-xs text-gray-500 whitespace-pre-wrap font-mono">
                                    {error.stackTrace}
                                  </pre>
                                </div>
                              )}
                            </div>
                          </div>
                        )}

                        {/* Suggestions */}
                        {suggestions.length > 0 && showSuggestions && (
                          <div>
                            <h5 className="text-sm font-medium text-gray-900 mb-2 flex items-center space-x-1">
                              <Lightbulb className="h-4 w-4 text-yellow-500" />
                              <span>Suggested Actions</span>
                            </h5>
                            <div className="space-y-2">
                              {suggestions.map((suggestion, index) => (
                                <motion.div
                                  key={index}
                                  className="bg-white border border-gray-200 rounded-lg p-3 hover:bg-gray-50 cursor-pointer"
                                  onClick={() => handleSuggestionAction(error, suggestion)}
                                  whileHover={{ scale: 1.01 }}
                                  whileTap={{ scale: 0.99 }}
                                >
                                  <div className="flex items-start space-x-2">
                                    <div className={`p-1 rounded ${
                                      suggestion.type === 'fix' ? 'bg-green-100 text-green-600' :
                                      suggestion.type === 'debug' ? 'bg-blue-100 text-blue-600' :
                                      suggestion.type === 'retry' ? 'bg-yellow-100 text-yellow-600' :
                                      'bg-gray-100 text-gray-600'
                                    }`}>
                                      <Lightbulb className="h-3 w-3" />
                                    </div>
                                    <div className="flex-1">
                                      <h6 className="text-sm font-medium text-gray-900">
                                        {suggestion.title}
                                      </h6>
                                      <p className="text-xs text-gray-600">
                                        {suggestion.description}
                                      </p>
                                    </div>
                                  </div>
                                </motion.div>
                              ))}
                            </div>
                          </div>
                        )}

                        {/* Action Buttons */}
                        <div className="flex items-center space-x-2 pt-2 border-t border-gray-200">
                          {onErrorRetry && (
                            <motion.button
                              onClick={() => onErrorRetry(error)}
                              className="flex items-center space-x-1 px-3 py-1.5 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors"
                              whileHover={{ scale: 1.02 }}
                              whileTap={{ scale: 0.98 }}
                            >
                              <RefreshCw className="h-3 w-3" />
                              <span>Retry</span>
                            </motion.button>
                          )}

                          {onErrorReport && (
                            <motion.button
                              onClick={() => onErrorReport(error)}
                              className="flex items-center space-x-1 px-3 py-1.5 bg-gray-600 text-white text-sm rounded-lg hover:bg-gray-700 transition-colors"
                              whileHover={{ scale: 1.02 }}
                              whileTap={{ scale: 0.98 }}
                            >
                              <MessageSquare className="h-3 w-3" />
                              <span>Report</span>
                            </motion.button>
                          )}

                          <motion.button
                            onClick={() => window.open('/docs/troubleshooting', '_blank')}
                            className="flex items-center space-x-1 px-3 py-1.5 bg-gray-100 text-gray-700 text-sm rounded-lg hover:bg-gray-200 transition-colors"
                            whileHover={{ scale: 1.02 }}
                            whileTap={{ scale: 0.98 }}
                          >
                            <BookOpen className="h-3 w-3" />
                            <span>Documentation</span>
                          </motion.button>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              </motion.div>
            );
          })}
        </AnimatePresence>
      </div>

      {/* Footer */}
      {filteredErrors.length > 0 && (
        <div className="px-6 py-3 bg-gray-50 border-t border-gray-200 rounded-b-xl">
          <div className="flex items-center justify-between text-sm text-gray-600">
            <span>
              Showing {filteredErrors.length} of {errors.length} issues
            </span>
            <div className="flex items-center space-x-4">
              <label className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  checked={showSuggestions}
                  onChange={(e) => setShowSuggestions(e.target.checked)}
                  className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                />
                <span>Show suggestions</span>
              </label>
              <span className="text-gray-400">•</span>
              <button
                onClick={() => setExpandedErrors(new Set())}
                className="text-blue-600 hover:text-blue-700"
              >
                Collapse all
              </button>
            </div>
          </div>
        </div>
      )}
    </motion.div>
  );
};

export default PetriNetErrorHandler;