import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Play, Pause, SkipForward, RotateCcw, Settings, Activity,
  Clock, Zap, AlertTriangle, CheckCircle, XCircle, Download,
  ChevronRight, ChevronDown, Target, GitBranch, Layers,
  FastForward, Rewind, Square, PlayCircle, PauseCircle
} from 'lucide-react';
import toast from 'react-hot-toast';
import apiService from '../services/api';

// Advanced simulation states
const SIMULATION_STATES = {
  IDLE: 'idle',
  RUNNING: 'running',
  PAUSED: 'paused',
  STEP_BY_STEP: 'step_by_step',
  COMPLETED: 'completed',
  DEADLOCKED: 'deadlocked',
  ERROR: 'error',
  INTERACTIVE: 'interactive'
};

// Execution modes
const EXECUTION_MODES = {
  AUTOMATIC: 'automatic',
  STEP_BY_STEP: 'step_by_step',
  INTERACTIVE: 'interactive',
  REPLAY: 'replay'
};

const AdvancedSimulationControls = ({
  petriNet,
  initialMarking = {},
  onStateChange,
  onMarkingChange,
  onTraceEvent,
  onSimulationComplete,
  onTransitionSelected,
  className = "",
  disabled = false,
  showAdvancedControls = true,
  enableWebSocket = true
}) => {
  // Simulation state
  const [simulationState, setSimulationState] = useState(SIMULATION_STATES.IDLE);
  const [executionMode, setExecutionMode] = useState(EXECUTION_MODES.AUTOMATIC);
  const [currentMarking, setCurrentMarking] = useState(initialMarking);
  const [simulationSpeed, setSimulationSpeed] = useState(1);
  const [isDeterministic, setIsDeterministic] = useState(true);
  const [stepCount, setStepCount] = useState(0);
  const [executionTime, setExecutionTime] = useState(0);

  // Advanced features state
  const [enabledTransitions, setEnabledTransitions] = useState([]);
  const [selectedTransition, setSelectedTransition] = useState(null);
  const [simulationHistory, setSimulationHistory] = useState([]);
  const [breakpoints, setBreakpoints] = useState(new Set());
  const [watchedPlaces, setWatchedPlaces] = useState(new Set());

  // Replay state
  const [replayPosition, setReplayPosition] = useState(0);
  const [isReplaying, setIsReplaying] = useState(false);

  // UI state
  const [showSettings, setShowSettings] = useState(false);
  const [showTransitionSelector, setShowTransitionSelector] = useState(false);
  const [showBreakpoints, setShowBreakpoints] = useState(false);
  const [showHistory, setShowHistory] = useState(false);

  // WebSocket and real-time features
  const webSocketRef = useRef(null);
  const [isConnected, setIsConnected] = useState(false);
  const [realTimeMode, setRealTimeMode] = useState(false);

  // Timer refs
  const simulationTimer = useRef(null);
  const executionTimer = useRef(null);
  const startTime = useRef(null);

  // Initialize WebSocket connection for real-time simulation
  useEffect(() => {
    if (!enableWebSocket) return;

    const callbacks = {
      onConnect: () => {
        setIsConnected(true);
        toast.success('Connected to real-time simulation service');
      },

      onTokenMovement: (message) => {
        console.log('Real-time token movement:', message);
        if (realTimeMode) {
          // Update local state with real-time data
          if (onTraceEvent) {
            onTraceEvent({
              eventType: 'transition_fired',
              transitionId: message.transition,
              timestamp: message.timestamp,
              tokenMovements: {
                removed: [{ placeId: message.fromPlace, tokens: message.tokens }],
                added: [{ placeId: message.toPlace, tokens: message.tokens }]
              }
            });
          }
        }
      },

      onMarkingUpdate: (message) => {
        console.log('Real-time marking update:', message);
        if (realTimeMode) {
          setCurrentMarking(message.marking);
          if (onMarkingChange) {
            onMarkingChange(message.marking);
          }
        }
      },

      onSimulationCompleted: (message) => {
        console.log('Real-time simulation completed:', message);
        setSimulationState(SIMULATION_STATES.COMPLETED);
        if (onSimulationComplete) {
          onSimulationComplete(message);
        }
      },

      onError: (error) => {
        console.error('Real-time simulation error:', error);
        toast.error(`Simulation error: ${error.error?.message || 'Unknown error'}`);
        setSimulationState(SIMULATION_STATES.ERROR);
      },

      onClose: () => {
        setIsConnected(false);
      }
    };

    try {
      webSocketRef.current = apiService.petri.createWebSocketConnection(callbacks);
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
    }

    return () => {
      if (webSocketRef.current) {
        webSocketRef.current.close();
      }
    };
  }, [enableWebSocket, realTimeMode, onTraceEvent, onMarkingChange, onSimulationComplete]);

  // Initialize marking when petriNet changes
  useEffect(() => {
    if (petriNet && Object.keys(initialMarking).length > 0) {
      setCurrentMarking({ ...initialMarking });
      resetSimulation();
    }
  }, [petriNet, initialMarking]);

  // Update execution timer
  useEffect(() => {
    if (simulationState === SIMULATION_STATES.RUNNING || simulationState === SIMULATION_STATES.STEP_BY_STEP) {
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

  // Calculate enabled transitions
  const calculateEnabledTransitions = useCallback((marking) => {
    if (!petriNet || !petriNet.transitions || !petriNet.arcs) return [];

    return petriNet.transitions.filter(transition => {
      const inputArcs = petriNet.arcs.filter(arc => arc.to === transition.id);
      return inputArcs.every(arc => {
        const placeTokens = marking[arc.from] || 0;
        const requiredTokens = arc.weight || 1;
        return placeTokens >= requiredTokens;
      });
    });
  }, [petriNet]);

  // Update enabled transitions when marking changes
  useEffect(() => {
    const enabled = calculateEnabledTransitions(currentMarking);
    setEnabledTransitions(enabled);
  }, [currentMarking, calculateEnabledTransitions]);

  // Start real-time simulation
  const startRealTimeSimulation = useCallback(() => {
    if (!webSocketRef.current || !isConnected) {
      toast.error('Not connected to real-time service');
      return;
    }

    setRealTimeMode(true);
    setSimulationState(SIMULATION_STATES.RUNNING);

    const config = {
      maxSteps: 1000,
      stepDelayMs: Math.max(100, 1000 / simulationSpeed),
      mode: isDeterministic ? 'deterministic' : 'random'
    };

    webSocketRef.current.startSimulation(petriNet, config);
    toast.success('Real-time simulation started');
  }, [petriNet, simulationSpeed, isDeterministic, isConnected]);

  // Start local simulation
  const startLocalSimulation = useCallback(() => {
    if (simulationState === SIMULATION_STATES.RUNNING) return;

    setSimulationState(SIMULATION_STATES.RUNNING);
    setRealTimeMode(false);
    startTime.current = Date.now();

    // Start simulation loop
    const stepInterval = Math.max(100, 1000 / simulationSpeed);
    simulationTimer.current = setInterval(() => {
      performStep();
    }, stepInterval);

    toast.success('Local simulation started');
  }, [simulationState, simulationSpeed]);

  // Perform single step
  const performStep = useCallback(() => {
    const enabled = calculateEnabledTransitions(currentMarking);

    if (enabled.length === 0) {
      // Check for completion or deadlock
      const hasTokens = Object.values(currentMarking).some(tokens => tokens > 0);
      const newState = hasTokens ? SIMULATION_STATES.DEADLOCKED : SIMULATION_STATES.COMPLETED;

      setSimulationState(newState);
      if (simulationTimer.current) {
        clearInterval(simulationTimer.current);
        simulationTimer.current = null;
      }

      toast[newState === SIMULATION_STATES.COMPLETED ? 'success' : 'error'](
        newState === SIMULATION_STATES.COMPLETED ? 'Simulation completed' : 'Simulation deadlocked'
      );
      return false;
    }

    // Select transition to fire
    let transitionToFire;
    if (executionMode === EXECUTION_MODES.INTERACTIVE) {
      // Wait for user selection
      setShowTransitionSelector(true);
      setSimulationState(SIMULATION_STATES.INTERACTIVE);
      return false;
    } else if (isDeterministic) {
      transitionToFire = enabled[0];
    } else {
      const randomIndex = Math.floor(Math.random() * enabled.length);
      transitionToFire = enabled[randomIndex];
    }

    // Check breakpoints
    if (breakpoints.has(transitionToFire.id)) {
      setSimulationState(SIMULATION_STATES.PAUSED);
      setSelectedTransition(transitionToFire);
      toast.info(`Breakpoint hit: ${transitionToFire.name || transitionToFire.id}`);
      return false;
    }

    // Fire transition
    fireTransition(transitionToFire.id);
    return true;
  }, [currentMarking, calculateEnabledTransitions, executionMode, isDeterministic, breakpoints]);

  // Fire a specific transition
  const fireTransition = useCallback((transitionId) => {
    if (!petriNet) return;

    const transition = petriNet.transitions.find(t => t.id === transitionId);
    if (!transition) return;

    const enabled = calculateEnabledTransitions(currentMarking);
    if (!enabled.find(t => t.id === transitionId)) {
      toast.error('Transition is not enabled');
      return;
    }

    // Calculate new marking
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

    // Create history entry
    const historyEntry = {
      step: stepCount + 1,
      timestamp: Date.now(),
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
    setSimulationHistory(prev => [...prev, historyEntry]);

    // Check watched places
    watchedPlaces.forEach(placeId => {
      if (newMarking[placeId] !== currentMarking[placeId]) {
        toast.info(`Watched place ${placeId}: ${currentMarking[placeId]} → ${newMarking[placeId]} tokens`);
      }
    });

    // Notify callbacks
    if (onMarkingChange) {
      onMarkingChange(newMarking);
    }

    if (onTraceEvent) {
      onTraceEvent({
        eventType: 'transition_fired',
        ...historyEntry
      });
    }

    setSelectedTransition(null);
    setShowTransitionSelector(false);
  }, [petriNet, currentMarking, stepCount, calculateEnabledTransitions, watchedPlaces, onMarkingChange, onTraceEvent]);

  // Control functions
  const pauseSimulation = useCallback(() => {
    setSimulationState(SIMULATION_STATES.PAUSED);
    if (simulationTimer.current) {
      clearInterval(simulationTimer.current);
      simulationTimer.current = null;
    }
    if (webSocketRef.current && realTimeMode) {
      webSocketRef.current.pauseSimulation();
    }
    toast.success('Simulation paused');
  }, [realTimeMode]);

  const stepSimulation = useCallback(() => {
    if (simulationState === SIMULATION_STATES.RUNNING) {
      pauseSimulation();
    }

    if (executionMode === EXECUTION_MODES.INTERACTIVE) {
      const enabled = calculateEnabledTransitions(currentMarking);
      if (enabled.length > 0) {
        setShowTransitionSelector(true);
      }
    } else {
      performStep();
    }
  }, [simulationState, executionMode, pauseSimulation, performStep, calculateEnabledTransitions, currentMarking]);

  const resetSimulation = useCallback(() => {
    if (simulationTimer.current) {
      clearInterval(simulationTimer.current);
      simulationTimer.current = null;
    }

    setSimulationState(SIMULATION_STATES.IDLE);
    setCurrentMarking({ ...initialMarking });
    setStepCount(0);
    setExecutionTime(0);
    setSimulationHistory([]);
    setReplayPosition(0);
    setSelectedTransition(null);
    setShowTransitionSelector(false);
    setRealTimeMode(false);
    startTime.current = null;

    if (webSocketRef.current) {
      webSocketRef.current.resetSimulation();
    }

    toast.success('Simulation reset');
  }, [initialMarking]);

  // Replay functions
  const startReplay = useCallback(() => {
    if (simulationHistory.length === 0) {
      toast.error('No simulation history to replay');
      return;
    }

    setIsReplaying(true);
    setReplayPosition(0);
    setSimulationState(SIMULATION_STATES.PAUSED);

    // Reset to initial state
    setCurrentMarking({ ...initialMarking });
    setStepCount(0);

    toast.success('Replay mode started');
  }, [simulationHistory, initialMarking]);

  const replayStep = useCallback((direction = 'forward') => {
    if (!isReplaying || simulationHistory.length === 0) return;

    let newPosition = replayPosition;
    if (direction === 'forward' && replayPosition < simulationHistory.length) {
      newPosition = replayPosition + 1;
    } else if (direction === 'backward' && replayPosition > 0) {
      newPosition = replayPosition - 1;
    }

    if (newPosition !== replayPosition) {
      setReplayPosition(newPosition);

      // Apply state up to this position
      let marking = { ...initialMarking };
      for (let i = 0; i < newPosition; i++) {
        marking = { ...simulationHistory[i].newMarking };
      }

      setCurrentMarking(marking);
      setStepCount(newPosition);

      if (onMarkingChange) {
        onMarkingChange(marking);
      }
    }
  }, [isReplaying, replayPosition, simulationHistory, initialMarking, onMarkingChange]);

  const exitReplay = useCallback(() => {
    setIsReplaying(false);
    setReplayPosition(0);
    setSimulationState(SIMULATION_STATES.IDLE);
    toast.success('Exited replay mode');
  }, []);

  // Breakpoint management
  const toggleBreakpoint = useCallback((transitionId) => {
    const newBreakpoints = new Set(breakpoints);
    if (breakpoints.has(transitionId)) {
      newBreakpoints.delete(transitionId);
      toast.success('Breakpoint removed');
    } else {
      newBreakpoints.add(transitionId);
      toast.success('Breakpoint added');
    }
    setBreakpoints(newBreakpoints);
  }, [breakpoints]);

  // Watch management
  const toggleWatch = useCallback((placeId) => {
    const newWatched = new Set(watchedPlaces);
    if (watchedPlaces.has(placeId)) {
      newWatched.delete(placeId);
      toast.success('Place unwatched');
    } else {
      newWatched.add(placeId);
      toast.success('Place watched');
    }
    setWatchedPlaces(newWatched);
  }, [watchedPlaces]);

  // Export simulation data
  const exportSimulation = useCallback(() => {
    const exportData = {
      petriNet,
      initialMarking,
      finalMarking: currentMarking,
      simulationState,
      stepCount,
      executionTime,
      history: simulationHistory,
      breakpoints: Array.from(breakpoints),
      watchedPlaces: Array.from(watchedPlaces)
    };

    const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `simulation-${Date.now()}.json`;
    link.click();
    URL.revokeObjectURL(link.href);

    toast.success('Simulation data exported');
  }, [petriNet, initialMarking, currentMarking, simulationState, stepCount, executionTime, simulationHistory, breakpoints, watchedPlaces]);

  // Format execution time
  const formatTime = (ms) => {
    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;

    if (minutes > 0) {
      return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
    }
    return `${remainingSeconds}.${Math.floor((ms % 1000) / 100)}s`;
  };

  // Get state info for display
  const getStateInfo = (state) => {
    switch (state) {
      case SIMULATION_STATES.IDLE:
        return { icon: Clock, color: 'text-gray-500', bgColor: 'bg-gray-100', label: 'Ready' };
      case SIMULATION_STATES.RUNNING:
        return { icon: Activity, color: 'text-blue-600', bgColor: 'bg-blue-100', label: 'Running' };
      case SIMULATION_STATES.PAUSED:
        return { icon: PauseCircle, color: 'text-yellow-600', bgColor: 'bg-yellow-100', label: 'Paused' };
      case SIMULATION_STATES.STEP_BY_STEP:
        return { icon: Target, color: 'text-purple-600', bgColor: 'bg-purple-100', label: 'Step Mode' };
      case SIMULATION_STATES.INTERACTIVE:
        return { icon: GitBranch, color: 'text-orange-600', bgColor: 'bg-orange-100', label: 'Interactive' };
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

  const canStart = simulationState === SIMULATION_STATES.IDLE || simulationState === SIMULATION_STATES.PAUSED;
  const canPause = simulationState === SIMULATION_STATES.RUNNING;
  const canStep = simulationState !== SIMULATION_STATES.RUNNING;
  const canReset = simulationState !== SIMULATION_STATES.IDLE;

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
            <motion.div
              className={`p-2 rounded-lg ${stateInfo.bgColor}`}
              animate={{
                scale: simulationState === SIMULATION_STATES.RUNNING ? [1, 1.05, 1] : 1
              }}
              transition={{ duration: 1, repeat: simulationState === SIMULATION_STATES.RUNNING ? Infinity : 0 }}
            >
              <StateIcon className={`h-5 w-5 ${stateInfo.color}`} />
            </motion.div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900">Advanced Simulation Controls</h3>
              <div className="flex items-center space-x-4 text-sm text-gray-500">
                <span className={`font-medium ${stateInfo.color}`}>{stateInfo.label}</span>
                <span>Step {stepCount}</span>
                <span>{formatTime(executionTime)}</span>
                <span>{enabledTransitions.length} enabled</span>
                {isConnected && (
                  <span className="text-green-600">● Connected</span>
                )}
              </div>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <motion.button
              onClick={() => setShowHistory(!showHistory)}
              className={`p-2 rounded-lg border transition-colors ${
                showHistory ? 'bg-blue-50 border-blue-200 text-blue-700' : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
              }`}
              whileHover={{ scale: 1.05 }}
              title="Simulation History"
            >
              <Layers className="h-4 w-4" />
            </motion.button>

            <motion.button
              onClick={() => setShowBreakpoints(!showBreakpoints)}
              className={`p-2 rounded-lg border transition-colors ${
                showBreakpoints ? 'bg-red-50 border-red-200 text-red-700' : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
              }`}
              whileHover={{ scale: 1.05 }}
              title="Breakpoints"
            >
              <Target className="h-4 w-4" />
              {breakpoints.size > 0 && (
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full w-5 h-5 flex items-center justify-center">
                  {breakpoints.size}
                </span>
              )}
            </motion.button>

            <motion.button
              onClick={() => setShowSettings(!showSettings)}
              className={`p-2 rounded-lg border transition-colors ${
                showSettings ? 'bg-gray-100 border-gray-300 text-gray-700' : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
              }`}
              whileHover={{ scale: 1.05 }}
              title="Settings"
            >
              <Settings className="h-4 w-4" />
            </motion.button>
          </div>
        </div>
      </div>

      {/* Main Controls */}
      <div className="px-6 py-4">
        <div className="flex items-center space-x-3">
          {/* Primary Controls */}
          {canStart && !isReplaying && (
            <motion.button
              onClick={isConnected ? startRealTimeSimulation : startLocalSimulation}
              disabled={disabled || enabledTransitions.length === 0}
              className="flex items-center space-x-2 px-4 py-2 bg-green-600 text-white rounded-lg font-medium hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              whileHover={{ scale: disabled ? 1 : 1.02 }}
              whileTap={{ scale: disabled ? 1 : 0.98 }}
            >
              <Play className="h-4 w-4" />
              <span>{isConnected ? 'Real-time' : 'Local'}</span>
            </motion.button>
          )}

          {canPause && !isReplaying && (
            <motion.button
              onClick={pauseSimulation}
              disabled={disabled}
              className="flex items-center space-x-2 px-4 py-2 bg-yellow-600 text-white rounded-lg font-medium hover:bg-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              whileHover={{ scale: disabled ? 1 : 1.02 }}
              whileTap={{ scale: disabled ? 1 : 0.98 }}
            >
              <Pause className="h-4 w-4" />
              <span>Pause</span>
            </motion.button>
          )}

          {canStep && !isReplaying && (
            <motion.button
              onClick={stepSimulation}
              disabled={disabled || enabledTransitions.length === 0}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              whileHover={{ scale: disabled ? 1 : 1.02 }}
              whileTap={{ scale: disabled ? 1 : 0.98 }}
            >
              <SkipForward className="h-4 w-4" />
              <span>Step</span>
            </motion.button>
          )}

          {/* Replay Controls */}
          {isReplaying && (
            <>
              <motion.button
                onClick={() => replayStep('backward')}
                disabled={replayPosition === 0}
                className="flex items-center space-x-2 px-3 py-2 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <Rewind className="h-4 w-4" />
              </motion.button>

              <motion.button
                onClick={() => replayStep('forward')}
                disabled={replayPosition >= simulationHistory.length}
                className="flex items-center space-x-2 px-3 py-2 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <FastForward className="h-4 w-4" />
              </motion.button>

              <span className="text-sm text-gray-600">
                {replayPosition} / {simulationHistory.length}
              </span>

              <motion.button
                onClick={exitReplay}
                className="flex items-center space-x-2 px-3 py-2 bg-gray-600 text-white rounded-lg font-medium hover:bg-gray-700 transition-colors"
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <Square className="h-4 w-4" />
                <span>Exit</span>
              </motion.button>
            </>
          )}

          {/* Utility Controls */}
          {!isReplaying && (
            <>
              {simulationHistory.length > 0 && (
                <motion.button
                  onClick={startReplay}
                  className="flex items-center space-x-2 px-4 py-2 bg-purple-600 text-white rounded-lg font-medium hover:bg-purple-700 transition-colors"
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                >
                  <PlayCircle className="h-4 w-4" />
                  <span>Replay</span>
                </motion.button>
              )}

              {canReset && (
                <motion.button
                  onClick={resetSimulation}
                  disabled={disabled}
                  className="flex items-center space-x-2 px-4 py-2 bg-gray-600 text-white rounded-lg font-medium hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                  whileHover={{ scale: disabled ? 1 : 1.02 }}
                  whileTap={{ scale: disabled ? 1 : 0.98 }}
                >
                  <RotateCcw className="h-4 w-4" />
                  <span>Reset</span>
                </motion.button>
              )}

              <motion.button
                onClick={exportSimulation}
                className="flex items-center space-x-2 px-4 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 transition-colors"
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <Download className="h-4 w-4" />
                <span>Export</span>
              </motion.button>
            </>
          )}
        </div>

        {/* Interactive Transition Selector */}
        <AnimatePresence>
          {showTransitionSelector && enabledTransitions.length > 0 && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="mt-4 p-4 bg-orange-50 border border-orange-200 rounded-lg"
            >
              <h4 className="text-sm font-medium text-orange-800 mb-3">
                Select Transition to Fire ({enabledTransitions.length} enabled):
              </h4>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2">
                {enabledTransitions.map((transition) => (
                  <motion.button
                    key={transition.id}
                    onClick={() => fireTransition(transition.id)}
                    className="flex items-center space-x-2 p-3 bg-white border border-orange-300 rounded-lg text-left hover:bg-orange-50 transition-colors"
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                  >
                    <ChevronRight className="h-4 w-4 text-orange-600" />
                    <div>
                      <div className="font-medium text-gray-900">
                        {transition.name || transition.id}
                      </div>
                      {breakpoints.has(transition.id) && (
                        <div className="text-xs text-red-600">● Breakpoint</div>
                      )}
                    </div>
                  </motion.button>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* No enabled transitions message */}
        {enabledTransitions.length === 0 && simulationState !== SIMULATION_STATES.COMPLETED && (
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

export default AdvancedSimulationControls;