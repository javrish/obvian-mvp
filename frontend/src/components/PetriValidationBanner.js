import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  CheckCircle,
  AlertCircle,
  XCircle,
  Clock,
  Shield,
  Zap,
  BarChart3,
  RefreshCw,
  AlertTriangle,
  Info
} from 'lucide-react';

const PetriValidationBanner = ({
  validationResult,
  isValidating = false,
  onRetry,
  className = ""
}) => {
  if (isValidating) {
    return (
      <motion.div
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        className={`bg-blue-50 border border-blue-200 rounded-lg p-4 ${className}`}
      >
        <div className="flex items-center space-x-3">
          <RefreshCw className="h-5 w-5 text-blue-600 animate-spin" />
          <div>
            <h3 className="text-sm font-medium text-blue-900">Validating Petri Net</h3>
            <p className="text-sm text-blue-700">
              Running formal verification checks...
            </p>
          </div>
        </div>
      </motion.div>
    );
  }

  if (!validationResult) return null;

  const { report } = validationResult;
  const isPass = report?.status === 'PASS';
  const isFail = report?.status === 'FAIL';
  const isInconclusive = report?.status === 'INCONCLUSIVE';

  const getStatusIcon = () => {
    if (isPass) return <CheckCircle className="h-6 w-6 text-green-600" />;
    if (isFail) return <XCircle className="h-6 w-6 text-red-600" />;
    if (isInconclusive) return <AlertTriangle className="h-6 w-6 text-yellow-600" />;
    return <AlertCircle className="h-6 w-6 text-gray-600" />;
  };

  const getStatusColors = () => {
    if (isPass) return {
      bg: 'bg-green-50',
      border: 'border-green-200',
      text: 'text-green-900',
      subtext: 'text-green-700'
    };
    if (isFail) return {
      bg: 'bg-red-50',
      border: 'border-red-200',
      text: 'text-red-900',
      subtext: 'text-red-700'
    };
    if (isInconclusive) return {
      bg: 'bg-yellow-50',
      border: 'border-yellow-200',
      text: 'text-yellow-900',
      subtext: 'text-yellow-700'
    };
    return {
      bg: 'bg-gray-50',
      border: 'border-gray-200',
      text: 'text-gray-900',
      subtext: 'text-gray-700'
    };
  };

  const colors = getStatusColors();

  const checkDetails = [
    {
      key: 'deadlock',
      name: 'Deadlock Detection',
      icon: Shield,
      description: 'Ensures workflow cannot get stuck'
    },
    {
      key: 'reachability',
      name: 'Reachability Analysis',
      icon: Zap,
      description: 'Verifies all states are accessible'
    },
    {
      key: 'liveness',
      name: 'Liveness Check',
      icon: BarChart3,
      description: 'Confirms progress is always possible'
    },
    {
      key: 'boundedness',
      name: 'Boundedness Verification',
      icon: Clock,
      description: 'Validates resource usage limits'
    }
  ];

  const getCheckIcon = (status) => {
    switch (status) {
      case 'PASS': return <CheckCircle className="h-4 w-4 text-green-500" />;
      case 'FAIL': return <XCircle className="h-4 w-4 text-red-500" />;
      case 'INCONCLUSIVE': return <AlertTriangle className="h-4 w-4 text-yellow-500" />;
      default: return <AlertCircle className="h-4 w-4 text-gray-400" />;
    }
  };

  return (
    <AnimatePresence>
      <motion.div
        initial={{ opacity: 0, y: -20, height: 0 }}
        animate={{ opacity: 1, y: 0, height: 'auto' }}
        exit={{ opacity: 0, y: -20, height: 0 }}
        className={`${colors.bg} ${colors.border} border rounded-lg p-6 space-y-4 ${className}`}
      >
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center space-x-3">
            {getStatusIcon()}
            <div>
              <h3 className={`text-lg font-semibold ${colors.text}`}>
                {isPass && "Validation Passed"}
                {isFail && "Validation Failed"}
                {isInconclusive && "Validation Inconclusive"}
              </h3>
              <p className={`text-sm ${colors.subtext}`}>
                {report?.summaryMessage || "Formal verification completed"}
              </p>
            </div>
          </div>

          {(isFail || isInconclusive) && onRetry && (
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={onRetry}
              className="flex items-center space-x-2 px-3 py-2 bg-white border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-50 transition-colors"
            >
              <RefreshCw className="h-4 w-4" />
              <span className="text-sm">Retry</span>
            </motion.button>
          )}
        </div>

        {/* Detailed Checks */}
        {report?.checks && (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {checkDetails.map(({ key, name, icon: Icon, description }) => {
              const checkStatus = report.checks[key];
              const isPassed = checkStatus === 'PASS';

              return (
                <motion.div
                  key={key}
                  initial={{ opacity: 0, scale: 0.95 }}
                  animate={{ opacity: 1, scale: 1 }}
                  transition={{ delay: 0.1 }}
                  className={`
                    p-4 rounded-lg border transition-colors
                    ${isPassed
                      ? 'bg-white border-green-200 hover:border-green-300'
                      : 'bg-white border-gray-200 hover:border-gray-300'
                    }
                  `}
                >
                  <div className="flex items-start justify-between mb-2">
                    <Icon className={`h-5 w-5 ${isPassed ? 'text-green-600' : 'text-gray-400'}`} />
                    {getCheckIcon(checkStatus)}
                  </div>
                  <h4 className="font-medium text-gray-900 text-sm mb-1">{name}</h4>
                  <p className="text-xs text-gray-600 mb-2">{description}</p>
                  <div className={`
                    text-xs px-2 py-1 rounded-full font-medium inline-block
                    ${checkStatus === 'PASS'
                      ? 'bg-green-100 text-green-700'
                      : checkStatus === 'FAIL'
                      ? 'bg-red-100 text-red-700'
                      : checkStatus === 'INCONCLUSIVE'
                      ? 'bg-yellow-100 text-yellow-700'
                      : 'bg-gray-100 text-gray-600'
                    }
                  `}>
                    {checkStatus || 'Unknown'}
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}

        {/* Performance Metrics */}
        {report && (
          <div className="flex items-center justify-between pt-4 border-t border-gray-200">
            <div className="flex items-center space-x-6 text-sm">
              {report.statesExplored && (
                <div className="flex items-center space-x-2">
                  <BarChart3 className="h-4 w-4 text-gray-500" />
                  <span className="text-gray-600">
                    {report.statesExplored.toLocaleString()} states explored
                  </span>
                </div>
              )}
              {report.executionTimeMs && (
                <div className="flex items-center space-x-2">
                  <Clock className="h-4 w-4 text-gray-500" />
                  <span className="text-gray-600">
                    {report.executionTimeMs}ms
                  </span>
                </div>
              )}
            </div>

            {/* Status Badge */}
            <div className={`
              px-3 py-1 rounded-full text-sm font-medium
              ${isPass
                ? 'bg-green-100 text-green-800'
                : isFail
                ? 'bg-red-100 text-red-800'
                : isInconclusive
                ? 'bg-yellow-100 text-yellow-800'
                : 'bg-gray-100 text-gray-800'
              }
            `}>
              {report.status}
            </div>
          </div>
        )}

        {/* Hints and Recommendations */}
        {report?.hints && report.hints.length > 0 && (
          <div className="pt-4 border-t border-gray-200">
            <div className="flex items-start space-x-2">
              <Info className="h-4 w-4 text-blue-500 mt-0.5 flex-shrink-0" />
              <div>
                <h4 className="text-sm font-medium text-gray-900 mb-2">
                  {isPass ? 'Success Notes' : 'Recommendations'}
                </h4>
                <ul className="space-y-1">
                  {report.hints.map((hint, index) => (
                    <li key={index} className="text-sm text-gray-700">
                      • {hint}
                    </li>
                  ))}
                </ul>
              </div>
            </div>
          </div>
        )}

        {/* Error Details for Failed/Inconclusive */}
        {(isFail || isInconclusive) && validationResult.error && (
          <div className="pt-4 border-t border-gray-200">
            <div className="flex items-start space-x-2">
              <AlertCircle className="h-4 w-4 text-red-500 mt-0.5 flex-shrink-0" />
              <div>
                <h4 className="text-sm font-medium text-red-900 mb-2">
                  {isInconclusive ? 'Analysis Limitations' : 'Validation Issues'}
                </h4>
                <div className="bg-white rounded-md p-3 border border-red-200">
                  <p className="text-sm text-red-800 mb-2 font-medium">
                    {validationResult.error.message}
                  </p>
                  {validationResult.error.details?.suggestion && (
                    <p className="text-xs text-red-700">
                      <strong>Suggestion:</strong> {validationResult.error.details.suggestion}
                    </p>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </motion.div>
    </AnimatePresence>
  );
};

export default PetriValidationBanner;