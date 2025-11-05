import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Play,
  Pause,
  SkipForward,
  RotateCcw,
  Settings,
  Activity,
  Clock,
  Zap,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Download
} from 'lucide-react';
import toast from 'react-hot-toast';

// Simulation states
const SIMULATION_STATES = {
  IDLE: 'idle',
  RUNNING: 'running',
  PAUSED: 'paused',
  COMPLETED: 'completed',
  DEADLOCKED: 'deadlocked',
  ERROR: 'error'
};

// Speed presets
const SPEED_PRESETS = [
  { value: 0.5, label: '0.5x', description: 'Half speed' },
  { value: 1, label: '1x', description: 'Normal speed' },
  { value: 1.5, label: '1.5x', description: 'Fast' },
  { value: 2, label: '2x', description: 'Double speed' },
  { value: 4, label: '4x', description: 'Maximum speed' }
];

const TokenSimulationControls = ({
  petriNet,
  initialMarking = {},
  onStateChange,
  onMarkingChange,
  onTraceEvent,
  onSimulationComplete,
  className = "",
  disabled = false,
  showSettings = true
}) => {
  // Simulation state
  const [simulationState, setSimulationState] = useState(SIMULATION_STATES.IDLE);
  const [currentMarking, setCurrentMarking] = useState(initialMarking);
  const [simulationSpeed, setSimulationSpeed] = useState(1);
  const [isDeterministic, setIsDeterministic] = useState(true);
  const [stepCount, setStepCount] = useState(0);
  const [executionTime, setExecutionTime] = useState(0);
  const [showSettingsPanel, setShowSettingsPanel] = useState(false);

  // Simulation internals
  const simulationTimer = useRef(null);
  const startTime = useRef(null);
  const executionTimer = useRef(null);
  const traceEvents = useRef([]);
  const enabledTransitions = useRef([]);

  // Initialize marking when petriNet or initialMarking changes
  useEffect(() => {
    if (petriNet && Object.keys(initialMarking).length > 0) {
      setCurrentMarking({ ...initialMarking });
      resetSimulation();
    }
  }, [petriNet, initialMarking]);

  // Update execution timer
  useEffect(() => {
    if (simulationState === SIMULATION_STATES.RUNNING) {
      executionTimer.current = setInterval(() => {
        setExecutionTime(prev => prev + 100);
      }, 100);
    } else {
      if (executionTimer.current) {
        clearInterval(executionTimer.current);
        executionTimer.current = null;
      }
    }

    return () => {
      if (executionTimer.current) {
        clearInterval(executionTimer.current);
      }
    };
  }, [simulationState]);

  // Notify parent of state changes
  useEffect(() => {
    if (onStateChange) {
      onStateChange({
        state: simulationState,
        marking: currentMarking,
        stepCount,
        executionTime,
        enabledTransitions: enabledTransitions.current
      });
    }
  }, [simulationState, currentMarking, stepCount, executionTime, onStateChange]);

  // Calculate enabled transitions for current marking
  const calculateEnabledTransitions = useCallback((marking) => {
    if (!petriNet || !petriNet.transitions || !petriNet.arcs) return [];

    return petriNet.transitions.filter(transition => {
      // Check if transition is enabled by examining input places
      const inputArcs = petriNet.arcs.filter(arc => arc.to === transition.id);

      return inputArcs.every(arc => {
        const placeTokens = marking[arc.from] || 0;
        const requiredTokens = arc.weight || 1;
        return placeTokens >= requiredTokens;
      });
    });
  }, [petriNet]);

  // Fire a transition
  const fireTransition = useCallback((transitionId) => {
    if (!petriNet || simulationState !== SIMULATION_STATES.RUNNING && simulationState !== SIMULATION_STATES.PAUSED) {
      return null;
    }

    const transition = petriNet.transitions.find(t => t.id === transitionId);
    if (!transition) return null;

    const enabled = calculateEnabledTransitions(currentMarking);
    if (!enabled.find(t => t.id === transitionId)) {
      return null; // Transition not enabled
    }

    // Calculate new marking after firing
    const newMarking = { ...currentMarking };
    const inputArcs = petriNet.arcs.filter(arc => arc.to === transitionId);
    const outputArcs = petriNet.arcs.filter(arc => arc.from === transitionId);

    // Remove tokens from input places
    inputArcs.forEach(arc => {
      const tokens = arc.weight || 1;
      newMarking[arc.from] = Math.max(0, (newMarking[arc.from] || 0) - tokens);
    });

    // Add tokens to output places
    outputArcs.forEach(arc => {
      const tokens = arc.weight || 1;
      newMarking[arc.to] = (newMarking[arc.to] || 0) + tokens;
    });

    // Create trace event
    const traceEvent = {
      timestamp: Date.now(),
      stepNumber: stepCount + 1,
      eventType: 'transition_fired',
      transitionId,
      transitionName: transition.name || transition.id,
      previousMarking: { ...currentMarking },
      newMarking: { ...newMarking },
      tokenMovements: {
        removed: inputArcs.map(arc => ({ placeId: arc.from, tokens: arc.weight || 1 })),
        added: outputArcs.map(arc => ({ placeId: arc.to, tokens: arc.weight || 1 }))
      }
    };

    // Update state
    setCurrentMarking(newMarking);
    setStepCount(prev => prev + 1);
    traceEvents.current.push(traceEvent);

    // Notify callbacks
    if (onMarkingChange) {
      onMarkingChange(newMarking);
    }

    if (onTraceEvent) {
      onTraceEvent(traceEvent);
    }

    return traceEvent;
  }, [petriNet, currentMarking, simulationState, stepCount, onMarkingChange, onTraceEvent, calculateEnabledTransitions]);

  // Perform simulation step
  const performStep = useCallback(() => {
    const enabled = calculateEnabledTransitions(currentMarking);

    if (enabled.length === 0) {
      // No enabled transitions - check if completed or deadlocked
      const hasTokens = Object.values(currentMarking).some(tokens => tokens > 0);
      const newState = hasTokens ? SIMULATION_STATES.DEADLOCKED : SIMULATION_STATES.COMPLETED;

      setSimulationState(newState);
      if (simulationTimer.current) {
        clearInterval(simulationTimer.current);
        simulationTimer.current = null;
      }

      if (onSimulationComplete) {
        onSimulationComplete({
          state: newState,
          finalMarking: currentMarking,
          totalSteps: stepCount,
          executionTime,
          trace: traceEvents.current
        });
      }

      const message = newState === SIMULATION_STATES.COMPLETED
        ? 'Simulation completed successfully!'
        : 'Simulation deadlocked - no transitions can fire';

      toast[newState === SIMULATION_STATES.COMPLETED ? 'success' : 'error'](message);
      return false;
    }

    // Select transition to fire
    let transitionToFire;
    if (isDeterministic) {
      // Select first enabled transition deterministically
      transitionToFire = enabled[0];
    } else {
      // Select random enabled transition
      const randomIndex = Math.floor(Math.random() * enabled.length);
      transitionToFire = enabled[randomIndex];
    }

    // Fire the selected transition
    const event = fireTransition(transitionToFire.id);
    return event !== null;
  }, [currentMarking, stepCount, executionTime, isDeterministic, calculateEnabledTransitions, fireTransition, onSimulationComplete]);

  // Start simulation
  const startSimulation = useCallback(() => {
    if (simulationState === SIMULATION_STATES.RUNNING) return;

    setSimulationState(SIMULATION_STATES.RUNNING);
    if (!startTime.current) {
      startTime.current = Date.now();
    }

    // Start simulation loop
    const stepInterval = Math.max(100, 1000 / simulationSpeed);
    simulationTimer.current = setInterval(() => {
      if (!performStep()) {
        clearInterval(simulationTimer.current);
        simulationTimer.current = null;
      }
    }, stepInterval);

    toast.success('Simulation started');
  }, [simulationState, simulationSpeed, performStep]);

  // Pause simulation
  const pauseSimulation = useCallback(() => {
    if (simulationState !== SIMULATION_STATES.RUNNING) return;

    setSimulationState(SIMULATION_STATES.PAUSED);
    if (simulationTimer.current) {
      clearInterval(simulationTimer.current);
      simulationTimer.current = null;
    }

    toast.success('Simulation paused');
  }, [simulationState]);

  // Perform single step
  const stepSimulation = useCallback(() => {
    if (simulationState === SIMULATION_STATES.RUNNING) {
      pauseSimulation();
    }

    if (simulationState === SIMULATION_STATES.IDLE && stepCount === 0) {
      setSimulationState(SIMULATION_STATES.PAUSED);
      startTime.current = Date.now();
    }

    performStep();
  }, [simulationState, stepCount, pauseSimulation, performStep]);

  // Reset simulation
  const resetSimulation = useCallback(() => {
    if (simulationTimer.current) {
      clearInterval(simulationTimer.current);
      simulationTimer.current = null;
    }

    setSimulationState(SIMULATION_STATES.IDLE);
    setCurrentMarking({ ...initialMarking });
    setStepCount(0);
    setExecutionTime(0);
    startTime.current = null;
    traceEvents.current = [];

    toast.success('Simulation reset');
  }, [initialMarking]);

  // Update enabled transitions when marking changes
  useEffect(() => {
    enabledTransitions.current = calculateEnabledTransitions(currentMarking);
  }, [currentMarking, calculateEnabledTransitions]);

  // Get status icon and color for current state
  const getStateInfo = (state) => {
    switch (state) {
      case SIMULATION_STATES.IDLE:
        return { icon: Clock, color: 'text-gray-500', bgColor: 'bg-gray-100', label: 'Ready' };
      case SIMULATION_STATES.RUNNING:
        return { icon: Activity, color: 'text-blue-600', bgColor: 'bg-blue-100', label: 'Running' };
      case SIMULATION_STATES.PAUSED:
        return { icon: Pause, color: 'text-yellow-600', bgColor: 'bg-yellow-100', label: 'Paused' };
      case SIMULATION_STATES.COMPLETED:
        return { icon: CheckCircle, color: 'text-green-600', bgColor: 'bg-green-100', label: 'Completed' };
      case SIMULATION_STATES.DEADLOCKED:
        return { icon: XCircle, color: 'text-red-600', bgColor: 'bg-red-100', label: 'Deadlocked' };
      case SIMULATION_STATES.ERROR:
        return { icon: AlertTriangle, color: 'text-red-600', bgColor: 'bg-red-100', label: 'Error' };
      default:
        return { icon: Clock, color: 'text-gray-500', bgColor: 'bg-gray-100', label: 'Unknown' };
    }
  };

  const stateInfo = getStateInfo(simulationState);
  const StateIcon = stateInfo.icon;
  const isRunning = simulationState === SIMULATION_STATES.RUNNING;
  const canStep = simulationState === SIMULATION_STATES.IDLE || simulationState === SIMULATION_STATES.PAUSED;
  const canStart = simulationState === SIMULATION_STATES.IDLE || simulationState === SIMULATION_STATES.PAUSED;
  const canPause = simulationState === SIMULATION_STATES.RUNNING;
  const canReset = simulationState !== SIMULATION_STATES.IDLE;

  // Format execution time
  const formatTime = (ms) => {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    const remainingMs = ms % 1000;

    if (minutes > 0) {
      return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
    }
    return `${remainingSeconds}.${Math.floor(remainingMs / 100)}s`;
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={`bg-white rounded-xl border border-gray-200 shadow-sm ${className}`}
    >
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-100">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <motion.div
              className={`p-2 rounded-lg ${stateInfo.bgColor}`}
              animate={{ scale: isRunning ? [1, 1.1, 1] : 1 }}
              transition={{ duration: 1, repeat: isRunning ? Infinity : 0 }}
            >
              <StateIcon className={`h-5 w-5 ${stateInfo.color}`} />
            </motion.div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900">Token Simulation</h3>
              <div className="flex items-center space-x-4 text-sm text-gray-500">
                <span className={`font-medium ${stateInfo.color}`}>{stateInfo.label}</span>
                <span>Step {stepCount}</span>
                <span>{formatTime(executionTime)}</span>
                <span>{enabledTransitions.current.length} transitions enabled</span>
              </div>
            </div>
          </div>

          {showSettings && (
            <motion.button
              onClick={() => setShowSettingsPanel(!showSettingsPanel)}
              className={`p-2 rounded-lg border transition-colors ${
                showSettingsPanel
                  ? 'bg-primary-50 border-primary-200 text-primary-700'
                  : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
              }`}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              title="Simulation Settings"
            >
              <Settings className="h-4 w-4" />
            </motion.button>
          )}
        </div>
      </div>

      {/* Settings Panel */}
      <AnimatePresence>
        {showSettingsPanel && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.2 }}
            className="border-b border-gray-100 overflow-hidden"
          >
            <div className="px-6 py-4 space-y-4">
              {/* Speed Control */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Simulation Speed
                </label>
                <div className="flex items-center space-x-2">
                  {SPEED_PRESETS.map(preset => (
                    <motion.button
                      key={preset.value}
                      onClick={() => setSimulationSpeed(preset.value)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
                        simulationSpeed === preset.value
                          ? 'bg-primary-100 text-primary-700 border border-primary-300'
                          : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                      }`}
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      title={preset.description}
                    >
                      {preset.label}
                    </motion.button>
                  ))}
                </div>
              </div>

              {/* Mode Control */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  Execution Mode
                </label>
                <div className="flex items-center space-x-4">
                  <motion.button
                    onClick={() => setIsDeterministic(true)}
                    className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-sm transition-colors ${
                      isDeterministic
                        ? 'bg-primary-100 text-primary-700 border border-primary-300'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                  >
                    <Zap className="h-4 w-4" />
                    <span>Deterministic</span>
                  </motion.button>

                  <motion.button
                    onClick={() => setIsDeterministic(false)}
                    className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-sm transition-colors ${
                      !isDeterministic
                        ? 'bg-primary-100 text-primary-700 border border-primary-300'
                        : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                    }`}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                  >
                    <Activity className="h-4 w-4" />
                    <span>Random</span>
                  </motion.button>
                </div>
                <p className="text-xs text-gray-500 mt-1">
                  {isDeterministic
                    ? 'Always fires the first enabled transition'
                    : 'Randomly selects from enabled transitions'}
                </p>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Control Buttons */}
      <div className="px-6 py-4">
        <div className="flex items-center space-x-3">
          {/* Play/Pause Button */}
          {canStart && (
            <motion.button
              onClick={startSimulation}
              disabled={disabled || enabledTransitions.current.length === 0}
              className="flex items-center space-x-2 px-4 py-2 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              whileHover={{ scale: disabled ? 1 : 1.02 }}
              whileTap={{ scale: disabled ? 1 : 0.98 }}
              title="Start simulation"
            >
              <Play className="h-4 w-4" />
              <span>Play</span>
            </motion.button>
          )}

          {canPause && (
            <motion.button
              onClick={pauseSimulation}
              disabled={disabled}
              className="flex items-center space-x-2 px-4 py-2 bg-yellow-600 text-white rounded-lg font-medium hover:bg-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              whileHover={{ scale: disabled ? 1 : 1.02 }}
              whileTap={{ scale: disabled ? 1 : 0.98 }}
              title="Pause simulation"
            >
              <Pause className="h-4 w-4" />
              <span>Pause</span>
            </motion.button>
          )}

          {/* Step Button */}
          <motion.button
            onClick={stepSimulation}
            disabled={disabled || (canStep && enabledTransitions.current.length === 0)}
            className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            whileHover={{ scale: disabled ? 1 : 1.02 }}
            whileTap={{ scale: disabled ? 1 : 0.98 }}
            title="Execute one step"
          >
            <SkipForward className="h-4 w-4" />
            <span>Step</span>
          </motion.button>

          {/* Reset Button */}
          {canReset && (
            <motion.button
              onClick={resetSimulation}
              disabled={disabled}
              className="flex items-center space-x-2 px-4 py-2 bg-gray-600 text-white rounded-lg font-medium hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              whileHover={{ scale: disabled ? 1 : 1.02 }}
              whileTap={{ scale: disabled ? 1 : 0.98 }}
              title="Reset simulation"
            >
              <RotateCcw className="h-4 w-4" />
              <span>Reset</span>
            </motion.button>
          )}

          {/* Export Button */}
          <motion.button
            onClick={() => {
              const data = {
                petriNet,
                initialMarking,
                finalMarking: currentMarking,
                simulationState,
                stepCount,
                executionTime,
                trace: traceEvents.current
              };

              const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
              const link = document.createElement('a');
              link.href = URL.createObjectURL(blob);
              link.download = `petri-simulation-${Date.now()}.json`;
              link.click();
              URL.revokeObjectURL(link.href);

              toast.success('Simulation data exported');
            }}
            className="flex items-center space-x-2 px-4 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            title="Export simulation data"
          >
            <Download className="h-4 w-4" />
            <span>Export</span>
          </motion.button>
        </div>

        {/* Status Information */}
        {enabledTransitions.current.length === 0 && simulationState !== SIMULATION_STATES.COMPLETED && (
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mt-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg"
          >
            <div className="flex items-center space-x-2">
              <AlertTriangle className="h-4 w-4 text-yellow-600" />
              <span className="text-sm text-yellow-800">
                No transitions can fire with the current token distribution
              </span>
            </div>
          </motion.div>
        )}
      </div>
    </motion.div>
  );
};

export default TokenSimulationControls;