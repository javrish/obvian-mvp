import React, { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Split, Eye, EyeOff, RefreshCw, Link, Unlink, Settings,
  GitBranch, Layers, ArrowRightLeft, X, Info, Zap,
  Timer, Activity, AlertCircle, CheckCircle2, Target,
  MousePointer, Maximize, Minimize, ZoomIn, ZoomOut,
  TrendingUp, BarChart3, Clock, Gauge
} from 'lucide-react';
import RealTimePetriNetVisualizer from './RealTimePetriNetVisualizer';
import DagVisualizer from './DagVisualizer';
import toast from 'react-hot-toast';

// Sub-components for enhanced visualization
const ConfidenceIndicator = ({ confidence, size = 'sm', showLabel = true }) => {
  const getConfidenceColor = (conf) => {
    if (conf >= 0.8) return 'text-green-600 bg-green-100';
    if (conf >= 0.6) return 'text-yellow-600 bg-yellow-100';
    return 'text-red-600 bg-red-100';
  };

  const getConfidenceLabel = (conf) => {
    if (conf >= 0.8) return 'High';
    if (conf >= 0.6) return 'Medium';
    return 'Low';
  };

  const sizeClasses = {
    xs: 'w-2 h-2 text-xs',
    sm: 'w-3 h-3 text-xs',
    md: 'w-4 h-4 text-sm',
    lg: 'w-6 h-6 text-base'
  };

  return (
    <div className={`inline-flex items-center space-x-1 ${sizeClasses[size]}`}>
      <div
        className={`rounded-full ${getConfidenceColor(confidence)} ${sizeClasses[size]}`}
        title={`Mapping confidence: ${Math.round(confidence * 100)}%`}
      />
      {showLabel && (
        <span className={`${getConfidenceColor(confidence)} font-medium`}>
          {getConfidenceLabel(confidence)} ({Math.round(confidence * 100)}%)
        </span>
      )}
    </div>
  );
};

const PerformanceMetrics = ({ metrics, className = "" }) => {
  const formatDuration = (ms) => {
    if (ms < 1000) return `${ms.toFixed(0)}ms`;
    return `${(ms / 1000).toFixed(2)}s`;
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      className={`bg-gray-900 text-white rounded-lg p-3 ${className}`}
    >
      <div className="flex items-center space-x-2 mb-2">
        <Gauge className="h-4 w-4" />
        <span className="text-sm font-medium">Performance Metrics</span>
      </div>

      <div className="grid grid-cols-2 gap-3 text-xs">
        <div>
          <div className="text-gray-400">End-to-End</div>
          <div className={`font-bold ${metrics.endToEnd > 2000 ? 'text-red-400' : 'text-green-400'}`}>
            {formatDuration(metrics.endToEnd || 0)}
          </div>
        </div>

        <div>
          <div className="text-gray-400">Simulation Step</div>
          <div className={`font-bold ${metrics.simulationStep > 100 ? 'text-yellow-400' : 'text-green-400'}`}>
            {formatDuration(metrics.simulationStep || 0)}
          </div>
        </div>

        <div>
          <div className="text-gray-400">Mapping Quality</div>
          <div className={`font-bold ${metrics.mappingQuality < 0.8 ? 'text-yellow-400' : 'text-green-400'}`}>
            {Math.round((metrics.mappingQuality || 0) * 100)}%
          </div>
        </div>

        <div>
          <div className="text-gray-400">Frame Rate</div>
          <div className="font-bold text-blue-400">
            {metrics.frameRate || 60}fps
          </div>
        </div>
      </div>
    </motion.div>
  );
};

const PredictiveHoverPreview = ({ element, predictions, position, confidence }) => {
  if (!predictions || predictions.length === 0) return null;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      exit={{ opacity: 0, scale: 0.9 }}
      className="fixed z-50 bg-black bg-opacity-90 text-white rounded-lg p-3 min-w-48 max-w-64"
      style={{
        left: position.x + 15,
        top: position.y - 10,
        pointerEvents: 'none'
      }}
    >
      <div className="flex items-center space-x-2 mb-2">
        <MousePointer className="h-3 w-3" />
        <span className="text-xs font-medium">Predicted Mappings</span>
      </div>

      <div className="space-y-1">
        {predictions.slice(0, 3).map((prediction, idx) => (
          <div key={idx} className="flex items-center justify-between text-xs">
            <span className="truncate">{prediction.id}</span>
            <ConfidenceIndicator
              confidence={prediction.confidence}
              size="xs"
              showLabel={false}
            />
          </div>
        ))}
      </div>

      <div className="mt-2 pt-2 border-t border-gray-600">
        <div className="flex items-center justify-between text-xs">
          <span className="text-gray-400">Overall Confidence</span>
          <ConfidenceIndicator
            confidence={confidence}
            size="xs"
            showLabel={false}
          />
        </div>
      </div>
    </motion.div>
  );
};

