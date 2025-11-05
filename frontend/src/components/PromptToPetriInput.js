import React, { useState, useEffect, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Sparkles,
  Send,
  Loader,
  CheckCircle,
  AlertCircle,
  Lightbulb,
  MessageSquare,
  Zap,
  Clock,
  RefreshCw,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { debounce } from 'lodash';

const PromptToPetriInput = ({
  onParse,
  onBuild,
  initialPrompt = "",
  className = "",
  showExamples = true
}) => {
  const [prompt, setPrompt] = useState(initialPrompt);
  const [templateHint, setTemplateHint] = useState('');
  const [parsing, setParsing] = useState(false);
  const [building, setBuilding] = useState(false);
  const [parseResult, setParseResult] = useState(null);
  const [parseError, setParseError] = useState(null);
  const [confidence, setConfidence] = useState(0);
  const [showExamplesPanel, setShowExamplesPanel] = useState(false);
  const [realTimeFeedback, setRealTimeFeedback] = useState(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  // Template examples with different patterns
  const templates = [
    {
      id: 'devops',
      name: 'DevOps CI/CD',
      icon: '🚀',
      description: 'Continuous integration and deployment workflows',
      examples: [
        "Every time I push code: run tests; if pass deploy to staging; if fail alert Slack",
        "When PR is merged: build app; run security scan; if clean deploy to production",
        "On release tag: compile assets; run integration tests; deploy to staging then production"
      ]
    },
    {
      id: 'football',
      name: 'Football Training',
      icon: '⚽',
      description: 'Training drills and team activities',
      examples: [
        "warm-up, then pass and shoot in parallel, then cooldown",
        "Start with stretching; do passing drills and fitness together; finish with scrimmage",
        "Individual warm-up; team formation practice; penalty kicks; group cooldown"
      ]
    }
  ];

  // Real-time analysis (debounced)
  const analyzePrompt = useCallback(
    debounce(async (text) => {
      if (!text.trim() || text.length < 10) {
        setRealTimeFeedback(null);
        return;
      }

      setIsAnalyzing(true);

      // Simulate analysis
      await new Promise(resolve => setTimeout(resolve, 500));

      // Basic pattern matching for feedback
      const feedback = {
        patterns: [],
        suggestions: [],
        confidence: 0
      };

      // DevOps patterns
      if (text.toLowerCase().includes('test') && text.toLowerCase().includes('deploy')) {
        feedback.patterns.push('DevOps CI/CD detected');
        feedback.confidence += 0.3;
        feedback.suggestions.push('Consider adding error handling for failed deployments');
      }

      // Football patterns
      if (text.toLowerCase().includes('warm') || text.toLowerCase().includes('cool')) {
        feedback.patterns.push('Football training workflow detected');
        feedback.confidence += 0.25;
        feedback.suggestions.push('Include cooldown activities for safety');
      }

      // Conditional logic
      if (text.includes('if ') || text.includes('when ')) {
        feedback.patterns.push('Conditional branching detected');
        feedback.confidence += 0.2;
      }

      // Parallel execution
      if (text.includes(' and ') || text.includes('parallel') || text.includes('together')) {
        feedback.patterns.push('Parallel execution detected');
        feedback.confidence += 0.2;
        feedback.suggestions.push('Parallel activities will be modeled as concurrent transitions');
      }

      // Sequential flow
      if (text.includes('then ') || text.includes('; ')) {
        feedback.patterns.push('Sequential flow detected');
        feedback.confidence += 0.15;
      }

      feedback.confidence = Math.min(feedback.confidence, 1);

      if (feedback.patterns.length === 0) {
        feedback.suggestions.push('Try using keywords like "if", "then", "parallel", or "when"');
        feedback.suggestions.push('Consider starting with one of the example templates');
      }

      setRealTimeFeedback(feedback);
      setIsAnalyzing(false);
    }, 800),
    []
  );

  useEffect(() => {
    analyzePrompt(prompt);
  }, [prompt, analyzePrompt]);

  const handleParse = async () => {
    if (!prompt.trim()) {
      setParseError('Please enter a workflow description');
      return;
    }

    setParsing(true);
    setParseError(null);
    setParseResult(null);

    try {
      const parseData = {
        text: prompt,
        templateHint: templateHint || undefined
      };

      const result = await onParse(parseData);
      setParseResult(result);
      setConfidence(result.confidence || 0);
    } catch (error) {
      console.error('Parse error:', error);
      setParseError(error.response?.data?.error?.message || error.message || 'Failed to parse workflow');
    } finally {
      setParsing(false);
    }
  };

  const handleBuildAndVisualize = async () => {
    if (!parseResult?.intent) return;

    setBuilding(true);

    try {
      const buildData = {
        intent: parseResult.intent,
        generateDag: true
      };

      await onBuild(buildData);
    } catch (error) {
      console.error('Build error:', error);
      setParseError(error.response?.data?.error?.message || error.message || 'Failed to build Petri net');
    } finally {
      setBuilding(false);
    }
  };

  // Combined parse + build in one step
  const handleParseAndBuild = async () => {
    if (!prompt.trim()) {
      setParseError('Please enter a workflow description');
      return;
    }

    setParsing(true);
    setParseError(null);
    setParseResult(null);

    try {
      // Step 1: Parse
      const parseData = {
        text: prompt,
        templateHint: templateHint || undefined
      };

      const result = await onParse(parseData);
      setParseResult(result);
      setConfidence(result.confidence || 0);

      // Step 2: Build immediately
      if (result.intent) {
        setBuilding(true);

        try {
          const buildData = {
            intent: result.intent,
            generateDag: true
          };

          await onBuild(buildData);
        } catch (buildError) {
          console.error('Build error:', buildError);
          throw buildError;
        }
      }
    } catch (error) {
      console.error('Parse/Build error:', error);
      setParseError(error.response?.data?.error?.message || error.message || 'Failed to parse/build workflow');
    } finally {
      setParsing(false);
      setBuilding(false);
    }
  };

  const insertExample = (example) => {
    setPrompt(example);
    setShowExamplesPanel(false);
  };

  const getConfidenceColor = () => {
    if (confidence >= 0.8) return 'text-green-600';
    if (confidence >= 0.6) return 'text-yellow-600';
    if (confidence >= 0.4) return 'text-orange-600';
    return 'text-red-600';
  };

  const getConfidenceText = () => {
    if (confidence >= 0.8) return 'High confidence';
    if (confidence >= 0.6) return 'Medium confidence';
    if (confidence >= 0.4) return 'Low confidence';
    return 'Very low confidence';
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={`space-y-6 ${className}`}
    >
      {/* Header */}
      <div className="text-center">
        <div className="flex items-center justify-center space-x-2 mb-4">
          <Sparkles className="h-8 w-8 text-purple-600" />
          <h2 className="text-2xl font-bold text-gray-900">Natural Language to Petri Net</h2>
        </div>
        <p className="text-gray-600 max-w-2xl mx-auto">
          Describe your workflow in plain English and watch it transform into a formal Petri net representation
        </p>
      </div>

      {/* Main Input Area */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden">
        {/* Template Hint Selection */}
        <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <label className="text-sm font-medium text-gray-700">Template Hint:</label>
              <select
                value={templateHint}
                onChange={(e) => setTemplateHint(e.target.value)}
                className="text-sm border-gray-300 rounded-md focus:ring-purple-500 focus:border-purple-500"
              >
                <option value="">Auto-detect</option>
                <option value="devops">DevOps CI/CD</option>
                <option value="football">Football Training</option>
              </select>
            </div>

            {showExamples && (
              <motion.button
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => setShowExamplesPanel(!showExamplesPanel)}
                className="flex items-center space-x-2 px-3 py-1 text-sm text-purple-600 hover:text-purple-800 transition-colors"
              >
                <Lightbulb className="h-4 w-4" />
                <span>Examples</span>
                {showExamplesPanel ? (
                  <ChevronUp className="h-4 w-4" />
                ) : (
                  <ChevronDown className="h-4 w-4" />
                )}
              </motion.button>
            )}
          </div>
        </div>

        {/* Examples Panel */}
        <AnimatePresence>
          {showExamplesPanel && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="border-b border-gray-200 bg-gray-50"
            >
              <div className="p-4 space-y-4">
                {templates.map(template => (
                  <div key={template.id} className="space-y-2">
                    <div className="flex items-center space-x-2">
                      <span className="text-lg">{template.icon}</span>
                      <h4 className="font-medium text-gray-900">{template.name}</h4>
                      <span className="text-xs text-gray-500">- {template.description}</span>
                    </div>
                    <div className="grid gap-2">
                      {template.examples.map((example, idx) => (
                        <motion.button
                          key={idx}
                          whileHover={{ scale: 1.01 }}
                          whileTap={{ scale: 0.99 }}
                          onClick={() => insertExample(example)}
                          className="text-left text-sm p-3 bg-white rounded border border-gray-200 hover:border-purple-300 hover:bg-purple-50 transition-all"
                        >
                          "{example}"
                        </motion.button>
                      ))}
                    </div>
                  </div>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Text Input */}
        <div className="p-4">
          <div className="relative">
            <textarea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              placeholder="Describe your workflow in natural language...

Examples:
• Every time I push code: run tests; if pass deploy to staging; if fail alert Slack
• warm-up, then pass and shoot in parallel, then cooldown"
              className="w-full h-32 p-3 border-gray-300 rounded-lg focus:ring-purple-500 focus:border-purple-500 resize-none"
              disabled={parsing || building}
            />

            {/* Character count */}
            <div className="absolute bottom-2 right-2 text-xs text-gray-400">
              {prompt.length} characters
            </div>
          </div>

          {/* Real-time Feedback */}
          <AnimatePresence>
            {realTimeFeedback && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
                className="mt-3 p-3 bg-blue-50 border border-blue-200 rounded-lg"
              >
                <div className="flex items-start space-x-2">
                  {isAnalyzing ? (
                    <Loader className="h-4 w-4 text-blue-600 animate-spin mt-0.5" />
                  ) : (
                    <Zap className="h-4 w-4 text-blue-600 mt-0.5" />
                  )}
                  <div className="flex-1">
                    <h4 className="text-sm font-medium text-blue-900 mb-1">
                      {isAnalyzing ? 'Analyzing...' : 'Pattern Analysis'}
                    </h4>

                    {realTimeFeedback.patterns.length > 0 && (
                      <div className="mb-2">
                        <p className="text-xs text-blue-700 mb-1">Detected patterns:</p>
                        <div className="flex flex-wrap gap-1">
                          {realTimeFeedback.patterns.map((pattern, idx) => (
                            <span
                              key={idx}
                              className="inline-block px-2 py-1 text-xs bg-blue-100 text-blue-800 rounded-full"
                            >
                              {pattern}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}

                    {realTimeFeedback.suggestions.length > 0 && (
                      <div>
                        <p className="text-xs text-blue-700 mb-1">Suggestions:</p>
                        <ul className="text-xs text-blue-600 space-y-1">
                          {realTimeFeedback.suggestions.map((suggestion, idx) => (
                            <li key={idx}>• {suggestion}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </div>

                  {realTimeFeedback.confidence > 0 && (
                    <div className="text-right">
                      <div className={`text-xs font-medium ${getConfidenceColor()}`}>
                        {Math.round(realTimeFeedback.confidence * 100)}%
                      </div>
                      <div className="text-xs text-gray-500">{getConfidenceText()}</div>
                    </div>
                  )}
                </div>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Action Buttons */}
          <div className="mt-4 space-y-3">
            {/* Status Messages */}
            <div className="flex items-center space-x-3 flex-wrap min-h-[24px]">
              {parseResult && (
                <div className="flex items-center space-x-2">
                  <CheckCircle className="h-4 w-4 text-green-500" />
                  <span className="text-sm text-green-700">
                    Parsed successfully
                  </span>
                  <span className={`text-xs font-medium ${getConfidenceColor()}`}>
                    ({Math.round(confidence * 100)}% confidence)
                  </span>
                </div>
              )}

              {parseError && (
                <div className="flex items-center space-x-2">
                  <AlertCircle className="h-4 w-4 text-red-500" />
                  <span className="text-sm text-red-700 text-xs">{parseError}</span>
                </div>
              )}
            </div>

            {/* Horizontally Scrollable Button Container */}
            <div className="overflow-x-auto overflow-y-hidden -mx-4 px-4 pb-2 scrollbar-hide">
              <div className="flex gap-3 min-w-max">
                {/* One-Click Build Button */}
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleParseAndBuild}
                  disabled={parsing || building || !prompt.trim()}
                  className="flex items-center justify-center space-x-2 px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:from-purple-700 hover:to-blue-700 transition-all shadow-lg whitespace-nowrap min-h-[44px]"
                >
                  {parsing || building ? (
                    <>
                      <Loader className="h-5 w-5 animate-spin" />
                      <span>{parsing ? 'Parsing...' : 'Building...'}</span>
                    </>
                  ) : (
                    <>
                      <Sparkles className="h-5 w-5" />
                      <span>Build Petri Net</span>
                    </>
                  )}
                </motion.button>

                {/* Advanced: Separate Parse Button */}
                <motion.button
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={handleParse}
                  disabled={parsing || building || !prompt.trim()}
                  className="flex items-center justify-center space-x-2 px-5 py-3 bg-gray-600 text-white rounded-lg font-medium text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-gray-700 transition-colors whitespace-nowrap min-h-[44px]"
                  title="Parse only (advanced)"
                >
                  <MessageSquare className="h-4 w-4" />
                  <span>Parse Only</span>
                </motion.button>

                {/* Advanced: Build Button (appears after parse) */}
                {parseResult && (
                  <motion.button
                    initial={{ opacity: 0, scale: 0.8 }}
                    animate={{ opacity: 1, scale: 1 }}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={handleBuildAndVisualize}
                    disabled={building}
                    className="flex items-center justify-center space-x-2 px-5 py-3 bg-green-600 text-white rounded-lg font-medium text-sm disabled:opacity-50 disabled:cursor-not-allowed hover:bg-green-700 transition-colors whitespace-nowrap min-h-[44px]"
                  >
                    <Send className="h-4 w-4" />
                    <span>Build Only</span>
                  </motion.button>
                )}
              </div>
            </div>

            {/* Scroll Hint - Mobile Only */}
            {parseResult && (
              <div className="text-xs text-gray-500 text-center sm:hidden">
                ← Swipe to see all buttons →
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Parse Result Preview */}
      <AnimatePresence>
        {parseResult && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="bg-green-50 border border-green-200 rounded-lg p-4"
          >
            <div className="flex items-start space-x-3">
              <CheckCircle className="h-5 w-5 text-green-600 mt-0.5" />
              <div className="flex-1">
                <h4 className="text-sm font-medium text-green-900 mb-2">
                  Intent Successfully Parsed
                </h4>

                <div className="bg-white rounded-md p-3 border border-green-200">
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                    <div>
                      <span className="font-medium text-gray-700">Workflow Name:</span>
                      <span className="ml-2 text-gray-900">{parseResult.intent.name}</span>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">Model Type:</span>
                      <span className="ml-2 text-gray-900">{parseResult.intent.modelType}</span>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">Steps:</span>
                      <span className="ml-2 text-gray-900">{parseResult.intent.steps?.length || 0}</span>
                    </div>
                    <div>
                      <span className="font-medium text-gray-700">Template:</span>
                      <span className="ml-2 text-gray-900">{parseResult.templateUsed || 'Auto-detected'}</span>
                    </div>
                  </div>

                  {parseResult.intent.description && (
                    <div className="mt-3 pt-3 border-t border-gray-200">
                      <span className="font-medium text-gray-700">Description:</span>
                      <p className="text-gray-900 text-sm mt-1">{parseResult.intent.description}</p>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default PromptToPetriInput;