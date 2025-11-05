import React, { useEffect, useRef, useState, useCallback } from 'react';
import cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ZoomIn, ZoomOut, RotateCcw, Download, Maximize2,
  Play, Pause, SkipForward, RefreshCw, Activity,
  CheckCircle, AlertTriangle, Loader, Zap
} from 'lucide-react';
import toast from 'react-hot-toast';
import apiService from '../services/api';

// Register dagre layout
cytoscape.use(dagre);

const RealTimePetriNetVisualizer = ({
  petriNet,
  onPetriNetChange,
  highlightedElements = [],
  onElementClick,
  onElementHover,
  onCrossHighlight,
  className = "",
  showControls = true,
  enableRealTime = true,
  readOnly = false
}) => {
  const cyRef = useRef(null);
  const cyInstance = useRef(null);
  const webSocketRef = useRef(null);

  // Visualization state
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState('disconnected');

  // Real-time simulation state
  const [simulationState, setSimulationState] = useState('idle');
  const [currentMarking, setCurrentMarking] = useState({});
  const [stepCount, setStepCount] = useState(0);
  const [validationProgress, setValidationProgress] = useState(null);
  const [performanceMetrics, setPerformanceMetrics] = useState({});

  // Animation state
  const [animatingTokens, setAnimatingTokens] = useState(new Map());
  const animationQueue = useRef([]);
  const animationTimer = useRef(null);

  // Initialize Cytoscape
  useEffect(() => {
    if (!petriNet || !cyRef.current) return;

    const cy = cytoscape({
      container: cyRef.current,
      elements: convertPetriNetToCytoscapeElements(petriNet),
      style: getCytoscapeStyle(),
      layout: {
        name: 'dagre',
        directed: true,
        padding: 30,
        spacingFactor: 1.5,
        rankSep: 120,
        nodeSep: 80,
        edgeSep: 20,
        ranker: 'tight-tree'
      },
      wheelSensitivity: 0.2,
      minZoom: 0.1,
      maxZoom: 4,
      boxSelectionEnabled: !readOnly,
      panningEnabled: true,
      userZoomingEnabled: true,
      userPanningEnabled: true
    });

    cyInstance.current = cy;

    // Add event listeners for interaction
    if (!readOnly) {
      cy.on('tap', 'node, edge', (evt) => {
        const element = evt.target;
        handleElementInteraction(element, 'click');
      });

      cy.on('mouseover', 'node, edge', (evt) => {
        const element = evt.target;
        handleElementInteraction(element, 'hover');
      });

      cy.on('mouseout', 'node, edge', (evt) => {
        const element = evt.target;
        handleElementInteraction(element, 'unhover');
      });
    }

    // Fit to viewport
    setTimeout(() => {
      if (cy && cy.elements().length > 0) {
        cy.fit();
        cy.center();
      }
    }, 100);

    return () => {
      if (cyInstance.current) {
        cyInstance.current.destroy();
        cyInstance.current = null;
      }
    };
  }, [petriNet, readOnly]);

  // Initialize WebSocket connection for real-time updates
  useEffect(() => {
    if (!enableRealTime) return;

    const callbacks = {
      onConnect: () => {
        setIsConnected(true);
        setConnectionStatus('connected');
        toast.success('Connected to real-time P3Net service');
      },

      onConnectionEstablished: (message) => {
        console.log('P3Net connection established:', message);

        // Subscribe to all relevant events
        if (webSocketRef.current) {
          webSocketRef.current.subscribe([
            'TOKEN_MOVEMENT', 'MARKING_UPDATE', 'SIMULATION_STARTED',
            'SIMULATION_COMPLETED', 'VALIDATION_PROGRESS', 'VALIDATION_COMPLETED'
          ]);
        }
      },

      onTokenMovement: (message) => {
        console.log('Token movement:', message);
        animateTokenMovement(message);
      },

      onMarkingUpdate: (message) => {
        console.log('Marking update:', message);
        setCurrentMarking(message.marking);
        updateTokenDisplay(message.marking);
      },

      onSimulationStarted: (message) => {
        console.log('Simulation started:', message);
        setSimulationState('running');
        setStepCount(0);
        if (message.initialMarking) {
          setCurrentMarking(message.initialMarking);
          updateTokenDisplay(message.initialMarking);
        }
        toast.success('Real-time simulation started');
      },

      onSimulationCompleted: (message) => {
        console.log('Simulation completed:', message);
        setSimulationState('completed');
        setStepCount(message.totalSteps || 0);

        const statusMessage = message.finalStatus === 'SUCCESS'
          ? 'Simulation completed successfully'
          : `Simulation ended: ${message.finalStatus}`;

        toast.success(statusMessage);
      },

      onValidationStarted: (message) => {
        console.log('Validation started:', message);
        setValidationProgress({ progress: 0, currentCheck: 'Starting validation...' });
      },

      onValidationProgress: (message) => {
        console.log('Validation progress:', message);
        setValidationProgress({
          progress: message.progress || 0,
          statesExplored: message.statesExplored || 0,
          currentCheck: message.currentCheck || 'Validating...'
        });
      },

      onValidationCompleted: (message) => {
        console.log('Validation completed:', message);
        setValidationProgress(null);

        const isValid = message.result?.isValid;
        toast[isValid ? 'success' : 'error'](
          isValid ? 'Validation passed' : 'Validation failed'
        );
      },

      onError: (error) => {
        console.error('P3Net WebSocket error:', error);
        toast.error(`P3Net error: ${error.error?.message || 'Connection error'}`);
        setConnectionStatus('error');
      },

      onClose: (event) => {
        setIsConnected(false);
        setConnectionStatus('disconnected');
        if (event.code !== 1000) { // Not a normal closure
          toast.error('Connection to P3Net service lost');
        }
      }
    };

    try {
      webSocketRef.current = apiService.petri.createWebSocketConnection(callbacks);
    } catch (error) {
      console.error('Failed to create WebSocket connection:', error);
      setConnectionStatus('error');
      toast.error('Failed to connect to real-time service');
    }

    return () => {
      if (webSocketRef.current && webSocketRef.current.readyState === WebSocket.OPEN) {
        webSocketRef.current.close();
      }
      webSocketRef.current = null;
    };
  }, [enableRealTime]);

  // Update highlighting
  useEffect(() => {
    if (!cyInstance.current) return;

    const cy = cyInstance.current;
    cy.elements().removeClass('highlighted cross-highlighted');

    if (highlightedElements && highlightedElements.length > 0) {
      highlightedElements.forEach(elementId => {
        const element = cy.getElementById(elementId);
        if (element && element.length > 0) {
          element.addClass('highlighted');
        }
      });
    }
  }, [highlightedElements]);

  // Handle element interactions
  const handleElementInteraction = useCallback((element, action) => {
    const elementData = {
      id: element.id(),
      type: element.data('type'),
      data: element.data(),
      action
    };

    if (action === 'click' && onElementClick) {
      onElementClick(elementData);
    } else if ((action === 'hover' || action === 'unhover') && onElementHover) {
      onElementHover(elementData);
    }

    // Trigger cross-highlighting
    if (onCrossHighlight && action === 'hover') {
      const mappedElements = mapPetriElementToDAG(elementData);
      onCrossHighlight(mappedElements, action);
    }
  }, [onElementClick, onElementHover, onCrossHighlight]);

  // Real-time token animation
  const animateTokenMovement = useCallback((tokenMovement) => {
    if (!cyInstance.current) return;

    const cy = cyInstance.current;
    const fromPlace = cy.getElementById(tokenMovement.fromPlace);
    const toPlace = cy.getElementById(tokenMovement.toPlace);

    if (fromPlace.length === 0 || toPlace.length === 0) return;

    // Create flying token animation
    createFlyingToken(
      fromPlace.renderedPosition(),
      toPlace.renderedPosition(),
      tokenMovement.tokens || 1
    );

    // Add visual feedback to places
    fromPlace.addClass('token-removing');
    toPlace.addClass('token-adding');

    setTimeout(() => {
      fromPlace.removeClass('token-removing');
      toPlace.removeClass('token-adding');
    }, 800);
  }, []);

  // Update token display on places
  const updateTokenDisplay = useCallback((marking) => {
    if (!cyInstance.current) return;

    const cy = cyInstance.current;

    // Update all places
    cy.elements('.place').forEach(place => {
      const placeId = place.id();
      const tokens = marking[placeId] || 0;

      place.data('tokens', tokens);

      if (tokens > 0) {
        place.addClass('has-tokens');
        if (tokens > 1) {
          place.style('label', `${place.data('originalLabel') || place.data('label')} (${tokens})`);
        } else {
          place.style('label', place.data('originalLabel') || place.data('label'));
        }
      } else {
        place.removeClass('has-tokens');
        place.style('label', place.data('originalLabel') || place.data('label'));
      }
    });
  }, []);

  // Create flying token animation
  const createFlyingToken = useCallback((startPos, endPos, tokenCount) => {
    if (!cyRef.current) return;

    for (let i = 0; i < tokenCount; i++) {
      setTimeout(() => {
        const token = document.createElement('div');
        token.className = 'flying-token';
        token.style.cssText = `
          position: absolute;
          width: 12px;
          height: 12px;
          border-radius: 50%;
          background: linear-gradient(135deg, #3b82f6, #1d4ed8);
          border: 2px solid #1e40af;
          left: ${startPos.x}px;
          top: ${startPos.y}px;
          z-index: 1000;
          pointer-events: none;
          transition: all 1000ms cubic-bezier(0.25, 0.46, 0.45, 0.94);
          box-shadow: 0 2px 8px rgba(59, 130, 246, 0.4);
        `;

        cyRef.current.appendChild(token);

        // Animate to end position
        requestAnimationFrame(() => {
          token.style.left = `${endPos.x}px`;
          token.style.top = `${endPos.y}px`;
          token.style.transform = 'scale(0.8)';
          token.style.opacity = '0.7';
        });

        // Remove after animation
        setTimeout(() => {
          if (token.parentNode) {
            token.parentNode.removeChild(token);
          }
        }, 1200);
      }, i * 100);
    }
  }, []);

  // Real-time control functions
  const startRealTimeSimulation = useCallback(() => {
    if (!webSocketRef.current || !petriNet) {
      toast.error('WebSocket not connected or no Petri net loaded');
      return;
    }

    const config = {
      maxSteps: 100,
      stepDelayMs: 1000,
      mode: 'deterministic'
    };

    webSocketRef.current.startSimulation(petriNet, config);
    setSimulationState('starting');
  }, [petriNet]);

  const stepRealTimeSimulation = useCallback(() => {
    if (!webSocketRef.current) {
      toast.error('WebSocket not connected');
      return;
    }

    webSocketRef.current.stepSimulation();
  }, []);

  const pauseRealTimeSimulation = useCallback(() => {
    if (!webSocketRef.current) {
      toast.error('WebSocket not connected');
      return;
    }

    webSocketRef.current.pauseSimulation();
    setSimulationState('paused');
  }, []);

  const resetRealTimeSimulation = useCallback(() => {
    if (!webSocketRef.current) {
      toast.error('WebSocket not connected');
      return;
    }

    webSocketRef.current.resetSimulation();
    setSimulationState('idle');
    setStepCount(0);
    setCurrentMarking({});
  }, []);

  const startRealTimeValidation = useCallback(() => {
    if (!webSocketRef.current) {
      toast.error('WebSocket not connected');
      return;
    }

    const config = {
      enableDeadlockCheck: true,
      enableReachabilityCheck: true,
      enableLivenessCheck: true,
      maxStates: 10000
    };

    webSocketRef.current.startValidation(config);
  }, []);

  // Control functions for basic visualization
  const handleZoomIn = () => cyInstance.current?.zoom(cyInstance.current.zoom() * 1.25);
  const handleZoomOut = () => cyInstance.current?.zoom(cyInstance.current.zoom() * 0.8);
  const handleReset = () => {
    if (cyInstance.current) {
      cyInstance.current.fit();
      cyInstance.current.center();
    }
  };

  const handleDownload = () => {
    if (cyInstance.current) {
      const png = cyInstance.current.png({
        output: 'blob',
        bg: '#ffffff',
        full: true,
        scale: 2
      });

      const link = document.createElement('a');
      link.href = URL.createObjectURL(png);
      link.download = `petri-net-${petriNet?.name || 'diagram'}.png`;
      link.click();
      URL.revokeObjectURL(link.href);
    }
  };

  // Get connection status display
  const getConnectionStatusDisplay = () => {
    switch (connectionStatus) {
      case 'connected':
        return { icon: CheckCircle, color: 'text-green-600', bg: 'bg-green-100' };
      case 'error':
        return { icon: AlertTriangle, color: 'text-red-600', bg: 'bg-red-100' };
      case 'connecting':
        return { icon: Loader, color: 'text-yellow-600', bg: 'bg-yellow-100' };
      default:
        return { icon: Activity, color: 'text-gray-600', bg: 'bg-gray-100' };
    }
  };

  const statusDisplay = getConnectionStatusDisplay();
  const StatusIcon = statusDisplay.icon;

  // Convert Petri net data to Cytoscape elements
  const convertPetriNetToCytoscapeElements = (petriNet) => {
    const elements = [];

    // Add places (circles) with test attributes
    if (petriNet.places) {
      petriNet.places.forEach(place => {
        elements.push({
          data: {
            id: place.id,
            label: place.name || place.id,
            originalLabel: place.name || place.id,
            type: 'place',
            capacity: place.capacity,
            tokens: petriNet.initialMarking?.[place.id] || 0,
            originalData: place,
            // Test attributes
            'data-type': 'place',
            'data-element-id': place.id,
            'data-element-name': place.name || place.id
          },
          classes: 'place petri-place'
        });
      });
    }

    // Add transitions (rectangles) with test attributes
    if (petriNet.transitions) {
      petriNet.transitions.forEach(transition => {
        elements.push({
          data: {
            id: transition.id,
            label: transition.name || transition.id,
            type: 'transition',
            originalData: transition,
            // Test attributes
            'data-type': 'transition',
            'data-element-id': transition.id,
            'data-element-name': transition.name || transition.id
          },
          classes: 'transition petri-transition'
        });
      });
    }

    // Add arcs (edges) with test attributes
    if (petriNet.arcs) {
      petriNet.arcs.forEach((arc, index) => {
        elements.push({
          data: {
            id: `arc_${index}`,
            source: arc.from,
            target: arc.to,
            weight: arc.weight || 1,
            type: 'arc',
            originalData: arc,
            // Test attributes
            'data-type': 'edge',
            'data-from': arc.from,
            'data-to': arc.to
          },
          classes: 'arc petri-arc'
        });
      });
    }

    return elements;
  };

  // Enhanced Cytoscape styles with real-time animations
  const getCytoscapeStyle = () => [
    // Place styles
    {
      selector: '.place',
      style: {
        'shape': 'ellipse',
        'width': '70px',
        'height': '70px',
        'background-color': '#f8fafc',
        'border-width': '2px',
        'border-color': '#64748b',
        'label': 'data(label)',
        'text-valign': 'center',
        'text-halign': 'center',
        'font-size': '12px',
        'font-weight': '500',
        'color': '#1e293b',
        'text-wrap': 'wrap',
        'text-max-width': '60px',
        'transition-property': 'background-color, border-color, border-width',
        'transition-duration': '0.3s'
      }
    },
    // Place with tokens
    {
      selector: '.place.has-tokens',
      style: {
        'background-color': '#dbeafe',
        'border-color': '#3b82f6',
        'border-width': '3px'
      }
    },
    // Token animation states
    {
      selector: '.place.token-removing',
      style: {
        'background-color': '#fecaca',
        'border-color': '#ef4444',
        'border-width': '4px'
      }
    },
    {
      selector: '.place.token-adding',
      style: {
        'background-color': '#bbf7d0',
        'border-color': '#10b981',
        'border-width': '4px'
      }
    },
    // Transition styles
    {
      selector: '.transition',
      style: {
        'shape': 'rectangle',
        'width': '90px',
        'height': '45px',
        'background-color': '#fef3c7',
        'border-width': '2px',
        'border-color': '#d97706',
        'label': 'data(label)',
        'text-valign': 'center',
        'text-halign': 'center',
        'font-size': '11px',
        'font-weight': '500',
        'color': '#92400e',
        'text-wrap': 'wrap',
        'text-max-width': '80px'
      }
    },
    // Arc styles
    {
      selector: '.arc',
      style: {
        'width': '2px',
        'line-color': '#64748b',
        'target-arrow-color': '#64748b',
        'target-arrow-shape': 'triangle',
        'target-arrow-size': '10px',
        'curve-style': 'bezier',
        'edge-text-rotation': 'autorotate',
        'font-size': '10px',
        'color': '#64748b'
      }
    },
    // Highlighted elements
    {
      selector: '.highlighted',
      style: {
        'border-width': '4px',
        'border-color': '#8b5cf6',
        'background-color': '#ede9fe',
        'line-color': '#8b5cf6',
        'target-arrow-color': '#8b5cf6',
        'z-index': 999
      }
    }
  ];

  // Map Petri elements to DAG elements
  const mapPetriElementToDAG = (element) => {
    if (element.type === 'transition') {
      return [{
        id: element.data.originalData?.dagNodeId || element.id,
        type: 'node'
      }];
    } else if (element.type === 'place') {
      return [{
        id: element.data.originalData?.edgeId || element.id,
        type: 'edge'
      }];
    }
    return [];
  };

  if (!petriNet) {
    return (
      <div className={`flex items-center justify-center h-96 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300 ${className}`}>
        <div className="text-center">
          <Activity className="h-12 w-12 mx-auto mb-2 text-gray-400" />
          <p className="text-sm text-gray-600">No Petri net data to visualize</p>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`relative bg-white rounded-xl border border-gray-200 shadow-lg ${isFullscreen ? 'fixed inset-4 z-50' : ''} ${className}`}
    >
      {/* Header with connection status */}
      {enableRealTime && (
        <div className="absolute top-4 left-4 z-10 flex items-center space-x-3">
          <div className={`flex items-center space-x-2 px-3 py-2 rounded-lg shadow-md ${statusDisplay.bg}`}>
            <StatusIcon className={`h-4 w-4 ${statusDisplay.color}`} />
            <span className={`text-sm font-medium ${statusDisplay.color}`}>
              {connectionStatus.charAt(0).toUpperCase() + connectionStatus.slice(1)}
            </span>
          </div>

          {simulationState !== 'idle' && (
            <div className="bg-white rounded-lg shadow-md px-3 py-2">
              <span className="text-sm text-gray-600">Step: {stepCount}</span>
            </div>
          )}
        </div>
      )}

      {/* Real-time Controls */}
      {enableRealTime && showControls && (
        <div className="absolute top-4 right-4 z-10 flex flex-wrap gap-2">
          <motion.button
            onClick={startRealTimeSimulation}
            disabled={!isConnected || simulationState === 'running'}
            className="p-2 bg-green-600 text-white rounded-lg shadow-md hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            title="Start Real-time Simulation"
          >
            <Play className="h-4 w-4" />
          </motion.button>

          <motion.button
            onClick={stepRealTimeSimulation}
            disabled={!isConnected}
            className="p-2 bg-blue-600 text-white rounded-lg shadow-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            title="Step Simulation"
          >
            <SkipForward className="h-4 w-4" />
          </motion.button>

          <motion.button
            onClick={pauseRealTimeSimulation}
            disabled={!isConnected || simulationState !== 'running'}
            className="p-2 bg-yellow-600 text-white rounded-lg shadow-md hover:bg-yellow-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            title="Pause Simulation"
          >
            <Pause className="h-4 w-4" />
          </motion.button>

          <motion.button
            onClick={resetRealTimeSimulation}
            disabled={!isConnected}
            className="p-2 bg-gray-600 text-white rounded-lg shadow-md hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            title="Reset Simulation"
          >
            <RefreshCw className="h-4 w-4" />
          </motion.button>

          <motion.button
            onClick={startRealTimeValidation}
            disabled={!isConnected}
            className="p-2 bg-purple-600 text-white rounded-lg shadow-md hover:bg-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            title="Start Validation"
          >
            <Zap className="h-4 w-4" />
          </motion.button>
        </div>
      )}

      {/* Basic visualization controls */}
      {showControls && (
        <div className="absolute bottom-4 right-4 z-10 flex space-x-2">
          <motion.button
            onClick={handleZoomIn}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            whileHover={{ scale: 1.05 }}
            title="Zoom In"
          >
            <ZoomIn className="h-4 w-4 text-gray-600" />
          </motion.button>

          <motion.button
            onClick={handleZoomOut}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            whileHover={{ scale: 1.05 }}
            title="Zoom Out"
          >
            <ZoomOut className="h-4 w-4 text-gray-600" />
          </motion.button>

          <motion.button
            onClick={handleReset}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            whileHover={{ scale: 1.05 }}
            title="Reset View"
          >
            <RotateCcw className="h-4 w-4 text-gray-600" />
          </motion.button>

          <motion.button
            onClick={handleDownload}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            whileHover={{ scale: 1.05 }}
            title="Download"
          >
            <Download className="h-4 w-4 text-gray-600" />
          </motion.button>

          <motion.button
            onClick={() => setIsFullscreen(!isFullscreen)}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            whileHover={{ scale: 1.05 }}
            title="Fullscreen"
          >
            <Maximize2 className="h-4 w-4 text-gray-600" />
          </motion.button>
        </div>
      )}

      {/* Validation Progress */}
      <AnimatePresence>
        {validationProgress && (
          <motion.div
            initial={{ opacity: 0, y: -20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="absolute top-16 left-4 right-4 z-10 bg-white rounded-lg shadow-lg border border-gray-200 p-4"
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-900">
                Validation Progress
              </span>
              <span className="text-sm text-gray-600">
                {validationProgress.progress}%
              </span>
            </div>
            <div className="w-full bg-gray-200 rounded-full h-2 mb-2">
              <div
                className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${validationProgress.progress}%` }}
              />
            </div>
            <div className="flex justify-between text-xs text-gray-500">
              <span>{validationProgress.currentCheck}</span>
              <span>States: {validationProgress.statesExplored || 0}</span>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Cytoscape Container with Test Attributes */}
      <div className="relative">
        <div
          ref={cyRef}
          className={`w-full ${isFullscreen ? 'h-full' : 'h-96'} rounded-xl petri-net-workflow`}
          style={{ background: '#fafafa' }}
          data-petri-net-visualization="true"
        />

        {/* Semantic SVG overlay for test selectors (hidden from view) */}
        <svg
          className="absolute inset-0 pointer-events-none opacity-0"
          data-test-overlay="petri-net"
          aria-hidden="true"
        >
          {petriNet && (
            <>
              {/* Places */}
              {petriNet.places?.map(place => (
                <circle
                  key={place.id}
                  data-type="place"
                  data-element-id={place.id}
                  data-state={place.name || place.id}
                  className="petri-place"
                  r="1"
                />
              ))}

              {/* Transitions */}
              {petriNet.transitions?.map(transition => (
                <rect
                  key={transition.id}
                  data-type="transition"
                  data-element-id={transition.id}
                  data-action={transition.name || transition.id}
                  className="petri-transition"
                  width="1"
                  height="1"
                />
              ))}

              {/* Arcs/Edges */}
              {petriNet.arcs?.map((arc, index) => (
                <line
                  key={index}
                  data-type="edge"
                  data-from={arc.from}
                  data-to={arc.to}
                  className="petri-arc"
                  x1="0"
                  y1="0"
                  x2="1"
                  y2="1"
                />
              ))}

              {/* Labels */}
              {petriNet.transitions?.map(transition => (
                <text
                  key={`label-${transition.id}`}
                  className="label"
                  data-label-for={transition.id}
                >
                  {transition.name || transition.id}
                </text>
              ))}
            </>
          )}
        </svg>
      </div>

      {/* Fullscreen overlay */}
      {isFullscreen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40"
          onClick={() => setIsFullscreen(false)}
        />
      )}
    </motion.div>
  );
};

export default RealTimePetriNetVisualizer;