import React, { useState, useCallback, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Split, Eye, EyeOff, RefreshCw, Link, Unlink, Settings,
  GitBranch, Layers, ArrowRightLeft, X, Info, Zap,
  ChevronLeft, ChevronRight, Maximize2, Minimize2,
  MoreHorizontal, Menu, CheckCircle
} from 'lucide-react';
import RealTimePetriNetVisualizer from './RealTimePetriNetVisualizer';
import DagVisualizer from './DagVisualizer';
import { useResponsive } from '../hooks/useResponsive';
import { useSwipe } from '../hooks/useTouch';
import toast from 'react-hot-toast';

const DualGraphView = ({
  petriNet,
  dag,
  onPetriNetChange,
  onDagChange,
  className = "",
  onElementSelect,
  initialLayout = 'side-by-side', // 'side-by-side', 'stacked', 'petri-only', 'dag-only', 'single'
  enableRealTime = true,
  enableAdvancedMapping = true,
  showMappingStats = true,
  isMobile = false,
  isTablet = false,
  compactMode = false
}) => {
  const responsive = useResponsive();
  const [layout, setLayout] = useState(() => {
    if (responsive.isMobile) return 'single';
    return initialLayout;
  });
  const [highlightedPetriElements, setHighlightedPetriElements] = useState([]);
  const [highlightedDagElements, setHighlightedDagElements] = useState([]);
  const [crossHighlightingEnabled, setCrossHighlightingEnabled] = useState(!responsive.isMobile);
  const [selectedElement, setSelectedElement] = useState(null);
  const [hoveredElement, setHoveredElement] = useState(null);
  const [showMappingInfo, setShowMappingInfo] = useState(false);
  const [showAdvancedSettings, setShowAdvancedSettings] = useState(false);
  const [currentView, setCurrentView] = useState('petri'); // 'petri' or 'dag' for single view mode
  const [showControls, setShowControls] = useState(!compactMode);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // Advanced mapping state
  const [mappingQuality, setMappingQuality] = useState({ coverage: 0, confidence: 0 });
  const [syncStates, setSyncStates] = useState({
    marking: true,
    simulation: true,
    validation: true
  });

  // Responsive state management
  useEffect(() => {
    if (responsive.isMobile) {
      setLayout('single');
      setCrossHighlightingEnabled(false);
      setShowMappingInfo(false);
      setShowAdvancedSettings(false);
    } else if (responsive.isTablet) {
      if (layout === 'single') setLayout('stacked');
      setCrossHighlightingEnabled(true);
    } else {
      if (layout === 'single') setLayout('side-by-side');
      setCrossHighlightingEnabled(true);
    }
  }, [responsive.breakpoint]);

  // Swipe navigation for mobile single view
  const swipeHandlers = useSwipe({
    onSwipeLeft: () => {
      if (responsive.isMobile && layout === 'single' && currentView === 'petri') {
        setCurrentView('dag');
      }
    },
    onSwipeRight: () => {
      if (responsive.isMobile && layout === 'single' && currentView === 'dag') {
        setCurrentView('petri');
      }
    }
  });

  // Element mapping for advanced cross-highlighting
  const elementMapping = useRef(new Map());
  const reverseMapping = useRef(new Map());
  const mappingStats = useRef({ total: 0, mapped: 0, confidence: 0 });

  // Initialize advanced element mapping when data changes
  useEffect(() => {
    if (!petriNet || !dag || !enableAdvancedMapping) return;

    const mapping = new Map();
    const reverse = new Map();
    let totalMappings = 0;
    let successfulMappings = 0;
    let confidenceScore = 0;

    // Enhanced P3Net transitions to DAG nodes mapping
    if (petriNet.transitions && dag.nodes) {
      petriNet.transitions.forEach(transition => {
        totalMappings++;

        // Multiple mapping strategies for better coverage
        const candidates = dag.nodes.filter(node => {
          return (
            node.id === transition.id || // Direct ID match
            node.name === transition.name || // Name match
            node.metadata?.petriTransitionId === transition.id || // Explicit metadata
            node.metadata?.originalTransitionId === transition.id || // Legacy mapping
            (transition.metadata?.dagNodeId && node.id === transition.metadata.dagNodeId) // Reverse metadata
          );
        });

        if (candidates.length > 0) {
          const bestMatch = candidates[0]; // Take first/best candidate
          const confidence = candidates.length === 1 &&
                           (bestMatch.id === transition.id || bestMatch.name === transition.name) ? 1.0 : 0.8;

          mapping.set(`petri_transition_${transition.id}`, {
            type: 'dag_node',
            id: bestMatch.id,
            element: bestMatch,
            confidence
          });

          reverse.set(`dag_node_${bestMatch.id}`, {
            type: 'petri_transition',
            id: transition.id,
            element: transition,
            confidence
          });

          successfulMappings++;
          confidenceScore += confidence;
        }
      });
    }

    // Enhanced P3Net places to DAG elements mapping
    if (petriNet.places && dag.edges) {
      petriNet.places.forEach(place => {
        totalMappings++;

        // Find DAG edges that reference this place
        const relatedEdges = dag.edges.filter(edge => {
          return (
            edge.metadata?.petriPlaceId === place.id ||
            edge.metadata?.viaPlace === place.id ||
            edge.metadata?.places?.includes(place.id) ||
            (place.metadata?.relatedEdges && place.metadata.relatedEdges.includes(edge.from + '_' + edge.to))
          );
        });

        if (relatedEdges.length > 0) {
          relatedEdges.forEach(edge => {
            const edgeId = `${edge.from}_${edge.to}`;
            const confidence = 0.7; // Moderate confidence for place-edge mappings

            mapping.set(`petri_place_${place.id}`, {
              type: 'dag_edge',
              id: edgeId,
              element: edge,
              confidence
            });

            reverse.set(`dag_edge_${edgeId}`, {
              type: 'petri_place',
              id: place.id,
              element: place,
              confidence
            });
          });

          successfulMappings++;
          confidenceScore += 0.7;
        }
      });
    }

    // Update mapping references and stats
    elementMapping.current = mapping;
    reverseMapping.current = reverse;

    const coverage = totalMappings > 0 ? (successfulMappings / totalMappings) * 100 : 0;
    const avgConfidence = successfulMappings > 0 ? (confidenceScore / successfulMappings) * 100 : 0;

    mappingStats.current = {
      total: totalMappings,
      mapped: successfulMappings,
      coverage,
      confidence: avgConfidence
    };

    setMappingQuality({ coverage, confidence: avgConfidence });

    // Notify about mapping quality
    if (showMappingStats && successfulMappings > 0) {
      toast.success(`Cross-highlighting enabled: ${Math.round(coverage)}% coverage`);
    }

  }, [petriNet, dag, enableAdvancedMapping, showMappingStats]);

  // Enhanced P3Net to DAG mapping with advanced features
  const mapPetriToDag = useCallback((petriElementId, elementType = 'unknown') => {
    if (!dag || !crossHighlightingEnabled) return [];

    const elementKey = `petri_${elementType}_${petriElementId}`;
    const mapped = elementMapping.current.get(elementKey);

    if (mapped) {
      return [mapped.id];
    }

    // Fallback to legacy mapping logic
    if (petriNet?.transitions?.some(t => t.id === petriElementId)) {
      return dag.nodes?.filter(n => n.id === petriElementId).map(n => n.id) || [];
    }

    if (petriNet?.places?.some(p => p.id === petriElementId)) {
      const relatedEdges = dag.edges?.filter(e =>
        e.meta?.places?.includes(petriElementId)
      ) || [];
      return relatedEdges.map((_, index) => `dag_edge_${index}`);
    }

    return [];
  }, [petriNet, dag, crossHighlightingEnabled]);

  // Enhanced DAG to P3Net mapping
  const mapDagToPetri = useCallback((dagElementId, elementType = 'unknown') => {
    if (!petriNet || !crossHighlightingEnabled) return [];

    const elementKey = `dag_${elementType}_${dagElementId}`;
    const mapped = reverseMapping.current.get(elementKey);

    if (mapped) {
      return [mapped.id];
    }

    // Enhanced fallback logic
    if (dag?.nodes?.some(n => n.id === dagElementId)) {
      return petriNet.transitions?.filter(t => t.id === dagElementId).map(t => t.id) || [];
    }

    if (dagElementId.includes('_')) {
      const edge = dag?.edges?.find(e => `${e.from}_${e.to}` === dagElementId);
      if (edge?.meta?.places) {
        return edge.meta.places;
      }
    }

    return [];
  }, [petriNet, dag, crossHighlightingEnabled]);

  const handlePetriElementHover = useCallback((element) => {
    if (element.action === 'hover') {
      const correspondingDagElements = mapPetriToDag(element.id, element.type);
      setHighlightedPetriElements([element.id]);
      setHighlightedDagElements(correspondingDagElements);
      setHoveredElement({ source: 'petri', element });
    } else {
      setHighlightedPetriElements([]);
      setHighlightedDagElements([]);
      setHoveredElement(null);
    }
  }, [mapPetriToDag]);

  const handleDagElementHover = useCallback((element) => {
    if (element.action === 'hover') {
      const correspondingPetriElements = mapDagToPetri(element.id, element.type);
      setHighlightedDagElements([element.id]);
      setHighlightedPetriElements(correspondingPetriElements);
      setHoveredElement({ source: 'dag', element });
    } else {
      setHighlightedPetriElements([]);
      setHighlightedDagElements([]);
      setHoveredElement(null);
    }
  }, [mapDagToPetri]);

  const handlePetriElementClick = useCallback((element) => {
    const correspondingDagElements = mapPetriToDag(element.id, element.type);
    setSelectedElement({ source: 'petri', element, mapped: correspondingDagElements });

    // Enhanced selection with mapping details
    const mappingInfo = elementMapping.current.get(`petri_${element.type}_${element.id}`);

    onElementSelect?.({
      source: 'petri',
      petriElement: element,
      dagElements: correspondingDagElements.map(id => ({ id, type: 'dag_element' })),
      mapping: mappingInfo
    });
  }, [mapPetriToDag, onElementSelect]);

  const handleDagElementClick = useCallback((element) => {
    const correspondingPetriElements = mapDagToPetri(element.id, element.type);
    setSelectedElement({ source: 'dag', element, mapped: correspondingPetriElements });

    // Enhanced selection with mapping details
    const mappingInfo = reverseMapping.current.get(`dag_${element.type}_${element.id}`);

    onElementSelect?.({
      source: 'dag',
      dagElement: element,
      petriElements: correspondingPetriElements.map(id => ({ id, type: 'petri_element' })),
      mapping: mappingInfo
    });
  }, [mapDagToPetri, onElementSelect]);

  const resetHighlighting = () => {
    setHighlightedPetriElements([]);
    setHighlightedDagElements([]);
    setSelectedElement(null);
    setHoveredElement(null);
  };

  const getLayoutClasses = () => {
    if (responsive.isMobile) {
      return 'space-y-4';
    }

    switch (layout) {
      case 'side-by-side':
        return responsive.isTablet ? 'space-y-4' : 'grid grid-cols-2 gap-6';
      case 'stacked':
        return 'space-y-6';
      case 'petri-only':
      case 'dag-only':
      case 'single':
        return 'grid grid-cols-1';
      default:
        return responsive.isTablet ? 'space-y-4' : 'grid grid-cols-2 gap-6';
    }
  };

  const getVisualizationHeight = () => {
    if (responsive.isMobile) return 'h-64';
    if (responsive.isTablet) return compactMode ? 'h-72' : 'h-80';
    return compactMode ? 'h-80' : 'h-96';
  };

  const toggleView = () => {
    if (responsive.isMobile && layout === 'single') {
      setCurrentView(currentView === 'petri' ? 'dag' : 'petri');
    } else {
      const layouts = responsive.isMobile ? ['single'] :
                     responsive.isTablet ? ['stacked', 'single', 'petri-only', 'dag-only'] :
                     ['side-by-side', 'stacked', 'petri-only', 'dag-only'];
      const currentIndex = layouts.indexOf(layout);
      const nextIndex = (currentIndex + 1) % layouts.length;
      setLayout(layouts[nextIndex]);
    }
  };

  // Mobile view indicator
  const MobileViewIndicator = () => (
    <div className="flex items-center justify-center space-x-2 py-2 bg-gray-50 border-b border-gray-200">
      <button
        onClick={() => setCurrentView('petri')}
        className={`px-3 py-1 rounded-full text-xs font-medium transition-all duration-200 touch-manipulation ${
          currentView === 'petri' ? 'bg-purple-100 text-purple-800' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
        }`}
      >
        Petri Net
      </button>
      <button
        onClick={() => setCurrentView('dag')}
        className={`px-3 py-1 rounded-full text-xs font-medium transition-all duration-200 touch-manipulation ${
          currentView === 'dag' ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
        }`}
      >
        DAG View
      </button>
      {petriNet && dag && (
        <div className="text-xs text-gray-500">
          Swipe to navigate
        </div>
      )}
    </div>
  );

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={`space-y-4 ${isFullscreen ? 'fixed inset-0 z-50 bg-white p-4' : ''} ${className}`}
    >
      {/* Mobile View Indicator */}
      {responsive.isMobile && layout === 'single' && <MobileViewIndicator />}

      {/* Controls */}
      {showControls && (
        <div className={`flex flex-col gap-4 bg-white rounded-lg border border-gray-200 p-4 ${
          responsive.isMobile ? 'space-y-3' : 'sm:flex-row sm:items-center sm:justify-between'
        }`}>
          <div className={responsive.isMobile ? 'text-center' : ''}>
            <h3 className={`font-semibold text-gray-900 ${
              responsive.isMobile ? 'text-base' : 'text-lg'
            }`}>Dual Graph View</h3>
            {!responsive.isMobile && (
              <p className="text-sm text-gray-600">
                Interactive visualization showing both Petri net and DAG representations
              </p>
            )}
          </div>

          <div className={`flex items-center justify-center ${
            responsive.isMobile ? 'space-x-2' : 'space-x-3'
          }`}>
            {/* Mobile: Simplified Controls */}
            {responsive.isMobile ? (
              <>
                <button
                  onClick={toggleView}
                  className="p-2 rounded-lg border border-gray-300 text-gray-600 hover:text-gray-900 hover:bg-gray-50 transition-colors touch-manipulation min-h-[44px] min-w-[44px]"
                  title="Toggle layout"
                >
                  <Layers className="h-4 w-4" />
                </button>

                <button
                  onClick={() => setShowControls(!showControls)}
                  className="p-2 rounded-lg border border-gray-300 text-gray-600 hover:text-gray-900 hover:bg-gray-50 transition-colors touch-manipulation min-h-[44px] min-w-[44px]"
                  title="Toggle controls"
                >
                  <MoreHorizontal className="h-4 w-4" />
                </button>

                <button
                  onClick={() => setIsFullscreen(!isFullscreen)}
                  className="p-2 rounded-lg border border-gray-300 text-gray-600 hover:text-gray-900 hover:bg-gray-50 transition-colors touch-manipulation min-h-[44px] min-w-[44px]"
                  title={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
                >
                  {isFullscreen ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
                </button>
              </>
            ) : (
              <>
                {/* Desktop/Tablet: Full Controls */}
                {!responsive.isTablet && (
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
                      onClick={() => setLayout('stacked')}
                      className={`p-2 rounded-md transition-colors ${
                        layout === 'stacked'
                          ? 'bg-white shadow-sm text-purple-600'
                          : 'text-gray-600 hover:text-gray-900'
                      }`}
                      title="Stacked"
                    >
                      <svg className="h-4 w-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <rect x="3" y="3" width="18" height="7" rx="1" />
                        <rect x="3" y="14" width="18" height="7" rx="1" />
                      </svg>
                    </motion.button>
                  </div>
                )}

                {/* View Toggle */}
                <div className="flex items-center space-x-1 bg-gray-100 rounded-lg p-1">
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => setLayout('petri-only')}
                    className={`p-2 rounded-md transition-colors ${
                      layout === 'petri-only'
                        ? 'bg-white shadow-sm text-purple-600'
                        : 'text-gray-600 hover:text-gray-900'
                    }`}
                    title="Petri Net Only"
                  >
                    <span className="text-xs font-bold">P</span>
                  </motion.button>

                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={() => setLayout('dag-only')}
                    className={`p-2 rounded-md transition-colors ${
                      layout === 'dag-only'
                        ? 'bg-white shadow-sm text-purple-600'
                        : 'text-gray-600 hover:text-gray-900'
                    }`}
                    title="DAG Only"
                  >
                    <span className="text-xs font-bold">D</span>
                  </motion.button>
                </div>

                {/* Cross-highlighting Toggle */}
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setCrossHighlightingEnabled(!crossHighlightingEnabled)}
                  className={`p-2 rounded-lg border transition-colors ${
                    crossHighlightingEnabled
                      ? 'bg-purple-100 border-purple-300 text-purple-700'
                      : 'bg-gray-100 border-gray-300 text-gray-600'
                  }`}
                  title={`Cross-highlighting ${crossHighlightingEnabled ? 'enabled' : 'disabled'}`}
                >
                  {crossHighlightingEnabled ? (
                    <Link className="h-4 w-4" />
                  ) : (
                    <Unlink className="h-4 w-4" />
                  )}
                </motion.button>

                {/* Advanced Controls - Desktop Only */}
                {responsive.isDesktop && (
                  <>
                    {/* Mapping Info Toggle */}
                    {enableAdvancedMapping && (
                      <motion.button
                        whileHover={{ scale: 1.05 }}
                        whileTap={{ scale: 0.95 }}
                        onClick={() => setShowMappingInfo(!showMappingInfo)}
                        className={`p-2 rounded-lg border transition-colors ${
                          showMappingInfo
                            ? 'bg-blue-100 border-blue-300 text-blue-700'
                            : 'bg-gray-100 border-gray-300 text-gray-600'
                        }`}
                        title="Show mapping information"
                      >
                        <Info className="h-4 w-4" />
                      </motion.button>
                    )}

                    {/* Advanced Settings */}
                    <motion.button
                      whileHover={{ scale: 1.05 }}
                      whileTap={{ scale: 0.95 }}
                      onClick={() => setShowAdvancedSettings(!showAdvancedSettings)}
                      className={`p-2 rounded-lg border transition-colors ${
                        showAdvancedSettings
                          ? 'bg-gray-200 border-gray-400 text-gray-700'
                          : 'bg-gray-100 border-gray-300 text-gray-600'
                      }`}
                      title="Advanced settings"
                    >
                      <Settings className="h-4 w-4" />
                    </motion.button>
                  </>
                )}

                {/* Reset Button */}
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={resetHighlighting}
                  className="p-2 rounded-lg border border-gray-300 text-gray-600 hover:text-gray-900 hover:bg-gray-50 transition-colors"
                  title="Clear Highlighting"
                >
                  <RefreshCw className="h-4 w-4" />
                </motion.button>
              </>
            )}
          </div>
        </div>
      )}

      {/* Advanced Mapping Information - Hidden on Mobile */}
      <AnimatePresence>
        {showMappingInfo && enableAdvancedMapping && !responsive.isMobile && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="bg-gradient-to-r from-blue-50 to-purple-50 border border-blue-200 rounded-lg p-4"
          >
            <div className="flex items-center justify-between mb-3">
              <h4 className={`font-semibold text-gray-900 ${
                responsive.isTablet ? 'text-base' : 'text-lg'
              }`}>Element Mapping Statistics</h4>
              <button
                onClick={() => setShowMappingInfo(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className={`grid gap-4 mb-4 ${
              responsive.isTablet ? 'grid-cols-2' : 'grid-cols-4'
            }`}>
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
                <div className="text-sm text-green-700">Mapped</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-purple-900">
                  {Math.round(mappingQuality.coverage)}%
                </div>
                <div className="text-sm text-purple-700">Coverage</div>
              </div>
              <div className="text-center">
                <div className="text-2xl font-bold text-orange-900">
                  {Math.round(mappingQuality.confidence)}%
                </div>
                <div className="text-sm text-orange-700">Confidence</div>
              </div>
            </div>

            {/* Mapping Quality Bar */}
            <div className="mb-3">
              <div className="flex justify-between text-sm text-gray-600 mb-1">
                <span>Mapping Quality</span>
                <span>{Math.round((mappingQuality.coverage + mappingQuality.confidence) / 2)}%</span>
              </div>
              <div className="w-full bg-gray-200 rounded-full h-2">
                <div
                  className={`h-2 rounded-full transition-all duration-500 ${
                    mappingQuality.coverage > 80 ? 'bg-gradient-to-r from-green-500 to-blue-500' :
                    mappingQuality.coverage > 60 ? 'bg-gradient-to-r from-yellow-500 to-green-500' :
                    'bg-gradient-to-r from-red-500 to-yellow-500'
                  }`}
                  style={{ width: `${(mappingQuality.coverage + mappingQuality.confidence) / 2}%` }}
                />
              </div>
            </div>

            <div className="text-sm text-gray-600">
              <strong>Mapping Strategy:</strong> Advanced multi-criteria matching with ID, name, and metadata correlation
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Advanced Settings Panel - Hidden on Mobile */}
      <AnimatePresence>
        {showAdvancedSettings && !responsive.isMobile && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="bg-gray-50 border border-gray-200 rounded-lg p-4"
          >
            <div className="flex items-center justify-between mb-4">
              <h4 className="text-lg font-semibold text-gray-900">Advanced Settings</h4>
              <button
                onClick={() => setShowAdvancedSettings(false)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className={`grid gap-4 ${
              responsive.isTablet ? 'grid-cols-2' : 'grid-cols-3'
            }`}>
              <div>
                <h5 className="text-sm font-medium text-gray-700 mb-2">Synchronization</h5>
                <div className="space-y-2">
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={syncStates.marking}
                      onChange={(e) => setSyncStates(prev => ({ ...prev, marking: e.target.checked }))}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">Token Marking</span>
                  </label>
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={syncStates.simulation}
                      onChange={(e) => setSyncStates(prev => ({ ...prev, simulation: e.target.checked }))}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">Simulation State</span>
                  </label>
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={syncStates.validation}
                      onChange={(e) => setSyncStates(prev => ({ ...prev, validation: e.target.checked }))}
                      className="rounded border-gray-300 text-blue-600 focus:ring-blue-500"
                    />
                    <span className="ml-2 text-sm text-gray-600">Validation Results</span>
                  </label>
                </div>
              </div>

              <div>
                <h5 className="text-sm font-medium text-gray-700 mb-2">Highlighting</h5>
                <div className="space-y-2">
                  <button
                    onClick={() => toast.info('Recalculating element mappings...')}
                    className="w-full px-3 py-2 bg-blue-600 text-white text-sm rounded-lg hover:bg-blue-700 transition-colors"
                  >
                    Refresh Mappings
                  </button>
                  <button
                    onClick={resetHighlighting}
                    className="w-full px-3 py-2 bg-gray-600 text-white text-sm rounded-lg hover:bg-gray-700 transition-colors"
                  >
                    Clear Selection
                  </button>
                </div>
              </div>

              <div>
                <h5 className="text-sm font-medium text-gray-700 mb-2">Debug Info</h5>
                <div className="text-xs text-gray-500 space-y-1">
                  <div>P3Net Elements: {(petriNet?.places?.length || 0) + (petriNet?.transitions?.length || 0)}</div>
                  <div>DAG Elements: {(dag?.nodes?.length || 0) + (dag?.edges?.length || 0)}</div>
                  <div>Active Mappings: {elementMapping.current.size}</div>
                  <div>Hover State: {hoveredElement ? `${hoveredElement.source}:${hoveredElement.element.type}` : 'None'}</div>
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Cross-highlighting Status - Compact on Mobile */}
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
                  Cross-highlighting active
                </span>
                {hoveredElement && (
                  <span className="text-xs text-blue-600 bg-blue-100 px-2 py-1 rounded">
                    {hoveredElement.source === 'petri' ? 'P3Net' : 'DAG'}: {hoveredElement.element.type}
                  </span>
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

      {/* Graph Visualizations */}
      <div className={getLayoutClasses()} {...(responsive.isMobile ? swipeHandlers : {})}>
        {/* Petri Net View */}
        {((layout === 'side-by-side' || layout === 'stacked' || layout === 'petri-only') ||
          (layout === 'single' && currentView === 'petri')) && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className={`space-y-3 ${responsive.isMobile ? 'touch-manipulation' : ''}`}
          >
            <div className={`flex items-center justify-between ${
              responsive.isMobile ? 'px-4' : ''
            }`}>
              <div>
                <h4 className={`font-semibold text-gray-900 ${
                  responsive.isMobile ? 'text-sm' : 'text-md'
                }`}>Petri Net View</h4>
                {!responsive.isMobile && (
                  <p className="text-xs text-gray-600">Places, transitions, and token flow</p>
                )}
              </div>
              {!responsive.isMobile && (
                <div className="flex items-center space-x-2">
                  <div className="w-3 h-3 rounded-full bg-gray-200 border-2 border-gray-400"></div>
                  <span className="text-xs text-gray-600">Place</span>
                  <div className="w-3 h-2 bg-yellow-200 border border-yellow-600"></div>
                  <span className="text-xs text-gray-600">Transition</span>
                </div>
              )}
              {/* Mobile navigation arrows */}
              {responsive.isMobile && layout === 'single' && petriNet && dag && (
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => setCurrentView('dag')}
                    className="p-2 rounded-lg border border-gray-300 text-gray-600 hover:text-gray-900 hover:bg-gray-50 transition-colors touch-manipulation"
                    title="View DAG"
                  >
                    <ChevronRight className="h-4 w-4" />
                  </button>
                </div>
              )}
            </div>

            <RealTimePetriNetVisualizer
              petriNet={petriNet}
              onPetriNetChange={onPetriNetChange}
              highlightedElements={highlightedPetriElements}
              onElementClick={handlePetriElementClick}
              onElementHover={responsive.isMobile ? null : handlePetriElementHover}
              enableRealTime={enableRealTime && !responsive.isMobile}
              showControls={layout === 'petri-only' && !compactMode}
              className={getVisualizationHeight()}
              isMobile={responsive.isMobile}
              compactMode={compactMode}
            />
          </motion.div>
        )}

        {/* DAG View */}
        {((layout === 'side-by-side' || layout === 'stacked' || layout === 'dag-only') ||
          (layout === 'single' && currentView === 'dag')) && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className={`space-y-3 ${responsive.isMobile ? 'touch-manipulation' : ''}`}
          >
            <div className={`flex items-center justify-between ${
              responsive.isMobile ? 'px-4' : ''
            }`}>
              {/* Mobile navigation arrows */}
              {responsive.isMobile && layout === 'single' && petriNet && dag && (
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => setCurrentView('petri')}
                    className="p-2 rounded-lg border border-gray-300 text-gray-600 hover:text-gray-900 hover:bg-gray-50 transition-colors touch-manipulation"
                    title="View Petri Net"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </button>
                </div>
              )}
              <div className={responsive.isMobile ? 'flex-1 text-center' : ''}>
                <h4 className={`font-semibold text-gray-900 ${
                  responsive.isMobile ? 'text-sm' : 'text-md'
                }`}>DAG View</h4>
                {!responsive.isMobile && (
                  <p className="text-xs text-gray-600">Simplified task dependencies</p>
                )}
              </div>
              {!responsive.isMobile && (
                <div className="flex items-center space-x-2">
                  <div className="w-4 h-2 bg-blue-200 border border-blue-600 rounded"></div>
                  <span className="text-xs text-gray-600">Task</span>
                  <svg className="w-3 h-2" viewBox="0 0 12 8">
                    <line x1="0" y1="4" x2="8" y2="4" stroke="#0284c7" strokeWidth="1" markerEnd="url(#arrowhead-small)" />
                    <defs>
                      <marker id="arrowhead-small" markerWidth="8" markerHeight="6" refX="7" refY="3" orient="auto">
                        <polygon points="0 0, 8 3, 0 6" fill="#0284c7" />
                      </marker>
                    </defs>
                  </svg>
                  <span className="text-xs text-gray-600">Flow</span>
                </div>
              )}
              {responsive.isMobile && (
                <div className="w-8"></div> // Spacer for centering
              )}
            </div>

            <DagVisualizer
              dag={dag}
              highlightedElements={highlightedDagElements}
              onElementClick={handleDagElementClick}
              onElementHover={responsive.isMobile ? null : handleDagElementHover}
              showControls={layout === 'dag-only' && !compactMode}
              className={getVisualizationHeight()}
              isMobile={responsive.isMobile}
              compactMode={compactMode}
            />
          </motion.div>
        )}
      </div>

      {/* Selected Element Details - Responsive */}
      <AnimatePresence>
        {selectedElement && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 20 }}
            className={`bg-white border border-gray-200 rounded-lg shadow-lg ${
              responsive.isMobile ? 'p-3 mx-4' : 'p-4'
            }`}
          >
            <div className="flex items-start justify-between mb-3">
              <h4 className="text-lg font-semibold text-gray-900">Selected Element Details</h4>
              <button
                onClick={() => setSelectedElement(null)}
                className="text-gray-400 hover:text-gray-600"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className={`grid gap-4 ${
              responsive.isMobile ? 'grid-cols-1' : 'md:grid-cols-2'
            }`}>
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

              {/* Mapped Elements */}
              <div className="bg-blue-50 rounded-lg p-3">
                <h5 className="text-sm font-medium text-gray-700 mb-2">
                  Mapped to ({selectedElement.source === 'petri' ? 'DAG' : 'P3Net'})
                </h5>
                {selectedElement.mapped.length > 0 ? (
                  <div className="space-y-2">
                    {selectedElement.mapped.map((mappedId, index) => (
                      <div key={index} className="bg-white rounded px-2 py-1 text-sm">
                        <strong>ID:</strong> {mappedId}
                      </div>
                    ))}
                    <div className="text-xs text-blue-600 mt-2">
                      <Zap className="h-3 w-3 inline mr-1" />
                      {selectedElement.mapped.length} cross-highlighted element{selectedElement.mapped.length !== 1 ? 's' : ''}
                    </div>
                  </div>
                ) : (
                  <div className="text-sm text-gray-500 italic">
                    No mapped elements found
                  </div>
                )}
              </div>
            </div>

            {/* Mapping Quality Indicator */}
            {selectedElement.mapping && (
              <div className="mt-3 p-2 bg-green-50 border border-green-200 rounded">
                <div className="flex items-center space-x-2">
                  <CheckCircle className="h-4 w-4 text-green-600" />
                  <span className="text-sm text-green-800">
                    Mapping Confidence: {Math.round((selectedElement.mapping.confidence || 0.5) * 100)}%
                  </span>
                </div>
              </div>
            )}
          </motion.div>
        )}
      </AnimatePresence>

      {/* Cross-highlighting Legend - Hidden on Mobile */}
      {crossHighlightingEnabled && !selectedElement && !responsive.isMobile && (
        <div className="bg-gradient-to-r from-gray-50 to-blue-50 rounded-lg border border-gray-200 p-4">
          <div className="flex items-center space-x-2 mb-3">
            <GitBranch className="h-5 w-5 text-blue-600" />
            <h5 className="text-sm font-medium text-gray-900">Cross-highlighting Guide</h5>
          </div>

          <div className={`grid gap-6 text-sm ${
            responsive.isTablet ? 'grid-cols-1' : 'md:grid-cols-2'
          }`}>
            <div className="space-y-3">
              <div>
                <h6 className="font-medium text-purple-800 mb-2">P3Net → DAG Mapping</h6>
                <ul className="space-y-1 text-gray-600">
                  <li className="flex items-center">
                    <div className="w-2 h-2 bg-yellow-200 border border-yellow-600 rounded mr-2"></div>
                    Transitions → Task nodes
                  </li>
                  <li className="flex items-center">
                    <div className="w-2 h-2 bg-gray-200 border border-gray-400 rounded-full mr-2"></div>
                    Places → Edge metadata
                  </li>
                </ul>
              </div>

              <div className="text-xs text-gray-500">
                <strong>Quality:</strong> {Math.round(mappingQuality.coverage)}% coverage,
                {Math.round(mappingQuality.confidence)}% confidence
              </div>
            </div>

            <div className="space-y-3">
              <div>
                <h6 className="font-medium text-blue-800 mb-2">DAG → P3Net Mapping</h6>
                <ul className="space-y-1 text-gray-600">
                  <li className="flex items-center">
                    <div className="w-2 h-2 bg-blue-200 border border-blue-600 rounded mr-2"></div>
                    Task nodes → Transitions
                  </li>
                  <li className="flex items-center">
                    <ArrowRightLeft className="h-3 w-3 text-gray-400 mr-2" />
                    Edge flows → Places
                  </li>
                </ul>
              </div>

              <div className="text-xs text-blue-600 bg-blue-100 px-2 py-1 rounded">
                <Info className="h-3 w-3 inline mr-1" />
                Hover elements to see cross-highlighting
              </div>
            </div>
          </div>

          <div className="mt-4 pt-3 border-t border-gray-200">
            <div className="flex items-center justify-between text-xs text-gray-500">
              <span>Advanced multi-criteria element mapping enabled</span>
              <span>{elementMapping.current.size} active mappings</span>
            </div>
          </div>
        </div>
      )}

      {/* Mobile Swipe Hint */}
      {responsive.isMobile && layout === 'single' && petriNet && dag && (
        <div className="text-xs text-gray-500 text-center py-2">
          Swipe left or right to switch between Petri Net and DAG views
        </div>
      )}
    </motion.div>
  );
};

export default DualGraphView;