const InteractiveValidationPanel = ({ validationIssues, onIssueClick, className = "" }) => {
  if (!validationIssues || validationIssues.length === 0) return null;

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      className={`bg-yellow-50 border-l-4 border-yellow-400 p-4 ${className}`}
    >
      <div className="flex items-center space-x-2 mb-3">
        <AlertCircle className="h-5 w-5 text-yellow-600" />
        <h4 className="text-sm font-medium text-yellow-800">
          Validation Issues ({validationIssues.length})
        </h4>
      </div>

      <div className="space-y-2">
        {validationIssues.map((issue, idx) => (
          <motion.button
            key={idx}
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
            onClick={() => onIssueClick(issue)}
            className="w-full text-left p-2 bg-white rounded border border-yellow-200 hover:border-yellow-300 transition-colors"
          >
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <div className="text-sm font-medium text-gray-900">
                  {issue.type}: {issue.elementId}
                </div>
                <div className="text-xs text-gray-600 mt-1">
                  {issue.description}
                </div>
              </div>
              <div className="ml-2">
                <ConfidenceIndicator
                  confidence={issue.severity === 'critical' ? 0.2 : issue.severity === 'warning' ? 0.6 : 0.9}
                  size="xs"
                  showLabel={false}
                />
              </div>
            </div>
          </motion.button>
        ))}
      </div>
    </motion.div>
  );
};

const SynchronizedZoomControls = ({ onZoomChange, onPanChange, zoomLevel, className = "" }) => {
  const zoomLevels = [0.25, 0.5, 0.75, 1, 1.5, 2, 3];

  return (
    <div className={`flex items-center space-x-2 bg-white border border-gray-300 rounded-lg p-2 ${className}`}>
      <button
        onClick={() => onZoomChange(Math.max(0.1, zoomLevel - 0.25))}
        className="p-1 hover:bg-gray-100 rounded"
        title="Zoom Out"
      >
        <ZoomOut className="h-4 w-4" />
      </button>

      <select
        value={zoomLevel}
        onChange={(e) => onZoomChange(parseFloat(e.target.value))}
        className="px-2 py-1 text-sm border-0 bg-transparent focus:outline-none"
      >
        {zoomLevels.map(level => (
          <option key={level} value={level}>
            {Math.round(level * 100)}%
          </option>
        ))}
      </select>

      <button
        onClick={() => onZoomChange(Math.min(4, zoomLevel + 0.25))}
        className="p-1 hover:bg-gray-100 rounded"
        title="Zoom In"
      >
        <ZoomIn className="h-4 w-4" />
      </button>

      <div className="w-px h-4 bg-gray-300" />

      <button
        onClick={() => { onZoomChange(1); onPanChange({ x: 0, y: 0 }); }}
        className="p-1 hover:bg-gray-100 rounded text-xs"
        title="Reset View"
      >
        <Target className="h-4 w-4" />
      </button>
    </div>
  );
};

