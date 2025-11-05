import React, { useState } from 'react';
import { AlertTriangle, X, Shield, Zap, Eye } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

/**
 * POC Warning Banner - Security Awareness Component
 * WCAG 2.1 AA compliant warning banner for demonstration environment
 */
const PocWarningBanner = ({
  className = "",
  closable = true,
  highContrastMode = false,
  reducedMotion = false
}) => {
  const [isVisible, setIsVisible] = useState(
    localStorage.getItem('poc-warning-dismissed') !== 'true'
  );

  const dismissBanner = () => {
    setIsVisible(false);
    localStorage.setItem('poc-warning-dismissed', 'true');
  };

  if (!isVisible) return null;

  return (
    <AnimatePresence>
      <motion.div
        initial={reducedMotion ? {} : { opacity: 0, y: -50 }}
        animate={reducedMotion ? {} : { opacity: 1, y: 0 }}
        exit={reducedMotion ? {} : { opacity: 0, y: -50 }}
        className={`sticky top-0 z-50 ${className}`}
        role="banner"
        aria-label="POC Environment Warning"
      >
        <div className={`px-4 py-3 ${
          highContrastMode
            ? 'bg-yellow-100 border-b-4 border-yellow-800 text-black'
            : 'bg-gradient-to-r from-yellow-400 to-orange-500 text-white'
        }`}>
          <div className="max-w-7xl mx-auto">
            <div className="flex items-center justify-between">
              {/* Warning Content */}
              <div className="flex items-center space-x-3 flex-1">
                <AlertTriangle
                  className={`h-6 w-6 flex-shrink-0 ${
                    highContrastMode ? 'text-black' : 'text-white'
                  }`}
                  aria-hidden="true"
                />

                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className={`font-semibold ${
                      highContrastMode ? 'text-black' : 'text-white'
                    }`}>
                      ⚠️ POC Environment
                    </span>
                    <span className={`text-sm ${
                      highContrastMode ? 'text-black' : 'text-white opacity-90'
                    }`}>
                      This is a proof-of-concept demonstration
                    </span>
                  </div>

                  {/* Feature highlights */}
                  <div className="flex flex-wrap items-center gap-4 mt-1 text-xs">
                    <div className="flex items-center gap-1">
                      <Zap className="h-3 w-3" aria-hidden="true" />
                      <span>Max 30 places/transitions</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Eye className="h-3 w-3" aria-hidden="true" />
                      <span>WCAG 2.1 AA accessible</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Shield className="h-3 w-3" aria-hidden="true" />
                      <span>Demo security only</span>
                    </div>
                  </div>
                </div>
              </div>

              {/* Dismiss Button */}
              {closable && (
                <button
                  onClick={dismissBanner}
                  className={`ml-4 p-1 rounded-md transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                    highContrastMode
                      ? 'hover:bg-yellow-200 focus:ring-yellow-600 text-black'
                      : 'hover:bg-white hover:bg-opacity-20 focus:ring-white text-white'
                  }`}
                  aria-label="Dismiss POC warning (this session only)"
                  title="Dismiss warning for this session"
                >
                  <X className="h-5 w-5" />
                </button>
              )}
            </div>
          </div>
        </div>

        {/* Screen reader announcement */}
        <div className="sr-only" aria-live="polite">
          Warning: You are using a proof-of-concept environment.
          This demonstration has limited performance and security features.
          Maximum network size is 30 places and 30 transitions for optimal performance.
        </div>
      </motion.div>
    </AnimatePresence>
  );
};

/**
 * Hook to check if current environment is POC
 */
export const usePocEnvironment = () => {
  const [isPoc, setIsPoc] = useState(false);
  const [pocHeaders, setPocHeaders] = useState({});

  React.useEffect(() => {
    // Check for POC headers from API responses
    const checkPocEnvironment = async () => {
      try {
        const response = await fetch('/api/health', { method: 'HEAD' });
        const headers = {};

        response.headers.forEach((value, key) => {
          if (key.toLowerCase().startsWith('x-poc')) {
            headers[key] = value;
          }
        });

        setPocHeaders(headers);
        setIsPoc(response.headers.get('x-poc-environment') === 'true');
      } catch (error) {
        // Assume POC if we can't verify (better safe than sorry)
        setIsPoc(true);
        console.warn('Could not verify POC environment status:', error);
      }
    };

    checkPocEnvironment();
  }, []);

  return { isPoc, pocHeaders };
};

/**
 * POC Info Modal for detailed information
 */
export const PocInfoModal = ({ isOpen, onClose, highContrastMode = false }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto" aria-labelledby="poc-info-title" role="dialog">
      <div className="flex items-center justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        {/* Background overlay */}
        <div
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          aria-hidden="true"
          onClick={onClose}
        ></div>

        {/* Modal panel */}
        <div className={`inline-block align-bottom rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle sm:max-w-lg sm:w-full ${
          highContrastMode ? 'bg-white border-4 border-black' : 'bg-white'
        }`}>
          <div className="px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
            <div className="sm:flex sm:items-start">
              <div className={`mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full sm:mx-0 sm:h-10 sm:w-10 ${
                highContrastMode ? 'bg-yellow-100' : 'bg-yellow-100'
              }`}>
                <AlertTriangle className="h-6 w-6 text-yellow-600" aria-hidden="true" />
              </div>
              <div className="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left">
                <h3 className={`text-lg leading-6 font-medium ${
                  highContrastMode ? 'text-black' : 'text-gray-900'
                }`} id="poc-info-title">
                  POC Environment Information
                </h3>
                <div className="mt-2">
                  <div className="text-sm text-gray-500 space-y-3">
                    <p>This is a <strong>proof-of-concept</strong> implementation of the Obvian Petri Net DAG system.</p>

                    <div>
                      <h4 className="font-semibold text-gray-700 mb-1">Performance Limitations:</h4>
                      <ul className="list-disc list-inside space-y-1">
                        <li>Maximum 30 places per network</li>
                        <li>Maximum 30 transitions per network</li>
                        <li>Optimized for demonstration purposes</li>
                      </ul>
                    </div>

                    <div>
                      <h4 className="font-semibold text-gray-700 mb-1">Accessibility Features:</h4>
                      <ul className="list-disc list-inside space-y-1">
                        <li>WCAG 2.1 AA compliant</li>
                        <li>Screen reader support</li>
                        <li>Keyboard navigation</li>
                        <li>Color-blind friendly visualizations</li>
                        <li>High contrast mode available</li>
                      </ul>
                    </div>

                    <div>
                      <h4 className="font-semibold text-gray-700 mb-1">Security:</h4>
                      <ul className="list-disc list-inside space-y-1">
                        <li>Demo environment security only</li>
                        <li>Not suitable for production data</li>
                        <li>Educational and testing purposes</li>
                      </ul>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div className={`px-4 py-3 sm:px-6 sm:flex sm:flex-row-reverse ${
            highContrastMode ? 'bg-gray-100' : 'bg-gray-50'
          }`}>
            <button
              type="button"
              onClick={onClose}
              className={`w-full inline-flex justify-center rounded-md border px-4 py-2 text-base font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 sm:ml-3 sm:w-auto sm:text-sm ${
                highContrastMode
                  ? 'border-black bg-white text-black hover:bg-gray-100 focus:ring-black'
                  : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus:ring-indigo-500'
              }`}
            >
              Got it
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default PocWarningBanner;