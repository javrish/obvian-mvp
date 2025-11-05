import React, { useEffect, useRef, useState } from 'react';
import cytoscape from 'cytoscape';
import dagre from 'cytoscape-dagre';
import { motion } from 'framer-motion';
import { ZoomIn, ZoomOut, RotateCcw, Download, Maximize2 } from 'lucide-react';

// Register dagre layout
cytoscape.use(dagre);

const DagVisualizer = ({
  dag,
  highlightedElements = [],
  onElementClick,
  onElementHover,
  className = "",
  showControls = true,
  readonly = false
}) => {
  const cyRef = useRef(null);
  const cyInstance = useRef(null);
  const [isFullscreen, setIsFullscreen] = useState(false);

  useEffect(() => {
    if (!dag || !cyRef.current) return;

    // Initialize Cytoscape instance
    const cy = cytoscape({
      container: cyRef.current,
      elements: convertDagToCytoscapeElements(dag),
      style: getCytoscapeStyle(),
      layout: {
        name: 'dagre',
        directed: true,
        padding: 20,
        spacingFactor: 1.5,
        rankSep: 120,
        nodeSep: 80,
        edgeSep: 10,
        ranker: 'tight-tree',
        rankDir: 'TB'
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
    if (onElementClick) {
      cy.on('tap', 'node, edge', (evt) => {
        const element = evt.target;
        onElementClick({
          id: element.id(),
          type: element.data('type'),
          data: element.data()
        });
      });
    }

    if (onElementHover) {
      cy.on('mouseover', 'node, edge', (evt) => {
        const element = evt.target;
        onElementHover({
          id: element.id(),
          type: element.data('type'),
          data: element.data(),
          action: 'hover'
        });
      });

      cy.on('mouseout', 'node, edge', (evt) => {
        const element = evt.target;
        onElementHover({
          id: element.id(),
          type: element.data('type'),
          data: element.data(),
          action: 'unhover'
        });
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
  }, [dag, onElementClick, onElementHover, readonly]);

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

  const convertDagToCytoscapeElements = (dag) => {
    const elements = [];

    // Add nodes
    if (dag.nodes) {
      dag.nodes.forEach(node => {
        elements.push({
          data: {
            id: node.id,
            label: node.name || node.id,
            type: 'dag_node',
            nodeType: node.type || 'TRANSITION',
            originalData: node
          },
          classes: 'dag-node'
        });
      });
    }

    // Add edges
    if (dag.edges) {
      dag.edges.forEach((edge, index) => {
        elements.push({
          data: {
            id: `dag_edge_${index}`,
            source: edge.from,
            target: edge.to,
            condition: edge.condition,
            type: 'dag_edge',
            originalData: edge
          },
          classes: 'dag-edge'
        });
      });
    }

    return elements;
  };

  const getCytoscapeStyle = () => [
    // DAG Node styles
    {
      selector: '.dag-node',
      style: {
        'shape': 'round-rectangle',
        'width': '100px',
        'height': '50px',
        'background-color': '#e0f2fe',
        'border-width': '2px',
        'border-color': '#0284c7',
        'label': 'data(label)',
        'text-valign': 'center',
        'text-halign': 'center',
        'font-size': '12px',
        'font-weight': '500',
        'color': '#0c4a6e',
        'text-wrap': 'wrap',
        'text-max-width': '90px',
        'padding': '5px'
      }
    },
    // DAG Edge styles
    {
      selector: '.dag-edge',
      style: {
        'width': '3px',
        'line-color': '#0284c7',
        'target-arrow-color': '#0284c7',
        'target-arrow-shape': 'triangle',
        'target-arrow-size': '12px',
        'curve-style': 'bezier',
        'edge-text-rotation': 'autorotate',
        'font-size': '11px',
        'color': '#0c4a6e',
        'text-background-color': '#ffffff',
        'text-background-opacity': 0.8,
        'text-background-padding': '3px'
      }
    },
    // Edge with condition
    {
      selector: '.dag-edge[condition]',
      style: {
        'label': 'data(condition)',
        'line-style': 'dashed'
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
    // Cross-highlighted elements (from Petri net view)
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
      link.download = `dag-${dag?.name || 'diagram'}.png`;
      link.click();
      URL.revokeObjectURL(link.href);
    }
  };

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen);
  };

  if (!dag) {
    return (
      <div className={`flex items-center justify-center h-96 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300 ${className}`}>
        <div className="text-center">
          <div className="text-gray-400 mb-2">
            <svg className="h-12 w-12 mx-auto" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M13 10V3L4 14h7v7l9-11h-7z" />
            </svg>
          </div>
          <p className="text-sm text-gray-600">No DAG data to visualize</p>
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
        <h4 className="text-sm font-medium text-gray-900 mb-2">DAG Legend</h4>
        <div className="space-y-1">
          <div className="flex items-center space-x-2">
            <div className="w-6 h-3 bg-blue-200 border-2 border-blue-600 rounded"></div>
            <span className="text-xs text-gray-600">Task Node</span>
          </div>
          <div className="flex items-center space-x-2">
            <svg className="w-4 h-2" viewBox="0 0 20 8">
              <line x1="0" y1="4" x2="16" y2="4" stroke="#0284c7" strokeWidth="2" markerEnd="url(#arrowhead)" />
              <defs>
                <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                  <polygon points="0 0, 10 3.5, 0 7" fill="#0284c7" />
                </marker>
              </defs>
            </svg>
            <span className="text-xs text-gray-600">Dependency</span>
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

export default DagVisualizer;