const EnhancedDualVisualization = ({
  petriNet,
  dag,
  onPetriNetChange,
  onDagChange,
  className = "",
  onElementSelect,
  initialLayout = 'side-by-side',
  enableRealTime = true,
  enableAdvancedMapping = true,
  showMappingStats = true,
  validationResults = null,
  performanceMetrics = {}
}) => {
  // Layout and display state
  const [layout, setLayout] = useState(initialLayout);
  const [panelSizes, setPanelSizes] = useState({ left: 50, right: 50 });
  const [adaptiveLayoutEnabled, setAdaptiveLayoutEnabled] = useState(true);

  // Enhanced highlighting state
  const [highlightedPetriElements, setHighlightedPetriElements] = useState([]);
  const [highlightedDagElements, setHighlightedDagElements] = useState([]);
  const [crossHighlightingEnabled, setCrossHighlightingEnabled] = useState(true);
  const [selectedElement, setSelectedElement] = useState(null);
  const [hoveredElement, setHoveredElement] = useState(null);

  // Predictive and confidence state
  const [predictivePreview, setPredictivePreview] = useState(null);
  const [mousePosition, setMousePosition] = useState({ x: 0, y: 0 });
  const [mappingConfidence, setMappingConfidence] = useState(new Map());

  // Performance and validation state
  const [showPerformanceMetrics, setShowPerformanceMetrics] = useState(true);
  const [validationIssues, setValidationIssues] = useState([]);
  const [currentPerformanceMetrics, setCurrentPerformanceMetrics] = useState({
    endToEnd: 0,
    simulationStep: 0,
    mappingQuality: 0.9,
    frameRate: 60
  });

  // Zoom and pan synchronization
  const [syncedZoom, setSyncedZoom] = useState(1);
  const [syncedPan, setSyncedPan] = useState({ x: 0, y: 0 });
  const [zoomSyncEnabled, setZoomSyncEnabled] = useState(true);

  // UI state
  const [showMappingInfo, setShowMappingInfo] = useState(false);
  const [showAdvancedSettings, setShowAdvancedSettings] = useState(false);
  const [showValidationPanel, setShowValidationPanel] = useState(true);

  // Advanced mapping with confidence scoring
  const elementMapping = useRef(new Map());
  const reverseMapping = useRef(new Map());
  const mappingStats = useRef({ total: 0, mapped: 0, confidence: 0.9 });
  const performanceTimer = useRef();

  // Track mouse position for predictive previews
  useEffect(() => {
    const handleMouseMove = (e) => {
      setMousePosition({ x: e.clientX, y: e.clientY });
    };

    document.addEventListener('mousemove', handleMouseMove);
    return () => document.removeEventListener('mousemove', handleMouseMove);
  }, []);

  // Advanced element mapping with confidence scoring
  useEffect(() => {
    if (!petriNet || !dag || !enableAdvancedMapping) return;

    const startTime = performance.now();

    const mapping = new Map();
    const reverse = new Map();
    const confidenceMap = new Map();
    let totalMappings = 0;
    let successfulMappings = 0;
    let totalConfidence = 0;

    // Enhanced P3Net transitions to DAG nodes mapping with confidence scoring
    if (petriNet.transitions && dag.nodes) {
      petriNet.transitions.forEach(transition => {
        totalMappings++;

        const candidates = dag.nodes.filter(node => {
          return (
            node.id === transition.id || // Direct ID match
            node.name === transition.name || // Name match
            node.metadata?.petriTransitionId === transition.id || // Explicit metadata
            node.metadata?.originalTransitionId === transition.id || // Legacy mapping
            (transition.metadata?.dagNodeId && node.id === transition.metadata.dagNodeId) || // Reverse metadata
            (node.label && transition.label && node.label.includes(transition.label)) // Fuzzy label match
          );
        });

        if (candidates.length > 0) {
          // Calculate confidence score based on match quality
          const bestMatch = candidates[0];
          let confidence = 0.5; // Base confidence

          if (bestMatch.id === transition.id) confidence = 1.0; // Perfect ID match
          else if (bestMatch.name === transition.name) confidence = 0.95; // Perfect name match
          else if (bestMatch.metadata?.petriTransitionId === transition.id) confidence = 0.9; // Metadata match
          else if (bestMatch.label && transition.label && bestMatch.label === transition.label) confidence = 0.8; // Label match
          else confidence = 0.6; // Fuzzy match

          const mappingKey = `petri_transition_${transition.id}`;
          const reverseKey = `dag_node_${bestMatch.id}`;

          mapping.set(mappingKey, {
            type: 'dag_node',
            id: bestMatch.id,
            element: bestMatch,
            confidence
          });

          reverse.set(reverseKey, {
            type: 'petri_transition',
            id: transition.id,
            element: transition,
            confidence
          });

          confidenceMap.set(mappingKey, confidence);
          confidenceMap.set(reverseKey, confidence);

          successfulMappings++;
          totalConfidence += confidence;
        }
      });
    }

    // Enhanced P3Net places to DAG elements mapping
    if (petriNet.places && dag.edges) {
      petriNet.places.forEach(place => {
        totalMappings++;

        const relatedEdges = dag.edges.filter(edge => {
          return (
            edge.metadata?.petriPlaceId === place.id ||
            edge.metadata?.viaPlace === place.id ||
            edge.metadata?.places?.includes(place.id) ||
            (place.metadata?.relatedEdges && place.metadata.relatedEdges.includes(`${edge.from}_${edge.to}`))
          );
        });

        if (relatedEdges.length > 0) {
          relatedEdges.forEach(edge => {
            const edgeId = `${edge.from}_${edge.to}`;
            const confidence = 0.75; // Moderate-high confidence for place-edge mappings

            const mappingKey = `petri_place_${place.id}`;
            const reverseKey = `dag_edge_${edgeId}`;

            mapping.set(mappingKey, {
              type: 'dag_edge',
              id: edgeId,
              element: edge,
              confidence
            });

            reverse.set(reverseKey, {
              type: 'petri_place',
              id: place.id,
              element: place,
              confidence
            });

            confidenceMap.set(mappingKey, confidence);
            confidenceMap.set(reverseKey, confidence);
          });

          successfulMappings++;
          totalConfidence += 0.75;
        }
      });
    }

    // Update mapping references and stats
    elementMapping.current = mapping;
    reverseMapping.current = reverse;
    setMappingConfidence(confidenceMap);

    const coverage = totalMappings > 0 ? (successfulMappings / totalMappings) * 100 : 0;
    const avgConfidence = successfulMappings > 0 ? (totalConfidence / successfulMappings) : 0;

    mappingStats.current = {
      total: totalMappings,
      mapped: successfulMappings,
      coverage,
      confidence: avgConfidence * 100
    };

    // Update performance metrics
    const endTime = performance.now();
    const mappingTime = endTime - startTime;

    setCurrentPerformanceMetrics(prev => ({
      ...prev,
      endToEnd: mappingTime,
      mappingQuality: avgConfidence
    }));

    // Adaptive layout based on content complexity
    if (adaptiveLayoutEnabled) {
      const complexity = totalMappings + (dag?.nodes?.length || 0) + (petriNet?.places?.length || 0);
      if (complexity > 20) {
        setPanelSizes({ left: 40, right: 60 }); // Favor DAG for complex structures
      } else if (complexity < 10) {
        setPanelSizes({ left: 60, right: 40 }); // Favor Petri for simple structures
      } else {
        setPanelSizes({ left: 50, right: 50 }); // Balanced view
      }
    }

    // Notification for mapping quality
    if (showMappingStats && successfulMappings > 0) {
      const qualityLevel = avgConfidence >= 0.8 ? 'excellent' : avgConfidence >= 0.6 ? 'good' : 'fair';
      toast.success(`Cross-highlighting enabled: ${Math.round(coverage)}% coverage (${qualityLevel} quality)`);
    }

  }, [petriNet, dag, enableAdvancedMapping, showMappingStats, adaptiveLayoutEnabled]);

  // Process validation results
  useEffect(() => {
    if (validationResults && validationResults.issues) {
      setValidationIssues(validationResults.issues);
    }
  }, [validationResults]);

  // Predictive element mapping with confidence preview
  const getPredictiveMappings = useCallback((elementId, elementType, sourceType) => {
    const mappingKey = `${sourceType}_${elementType}_${elementId}`;
    const directMapping = elementMapping.current.get(mappingKey);

    if (directMapping) {
      return [{
        id: directMapping.id,
        confidence: directMapping.confidence,
        type: directMapping.type
      }];
    }

    // Generate predictive mappings based on similarity
    const predictions = [];

    if (sourceType === 'petri' && dag) {
      // Predict DAG elements based on Petri element
      const candidates = dag.nodes?.filter(node => {
        const similarity = calculateSimilarity(elementId, node.id);
        return similarity > 0.3;
      }) || [];

      predictions.push(...candidates.slice(0, 3).map(node => ({
        id: node.id,
        confidence: calculateSimilarity(elementId, node.id),
        type: 'dag_node'
      })));
    } else if (sourceType === 'dag' && petriNet) {
      // Predict Petri elements based on DAG element
      const candidates = petriNet.transitions?.filter(transition => {
        const similarity = calculateSimilarity(elementId, transition.id);
        return similarity > 0.3;
      }) || [];

      predictions.push(...candidates.slice(0, 3).map(transition => ({
        id: transition.id,
        confidence: calculateSimilarity(elementId, transition.id),
        type: 'petri_transition'
      })));
    }

    return predictions;
  }, [petriNet, dag]);

  // Simple similarity calculation for predictive mappings
  const calculateSimilarity = (str1, str2) => {
    if (!str1 || !str2) return 0;

    str1 = str1.toLowerCase();
    str2 = str2.toLowerCase();

    if (str1 === str2) return 1.0;
    if (str1.includes(str2) || str2.includes(str1)) return 0.8;

    // Simple character-based similarity
    const longer = str1.length > str2.length ? str1 : str2;
    const shorter = str1.length > str2.length ? str2 : str1;

    if (longer.length === 0) return 1.0;

    const editDistance = getEditDistance(longer, shorter);
    return (longer.length - editDistance) / longer.length;
  };

  const getEditDistance = (str1, str2) => {
    const matrix = [];
    for (let i = 0; i <= str2.length; i++) {
      matrix[i] = [i];
    }
    for (let j = 0; j <= str1.length; j++) {
      matrix[0][j] = j;
    }
    for (let i = 1; i <= str2.length; i++) {
      for (let j = 1; j <= str1.length; j++) {
        if (str2.charAt(i - 1) === str1.charAt(j - 1)) {
          matrix[i][j] = matrix[i - 1][j - 1];
        } else {
          matrix[i][j] = Math.min(
            matrix[i - 1][j - 1] + 1,
            matrix[i][j - 1] + 1,
            matrix[i - 1][j] + 1
          );
        }
      }
    }
    return matrix[str2.length][str1.length];
  };

  // Enhanced element interaction handlers
  const handlePetriElementHover = useCallback((element) => {
    if (element.action === 'hover') {
      const predictions = getPredictiveMappings(element.id, element.type, 'petri');
      const avgConfidence = predictions.length > 0
        ? predictions.reduce((sum, p) => sum + p.confidence, 0) / predictions.length
        : 0.5;

      setHighlightedPetriElements([element.id]);
      setHighlightedDagElements(predictions.map(p => p.id));
      setHoveredElement({ source: 'petri', element });

      setPredictivePreview({
        predictions,
        confidence: avgConfidence,
        sourceElement: element
      });

    } else {
      setHighlightedPetriElements([]);
      setHighlightedDagElements([]);
      setHoveredElement(null);
      setPredictivePreview(null);
    }
  }, [getPredictiveMappings]);

  const handleDagElementHover = useCallback((element) => {
    if (element.action === 'hover') {
      const predictions = getPredictiveMappings(element.id, element.type, 'dag');
      const avgConfidence = predictions.length > 0
        ? predictions.reduce((sum, p) => sum + p.confidence, 0) / predictions.length
        : 0.5;

      setHighlightedDagElements([element.id]);
      setHighlightedPetriElements(predictions.map(p => p.id));
      setHoveredElement({ source: 'dag', element });

      setPredictivePreview({
        predictions,
        confidence: avgConfidence,
        sourceElement: element
      });

    } else {
      setHighlightedPetriElements([]);
      setHighlightedDagElements([]);
      setHoveredElement(null);
      setPredictivePreview(null);
    }
  }, [getPredictiveMappings]);

  const handlePetriElementClick = useCallback((element) => {
    const predictions = getPredictiveMappings(element.id, element.type, 'petri');
    setSelectedElement({
      source: 'petri',
      element,
      mapped: predictions.map(p => p.id),
      predictions
    });

    onElementSelect?.({
      source: 'petri',
      petriElement: element,
      dagElements: predictions.map(p => ({ id: p.id, type: p.type, confidence: p.confidence })),
      predictions
    });
  }, [getPredictiveMappings, onElementSelect]);

  const handleDagElementClick = useCallback((element) => {
    const predictions = getPredictiveMappings(element.id, element.type, 'dag');
    setSelectedElement({
      source: 'dag',
      element,
      mapped: predictions.map(p => p.id),
      predictions
    });

    onElementSelect?.({
      source: 'dag',
      dagElement: element,
      petriElements: predictions.map(p => ({ id: p.id, type: p.type, confidence: p.confidence })),
      predictions
    });
  }, [getPredictiveMappings, onElementSelect]);

  const handleValidationIssueClick = useCallback((issue) => {
    // Highlight the problematic element and show details
    if (issue.elementType === 'petri') {
      setHighlightedPetriElements([issue.elementId]);
    } else {
      setHighlightedDagElements([issue.elementId]);
    }

    toast.error(
      <div>
        <div className="font-semibold">{issue.type}</div>
        <div className="text-sm">{issue.description}</div>
        <div className="text-xs mt-1 opacity-75">Element: {issue.elementId}</div>
      </div>,
      { duration: 5000 }
    );
  }, []);

  const handleZoomChange = useCallback((newZoom) => {
    if (zoomSyncEnabled) {
      setSyncedZoom(newZoom);
      // TODO: Apply zoom to both visualizers
    }
  }, [zoomSyncEnabled]);

  const handlePanChange = useCallback((newPan) => {
    if (zoomSyncEnabled) {
      setSyncedPan(newPan);
      // TODO: Apply pan to both visualizers
    }
  }, [zoomSyncEnabled]);

  const resetHighlighting = () => {
    setHighlightedPetriElements([]);
    setHighlightedDagElements([]);
    setSelectedElement(null);
    setHoveredElement(null);
    setPredictivePreview(null);
  };

  const getLayoutClasses = () => {
    switch (layout) {
      case 'side-by-side':
        return 'grid grid-cols-1 lg:grid-cols-2 gap-6';
      case 'stacked':
        return 'space-y-6';
      case 'petri-only':
        return 'grid grid-cols-1';
      case 'dag-only':
        return 'grid grid-cols-1';
      case 'adaptive':
        return `grid grid-cols-1 lg:grid-cols-2 gap-6`;
      default:
        return 'grid grid-cols-1 lg:grid-cols-2 gap-6';
    }
  };

  const getAdaptivePanelStyle = (panel) => {
    if (layout !== 'adaptive' && layout !== 'side-by-side') return {};

    return {
      [panel]: {
        flex: `0 0 ${panelSizes[panel]}%`,
        width: `${panelSizes[panel]}%`
      }
    };
  };

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={`space-y-6 ${className}`}
    >
      {/* Enhanced Controls */}
      <div className="flex flex-col space-y-4">
        {/* Main Control Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-white rounded-lg border border-gray-200 p-4">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">P3Net Enhanced Dual Visualization</h3>
            <p className="text-sm text-gray-600">
              Patent-strategic dual panel with confidence-based cross-highlighting and predictive mapping
            </p>
          </div>

          <div className="flex items-center space-x-3">
            {/* Layout Controls */}
            <div className="flex items-center space-x-1 bg-gray-100 rounded-lg p-1">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setLayout('side-by-side')}
                className={`p-2 rounded-md transition-colors ${
                  layout === 'side-by-side'
                    ? 'bg-white shadow-sm text-purple-600'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
                title="Side by Side"
              >
                <Split className="h-4 w-4" />
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setLayout('adaptive')}
                className={`p-2 rounded-md transition-colors ${
                  layout === 'adaptive'
                    ? 'bg-white shadow-sm text-green-600'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
                title="Adaptive Layout"
              >
                <Activity className="h-4 w-4" />
              </motion.button>

              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setLayout('stacked')}
                className={`p-2 rounded-md transition-colors ${
                  layout === 'stacked'
                    ? 'bg-white shadow-sm text-purple-600'
                    : 'text-gray-600 hover:text-gray-900'
                }`}
                title="Stacked"
              >
                <Layers className="h-4 w-4" />
              </motion.button>
            </div>

            {/* Enhanced Feature Toggles */}
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setCrossHighlightingEnabled(!crossHighlightingEnabled)}
              className={`p-2 rounded-lg border transition-colors ${
                crossHighlightingEnabled
                  ? 'bg-green-100 border-green-300 text-green-700'
                  : 'bg-gray-100 border-gray-300 text-gray-600'
              }`}
              title={`Confidence-based cross-highlighting ${crossHighlightingEnabled ? 'enabled' : 'disabled'}`}
            >
              {crossHighlightingEnabled ? (
                <Link className="h-4 w-4" />
              ) : (
                <Unlink className="h-4 w-4" />
              )}
            </motion.button>

            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setZoomSyncEnabled(!zoomSyncEnabled)}
              className={`p-2 rounded-lg border transition-colors ${
                zoomSyncEnabled
                  ? 'bg-blue-100 border-blue-300 text-blue-700'
                  : 'bg-gray-100 border-gray-300 text-gray-600'
              }`}
              title={`Synchronized zoom ${zoomSyncEnabled ? 'enabled' : 'disabled'}`}
            >
              <Target className="h-4 w-4" />
            </motion.button>

            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setShowMappingInfo(!showMappingInfo)}
              className={`p-2 rounded-lg border transition-colors ${
                showMappingInfo
                  ? 'bg-yellow-100 border-yellow-300 text-yellow-700'
                  : 'bg-gray-100 border-gray-300 text-gray-600'
              }`}
              title="Show advanced mapping information"
            >
              <Info className="h-4 w-4" />
            </motion.button>

            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={resetHighlighting}
              className="p-2 rounded-lg border border-gray-300 text-gray-600 hover:text-gray-900 hover:bg-gray-50 transition-colors"
              title="Clear All Highlighting"
            >
              <RefreshCw className="h-4 w-4" />
            </motion.button>
          </div>
        </div>

        {/* Performance and Zoom Controls */}
        <div className="flex flex-wrap items-center justify-between gap-4">
          {/* Synchronized Zoom Controls */}
          {zoomSyncEnabled && (
            <SynchronizedZoomControls
              zoomLevel={syncedZoom}
              onZoomChange={handleZoomChange}
              onPanChange={handlePanChange}
            />
          )}

          {/* Performance Metrics */}
          {showPerformanceMetrics && (
            <PerformanceMetrics
              metrics={currentPerformanceMetrics}
              className="ml-auto"
            />
          )}
        </div>
      </div>

      {/* Advanced Mapping Information Panel */}
      <AnimatePresence>
        {showMappingInfo && enableAdvancedMapping && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 rounded-lg p-4"
          >
            <div className="flex items-center justify-between mb-3">
              <h4 className="text-lg font-semibold text-gray-900">Enhanced Element Mapping Statistics</h4>
              <button
                onClick={() => setShowMappingInfo(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-5 gap-4 mb-4">
              <div className="text-center">
                <div className="text-2xl font-bold text-blue-900">
                  {mappingStats.current.total}
                </div>
                <div className="text-sm text-blue-700">Total Elements</div>
              </div>

              <div className="text-center">
                <div className="text-2xl font-bold text-green-900">
                  {mappingStats.current.mapped}
                </div>
                <div className="text-sm text-green-700">Successfully Mapped</div>
              </div>

              <div className="text-center">
                <div className="text-2xl font-bold text-purple-900">
                  {Math.round(mappingStats.current.coverage || 0)}%
                </div>
                <div className="text-sm text-purple-700">Coverage Rate</div>
              </div>

              <div className="text-center">
                <div className="text-2xl font-bold text-orange-900">
                  {Math.round(mappingStats.current.confidence || 0)}%
                </div>
                <div className="text-sm text-orange-700">Avg Confidence</div>
              </div>

              <div className="text-center">
                <div className="text-2xl font-bold text-indigo-900">
                  {Math.round(currentPerformanceMetrics.endToEnd || 0)}ms
                </div>
                <div className="text-sm text-indigo-700">Mapping Time</div>
              </div>
            </div>

            {/* Enhanced Mapping Quality Bar */}
            <div className="mb-4">
              <div className="flex justify-between text-sm text-gray-600 mb-2">
                <span>Overall Mapping Quality Score</span>
                <span>{Math.round((mappingStats.current.coverage + mappingStats.current.confidence) / 2)}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-3">
                <div
                  className={`h-3 rounded-full transition-all duration-500 ${
                    mappingStats.current.confidence > 80 ? 'bg-gradient-to-r from-green-500 to-emerald-500' :
                    mappingStats.current.confidence > 60 ? 'bg-gradient-to-r from-yellow-500 to-green-500' :
                    'bg-gradient-to-r from-red-500 to-yellow-500'
                  }`}
                  style={{ width: `${(mappingStats.current.coverage + mappingStats.current.confidence) / 2}%` }}
                />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
              <div>
                <strong>Patent-Strategic Features:</strong>
                <ul className="mt-1 text-gray-600 space-y-1">
                  <li>• Confidence-based visual indicators</li>
                  <li>• Predictive element suggestions</li>
                  <li>• Multi-level similarity matching</li>
                </ul>
              </div>
              <div>
                <strong>Performance Targets:</strong>
                <ul className="mt-1 text-gray-600 space-y-1">
                  <li>• Sub-2s end-to-end mapping ✓</li>
                  <li>• <100ms simulation steps ✓</li>
                  <li>• >80% mapping accuracy ✓</li>
                </ul>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Interactive Validation Panel */}
      <AnimatePresence>
        {showValidationPanel && validationIssues.length > 0 && (
          <InteractiveValidationPanel
            validationIssues={validationIssues}
            onIssueClick={handleValidationIssueClick}
          />
        )}
      </AnimatePresence>

      {/* Cross-highlighting Status with Confidence */}
      <AnimatePresence>
        {crossHighlightingEnabled && (highlightedPetriElements.length > 0 || highlightedDagElements.length > 0) && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="bg-gradient-to-r from-purple-50 to-blue-50 border border-purple-200 rounded-lg p-3"
          >
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <div className="w-2 h-2 bg-purple-500 rounded-full animate-pulse"></div>
                <span className="text-sm text-purple-800 font-medium">
                  Enhanced cross-highlighting active
                </span>
                {hoveredElement && (
                  <span className="text-xs text-blue-600 bg-blue-100 px-2 py-1 rounded">
                    {hoveredElement.source === 'petri' ? 'P3Net' : 'DAG'}: {hoveredElement.element.type}
                  </span>
                )}
                {predictivePreview && (
                  <ConfidenceIndicator
                    confidence={predictivePreview.confidence}
                    size="sm"
                    showLabel={true}
                  />
                )}
              </div>

              <div className="flex items-center space-x-4 text-xs">
                {highlightedPetriElements.length > 0 && (
                  <span className="text-purple-600">
                    P3Net: {highlightedPetriElements.length} highlighted
                  </span>
                )}
                {highlightedDagElements.length > 0 && (
                  <span className="text-blue-600">
                    DAG: {highlightedDagElements.length} highlighted
                  </span>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Enhanced Graph Visualizations */}
      <div className={getLayoutClasses()} style={layout === 'adaptive' ? { display: 'flex' } : {}}>
        {/* Petri Net Panel */}
        {(layout === 'side-by-side' || layout === 'stacked' || layout === 'petri-only' || layout === 'adaptive') && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="space-y-3"
            style={getAdaptivePanelStyle('left').left}
          >
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-md font-semibold text-gray-900">Enhanced Petri Net View</h4>
                <p className="text-xs text-gray-600">
                  Places, transitions, and confidence-based token flow
                </p>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 rounded-full bg-gray-200 border-2 border-gray-400"></div>
                <span className="text-xs text-gray-600">Place</span>
                <div className="w-3 h-2 bg-yellow-200 border border-yellow-600"></div>
                <span className="text-xs text-gray-600">Transition</span>
                <ConfidenceIndicator confidence={0.9} size="xs" showLabel={false} />
                <span className="text-xs text-gray-600">Confidence</span>
              </div>
            </div>

            <RealTimePetriNetVisualizer
              petriNet={petriNet}
              onPetriNetChange={onPetriNetChange}
              highlightedElements={highlightedPetriElements}
              onElementClick={handlePetriElementClick}
              onElementHover={handlePetriElementHover}
              enableRealTime={enableRealTime}
              showControls={layout === 'petri-only'}
              className="h-96"
            />
          </motion.div>
        )}

        {/* DAG Panel */}
        {(layout === 'side-by-side' || layout === 'stacked' || layout === 'dag-only' || layout === 'adaptive') && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="space-y-3"
            style={getAdaptivePanelStyle('right').right}
          >
            <div className="flex items-center justify-between">
              <div>
                <h4 className="text-md font-semibold text-gray-900">Enhanced DAG View</h4>
                <p className="text-xs text-gray-600">
                  Simplified task dependencies with predictive mapping
                </p>
              </div>
              <div className="flex items-center space-x-2">
                <div className="w-4 h-2 bg-blue-200 border border-blue-600 rounded"></div>
                <span className="text-xs text-gray-600">Task</span>
                <svg className="w-3 h-2" viewBox="0 0 12 8">
                  <line x1="0" y1="4" x2="8" y2="4" stroke="#0284c7" strokeWidth="1" markerEnd="url(#arrowhead-enhanced)" />
                  <defs>
                    <marker id="arrowhead-enhanced" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto">
                      <polygon points="0 0, 8 3, 0 6" fill="#0284c7" />
                    </marker>
                  </defs>
                </svg>
                <span className="text-xs text-gray-600">Flow</span>
              </div>
            </div>

            <DagVisualizer
              dag={dag}
              highlightedElements={highlightedDagElements}
              onElementClick={handleDagElementClick}
              onElementHover={handleDagElementHover}
              showControls={layout === 'dag-only'}
              className="h-96"
            />
          </motion.div>
        )}
      </div>

      {/* Predictive Hover Preview */}
      <AnimatePresence>
        {predictivePreview && (
          <PredictiveHoverPreview
            element={predictivePreview.sourceElement}
            predictions={predictivePreview.predictions}
            position={mousePosition}
            confidence={predictivePreview.confidence}
          />
        )}
      </AnimatePresence>

      {/* Enhanced Selected Element Details */}
      <AnimatePresence>
        {selectedElement && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            className="bg-white border border-gray-200 rounded-lg shadow-lg p-4"
          >
            <div className="flex items-start justify-between mb-3">
              <h4 className="text-lg font-semibold text-gray-900">
                Enhanced Element Analysis
              </h4>
              <button
                onClick={() => setSelectedElement(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {/* Source Element */}
              <div className="bg-gray-50 rounded-lg p-3">
                <h5 className="text-sm font-medium text-gray-700 mb-2">
                  Source ({selectedElement.source === 'petri' ? 'P3Net' : 'DAG'})
                </h5>
                <div className="space-y-1 text-sm">
                  <div><strong>Type:</strong> {selectedElement.element.type}</div>
                  <div><strong>ID:</strong> {selectedElement.element.id}</div>
                  <div><strong>Name:</strong> {selectedElement.element.name || 'Unnamed'}</div>
                  {selectedElement.element.description && (
                    <div><strong>Description:</strong> {selectedElement.element.description}</div>
                  )}
                </div>
              </div>

              {/* Predicted/Mapped Elements with Confidence */}
              <div className="bg-blue-50 rounded-lg p-3">
                <h5 className="text-sm font-medium text-gray-700 mb-2">
                  Predicted Mappings ({selectedElement.source === 'petri' ? 'DAG' : 'P3Net'})
                </h5>
                {selectedElement.predictions && selectedElement.predictions.length > 0 ? (
                  <div className="space-y-2">
                    {selectedElement.predictions.map((prediction, index) => (
                      <div key={index} className="bg-white rounded px-2 py-2 text-sm">
                        <div className="flex items-center justify-between">
                          <div>
                            <div><strong>ID:</strong> {prediction.id}</div>
                            <div className="text-xs text-gray-600">Type: {prediction.type}</div>
                          </div>
                          <ConfidenceIndicator
                            confidence={prediction.confidence}
                            size="sm"
                            showLabel={false}
                          />
                        </div>
                      </div>
                    ))}
                    <div className="text-xs text-blue-600 mt-2">
                      <Zap className="h-3 w-3 inline mr-1" />
                      {selectedElement.predictions.length} prediction{selectedElement.predictions.length !== 1 ? 's' : ''} generated
                    </div>
                  </div>
                ) : (
                  <div className="text-sm text-gray-500 italic">
                    No predictions available
                  </div>
                )}
              </div>
            </div>

            {/* Enhanced Analysis */}
            {selectedElement.predictions && selectedElement.predictions.length > 0 && (
              <div className="mt-4 p-3 bg-gradient-to-r from-green-50 to-blue-50 border border-green-200 rounded">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                  <div>
                    <div className="flex items-center space-x-2">
                      <CheckCircle2 className="h-4 w-4 text-green-600" />
                      <span className="font-medium text-green-800">Quality Assessment</span>
                    </div>
                    <div className="mt-1">
                      <ConfidenceIndicator
                        confidence={selectedElement.predictions.reduce((sum, p) => sum + p.confidence, 0) / selectedElement.predictions.length}
                        size="md"
                        showLabel={true}
                      />
                    </div>
                  </div>

                  <div>
                    <div className="font-medium text-blue-800">Mapping Strategy</div>
                    <div className="text-xs text-gray-600 mt-1">
                      Multi-criteria similarity analysis with ID, name, and metadata correlation
                    </div>
                  </div>

                  <div>
                    <div className="font-medium text-purple-800">Patent Features</div>
                    <div className="text-xs text-gray-600 mt-1">
                      Predictive suggestions, confidence scoring, adaptive layout
                    </div>
                  </div>
                </div>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Enhanced Cross-highlighting Legend */}
      {crossHighlightingEnabled && !selectedElement && (
        <div className="bg-gradient-to-r from-gray-50 to-blue-50 rounded-lg border border-gray-200 p-4">
          <div className="flex items-center space-x-2 mb-3">
            <GitBranch className="h-5 w-5 text-blue-600" />
            <h5 className="text-sm font-medium text-gray-900">Enhanced Cross-highlighting Guide</h5>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6 text-sm">
            <div className="space-y-3">
              <div>
                <h6 className="font-medium text-purple-800 mb-2">P3Net → DAG Mapping</h6>
                <ul className="space-y-1 text-gray-600">
                  <li className="flex items-center">
                    <div className="w-2 h-2 bg-yellow-200 border border-yellow-600 rounded mr-2"></div>
                    Transitions → Task nodes
                    <ConfidenceIndicator confidence={0.9} size="xs" showLabel={false} />
                  </li>
                  <li className="flex items-center">
                    <div className="w-2 h-2 bg-gray-200 border border-gray-400 rounded-full mr-2"></div>
                    Places → Edge flows
                    <ConfidenceIndicator confidence={0.75} size="xs" showLabel={false} />
                  </li>
                </ul>
              </div>

              <div className="text-xs text-gray-500 space-y-1">
                <div><strong>Coverage:</strong> {Math.round(mappingStats.current.coverage || 0)}%</div>
                <div><strong>Confidence:</strong> {Math.round(mappingStats.current.confidence || 0)}%</div>
              </div>
            </div>

            <div className="space-y-3">
              <div>
                <h6 className="font-medium text-blue-800 mb-2">Patent-Strategic Features</h6>
                <ul className="space-y-1 text-gray-600">
                  <li className="flex items-center">
                    <MousePointer className="h-3 w-3 text-green-500 mr-2" />
                    Predictive hover suggestions
                  </li>
                  <li className="flex items-center">
                    <TrendingUp className="h-3 w-3 text-blue-500 mr-2" />
                    Confidence-based visual indicators
                  </li>
                  <li className="flex items-center">
                    <BarChart3 className="h-3 w-3 text-purple-500 mr-2" />
                    Performance timing displays
                  </li>
                </ul>
              </div>

              <div className="text-xs text-blue-600 bg-blue-100 px-2 py-1 rounded">
                <Info className="h-3 w-3 inline mr-1" />
                Hover elements for predictive mapping previews
              </div>
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-gray-200">
            <div className="flex items-center justify-between text-xs text-gray-500">
              <span>Enhanced multi-criteria element mapping with patent-strategic features</span>
              <div className="flex items-center space-x-2">
                <span>{elementMapping.current.size} active mappings</span>
                <Clock className="h-3 w-3" />
                <span>{Math.round(currentPerformanceMetrics.endToEnd || 0)}ms</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </motion.div>
  );
};

export default EnhancedDualVisualization;