import React, { useEffect, useRef, useState, useCallback } from 'react';
import cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import { motion } from 'framer-motion';
import { ZoomIn, ZoomOut, RotateCcw, Download, Maximize2, Play, Pause, SkipForward } from 'lucide-react';

// Register dagre layout
cytoscape.use(dagre);

const PetriNetVisualizer = ({
  petriNet,
  highlightedElements = [],
  currentMarking = {},
  animationSpeed = 1,
  traceEvents = [],
  simulationState = 'idle',
  onElementClick,
  onElementHover,
  onCrossHighlight,
  className = "",
  showControls = true,
  readonly = false,
  enableAnimation = true,
  highContrastMode = false
}) => {
  const cyRef = useRef(null);
  const cyInstance = useRef(null);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [animatingTokens, setAnimatingTokens] = useState(new Map());
  const animationQueue = useRef([]);
  const animationTimer = useRef(null);
  const lastProcessedEvent = useRef(-1);

  useEffect(() => {
    if (!petriNet || !cyRef.current) return;

    // Initialize Cytoscape instance
    const cy = cytoscape({
      container: cyRef.current,
      elements: convertPetriNetToCytoscapeElements(petriNet),
      style: getCytoscapeStyle(),
      layout: {
        name: 'dagre',
        directed: true,
        padding: 20,
        spacingFactor: 1.25,
        rankSep: 100,
        nodeSep: 50,
        edgeSep: 10,
        ranker: 'tight-tree'
      },
      wheelSensitivity: 0.2,
      minZoom: 0.1,
      maxZoom: 3,
      boxSelectionEnabled: !readonly,
      panningEnabled: true,
      userZoomingEnabled: true,
      userPanningEnabled: true
    });

    cyInstance.current = cy;

    // Add event listeners
    cy.on('tap', 'node, edge', (evt) => {
      const element = evt.target;
      const elementData = {
        id: element.id(),
        type: element.data('type'),
        data: element.data()
      };
      handleElementClick(elementData);
    });

    cy.on('mouseover', 'node, edge', (evt) => {
      const element = evt.target;
      const elementData = {
        id: element.id(),
        type: element.data('type'),
        data: element.data(),
        action: 'hover'
      };
      handleElementHover(elementData);
    });

    cy.on('mouseout', 'node, edge', (evt) => {
      const element = evt.target;
      const elementData = {
        id: element.id(),
        type: element.data('type'),
        data: element.data(),
        action: 'unhover'
      };
      handleElementHover(elementData);
    });

    // Fit to viewport
    setTimeout(() => {
      if (cy && cy.elements().length > 0) {
        cy.fit();
        cy.center();
      }
    }, 100);

    // Cleanup
    return () => {
      if (animationTimer.current) {
        clearTimeout(animationTimer.current);
        animationTimer.current = null;
      }
      if (cyInstance.current) {
        cyInstance.current.destroy();
        cyInstance.current = null;
      }
    };
  }, [petriNet, readonly, handleElementClick, handleElementHover]);

  // Update highlighting when highlightedElements changes
  useEffect(() => {
    if (!cyInstance.current) return;

    const cy = cyInstance.current;

    // Reset all highlighting
    cy.elements().removeClass('highlighted cross-highlighted');

    // Apply new highlighting
    if (highlightedElements && highlightedElements.length > 0) {
      highlightedElements.forEach(elementId => {
        const element = cy.getElementById(elementId);
        if (element && element.length > 0) {
          element.addClass('highlighted');
        }
      });
    }
  }, [highlightedElements]);

  // Process new trace events for animation
  useEffect(() => {
    if (!enableAnimation || !cyInstance.current || traceEvents.length === 0) return;

    const newEvents = traceEvents.slice(lastProcessedEvent.current + 1);
    if (newEvents.length === 0) return;

    newEvents.forEach(event => {
      if (event.eventType === 'transition_fired' && event.tokenMovements) {
        queueTokenAnimation(event);
      }
    });

    lastProcessedEvent.current = traceEvents.length - 1;
  }, [traceEvents, enableAnimation]);

  // Update token display based on current marking
  useEffect(() => {
    if (!cyInstance.current) return;

    const cy = cyInstance.current;

    // Update token counts and styles for all places
    Object.keys(currentMarking).forEach(placeId => {
      const place = cy.getElementById(placeId);
      if (place && place.length > 0) {
        const tokens = currentMarking[placeId] || 0;
        place.data('tokens', tokens);

        // Update visual representation
        if (tokens > 0) {
          place.addClass('has-tokens');
        } else {
          place.removeClass('has-tokens');
        }
      }
    });

    // Update token labels for places with multiple tokens
    cy.elements('.place').forEach(place => {
      const tokens = place.data('tokens') || 0;
      if (tokens > 1) {
        place.style('label', `${place.data('label')} (${tokens})`);
      } else {
        place.style('label', place.data('label'));
      }
    });
  }, [currentMarking]);

  // Queue token animation
  const queueTokenAnimation = useCallback((event) => {
    if (!event.tokenMovements) return;

    const animationSteps = [];

    // Create animation steps for token movements
    event.tokenMovements.removed?.forEach(movement => {
      animationSteps.push({
        type: 'token_removal',
        placeId: movement.placeId,
        tokens: movement.tokens,
        transitionId: event.transitionId,
        delay: 0
      });
    });

    event.tokenMovements.added?.forEach(movement => {
      animationSteps.push({
        type: 'token_addition',
        placeId: movement.placeId,
        tokens: movement.tokens,
        transitionId: event.transitionId,
        delay: 300 / animationSpeed // Add tokens after removal
      });
    });

    // Add to animation queue
    animationQueue.current.push(...animationSteps);

    // Start processing if not already running
    if (!animationTimer.current) {
      processAnimationQueue();
    }
  }, [animationSpeed]);

  // Process animation queue
  const processAnimationQueue = useCallback(() => {
    if (animationQueue.current.length === 0) {
      animationTimer.current = null;
      return;
    }

    const step = animationQueue.current.shift();
    if (!step) return;

    // Execute animation step after delay
    setTimeout(() => {
      executeAnimationStep(step);

      // Continue processing queue
      const nextDelay = 500 / animationSpeed; // Base delay between steps
      animationTimer.current = setTimeout(processAnimationQueue, nextDelay);
    }, step.delay);
  }, [animationSpeed]);

  // Execute individual animation step
  const executeAnimationStep = useCallback((step) => {
    if (!cyInstance.current) return;

    const cy = cyInstance.current;
    const place = cy.getElementById(step.placeId);
    const transition = cy.getElementById(step.transitionId);

    if (!place || place.length === 0) return;

    switch (step.type) {
      case 'token_removal':
        animateTokenRemoval(place, transition, step.tokens);
        break;
      case 'token_addition':
        animateTokenAddition(place, transition, step.tokens);
        break;
    }
  }, []);

  // Animate token removal from place
  const animateTokenRemoval = useCallback((place, transition, tokenCount) => {
    // Create visual token elements
    const placePosition = place.renderedPosition();
    const transitionPosition = transition ? transition.renderedPosition() : placePosition;

    for (let i = 0; i < tokenCount; i++) {
      setTimeout(() => {
        createFlyingToken(placePosition, transitionPosition, 'removal');
      }, i * (100 / animationSpeed));
    }

    // Add pulsing effect to place
    place.addClass('token-removing');
    setTimeout(() => {
      place.removeClass('token-removing');
    }, 600 / animationSpeed);
  }, [animationSpeed]);

  // Animate token addition to place
  const animateTokenAddition = useCallback((place, transition, tokenCount) => {
    // Create visual token elements
    const placePosition = place.renderedPosition();
    const transitionPosition = transition ? transition.renderedPosition() : placePosition;

    for (let i = 0; i < tokenCount; i++) {
      setTimeout(() => {
        createFlyingToken(transitionPosition, placePosition, 'addition');
      }, i * (100 / animationSpeed));
    }

    // Add pulsing effect to place
    place.addClass('token-adding');
    setTimeout(() => {
      place.removeClass('token-adding');
    }, 600 / animationSpeed);
  }, [animationSpeed]);

  // Create flying token animation
  const createFlyingToken = useCallback((startPos, endPos, type) => {
    if (!cyRef.current) return;

    const container = cyRef.current;
    const containerRect = container.getBoundingClientRect();

    // Create token element
    const token = document.createElement('div');
    token.className = `flying-token ${type}`;
    token.style.cssText = `
      position: absolute;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: ${type === 'removal' ? '#ef4444' : '#10b981'};
      border: 2px solid ${type === 'removal' ? '#dc2626' : '#059669'};
      left: ${startPos.x}px;
      top: ${startPos.y}px;
      z-index: 1000;
      pointer-events: none;
      transition: all ${800 / animationSpeed}ms cubic-bezier(0.25, 0.46, 0.45, 0.94);
      opacity: 1;
      transform: scale(1);
    `;

    container.appendChild(token);

    // Animate to end position
    requestAnimationFrame(() => {
      token.style.left = `${endPos.x}px`;
      token.style.top = `${endPos.y}px`;
      token.style.transform = 'scale(0.5)';
      token.style.opacity = '0';
    });

    // Remove token after animation
    setTimeout(() => {
      if (token.parentNode) {
        token.parentNode.removeChild(token);
      }
    }, 1000 / animationSpeed);
  }, [animationSpeed]);

  // Handle cross-highlighting with DAG view
  const handleElementClick = useCallback((element) => {
    if (onElementClick) {
      onElementClick(element);
    }

    // Trigger cross-highlighting
    if (onCrossHighlight) {
      const mappedElements = mapPetriElementToDAG(element);
      onCrossHighlight(mappedElements);
    }
  }, [onElementClick, onCrossHighlight]);

  const handleElementHover = useCallback((element) => {
    if (onElementHover) {
      onElementHover(element);
    }

    // Trigger cross-highlighting on hover
    if (onCrossHighlight && element.action === 'hover') {
      const mappedElements = mapPetriElementToDAG(element);
      onCrossHighlight(mappedElements, 'hover');
    }
  }, [onElementHover, onCrossHighlight]);

  // Map Petri net elements to DAG elements (implementation depends on mapping contract)
  const mapPetriElementToDAG = useCallback((element) => {
    // This mapping depends on the established contract between views
    // For now, return a simple mapping based on element type and ID
    if (element.type === 'transition') {
      // Transitions map to DAG nodes
      return [{
        id: element.data.originalData?.dagNodeId || element.id,
        type: 'node'
      }];
    } else if (element.type === 'place') {
      // Places map to edge metadata or intermediate states
      return [{
        id: element.data.originalData?.edgeId || element.id,
        type: 'edge'
      }];
    }
    return [];
  }, []);

  const convertPetriNetToCytoscapeElements = (petriNet) => {
    const elements = [];

    // Add places (circles)
    if (petriNet.places) {
      petriNet.places.forEach(place => {
        elements.push({
          data: {
            id: place.id,
            label: place.name || place.id,
            type: 'place',
            capacity: place.capacity,
            tokens: petriNet.initialMarking?.[place.id] || 0,
            originalData: place
          },
          classes: 'place'
        });
      });
    }

    // Add transitions (rectangles)
    if (petriNet.transitions) {
      petriNet.transitions.forEach(transition => {
        elements.push({
          data: {
            id: transition.id,
            label: transition.name || transition.id,
            type: 'transition',
            originalData: transition
          },
          classes: 'transition'
        });
      });
    }

    // Add arcs (edges)
    if (petriNet.arcs) {
      petriNet.arcs.forEach((arc, index) => {
        elements.push({
          data: {
            id: `arc_${index}`,
            source: arc.from,
            target: arc.to,
            weight: arc.weight || 1,
            type: 'arc',
            originalData: arc
          },
          classes: 'arc'
        });
      });
    }

    return elements;
  };

  const getCytoscapeStyle = () => [
    // Place styles (circles)
    {
      selector: '.place',
      style: {
        'shape': 'ellipse',
        'width': '60px',
        'height': '60px',
        'background-color': '#f3f4f6',
        'border-width': '2px',
        'border-color': '#6b7280',
        'label': 'data(label)',
        'text-valign': 'center',
        'text-halign': 'center',
        'font-size': '12px',
        'font-weight': '500',
        'color': '#374151',
        'text-wrap': 'wrap',
        'text-max-width': '50px',
        'transition-property': 'background-color, border-color, border-width',
        'transition-duration': '0.3s',
        'transition-timing-function': 'ease-in-out'
      }
    },
    // Place with tokens (using has-tokens class instead of tokens > 0 for better control)
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
    // Transition styles (rectangles)
    {
      selector: '.transition',
      style: {
        'shape': 'rectangle',
        'width': '80px',
        'height': '40px',
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
        'text-max-width': '70px'
      }
    },
    // Arc styles (edges)
    {
      selector: '.arc',
      style: {
        'width': '2px',
        'line-color': '#6b7280',
        'target-arrow-color': '#6b7280',
        'target-arrow-shape': 'triangle',
        'target-arrow-size': '8px',
        'curve-style': 'bezier',
        'edge-text-rotation': 'autorotate',
        'font-size': '10px',
        'color': '#6b7280'
      }
    },
    // Arc with weight > 1
    {
      selector: '.arc[weight > 1]',
      style: {
        'label': 'data(weight)',
        'text-background-color': '#ffffff',
        'text-background-opacity': 0.8,
        'text-background-padding': '2px'
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
    },
    // Cross-highlighted elements (from other view)
    {
      selector: '.cross-highlighted',
      style: {
        'border-width': '3px',
        'border-color': '#f59e0b',
        'background-color': '#fef3c7',
        'line-color': '#f59e0b',
        'target-arrow-color': '#f59e0b',
        'z-index': 998
      }
    },
    // Hover effects
    {
      selector: 'node:selected, edge:selected',
      style: {
        'border-width': '3px',
        'border-color': '#10b981',
        'line-color': '#10b981',
        'target-arrow-color': '#10b981'
      }
    }
  ];

  const handleZoomIn = () => {
    if (cyInstance.current) {
      cyInstance.current.zoom(cyInstance.current.zoom() * 1.25);
    }
  };

  const handleZoomOut = () => {
    if (cyInstance.current) {
      cyInstance.current.zoom(cyInstance.current.zoom() * 0.8);
    }
  };

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

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen);
  };

  if (!petriNet) {
    return (
      <div className={`flex items-center justify-center h-96 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300 ${className}`}
           role="img"
           aria-label="Empty Petri net visualization area">
        <div className="text-center">
          {/* eslint-disable-next-line no-undef */}
          <div className={`mb-2 ${highContrastMode ? 'text-black' : 'text-gray-400'}`}>
            <svg className="h-12 w-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 20l-5.447-2.724A1 1 0 013 16.382V5.618a1 1 0 011.447-.894L9 7m0 13l6-3m-6 3V7m6 10l4.553 2.276A1 1 0 0121 18.382V7.618a1 1 0 00-.553-.894L15 4m0 13V4m0 0L9 7" />
            </svg>
          </div>
          {/* eslint-disable-next-line no-undef */}
          <p className={`text-sm ${highContrastMode ? 'text-black' : 'text-gray-600'}`}>No Petri net data to visualize</p>
        </div>
      </div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className={`relative bg-white rounded-lg border border-gray-200 ${isFullscreen ? 'fixed inset-4 z-50' : ''} ${className}`}
    >
      {/* Controls */}
      {showControls && (
        <div className="absolute top-4 right-4 z-10 flex space-x-2">
          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={handleZoomIn}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            title="Zoom In"
          >
            <ZoomIn className="h-4 w-4 text-gray-600" />
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={handleZoomOut}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            title="Zoom Out"
          >
            <ZoomOut className="h-4 w-4 text-gray-600" />
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={handleReset}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            title="Reset View"
          >
            <RotateCcw className="h-4 w-4 text-gray-600" />
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={handleDownload}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            title="Download as PNG"
          >
            <Download className="h-4 w-4 text-gray-600" />
          </motion.button>

          <motion.button
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
            onClick={toggleFullscreen}
            className="p-2 bg-white rounded-lg shadow-md border border-gray-200 hover:bg-gray-50 transition-colors"
            title={isFullscreen ? "Exit Fullscreen" : "Fullscreen"}
          >
            <Maximize2 className="h-4 w-4 text-gray-600" />
          </motion.button>
        </div>
      )}

      {/* Legend */}
      <div className="absolute top-4 left-4 z-10 bg-white rounded-lg shadow-md border border-gray-200 p-3">
        <h4 className="text-sm font-medium text-gray-900 mb-2">Legend</h4>
        <div className="space-y-1">
          <div className="flex items-center space-x-2">
            <div className="w-4 h-4 rounded-full bg-gray-200 border-2 border-gray-400"></div>
            <span className="text-xs text-gray-600">Place</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-4 h-2 bg-yellow-200 border-2 border-yellow-600"></div>
            <span className="text-xs text-gray-600">Transition</span>
          </div>
          <div className="flex items-center space-x-2">
            <div className="w-4 h-4 rounded-full bg-blue-200 border-2 border-blue-400"></div>
            <span className="text-xs text-gray-600">Has Token</span>
          </div>
        </div>
      </div>

      {/* Cytoscape Container */}
      <div
        ref={cyRef}
        className={`w-full ${isFullscreen ? 'h-full' : 'h-96'} rounded-lg`}
        style={{ background: '#fafafa' }}
      />

      {/* Fullscreen overlay */}
      {isFullscreen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40"
          onClick={toggleFullscreen}
        />
      )}
    </motion.div>
  );
};

export default PetriNetVisualizer;