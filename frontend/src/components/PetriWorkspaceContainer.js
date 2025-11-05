import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  MessageSquare,
  Network,
  CheckCircle,
  Play,
  ArrowRight,
  Settings,
  Download,
  Eye,
  EyeOff,
  Layers,
  Activity,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  Pause,
  FastForward
} from 'lucide-react';
import toast from 'react-hot-toast';

import PromptToPetriInput from './PromptToPetriInput';
import DualGraphView from './DualGraphView';
import PetriValidationBanner from './PetriValidationBanner';
import PetriTracePanel from './PetriTracePanel';
import TabbedPetriView from './TabbedPetriView';
import apiService from '../services/api';

/**
 * PetriWorkspaceContainer - Main orchestration component for P3Net UI/UX
 *
 * Implements the complete prompt-to-execution pipeline:
 * Natural Language → IntentSpec → PetriNet → Validation → DAG Projection → Simulation/Execution
 *
 * Key Features:
 * - 4-step progress visualization with step validation
 * - Adaptive dual-panel layout (30% controls, 70% visualization)
 * - Real-time cross-highlighting between Petri net and DAG views
 * - Interactive validation diagnostics with suggested fixes
 * - Professional simulation controls with token animation
 * - Patent-strategic confidence-based element mapping
 */
const PetriWorkspaceContainer = () => {
  // Core workflow state
  const [currentStep, setCurrentStep] = useState('parse'); // 'parse', 'build', 'validate', 'simulate'
  const [workflowData, setWorkflowData] = useState({
    promptText: '',
    intentSpec: null,
    petriNet: null,
    validationResult: null,
    dagProjection: null,
    simulationResult: null,
    traceEvents: []
  });

  // UI state
  const [isProcessing, setIsProcessing] = useState(false);
  const [selectedElement, setSelectedElement] = useState(null);
  const [panelLayout, setPanelLayout] = useState('split-vertical'); // 'split-vertical', 'split-horizontal', 'toggle'
  const [tracePanelOpen, setTracePanelOpen] = useState(false);
  const [simulationSpeed, setSimulationSpeed] = useState(1.0);
  const [isSimulationPlaying, setIsSimulationPlaying] = useState(false);

  // Performance and error state
  const [processingTimes, setProcessingTimes] = useState({});
  const [errorState, setErrorState] = useState(null);

  // Step definitions for progress stepper
  const workflowSteps = useMemo(() => [
    {
      id: 'parse',
      label: 'Parse Intent',
      description: 'Transform natural language into structured intent',
      icon: MessageSquare,
      status: getStepStatus('parse'),
      duration: processingTimes.parse
    },
    {
      id: 'build',
      label: 'Build PetriNet',
      description: 'Construct formal Petri net from intent specification',
      icon: Network,
      status: getStepStatus('build'),
      duration: processingTimes.build
    },
    {
      id: 'validate',
      label: 'Validate',
      description: 'Perform formal verification and deadlock detection',
      icon: CheckCircle,
      status: getStepStatus('validate'),
      duration: processingTimes.validate
    },
    {
      id: 'simulate',
      label: 'Simulate',
      description: 'Execute token simulation with real-time visualization',
      icon: Play,
      status: getStepStatus('simulate'),
      duration: processingTimes.simulate
    }
  ], [currentStep, workflowData, processingTimes]);

  function getStepStatus(stepId) {
    const stepIndex = ['parse', 'build', 'validate', 'simulate'].indexOf(stepId);
    const currentIndex = ['parse', 'build', 'validate', 'simulate'].indexOf(currentStep);

    if (stepIndex < currentIndex) return 'completed';
    if (stepIndex === currentIndex) return isProcessing ? 'processing' : 'active';
    return 'pending';
  }

  // Step progression logic
  const canProgressToStep = useCallback((stepId) => {
    const requirements = {
      build: workflowData.intentSpec !== null,
      validate: workflowData.petriNet !== null,
      simulate: workflowData.validationResult?.status === 'PASS'
    };
    return requirements[stepId] || stepId === 'parse';
  }, [workflowData]);

  // Main workflow execution functions
  const executeParseStep = async (promptText) => {
    if (!promptText.trim()) {
      toast.error('Please enter a workflow description');
      return false;
    }

    setIsProcessing(true);
    setErrorState(null);
    const startTime = Date.now();

    try {
      const parseResult = await apiService.petri.parse({
        text: promptText,
        schemaVersion: '1.0'
      });

      if (parseResult.success && parseResult.intent) {
        setWorkflowData(prev => ({
          ...prev,
          promptText,
          intentSpec: parseResult.intent
        }));
        setProcessingTimes(prev => ({
          ...prev,
          parse: Date.now() - startTime
        }));

        toast.success(`Parsed successfully with ${parseResult.confidence}% confidence`);
        return true;
      } else {
        throw new Error(parseResult.error?.message || 'Failed to parse prompt');
      }
    } catch (error) {
      console.error('Parse error:', error);
      setErrorState({
        step: 'parse',
        message: error.message,
        suggestions: ['Try using keywords like "run tests", "if pass", "deploy"', 'Use patterns like "A, then B and C in parallel, then D"']
      });
      toast.error('Failed to parse prompt');
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  const executeBuildStep = async () => {
    if (!workflowData.intentSpec) return false;

    setIsProcessing(true);
    const startTime = Date.now();

    try {
      const buildResult = await apiService.petri.build({
        intent: workflowData.intentSpec,
        schemaVersion: '1.0'
      });

      if (buildResult.success && buildResult.petriNet) {
        setWorkflowData(prev => ({
          ...prev,
          petriNet: buildResult.petriNet
        }));
        setProcessingTimes(prev => ({
          ...prev,
          build: Date.now() - startTime
        }));

        toast.success('Petri net constructed successfully');
        return true;
      } else {
        throw new Error(buildResult.error?.message || 'Failed to build Petri net');
      }
    } catch (error) {
      console.error('Build error:', error);
      setErrorState({
        step: 'build',
        message: error.message,
        suggestions: ['Check for unmatched parallel branches', 'Ensure proper sequence flow']
      });
      toast.error('Failed to build Petri net');
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  const executeValidateStep = async () => {
    if (!workflowData.petriNet) return false;

    setIsProcessing(true);
    const startTime = Date.now();

    try {
      const validationResult = await apiService.petri.validate({
        petriNet: workflowData.petriNet,
        config: {
          kBound: 200,
          maxMillis: 5000,
          enableDeadlockCheck: true,
          enableReachabilityCheck: true
        },
        schemaVersion: '1.0'
      });

      if (validationResult.success) {
        setWorkflowData(prev => ({
          ...prev,
          validationResult: validationResult.validationResult
        }));
        setProcessingTimes(prev => ({
          ...prev,
          validate: Date.now() - startTime
        }));

        if (validationResult.validationResult?.petriStatus === 'PASS') {
          toast.success('Validation passed - workflow is safe for execution');
        } else {
          toast.error('Validation issues detected - check diagnostics');
        }
        return true;
      } else {
        throw new Error(validationResult.error?.message || 'Validation failed');
      }
    } catch (error) {
      console.error('Validation error:', error);
      setErrorState({
        step: 'validate',
        message: error.message,
        suggestions: ['Review workflow for potential deadlocks', 'Check reachability of all end states']
      });
      toast.error('Validation failed');
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  const executeSimulateStep = async () => {
    if (!workflowData.petriNet || workflowData.validationResult?.status !== 'PASS') return false;

    setIsProcessing(true);
    const startTime = Date.now();

    try {
      const simulationResult = await apiService.petri.simulate({
        petriNet: workflowData.petriNet,
        config: {
          seed: 42,
          mode: 'DETERMINISTIC',
          maxSteps: 100,
          enableTrace: true
        },
        schemaVersion: '1.0'
      });

      if (simulationResult.success) {
        setWorkflowData(prev => ({
          ...prev,
          simulationResult: simulationResult.result,
          traceEvents: simulationResult.result.trace || []
        }));
        setProcessingTimes(prev => ({
          ...prev,
          simulate: Date.now() - startTime
        }));

        toast.success('Simulation completed successfully');
        setTracePanelOpen(true); // Auto-open trace panel to show results
        return true;
      } else {
        throw new Error(simulationResult.error?.message || 'Simulation failed');
      }
    } catch (error) {
      console.error('Simulation error:', error);
      setErrorState({
        step: 'simulate',
        message: error.message,
        suggestions: ['Verify Petri net structure', 'Check initial marking configuration']
      });
      toast.error('Simulation failed');
      return false;
    } finally {
      setIsProcessing(false);
    }
  };

  // Step navigation handlers
  const handleStepClick = (stepId) => {
    if (canProgressToStep(stepId)) {
      setCurrentStep(stepId);
      setErrorState(null);
    }
  };

  const handleNextStep = async () => {
    const stepExecutors = {
      parse: () => executeParseStep(workflowData.promptText),
      build: executeBuildStep,
      validate: executeValidateStep,
      simulate: executeSimulateStep
    };

    const success = await stepExecutors[currentStep]?.();

    if (success) {
      const nextSteps = { parse: 'build', build: 'validate', validate: 'simulate' };
      const nextStep = nextSteps[currentStep];
      if (nextStep) {
        setCurrentStep(nextStep);
      }
    }
  };

  // Layout and panel management
  const togglePanelLayout = () => {
    const layouts = ['split-vertical', 'split-horizontal', 'toggle'];
    const currentIndex = layouts.indexOf(panelLayout);
    const nextIndex = (currentIndex + 1) % layouts.length;
    setPanelLayout(layouts[nextIndex]);
  };

  // Generate DAG projection when Petri net is available
  useEffect(() => {
    if (workflowData.petriNet && !workflowData.dagProjection) {
      const generateDAGProjection = async () => {
        try {
          const dagResult = await apiService.petri.dag({
            petriNet: workflowData.petriNet,
            schemaVersion: '1.0'
          });

          if (dagResult.success && dagResult.dag) {
            setWorkflowData(prev => ({
              ...prev,
              dagProjection: dagResult.dag
            }));
          }
        } catch (error) {
          console.error('DAG projection error:', error);
        }
      };

      generateDAGProjection();
    }
  }, [workflowData.petriNet]);

  return (
    <div className="petri-workspace-container h-screen flex flex-col bg-gray-50">
      {/* Header with Progress Stepper */}
      <div className="petri-workspace-header bg-white border-b border-gray-200 px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-6">
            <h1 className="text-2xl font-bold text-gray-900">P3Net Workspace</h1>

            {/* Progress Stepper */}
            <div className="flex items-center space-x-1">
              {workflowSteps.map((step, index) => (
                <React.Fragment key={step.id}>
                  <motion.button
                    onClick={() => handleStepClick(step.id)}
                    disabled={!canProgressToStep(step.id)}
                    className={`
                      flex items-center space-x-2 px-4 py-2 rounded-lg transition-all duration-200
                      ${step.status === 'completed' ? 'bg-green-100 text-green-800' :
                        step.status === 'active' ? 'bg-blue-100 text-blue-800' :
                        step.status === 'processing' ? 'bg-yellow-100 text-yellow-800' :
                        'bg-gray-100 text-gray-500'}
                      ${canProgressToStep(step.id) ? 'hover:scale-105 cursor-pointer' : 'cursor-not-allowed'}
                    `}
                    whileHover={canProgressToStep(step.id) ? { scale: 1.02 } : {}}
                    whileTap={canProgressToStep(step.id) ? { scale: 0.98 } : {}}
                  >
                    <step.icon size={16} />
                    <span className="text-sm font-medium">{step.label}</span>
                    {step.duration && (
                      <span className="text-xs opacity-70">({step.duration}ms)</span>
                    )}
                  </motion.button>

                  {index < workflowSteps.length - 1 && (
                    <ArrowRight size={16} className="text-gray-400" />
                  )}
                </React.Fragment>
              ))}
            </div>
          </div>

          {/* Layout Controls */}
          <div className="flex items-center space-x-2">
            <button
              onClick={togglePanelLayout}
              className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
              title="Toggle layout"
            >
              <Layers size={20} />
            </button>

            <button
              onClick={() => setTracePanelOpen(!tracePanelOpen)}
              className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
              title="Toggle trace panel"
            >
              {tracePanelOpen ? <EyeOff size={20} /> : <Eye size={20} />}
            </button>
          </div>
        </div>
      </div>

      {/* Main Content Area */}
      <div className="petri-workspace-content flex-1 flex overflow-hidden">
        {/* Left Panel - Controls and Input */}
        <div className="petri-workspace-left-panel w-1/3 bg-white border-r border-gray-200 flex flex-col">
          {/* Current Step Content */}
          <div className="flex-1 p-6 overflow-y-auto">
            <AnimatePresence mode="wait">
              {currentStep === 'parse' && (
                <motion.div
                  key="parse-panel"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.2 }}
                >
                  <PromptToPetriInput
                    value={workflowData.promptText}
                    onChange={(text) => setWorkflowData(prev => ({ ...prev, promptText: text }))}
                    onParse={(text) => executeParseStep(text)}
                    isProcessing={isProcessing}
                    error={errorState?.step === 'parse' ? errorState : null}
                  />
                </motion.div>
              )}

              {currentStep === 'build' && workflowData.intentSpec && (
                <motion.div
                  key="build-panel"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.2 }}
                >
                  <div>
                    <h3 className="text-lg font-semibold mb-4">Intent Specification</h3>
                    <pre className="bg-gray-100 p-4 rounded-lg text-sm overflow-auto max-h-96">
                      {JSON.stringify(workflowData.intentSpec, null, 2)}
                    </pre>
                  </div>
                </motion.div>
              )}

              {currentStep === 'validate' && workflowData.petriNet && (
                <motion.div
                  key="validate-panel"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.2 }}
                >
                  <PetriValidationBanner
                    validationResult={workflowData.validationResult}
                    isValidating={isProcessing}
                    onRevalidate={executeValidateStep}
                  />
                </motion.div>
              )}

              {currentStep === 'simulate' && workflowData.simulationResult && (
                <motion.div
                  key="simulate-panel"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  transition={{ duration: 0.2 }}
                >
                  <div>
                    <h3 className="text-lg font-semibold mb-4">Simulation Controls</h3>

                    {/* Simulation Playback Controls */}
                    <div className="space-y-4">
                      <div className="flex items-center space-x-2">
                        <button
                          onClick={() => setIsSimulationPlaying(!isSimulationPlaying)}
                          className="p-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
                        >
                          {isSimulationPlaying ? <Pause size={16} /> : <Play size={16} />}
                        </button>

                        <button
                          onClick={() => executeSimulateStep()}
                          className="p-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors"
                        >
                          <RotateCcw size={16} />
                        </button>

                        <button className="p-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition-colors">
                          <FastForward size={16} />
                        </button>
                      </div>

                      {/* Speed Control */}
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Speed: {simulationSpeed}x
                        </label>
                        <input
                          type="range"
                          min="0.1"
                          max="3.0"
                          step="0.1"
                          value={simulationSpeed}
                          onChange={(e) => setSimulationSpeed(parseFloat(e.target.value))}
                          className="w-full"
                        />
                      </div>

                      {/* Simulation Stats */}
                      <div className="bg-gray-100 p-4 rounded-lg">
                        <h4 className="font-medium mb-2">Simulation Results</h4>
                        <div className="text-sm space-y-1">
                          <div>Steps: {workflowData.traceEvents.length}</div>
                          <div>Final Marking: {workflowData.simulationResult?.finalMarking ?
                            Object.keys(workflowData.simulationResult.finalMarking).length + ' places' : 'N/A'}</div>
                          <div>Status: {workflowData.simulationResult?.status || 'Unknown'}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* Action Button */}
          <div className="p-6 border-t border-gray-200">
            <button
              onClick={handleNextStep}
              disabled={isProcessing || !canProgressToStep(currentStep)}
              className={`
                w-full py-3 px-4 rounded-lg font-medium transition-all duration-200
                ${isProcessing || !canProgressToStep(currentStep)
                  ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                  : 'bg-blue-600 text-white hover:bg-blue-700 hover:scale-105'
                }
              `}
            >
              {isProcessing ? (
                <div className="flex items-center justify-center space-x-2">
                  <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                  <span>Processing...</span>
                </div>
              ) : (
                `Execute ${workflowSteps.find(s => s.id === currentStep)?.label}`
              )}
            </button>
          </div>
        </div>

        {/* Right Panel - Visualization */}
        <div className="petri-workspace-right-panel flex-1 flex flex-col">
          {workflowData.petriNet && workflowData.dagProjection ? (
            <TabbedPetriView
              petriNet={workflowData.petriNet}
              dag={workflowData.dagProjection}
              validationResult={workflowData.validationResult}
              simulationResult={workflowData.simulationResult}
              selectedElement={selectedElement}
              onElementSelect={setSelectedElement}
              layout={panelLayout}
              simulationSpeed={simulationSpeed}
              isSimulationPlaying={isSimulationPlaying}
            />
          ) : (
            <div className="flex-1 flex items-center justify-center bg-gray-50">
              <div className="text-center">
                <Network size={64} className="mx-auto text-gray-400 mb-4" />
                <h3 className="text-lg font-medium text-gray-600 mb-2">
                  Waiting for Workflow Data
                </h3>
                <p className="text-gray-500">
                  Complete the parse and build steps to see the visualization
                </p>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Bottom Panel - Trace (Collapsible) */}
      <AnimatePresence>
        {tracePanelOpen && workflowData.traceEvents.length > 0 && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
            className="border-t border-gray-200 bg-white"
          >
            <PetriTracePanel
              traceEvents={workflowData.traceEvents}
              simulationResult={workflowData.simulationResult}
              onClose={() => setTracePanelOpen(false)}
            />
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default PetriWorkspaceContainer;