import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Network,
  Play,
  Pause,
  RotateCcw,
  Download,
  Settings,
  Eye,
  FileText,
  CheckCircle,
  AlertCircle,
  Loader,
  Split,
  Menu,
  X,
  ChevronLeft,
  ChevronRight,
  Maximize2,
  Minimize2
} from 'lucide-react';
import toast from 'react-hot-toast';

import PromptToPetriInput from './PromptToPetriInput';
import DualGraphView from './DualGraphView';
import PetriValidationBanner from './PetriValidationBanner';
import PetriProgressStepper from './PetriProgressStepper';
import ResponsiveLayout, { ResponsivePanel, ResponsiveGrid } from './ResponsiveLayout';
import { useResponsiveLayout } from '../hooks/useResponsive';
import { useSwipe } from '../hooks/useTouch';
import apiService from '../services/api';

const PetriNetWorkflow = () => {
  const responsive = useResponsiveLayout();

  const [currentStep, setCurrentStep] = useState('parse'); // 'parse', 'build', 'validate', 'simulate'
  const [petriNet, setPetriNet] = useState(null);
  const [dag, setDag] = useState(null);
  const [validationResult, setValidationResult] = useState(null);
  const [simulationResult, setSimulationResult] = useState(null);
  const [isValidating, setIsValidating] = useState(false);
  const [isSimulating, setIsSimulating] = useState(false);
  const [selectedElement, setSelectedElement] = useState(null);
  const [workflowHistory, setWorkflowHistory] = useState([]);
  const [processingTimes, setProcessingTimes] = useState({});
  const [stepStatuses, setStepStatuses] = useState({});

  // Responsive UI state
  const [mobilePanel, setMobilePanel] = useState('input'); // 'input', 'visualization', 'results'
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [compactMode, setCompactMode] = useState(responsive.isMobile);

  // Update compact mode based on breakpoint
  useEffect(() => {
    setCompactMode(responsive.isMobile);
    if (responsive.isMobile) {
      setSidebarOpen(false);
    }
  }, [responsive.isMobile]);

  // Swipe navigation for mobile panels
  const mobilePanels = ['input', 'visualization', 'results'];
  const currentPanelIndex = mobilePanels.indexOf(mobilePanel);

  const swipeHandlers = useSwipe({
    onSwipeLeft: () => {
      if (responsive.isMobile && currentPanelIndex < mobilePanels.length - 1) {
        setMobilePanel(mobilePanels[currentPanelIndex + 1]);
      }
    },
    onSwipeRight: () => {
      if (responsive.isMobile && currentPanelIndex > 0) {
        setMobilePanel(mobilePanels[currentPanelIndex - 1]);
      }
    }
  });

  // Load saved workflows on mount
  useEffect(() => {
    loadWorkflowHistory();
  }, []);

  const loadWorkflowHistory = () => {
    const saved = localStorage.getItem('petriNetWorkflows');
    if (saved) {
      setWorkflowHistory(JSON.parse(saved));
    }
  };

  const saveWorkflowToHistory = (workflow) => {
    const newWorkflow = {
      id: Date.now().toString(),
      name: workflow.name || 'Untitled Workflow',
      description: workflow.description || '',
      petriNet: workflow.petriNet,
      dag: workflow.dag,
      timestamp: new Date().toISOString()
    };

    const updated = [newWorkflow, ...workflowHistory].slice(0, 10); // Keep last 10
    setWorkflowHistory(updated);
    localStorage.setItem('petriNetWorkflows', JSON.stringify(updated));
  };

  const handleParsePrompt = async (parseData) => {
    const startTime = Date.now();
    setStepStatuses(prev => ({ ...prev, parse: 'processing' }));

    try {
      const result = await apiService.petri.parse(parseData);
      const duration = Date.now() - startTime;
      setProcessingTimes(prev => ({ ...prev, parse: duration }));
      setStepStatuses(prev => ({ ...prev, parse: 'completed' }));
      return result;
    } catch (error) {
      console.error('Parse error:', error);
      setStepStatuses(prev => ({ ...prev, parse: 'error' }));
      throw error;
    }
  };

  const handleBuildPetriNet = async (buildData) => {
    const startTime = Date.now();
    setStepStatuses(prev => ({ ...prev, build: 'processing' }));

    try {
      // Check if buildData already contains petriNet (from mock generation)
      let buildResult;

      if (buildData.petriNet) {
        // Mock data already generated, use it directly
        console.log('Using mock Petri net data from frontend generation');
        buildResult = buildData;
      } else {
        // Call backend build endpoint
        buildResult = await apiService.petri.build(buildData);
      }

      if (buildResult.petriNet) {
        setPetriNet(buildResult.petriNet);

        // Handle DAG generation
        let dagResult;
        if (buildResult.dag) {
          // DAG already provided (from mock)
          dagResult = { dag: buildResult.dag };
        } else {
          // Try to generate DAG from backend
          try {
            dagResult = await apiService.petri.dag({
              petriNet: buildResult.petriNet,
              includeMetadata: true
            });
          } catch (dagError) {
            // If DAG endpoint also returns 404, skip it
            console.warn('DAG endpoint not available, skipping DAG generation');
            dagResult = { dag: null };
          }
        }

        if (dagResult.dag) {
          setDag(dagResult.dag);
        }

        // Save to history
        saveWorkflowToHistory({
          name: buildResult.petriNet.name,
          description: buildResult.petriNet.description,
          petriNet: buildResult.petriNet,
          dag: dagResult.dag
        });

        const duration = Date.now() - startTime;
        setProcessingTimes(prev => ({ ...prev, build: duration }));
        setStepStatuses(prev => ({ ...prev, build: 'completed' }));
        setCurrentStep('build');
        toast.success('Petri net built successfully!');
      }
    } catch (error) {
      console.error('Build error:', error);
      setStepStatuses(prev => ({ ...prev, build: 'error' }));
      toast.error('Failed to build Petri net: ' + (error.response?.data?.error?.message || error.message));
      throw error;
    }
  };

  const handleValidatePetriNet = async () => {
    if (!petriNet) return;

    const startTime = Date.now();
    setIsValidating(true);
    setValidationResult(null);
    setStepStatuses(prev => ({ ...prev, validate: 'processing' }));

    try {
      const result = await apiService.petri.validate({
        petriNet: petriNet,
        config: {
          kBound: 200,
          maxMillis: 30000,
          enableDeadlockCheck: true,
          enableReachabilityCheck: true,
          enableLivenessCheck: true,
          enableBoundednessCheck: true
        }
      });

      setValidationResult(result);
      const duration = Date.now() - startTime;
      setProcessingTimes(prev => ({ ...prev, validate: duration }));
      setCurrentStep('validate');

      if (result.validationResult?.petriStatus === 'PASS') {
        setStepStatuses(prev => ({ ...prev, validate: 'completed' }));
        toast.success('Petri net validation passed!');
      } else if (result.validationResult?.petriStatus === 'FAIL') {
        setStepStatuses(prev => ({ ...prev, validate: 'error' }));
        toast.error('Petri net validation failed');
      } else {
        setStepStatuses(prev => ({ ...prev, validate: 'completed' }));
        toast.error('Petri net validation inconclusive');
      }
    } catch (error) {
      console.error('Validation error:', error);
      const errorMessage = error.response?.data?.error?.message || error.message;
      setValidationResult({
        error: {
          code: 'VALIDATION_ERROR',
          message: errorMessage
        }
      });
      setStepStatuses(prev => ({ ...prev, validate: 'error' }));
      toast.error('Validation failed: ' + errorMessage);
    } finally {
      setIsValidating(false);
    }
  };

  const handleSimulatePetriNet = async () => {
    if (!petriNet) return;

    const startTime = Date.now();
    setIsSimulating(true);
    setStepStatuses(prev => ({ ...prev, simulate: 'processing' }));

    try {
      const result = await apiService.petri.simulate({
        petriNet: petriNet,
        config: {
          seed: 42,
          mode: "DETERMINISTIC",
          maxSteps: 100,
          stepDelayMs: 100,
          enableTrace: true
        }
      });

      setSimulationResult(result);
      const duration = Date.now() - startTime;
      setProcessingTimes(prev => ({ ...prev, simulate: duration }));
      setStepStatuses(prev => ({ ...prev, simulate: 'completed' }));
      setCurrentStep('simulate');
      toast.success('Simulation completed successfully!');
    } catch (error) {
      console.error('Simulation error:', error);
      setStepStatuses(prev => ({ ...prev, simulate: 'error' }));
      toast.error('Simulation failed: ' + (error.response?.data?.error?.message || error.message));
    } finally {
      setIsSimulating(false);
    }
  };

  const handleElementSelect = (selection) => {
    setSelectedElement(selection);
    console.log('Element selected:', selection);
  };

  const handleReset = () => {
    setPetriNet(null);
    setDag(null);
    setValidationResult(null);
    setSimulationResult(null);
    setSelectedElement(null);
    setCurrentStep('parse');
    setProcessingTimes({});
    setStepStatuses({});
  };

  const handleDownloadWorkflow = () => {
    if (!petriNet) return;

    const workflow = {
      petriNet,
      dag,
      validationResult,
      simulationResult,
      timestamp: new Date().toISOString()
    };

    const blob = new Blob([JSON.stringify(workflow, null, 2)], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `petri-workflow-${petriNet.name || 'untitled'}.json`;
    link.click();
    URL.revokeObjectURL(url);

    toast.success('Workflow downloaded successfully!');
  };

  const getCurrentStepStatus = (step) => {
    // Return explicit status if set, otherwise derive from workflow state
    if (stepStatuses[step]) {
      return stepStatuses[step];
    }

    switch (step) {
      case 'parse':
        return petriNet ? 'completed' : currentStep === 'parse' ? 'active' : 'pending';
      case 'build':
        return petriNet && dag ? 'completed' : currentStep === 'build' ? 'active' : 'pending';
      case 'validate':
        return validationResult ? 'completed' : currentStep === 'validate' ? 'active' : 'pending';
      case 'simulate':
        return simulationResult ? 'completed' : currentStep === 'simulate' ? 'active' : 'pending';
      default:
        return 'pending';
    }
  };

  // Build step statuses for PetriProgressStepper
  const getStepStatusesForStepper = () => {
    return {
      parse: getCurrentStepStatus('parse'),
      build: getCurrentStepStatus('build'),
      validate: getCurrentStepStatus('validate'),
      simulate: getCurrentStepStatus('simulate')
    };
  };

  const handleStepClick = (stepId) => {
    // Allow navigation to previous steps or current step
    const stepOrder = ['parse', 'build', 'validate', 'simulate'];
    const clickedIndex = stepOrder.indexOf(stepId);
    const currentIndex = stepOrder.indexOf(currentStep);

    if (clickedIndex <= currentIndex || getCurrentStepStatus(stepId) === 'completed') {
      setCurrentStep(stepId);
    }
  };

  // Responsive header component
  const ResponsiveHeader = () => (
    <div className={`text-center ${responsive.isMobile ? 'px-4 py-6' : 'px-8 py-12'}`}>
      <motion.div
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ type: "spring", stiffness: 200 }}
        className={`inline-flex items-center justify-center bg-purple-100 rounded-full mb-4 ${
          responsive.isMobile ? 'w-12 h-12' : responsive.isTablet ? 'w-14 h-14' : 'w-16 h-16'
        }`}
      >
        <Network className={`text-purple-600 ${
          responsive.isMobile ? 'h-6 w-6' : responsive.isTablet ? 'h-7 w-7' : 'h-8 w-8'
        }`} />
      </motion.div>
      <h1 className={`font-bold text-gray-900 mb-4 ${
        responsive.isMobile ? 'text-2xl' : responsive.isTablet ? 'text-3xl' : 'text-4xl'
      }`}>
        {responsive.isMobile ? 'Petri Net Designer' : 'Petri Net Workflow Designer'}
      </h1>
      {!responsive.isMobile && (
        <p className={`text-gray-600 mx-auto ${
          responsive.isTablet ? 'text-lg max-w-2xl' : 'text-xl max-w-3xl'
        }`}>
          Transform natural language descriptions into formal Petri net workflows with interactive visualization and validation
        </p>
      )}
    </div>
  );

  // Mobile panel indicator
  const MobilePanelIndicator = () => (
    <div className="flex justify-center space-x-2 py-3 bg-white border-b border-gray-200">
      {mobilePanels.map((panel, index) => {
        const isActive = panel === mobilePanel;
        const panelTitles = { input: 'Input', visualization: 'View', results: 'Results' };

        return (
          <button
            key={panel}
            onClick={() => setMobilePanel(panel)}
            className={`px-3 py-1 rounded-full text-xs font-medium transition-all duration-200 touch-manipulation ${
              isActive ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {panelTitles[panel]}
          </button>
        );
      })}
    </div>
  );

  // Sidebar content for mobile
  const SidebarContent = () => (
    <div className="h-full flex flex-col bg-white">
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h2 className="font-semibold text-gray-900">Menu</h2>
          <button
            onClick={() => setSidebarOpen(false)}
            className="p-2 rounded-lg hover:bg-gray-100 transition-colors touch-manipulation min-h-[44px] min-w-[44px]"
          >
            <X size={20} />
          </button>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        <div>
          <h3 className="text-sm font-semibold text-gray-900 mb-3">Quick Actions</h3>
          <div className="space-y-2">
            <button
              onClick={() => {
                handleReset();
                setSidebarOpen(false);
              }}
              className="w-full py-2 px-4 rounded-lg font-medium transition-colors text-sm border border-gray-300 text-gray-600 hover:bg-gray-50 text-left"
            >
              Reset Workflow
            </button>

            {petriNet && (
              <button
                onClick={() => {
                  handleDownloadWorkflow();
                  setSidebarOpen(false);
                }}
                className="w-full py-2 px-4 rounded-lg font-medium transition-colors text-sm border border-gray-300 text-gray-600 hover:bg-gray-50 text-left flex items-center space-x-2"
              >
                <Download size={16} />
                <span>Download</span>
              </button>
            )}
          </div>
        </div>

        {workflowHistory.length > 0 && (
          <div>
            <h3 className="text-sm font-semibold text-gray-900 mb-3">Recent Workflows</h3>
            <div className="space-y-2">
              {workflowHistory.slice(0, 3).map((workflow) => (
                <button
                  key={workflow.id}
                  onClick={() => {
                    setPetriNet(workflow.petriNet);
                    setDag(workflow.dag);
                    setCurrentStep('build');
                    setSidebarOpen(false);
                  }}
                  className="w-full text-left p-3 bg-gray-50 hover:bg-gray-100 rounded-lg border border-gray-200 transition-colors"
                >
                  <div className="font-medium text-gray-900 text-sm truncate">
                    {workflow.name}
                  </div>
                  <div className="text-xs text-gray-600 mt-1">
                    {new Date(workflow.timestamp).toLocaleDateString()}
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );

  return (
    <ResponsiveLayout
      className={`min-h-screen bg-gray-50 ${isFullscreen ? 'fixed inset-0 z-50 bg-white' : ''}`}
      header={
        <div className="flex items-center justify-between w-full">
          <div className="flex items-center space-x-3">
            {responsive.isMobile && (
              <button
                onClick={() => setSidebarOpen(true)}
                className="p-2 rounded-lg hover:bg-gray-100 transition-colors touch-manipulation min-h-[44px] min-w-[44px]"
                aria-label="Open menu"
              >
                <Menu size={20} />
              </button>
            )}
            <h1 className={`font-bold text-gray-900 ${
              responsive.isMobile ? 'text-lg' : 'text-xl'
            }`}>
              P3Net Designer
            </h1>
          </div>

          <div className="flex items-center space-x-2">
            {!responsive.isMobile && (
              <button
                onClick={() => setCompactMode(!compactMode)}
                className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
                title={compactMode ? "Expand view" : "Compact view"}
              >
                {compactMode ? <Maximize2 size={16} /> : <Minimize2 size={16} />}
              </button>
            )}

            <button
              onClick={() => setIsFullscreen(!isFullscreen)}
              className="p-2 rounded-lg hover:bg-gray-100 transition-colors"
              title={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
            >
              {isFullscreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
            </button>
          </div>
        </div>
      }
      sidebar={responsive.isMobile ? <SidebarContent /> : null}
      enableSwipeGestures={responsive.isMobile}
    >
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className={responsive.getContainerClasses()}
      >
        {/* Header */}
        <ResponsiveHeader />

        {/* Mobile Panel Indicator */}
        {responsive.isMobile && <MobilePanelIndicator />}

        {/* Progress Steps with PetriProgressStepper */}
        {!responsive.isMobile && (
          <ResponsivePanel className="bg-white">
            <PetriProgressStepper
              currentStep={currentStep}
              stepStatuses={getStepStatusesForStepper()}
              processingTimes={processingTimes}
              onStepClick={handleStepClick}
              showTimings={responsive.isDesktop}
              orientation="auto"
              size="auto"
            />
          </ResponsivePanel>
        )}

        {/* Main Content Area with Responsive Layout */}
        <div
          className={`flex gap-6 min-h-96 ${
            responsive.isMobile ? 'flex-col' :
            responsive.shouldStackPanels() ? 'flex-col' :
            'flex-row'
          }`}
          {...(responsive.isMobile ? swipeHandlers : {})}
        >
          {/* Input/Controls Panel */}
          {(!responsive.isMobile || mobilePanel === 'input') && (
            <ResponsivePanel
              className={`bg-white ${
                responsive.isMobile ? 'w-full h-full' :
                responsive.shouldStackPanels() ? 'w-full' :
                'w-full lg:w-1/3'
              }`}
              title={responsive.isMobile ? "Workflow Input" : undefined}
            >
              <AnimatePresence mode="wait">
                {currentStep === 'parse' && (
                  <motion.div
                    key="parse"
                    initial={{ opacity: 0, x: responsive.isMobile ? 100 : -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: responsive.isMobile ? -100 : -20 }}
                    className={responsive.isMobile ? 'p-4' : 'p-6'}
                  >
                    <div className="mb-4">
                      <h2 className={`font-semibold text-gray-900 mb-2 ${
                        responsive.isMobile ? 'text-base' : 'text-lg'
                      }`}>Natural Language Input</h2>
                      <p className="text-sm text-gray-600">Describe your workflow in natural language</p>
                    </div>
                    <PromptToPetriInput
                      onParse={handleParsePrompt}
                      onBuild={handleBuildPetriNet}
                      showExamples={!responsive.isMobile}
                    />
                  </motion.div>
                )}

                {currentStep === 'build' && petriNet && (
                  <motion.div
                    key="build"
                    initial={{ opacity: 0, x: responsive.isMobile ? 100 : -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: responsive.isMobile ? -100 : -20 }}
                    className={responsive.isMobile ? 'p-4' : 'p-6'}
                  >
                    <div className="mb-4">
                      <h2 className={`font-semibold text-gray-900 mb-2 ${
                        responsive.isMobile ? 'text-base' : 'text-lg'
                      }`}>Petri Net Structure</h2>
                      <p className="text-sm text-gray-600">Generated formal workflow model</p>
                    </div>

                    <div className="space-y-4">
                      {/* Workflow Info */}
                      <div className="bg-gray-50 rounded-lg p-4">
                        <h3 className="font-semibold text-gray-900 mb-2">{petriNet.name}</h3>
                        {petriNet.description && (
                          <p className="text-gray-600 text-sm mb-3">{petriNet.description}</p>
                        )}
                        <ResponsiveGrid
                          columns={{ mobile: 3, tablet: 3, desktop: 3 }}
                          gap={{ mobile: 2, tablet: 3, desktop: 4 }}
                          className="text-center"
                        >
                          <div className="bg-white rounded p-2">
                            <div className={`font-bold text-purple-600 ${
                              responsive.isMobile ? 'text-base' : 'text-lg'
                            }`}>{petriNet.places?.length || 0}</div>
                            <div className="text-xs text-gray-500">Places</div>
                          </div>
                          <div className="bg-white rounded p-2">
                            <div className={`font-bold text-blue-600 ${
                              responsive.isMobile ? 'text-base' : 'text-lg'
                            }`}>{petriNet.transitions?.length || 0}</div>
                            <div className="text-xs text-gray-500">Transitions</div>
                          </div>
                          <div className="bg-white rounded p-2">
                            <div className={`font-bold text-green-600 ${
                              responsive.isMobile ? 'text-base' : 'text-lg'
                            }`}>{petriNet.arcs?.length || 0}</div>
                            <div className="text-xs text-gray-500">Arcs</div>
                          </div>
                        </ResponsiveGrid>
                      </div>

                      {/* Action Buttons */}
                      <div className={`space-y-3 ${
                        responsive.isMobile ? 'space-y-4' : ''
                      }`}>
                        <motion.button
                          whileHover={{ scale: responsive.isMobile ? 1.01 : 1.02 }}
                          whileTap={{ scale: 0.98 }}
                          onClick={handleValidatePetriNet}
                          disabled={isValidating}
                          className={`w-full flex items-center justify-center space-x-2 px-4 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors disabled:opacity-50 ${
                            responsive.isMobile ? 'py-4 touch-manipulation' : 'py-3'
                          }`}
                        >
                          {isValidating ? (
                            <>
                              <Loader className="h-4 w-4 animate-spin" />
                              <span>Validating...</span>
                            </>
                          ) : (
                            <>
                              <CheckCircle className="h-4 w-4" />
                              <span>Validate Workflow</span>
                            </>
                          )}
                        </motion.button>

                        <motion.button
                          whileHover={{ scale: responsive.isMobile ? 1.01 : 1.02 }}
                          whileTap={{ scale: 0.98 }}
                          onClick={handleSimulatePetriNet}
                          disabled={isSimulating}
                          className={`w-full flex items-center justify-center space-x-2 px-4 border border-green-600 text-green-600 rounded-lg font-medium hover:bg-green-50 transition-colors disabled:opacity-50 ${
                            responsive.isMobile ? 'py-4 touch-manipulation' : 'py-2'
                          }`}
                        >
                          {isSimulating ? (
                            <>
                              <Loader className="h-4 w-4 animate-spin" />
                              <span>Simulating...</span>
                            </>
                          ) : (
                            <>
                              <Play className="h-4 w-4" />
                              <span>Run Simulation</span>
                            </>
                          )}
                        </motion.button>

                        <motion.button
                          whileHover={{ scale: responsive.isMobile ? 1.01 : 1.02 }}
                          whileTap={{ scale: 0.98 }}
                          onClick={handleDownloadWorkflow}
                          className={`w-full flex items-center justify-center space-x-2 px-4 border border-gray-300 text-gray-600 rounded-lg font-medium hover:bg-gray-50 transition-colors ${
                            responsive.isMobile ? 'py-4 touch-manipulation' : 'py-2'
                          }`}
                        >
                          <Download className="h-4 w-4" />
                          <span>Download Workflow</span>
                        </motion.button>
                      </div>
                    </div>
                  </motion.div>
                )}

                {currentStep === 'validate' && (
                  <motion.div
                    key="validate"
                    initial={{ opacity: 0, x: responsive.isMobile ? 100 : -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: responsive.isMobile ? -100 : -20 }}
                    className={responsive.isMobile ? 'p-4' : 'p-6'}
                  >
                    <div className="mb-4">
                      <h2 className={`font-semibold text-gray-900 mb-2 ${
                        responsive.isMobile ? 'text-base' : 'text-lg'
                      }`}>Formal Validation</h2>
                      <p className="text-sm text-gray-600">Verify workflow correctness properties</p>
                    </div>

                    <PetriValidationBanner
                      validationResult={validationResult}
                      isValidating={isValidating}
                      onRetry={handleValidatePetriNet}
                    />
                  </motion.div>
                )}

                {currentStep === 'simulate' && simulationResult && (
                  <motion.div
                    key="simulate"
                    initial={{ opacity: 0, x: responsive.isMobile ? 100 : -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: responsive.isMobile ? -100 : -20 }}
                    className={responsive.isMobile ? 'p-4' : 'p-6'}
                  >
                    <div className="mb-4">
                      <h2 className={`font-semibold text-gray-900 mb-2 ${
                        responsive.isMobile ? 'text-base' : 'text-lg'
                      }`}>Token Simulation</h2>
                      <p className="text-sm text-gray-600">Interactive workflow execution results</p>
                    </div>

                    <div className="space-y-4">
                      {/* Simulation Stats */}
                      <ResponsiveGrid
                        columns={{ mobile: 1, tablet: 3, desktop: 3 }}
                        gap={{ mobile: 2, tablet: 3, desktop: 3 }}
                        className="text-center"
                      >
                        <div className="bg-purple-50 rounded-lg p-3">
                          <div className={`font-bold text-purple-600 ${
                            responsive.isMobile ? 'text-lg' : 'text-xl'
                          }`}>
                            {simulationResult.stepsExecuted}
                          </div>
                          <div className="text-xs text-gray-600">Steps</div>
                        </div>
                        <div className="bg-green-50 rounded-lg p-3">
                          <div className={`font-bold text-green-600 ${
                            responsive.isMobile ? 'text-lg' : 'text-xl'
                          }`}>
                            {simulationResult.status}
                          </div>
                          <div className="text-xs text-gray-600">Status</div>
                        </div>
                        <div className="bg-blue-50 rounded-lg p-3">
                          <div className={`font-bold text-blue-600 ${
                            responsive.isMobile ? 'text-lg' : 'text-xl'
                          }`}>
                            {simulationResult.executionTimeMs}ms
                          </div>
                          <div className="text-xs text-gray-600">Time</div>
                        </div>
                      </ResponsiveGrid>

                      {/* Trace Preview */}
                      {simulationResult.trace && simulationResult.trace.length > 0 && (
                        <div className="bg-gray-50 rounded-lg p-4">
                          <h4 className="font-medium text-gray-900 mb-3">Recent Events</h4>
                          <div className={`space-y-2 overflow-y-auto ${
                            responsive.isMobile ? 'max-h-32' : 'max-h-48'
                          }`}>
                            {simulationResult.trace.slice(0, responsive.isMobile ? 3 : 5).map((event, index) => (
                              <div key={index} className="flex items-center space-x-2 text-sm">
                                <div className="w-6 h-6 bg-purple-100 rounded-full flex items-center justify-center text-purple-600 text-xs font-medium flex-shrink-0">
                                  {event.sequenceNumber}
                                </div>
                                <div className="flex-1 text-gray-700 truncate">
                                  {event.transition}
                                </div>
                              </div>
                            ))}
                            {simulationResult.trace.length > (responsive.isMobile ? 3 : 5) && (
                              <div className="text-xs text-gray-500 text-center pt-2">
                                +{simulationResult.trace.length - (responsive.isMobile ? 3 : 5)} more events
                              </div>
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </ResponsivePanel>
          )}

          {/* Visualization Panel */}
          {(!responsive.isMobile || mobilePanel === 'visualization') && (
            <ResponsivePanel
              className={`bg-white ${
                responsive.isMobile ? 'w-full h-full' :
                responsive.shouldStackPanels() ? 'w-full' :
                'w-full lg:w-2/3'
              }`}
              title={responsive.isMobile ? "Network Visualization" : undefined}
            >
              {petriNet && dag ? (
                <div
                  className={responsive.isMobile ? 'h-96' : 'h-full'}
                  data-testid="petri-net-container"
                >
                  <DualGraphView
                    petriNet={petriNet}
                    dag={dag}
                    onElementSelect={handleElementSelect}
                    initialLayout={responsive.isMobile ? "single" : "side-by-side"}
                    isMobile={responsive.isMobile}
                    isTablet={responsive.isTablet}
                    compactMode={compactMode}
                  />

                  {/* Element Details - Hidden on mobile, shown in results panel */}
                  {selectedElement && !responsive.isMobile && (
                    <div className="border-t border-gray-200 p-4">
                      <h3 className="text-sm font-semibold text-gray-900 mb-2">Element Details</h3>
                      <div className="bg-gray-50 rounded p-3 text-xs">
                        <pre className="text-gray-800 whitespace-pre-wrap overflow-x-auto max-h-32">
                          {JSON.stringify(selectedElement, null, 2)}
                        </pre>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <div className={`flex items-center justify-center text-center ${
                  responsive.isMobile ? 'h-64 p-6' : 'h-full p-8'
                }`}>
                  <div>
                    <Network className={`mx-auto text-gray-400 mb-4 ${
                      responsive.isMobile ? 'h-12 w-12' : 'h-16 w-16'
                    }`} />
                    <h3 className={`font-medium text-gray-600 mb-2 ${
                      responsive.isMobile ? 'text-base' : 'text-lg'
                    }`}>
                      Workflow Visualization
                    </h3>
                    <p className="text-gray-500 text-sm">
                      Complete the parse and build steps to see your Petri net and DAG visualization
                    </p>
                  </div>
                </div>
              )}
            </ResponsivePanel>
          )}

          {/* Results Panel - Mobile Only */}
          {responsive.isMobile && mobilePanel === 'results' && (
            <ResponsivePanel
              className="bg-white w-full h-full"
              title="Element Details & Results"
            >
              <div className="p-4 space-y-4">
                {/* Mobile Progress Stepper */}
                <div>
                  <h3 className="font-semibold text-gray-900 mb-3">Progress</h3>
                  <PetriProgressStepper
                    currentStep={currentStep}
                    stepStatuses={getStepStatusesForStepper()}
                    processingTimes={processingTimes}
                    onStepClick={handleStepClick}
                    showTimings={true}
                    orientation="vertical"
                    size="small"
                  />
                </div>

                {/* Selected Element Details */}
                {selectedElement && (
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">Selected Element</h3>
                    <div className="bg-gray-50 rounded p-3">
                      <pre className="text-gray-800 whitespace-pre-wrap overflow-x-auto text-xs max-h-48">
                        {JSON.stringify(selectedElement, null, 2)}
                      </pre>
                    </div>
                  </div>
                )}

                {/* Workflow Stats */}
                {petriNet && (
                  <div>
                    <h3 className="font-semibold text-gray-900 mb-2">Workflow Stats</h3>
                    <div className="bg-gray-50 rounded p-3 text-sm">
                      <div className="space-y-1">
                        <div>Places: {petriNet.places?.length || 0}</div>
                        <div>Transitions: {petriNet.transitions?.length || 0}</div>
                        <div>Arcs: {petriNet.arcs?.length || 0}</div>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </ResponsivePanel>
          )}
        </div>

        {/* Quick Actions - Desktop Only */}
        {!responsive.isMobile && (
          <div className="fixed bottom-6 right-6 flex space-x-3">
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              onClick={handleReset}
              className="p-3 bg-gray-600 text-white rounded-full shadow-lg hover:bg-gray-700 transition-colors"
              title="Reset Workflow"
            >
              <RotateCcw className="h-5 w-5" />
            </motion.button>

            {petriNet && (
              <motion.button
                whileHover={{ scale: 1.1 }}
                whileTap={{ scale: 0.9 }}
                onClick={() => setCurrentStep('build')}
                className="p-3 bg-purple-600 text-white rounded-full shadow-lg hover:bg-purple-700 transition-colors"
                title="View Build Step"
              >
                <Eye className="h-5 w-5" />
              </motion.button>
            )}
          </div>
        )}

        {/* Workflow History Sidebar - Large Desktop Only */}
        {workflowHistory.length > 0 && responsive.isLarge && (
          <div className="fixed left-6 top-1/2 transform -translate-y-1/2 w-64 bg-white rounded-lg border border-gray-200 shadow-lg p-4 max-h-96 overflow-y-auto">
            <h3 className="text-sm font-semibold text-gray-900 mb-3">Recent Workflows</h3>
            <div className="space-y-2">
              {workflowHistory.slice(0, 5).map((workflow) => (
                <motion.button
                  key={workflow.id}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => {
                    setPetriNet(workflow.petriNet);
                    setDag(workflow.dag);
                    setCurrentStep('build');
                  }}
                  className="w-full text-left p-3 bg-gray-50 hover:bg-gray-100 rounded-lg border border-gray-200 transition-colors"
                >
                  <div className="font-medium text-gray-900 text-sm truncate">
                    {workflow.name}
                  </div>
                  <div className="text-xs text-gray-600 mt-1">
                    {new Date(workflow.timestamp).toLocaleDateString()}
                  </div>
                </motion.button>
              ))}
            </div>
          </div>
        )}

        {/* Mobile Swipe Hint */}
        {responsive.isMobile && mobilePanels.length > 1 && (
          <div className="text-xs text-gray-500 text-center py-2">
            Swipe left or right to navigate between panels
          </div>
        )}
      </motion.div>
    </ResponsiveLayout>
  );
};

export default PetriNetWorkflow;