import React, { Component } from 'react';
import { AlertTriangle, RefreshCw, Copy, Check } from 'lucide-react';
import { formatErrorForDisplay } from '../utils/errorMessages';

/**
 * Accessible Error Boundary Component with comprehensive error handling
 * WCAG 2.1 AA compliant with proper ARIA attributes and keyboard navigation
 */
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = {
      hasError: false,
      error: null,
      errorInfo: null,
      copied: false,
      showDetails: false
    };

    // Ref for focus management
    this.errorHeadingRef = React.createRef();
  }

  static getDerivedStateFromError(error) {
    return { hasError: true };
  }

  componentDidCatch(error, errorInfo) {
    this.setState({
      error,
      errorInfo
    });

    // Log error for debugging
    console.error('ErrorBoundary caught an error:', error, errorInfo);

    // Optional error reporting service integration
    if (this.props.onError) {
      this.props.onError(error, errorInfo);
    }

    // Focus the error heading for screen readers
    setTimeout(() => {
      if (this.errorHeadingRef.current) {
        this.errorHeadingRef.current.focus();
      }
    }, 100);
  }

  handleRetry = () => {
    this.setState({
      hasError: false,
      error: null,
      errorInfo: null,
      copied: false,
      showDetails: false
    });

    // Announce retry to screen readers
    this.announceToScreenReader('Retrying... Please wait.');

    // If parent provides a retry callback
    if (this.props.onRetry) {
      this.props.onRetry();
    }
  };

  handleCopyError = async () => {
    const { error, errorInfo } = this.state;
    const errorText = `Error: ${error?.message}\n\nStack Trace:\n${error?.stack}\n\nComponent Stack:\n${errorInfo?.componentStack}`;

    try {
      await navigator.clipboard.writeText(errorText);
      this.setState({ copied: true });
      this.announceToScreenReader('Error details copied to clipboard');

      // Reset copied state after 3 seconds
      setTimeout(() => {
        this.setState({ copied: false });
      }, 3000);
    } catch (clipboardError) {
      console.error('Failed to copy error to clipboard:', clipboardError);
      this.announceToScreenReader('Failed to copy error details. Please manually select and copy the text.');
    }
  };

  toggleDetails = () => {
    this.setState({ showDetails: !this.state.showDetails });
    const announcement = this.state.showDetails ? 'Error details hidden' : 'Error details shown';
    this.announceToScreenReader(announcement);
  };

  announceToScreenReader = (message) => {
    // Create a temporary element for screen reader announcements
    const announcement = document.createElement('div');
    announcement.setAttribute('aria-live', 'polite');
    announcement.setAttribute('aria-atomic', 'true');
    announcement.className = 'sr-only';
    announcement.textContent = message;

    document.body.appendChild(announcement);

    setTimeout(() => {
      document.body.removeChild(announcement);
    }, 1000);
  };

  render() {
    if (this.state.hasError) {
      const { error } = this.state;
      const errorDisplay = formatErrorForDisplay(error, true);
      const { highContrastMode = false, reducedMotion = false } = this.props;

      return (
        <div
          className={`min-h-screen flex items-center justify-center p-4 ${
            highContrastMode ? 'bg-white' : 'bg-red-50'
          }`}
          role="alert"
          aria-live="assertive"
        >
          <div className={`max-w-lg w-full rounded-lg shadow-lg p-6 ${
            highContrastMode
              ? 'bg-white border-4 border-black text-black'
              : 'bg-white border border-red-200'
          }`}>
            {/* Error Icon and Heading */}
            <div className="flex items-center mb-4">
              <AlertTriangle
                className={`h-8 w-8 mr-3 ${
                  highContrastMode ? 'text-black' : 'text-red-500'
                }`}
                aria-hidden="true"
              />
              <h1
                ref={this.errorHeadingRef}
                className={`text-xl font-semibold ${
                  highContrastMode ? 'text-black' : 'text-red-800'
                }`}
                tabIndex="-1"
              >
                {errorDisplay.title}
              </h1>
            </div>

            {/* POC Warning */}
            <div className={`mb-4 p-3 rounded border ${
              highContrastMode
                ? 'border-black bg-gray-100 text-black'
                : 'border-yellow-200 bg-yellow-50 text-yellow-800'
            }`}
                 role="note"
                 aria-label="POC Warning">
              <p className="text-sm font-medium">
                ⚠️ POC Environment - This is a proof-of-concept implementation
              </p>
              <p className="text-xs mt-1">
                Performance and features are limited for demonstration purposes.
              </p>
            </div>

            {/* Error Message */}
            <p className={`mb-4 ${
              highContrastMode ? 'text-black' : 'text-gray-700'
            }`}>
              {errorDisplay.message}
            </p>

            {/* Suggestions */}
            {errorDisplay.suggestions && errorDisplay.suggestions.length > 0 && (
              <div className="mb-6">
                <h2 className={`text-lg font-medium mb-2 ${
                  highContrastMode ? 'text-black' : 'text-gray-800'
                }`}>
                  Try these solutions:
                </h2>
                <ul className={`list-disc list-inside space-y-1 text-sm ${
                  highContrastMode ? 'text-black' : 'text-gray-600'
                }`}
                    role="list">
                  {errorDisplay.suggestions.map((suggestion, index) => (
                    <li key={index} role="listitem">{suggestion}</li>
                  ))}
                </ul>
              </div>
            )}

            {/* Action Buttons */}
            <div className="flex flex-wrap gap-3 mb-4">
              <button
                onClick={this.handleRetry}
                className={`inline-flex items-center px-4 py-2 rounded-md text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                  highContrastMode
                    ? 'bg-black text-white border-2 border-black hover:bg-gray-800 focus:ring-black'
                    : 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500'
                } ${reducedMotion ? '' : 'transform hover:scale-105'}`}
                aria-label="Retry the failed operation"
              >
                <RefreshCw className="h-4 w-4 mr-2" aria-hidden="true" />
                Retry
              </button>

              <button
                onClick={this.toggleDetails}
                className={`inline-flex items-center px-4 py-2 rounded-md text-sm font-medium border transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                  highContrastMode
                    ? 'border-black text-black hover:bg-gray-100 focus:ring-black'
                    : 'border-gray-300 text-gray-700 hover:bg-gray-50 focus:ring-gray-500'
                } ${reducedMotion ? '' : 'transform hover:scale-105'}`}
                aria-label={this.state.showDetails ? 'Hide technical details' : 'Show technical details'}
                aria-expanded={this.state.showDetails}
              >
                {this.state.showDetails ? 'Hide Details' : 'Show Details'}
              </button>

              <button
                onClick={this.handleCopyError}
                className={`inline-flex items-center px-4 py-2 rounded-md text-sm font-medium border transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 ${
                  highContrastMode
                    ? 'border-black text-black hover:bg-gray-100 focus:ring-black'
                    : 'border-gray-300 text-gray-700 hover:bg-gray-50 focus:ring-gray-500'
                } ${reducedMotion ? '' : 'transform hover:scale-105'}`}
                aria-label="Copy error details to clipboard"
              >
                {this.state.copied ? (
                  <>
                    <Check className="h-4 w-4 mr-2" aria-hidden="true" />
                    Copied!
                  </>
                ) : (
                  <>
                    <Copy className="h-4 w-4 mr-2" aria-hidden="true" />
                    Copy Error
                  </>
                )}
              </button>
            </div>

            {/* Technical Details */}
            {this.state.showDetails && (
              <details
                open
                className={`text-sm border rounded p-3 ${
                  highContrastMode
                    ? 'border-black bg-gray-100'
                    : 'border-gray-200 bg-gray-50'
                }`}
              >
                <summary className={`font-medium cursor-pointer ${
                  highContrastMode ? 'text-black' : 'text-gray-800'
                }`}>
                  Technical Information
                </summary>

                <div className="mt-3 space-y-3">
                  {error?.message && (
                    <div>
                      <strong className={highContrastMode ? 'text-black' : 'text-gray-700'}>
                        Error Message:
                      </strong>
                      <pre className={`mt-1 text-xs overflow-x-auto p-2 rounded ${
                        highContrastMode
                          ? 'bg-white border border-black text-black'
                          : 'bg-white border border-gray-300 text-gray-800'
                      }`}
                           role="text"
                           aria-label="Error message details">
                        {error.message}
                      </pre>
                    </div>
                  )}

                  {this.state.errorInfo?.componentStack && (
                    <div>
                      <strong className={highContrastMode ? 'text-black' : 'text-gray-700'}>
                        Component Stack:
                      </strong>
                      <pre className={`mt-1 text-xs overflow-x-auto p-2 rounded ${
                        highContrastMode
                          ? 'bg-white border border-black text-black'
                          : 'bg-white border border-gray-300 text-gray-800'
                      }`}
                           role="text"
                           aria-label="Component stack trace">
                        {this.state.errorInfo.componentStack}
                      </pre>
                    </div>
                  )}
                </div>
              </details>
            )}

            {/* Screen Reader Instructions */}
            <div className="sr-only" aria-live="polite">
              Use the Retry button to attempt the operation again,
              Show Details to see technical information,
              or Copy Error to copy error details for support.
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;