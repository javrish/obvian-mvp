import React, { useState, useCallback, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Layout,
  BarChart3,
  Network,
  Activity,
  Download,
  Settings,
  Maximize2,
  Minimize2
} from 'lucide-react';
import toast from 'react-hot-toast';

import TokenSimulationControls from './TokenSimulationControls';
import PetriTracePanel from './PetriTracePanel';
import PetriNetVisualizer from './PetriNetVisualizer';
import DagVisualizer from './DagVisualizer';
import apiService from '../services/api';

const PetriNetSimulation = ({
  petriNet,
  dagData,
  initialMarking = {},
  onSimulationComplete,
  className = ""
}) => {
  // Simulation state
  const [simulationState, setSimulationState] = useState({
    state: 'idle',
    marking: { ...initialMarking },
    stepCount: 0,
    executionTime: 0,
    enabledTransitions: []
  });

  const [traceEvents, setTraceEvents] = useState([]);
  const [crossHighlightedElements, setCrossHighlightedElements] = useState([]);
  const [currentView, setCurrentView] = useState('split'); // 'split', 'petri', 'dag', 'trace'
  const [animationSpeed, setAnimationSpeed] = useState(1);
  const [showSettings, setShowSettings] = useState(false);

  // Handle simulation state changes
  const handleSimulationStateChange = useCallback((newState) => {
    setSimulationState(newState);

    // Notify parent if simulation is complete
    if (newState.state === 'completed' || newState.state === 'deadlocked') {
      onSimulationComplete && onSimulationComplete(newState);
    }
  }, [onSimulationComplete]);

  // Handle trace events
  const handleTraceEvent = useCallback((event) => {
    setTraceEvents(prev => [...prev, event]);
  }, []);

  // Handle cross-highlighting between views
  const handleCrossHighlight = useCallback((elements, action = 'highlight') => {
    if (action === 'hover') {
      // Temporary highlighting on hover
      setCrossHighlightedElements(elements.map(el => el.id));

      // Clear after delay
      setTimeout(() => {
        setCrossHighlightedElements([]);
      }, 2000);
    } else {
      // Persistent highlighting
      setCrossHighlightedElements(elements.map(el => el.id));
    }
  }, []);

  // Export simulation data
  const exportSimulationData = useCallback(async () => {
    try {
      const data = {
        petriNet,
        dagData,
        simulationState,
        traceEvents,
        timestamp: new Date().toISOString(),
        metadata: {
          totalSteps: simulationState.stepCount,
          executionTime: simulationState.executionTime,
          finalState: simulationState.state,
          animationSpeed
        }
      };

      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `petri-simulation-${Date.now()}.json`;
      link.click();
      URL.revokeObjectURL(link.href);

      toast.success('Simulation data exported successfully');
    } catch (error) {
      console.error('Export failed:', error);
      toast.error('Failed to export simulation data');
    }
  }, [petriNet, dagData, simulationState, traceEvents, animationSpeed]);

  // View layouts
  const getViewLayout = () => {
    switch (currentView) {
      case 'petri':
        return 'grid-cols-1';
      case 'dag':
        return 'grid-cols-1';
      case 'trace':
        return 'grid-cols-1';
      case 'split':
      default:
        return 'lg:grid-cols-2';
    }
  };

  const renderVisualization = () => {
    const commonProps = {
      className: "h-96 lg:h-[500px]",
      highlightedElements: crossHighlightedElements,
      onCrossHighlight: handleCrossHighlight
    };

    switch (currentView) {
      case 'petri':
        return (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="col-span-full"
          >
            <PetriNetVisualizer
              petriNet={petriNet}
              currentMarking={simulationState.marking}
              traceEvents={traceEvents}
              animationSpeed={animationSpeed}
              simulationState={simulationState.state}
              enableAnimation={true}
              {...commonProps}
            />
          </motion.div>
        );

      case 'dag':
        return (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="col-span-full"
          >
            <DagVisualizer
              dagData={dagData}
              executionState={simulationState}
              {...commonProps}
            />
          </motion.div>
        );

      case 'trace':
        return (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            className="col-span-full"
          >
            <PetriTracePanel
              trace={traceEvents}
              petriNet={petriNet}
              className="h-[600px]"
              onEventClick={(event) => {
                // Highlight related elements when clicking trace events
                if (event.transitionId) {
                  handleCrossHighlight([{ id: event.transitionId, type: 'transition' }]);
                }
              }}
              onEventHover={(event, action) => {
                if (action === 'enter' && event.transitionId) {
                  handleCrossHighlight([{ id: event.transitionId, type: 'transition' }], 'hover');
                }
              }}
            />
          </motion.div>
        );

      case 'split':
      default:
        return (
          <>
            <motion.div
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.1 }}
            >
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-gray-900">Petri Net View</h3>
                  <motion.button
                    onClick={() => setCurrentView('petri')}
                    className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    title="Expand Petri Net View"
                  >
                    <Maximize2 className="h-4 w-4" />
                  </motion.button>
                </div>
                <PetriNetVisualizer
                  petriNet={petriNet}
                  currentMarking={simulationState.marking}
                  traceEvents={traceEvents}
                  animationSpeed={animationSpeed}
                  simulationState={simulationState.state}
                  enableAnimation={true}
                  {...commonProps}
                />
              </div>
            </motion.div>

            <motion.div
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: 0.2 }}
            >
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <h3 className="text-lg font-semibold text-gray-900">DAG View</h3>
                  <motion.button
                    onClick={() => setCurrentView('dag')}
                    className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    title="Expand DAG View"
                  >
                    <Maximize2 className="h-4 w-4" />
                  </motion.button>
                </div>
                <DagVisualizer
                  dagData={dagData}
                  executionState={simulationState}
                  {...commonProps}
                />
              </div>
            </motion.div>
          </>
        );
    }
  };

  if (!petriNet) {
    return (
      <div className={`flex items-center justify-center h-96 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300 ${className}`}>
        <div className="text-center">
          <Network className="h-12 w-12 text-gray-400 mx-auto mb-4" />
          <p className="text-lg font-medium text-gray-900 mb-2">No Petri Net Available</p>
          <p className="text-sm text-gray-600">Load a Petri net to start simulation</p>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={`space-y-6 ${className}`}
    >
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Petri Net Simulation</h2>
          <p className="text-gray-600 mt-1">Interactive token-based execution simulation</p>
        </div>

        <div className="flex items-center space-x-2">
          {/* View Toggle */}
          <div className="flex items-center bg-gray-100 rounded-lg p-1">
            {[
              { key: 'split', icon: Layout, label: 'Split View' },
              { key: 'petri', icon: Network, label: 'Petri Net' },
              { key: 'dag', icon: BarChart3, label: 'DAG' },
              { key: 'trace', icon: Activity, label: 'Trace' }
            ].map(view => (
              <motion.button
                key={view.key}
                onClick={() => setCurrentView(view.key)}
                className={`flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                  currentView === view.key
                    ? 'bg-white text-primary-700 shadow-sm'
                    : 'text-gray-600 hover:bg-white hover:shadow-sm'
                }`}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                title={view.label}
              >
                <view.icon className="h-4 w-4" />
                <span className="hidden sm:inline">{view.label}</span>
              </motion.button>
            ))}
          </div>

          {/* Actions */}
          <motion.button
            onClick={exportSimulationData}
            className="flex items-center space-x-2 px-4 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 transition-colors"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            title="Export Simulation Data"
          >
            <Download className="h-4 w-4" />
            <span className="hidden sm:inline">Export</span>
          </motion.button>

          <motion.button
            onClick={() => setShowSettings(!showSettings)}
            className={`p-2 rounded-lg border transition-colors ${
              showSettings
                ? 'bg-primary-50 border-primary-200 text-primary-700'
                : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
            }`}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            title="Simulation Settings"
          >
            <Settings className="h-4 w-4" />
          </motion.button>
        </div>
      </div>

      {/* Settings Panel */}
      <AnimatePresence>
        {showSettings && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="bg-gray-50 rounded-lg border border-gray-200 overflow-hidden"
          >
            <div className="p-6 space-y-4">
              <h3 className="text-lg font-semibold text-gray-900">Animation Settings</h3>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Animation Speed: {animationSpeed}x
                </label>
                <div className="flex items-center space-x-4">
                  <span className="text-sm text-gray-500">0.5x</span>
                  <input
                    type="range"
                    min="0.5"
                    max="4"
                    step="0.5"
                    value={animationSpeed}
                    onChange={(e) => setAnimationSpeed(parseFloat(e.target.value))}
                    className="flex-1 h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                  />
                  <span className="text-sm text-gray-500">4x</span>
                </div>
              </div>

              <div className="pt-4 border-t border-gray-200">
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                  <div>
                    <span className="text-gray-500">State:</span>
                    <span className="ml-2 font-medium capitalize">{simulationState.state}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">Steps:</span>
                    <span className="ml-2 font-medium">{simulationState.stepCount}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">Events:</span>
                    <span className="ml-2 font-medium">{traceEvents.length}</span>
                  </div>
                  <div>
                    <span className="text-gray-500">Enabled:</span>
                    <span className="ml-2 font-medium">{simulationState.enabledTransitions.length}</span>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Simulation Controls */}
      <TokenSimulationControls
        petriNet={petriNet}
        initialMarking={initialMarking}
        onStateChange={handleSimulationStateChange}
        onTraceEvent={handleTraceEvent}
        onSimulationComplete={onSimulationComplete}
        className="mb-6"
        showSettings={false} // Settings are in main panel
      />

      {/* Visualization Area */}
      <div className={`grid gap-6 ${getViewLayout()}`}>
        {renderVisualization()}
      </div>

      {/* Trace Panel - Always visible at bottom in split view */}
      {currentView === 'split' && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
        >
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-semibold text-gray-900">Execution Trace</h3>
            <motion.button
              onClick={() => setCurrentView('trace')}
              className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              title="Expand Trace View"
            >
              <Maximize2 className="h-4 w-4" />
            </motion.button>
          </div>
          <PetriTracePanel
            trace={traceEvents}
            petriNet={petriNet}
            className="max-h-80"
            onEventClick={(event) => {
              if (event.transitionId) {
                handleCrossHighlight([{ id: event.transitionId, type: 'transition' }]);
              }
            }}
            onEventHover={(event, action) => {
              if (action === 'enter' && event.transitionId) {
                handleCrossHighlight([{ id: event.transitionId, type: 'transition' }], 'hover');
              }
            }}
          />
        </motion.div>
      )}

      {/* Back to Split View */}
      {currentView !== 'split' && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="fixed bottom-6 right-6 z-10"
        >
          <motion.button
            onClick={() => setCurrentView('split')}
            className="flex items-center space-x-2 px-4 py-3 bg-primary-600 text-white rounded-lg shadow-lg font-medium hover:bg-primary-700 transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            <Minimize2 className="h-4 w-4" />
            <span>Back to Split View</span>
          </motion.button>
        </motion.div>
      )}
    </motion.div>
  );
};

export default PetriNetSimulation;