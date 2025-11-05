import React, { useState, useCallback, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Plus, Save, Upload, Download, Trash2, Edit3, Play,
  CheckCircle, AlertTriangle, Loader, Zap, GitBranch,
  Eye, EyeOff, Settings, RefreshCw, Copy, Grid
} from 'lucide-react';
import toast from 'react-hot-toast';
import apiService from '../services/api';
import RealTimePetriNetVisualizer from './RealTimePetriNetVisualizer';

const InteractiveWorkflowBuilder = ({
  onWorkflowChange,
  onValidationResult,
  initialWorkflow = null,
  className = ""
}) => {
  // Workflow state
  const [workflow, setWorkflow] = useState(initialWorkflow || {
    name: 'New Workflow',
    description: 'Interactive workflow built with P3Net',
    places: [],
    transitions: [],
    arcs: [],
    initialMarking: {}
  });

  // Builder state
  const [selectedTool, setSelectedTool] = useState('select');
  const [selectedElement, setSelectedElement] = useState(null);
  const [isBuilding, setIsBuilding] = useState(false);
  const [showGrid, setShowGrid] = useState(true);
  const [snapToGrid, setSnapToGrid] = useState(true);

  // Validation state
  const [isValidating, setIsValidating] = useState(false);
  const [validationResult, setValidationResult] = useState(null);
  const [showValidationPanel, setShowValidationPanel] = useState(false);

  // Natural language processing state
  const [naturalLanguageInput, setNaturalLanguageInput] = useState('');
  const [isProcessingNL, setIsProcessingNL] = useState(false);
  const [nlSuggestions, setNlSuggestions] = useState([]);

  // UI state
  const [showProperties, setShowProperties] = useState(false);
  const [showAdvancedOptions, setShowAdvancedOptions] = useState(false);

  // WebSocket connection for real-time validation
  const webSocketRef = useRef(null);
  const [isConnected, setIsConnected] = useState(false);

  // Initialize WebSocket connection
  useEffect(() => {
    const callbacks = {
      onConnect: () => {
        setIsConnected(true);
        toast.success('Connected to real-time validation service');
      },

      onValidationProgress: (message) => {
        console.log('Real-time validation progress:', message);
        // Update validation progress UI
      },

      onValidationCompleted: (message) => {
        console.log('Real-time validation completed:', message);
        setIsValidating(false);
        setValidationResult(message.result);
        setShowValidationPanel(true);

        if (onValidationResult) {
          onValidationResult(message.result);
        }

        toast[message.result?.isValid ? 'success' : 'error'](
          message.result?.isValid ? 'Workflow validated successfully!' : 'Workflow validation failed'
        );
      },

      onError: (error) => {
        console.error('Validation WebSocket error:', error);
        setIsValidating(false);
        toast.error(`Validation error: ${error.error?.message || 'Connection error'}`);
      },

      onClose: () => {
        setIsConnected(false);
      }
    };

    try {
      webSocketRef.current = apiService.petri.createWebSocketConnection(callbacks);
    } catch (error) {
      console.error('Failed to create validation WebSocket connection:', error);
    }

    return () => {
      if (webSocketRef.current) {
        webSocketRef.current.close();
      }
    };
  }, [onValidationResult]);

  // Handle natural language to P3Net conversion
  const handleNaturalLanguageProcess = useCallback(async () => {
    if (!naturalLanguageInput.trim()) {
      toast.error('Please enter a workflow description');
      return;
    }

    setIsProcessingNL(true);

    try {
      // First parse the natural language
      const parseResponse = await apiService.petri.parse({
        text: naturalLanguageInput,
        templateHint: 'workflow-builder'
      });

      if (!parseResponse.success) {
        throw new Error(parseResponse.error?.message || 'Failed to parse natural language');
      }

      // Then build the Petri net
      const buildResponse = await apiService.petri.build({
        intent: parseResponse.intent
      });

      if (!buildResponse.success) {
        throw new Error(buildResponse.error?.message || 'Failed to build Petri net');
      }

      // Update the workflow with the generated P3Net
      const newWorkflow = {
        ...workflow,
        ...buildResponse.petriNet,
        name: parseResponse.intent.name || workflow.name,
        description: parseResponse.intent.description || naturalLanguageInput
      };

      setWorkflow(newWorkflow);
      if (onWorkflowChange) {
        onWorkflowChange(newWorkflow);
      }

      toast.success('Workflow generated from natural language!');
      setNaturalLanguageInput('');

      // Automatically validate the generated workflow
      if (isConnected) {
        setTimeout(() => startRealTimeValidation(newWorkflow), 1000);
      }

    } catch (error) {
      console.error('Natural language processing error:', error);
      toast.error(`Failed to process natural language: ${error.message}`);
    } finally {
      setIsProcessingNL(false);
    }
  }, [naturalLanguageInput, workflow, onWorkflowChange, isConnected]);

  // Start real-time validation
  const startRealTimeValidation = useCallback((workflowToValidate = workflow) => {
    if (!webSocketRef.current || !isConnected) {
      toast.error('Not connected to validation service');
      return;
    }

    if (!workflowToValidate.places?.length || !workflowToValidate.transitions?.length) {
      toast.error('Workflow must have at least one place and one transition to validate');
      return;
    }

    setIsValidating(true);
    setValidationResult(null);

    // Send validation request via WebSocket
    webSocketRef.current.sendMessage('VALIDATION_START', {
      petriNet: workflowToValidate,
      config: {
        enableDeadlockCheck: true,
        enableReachabilityCheck: true,
        enableLivenessCheck: true,
        enableBoundednessCheck: true,
        maxStates: 50000,
        maxTimeMs: 30000
      }
    });

    toast.success('Starting real-time validation...');
  }, [workflow, isConnected]);

  // Add new place
  const addPlace = useCallback(() => {
    const newPlace = {
      id: `place_${Date.now()}`,
      name: `Place ${workflow.places.length + 1}`,
      description: 'New place',
      metadata: {}
    };

    const updatedWorkflow = {
      ...workflow,
      places: [...workflow.places, newPlace]
    };

    setWorkflow(updatedWorkflow);
    if (onWorkflowChange) {
      onWorkflowChange(updatedWorkflow);
    }

    setSelectedElement({ type: 'place', data: newPlace });
    setShowProperties(true);
  }, [workflow, onWorkflowChange]);

  // Add new transition
  const addTransition = useCallback(() => {
    const newTransition = {
      id: `transition_${Date.now()}`,
      name: `Transition ${workflow.transitions.length + 1}`,
      description: 'New transition',
      metadata: {}
    };

    const updatedWorkflow = {
      ...workflow,
      transitions: [...workflow.transitions, newTransition]
    };

    setWorkflow(updatedWorkflow);
    if (onWorkflowChange) {
      onWorkflowChange(updatedWorkflow);
    }

    setSelectedElement({ type: 'transition', data: newTransition });
    setShowProperties(true);
  }, [workflow, onWorkflowChange]);

  // Add new arc
  const addArc = useCallback((fromId, toId) => {
    const newArc = {
      from: fromId,
      to: toId,
      weight: 1,
      metadata: {}
    };

    const updatedWorkflow = {
      ...workflow,
      arcs: [...workflow.arcs, newArc]
    };

    setWorkflow(updatedWorkflow);
    if (onWorkflowChange) {
      onWorkflowChange(updatedWorkflow);
    }
  }, [workflow, onWorkflowChange]);

  // Update element properties
  const updateElementProperties = useCallback((elementType, elementId, updates) => {
    let updatedWorkflow = { ...workflow };

    if (elementType === 'place') {
      updatedWorkflow.places = workflow.places.map(place =>
        place.id === elementId ? { ...place, ...updates } : place
      );
    } else if (elementType === 'transition') {
      updatedWorkflow.transitions = workflow.transitions.map(transition =>
        transition.id === elementId ? { ...transition, ...updates } : transition
      );
    }

    setWorkflow(updatedWorkflow);
    if (onWorkflowChange) {
      onWorkflowChange(updatedWorkflow);
    }
  }, [workflow, onWorkflowChange]);

  // Delete element
  const deleteElement = useCallback((elementType, elementId) => {
    let updatedWorkflow = { ...workflow };

    if (elementType === 'place') {
      updatedWorkflow.places = workflow.places.filter(place => place.id !== elementId);
      // Remove arcs connected to this place
      updatedWorkflow.arcs = workflow.arcs.filter(arc =>
        arc.from !== elementId && arc.to !== elementId
      );
      // Remove from initial marking
      const newMarking = { ...workflow.initialMarking };
      delete newMarking[elementId];
      updatedWorkflow.initialMarking = newMarking;
    } else if (elementType === 'transition') {
      updatedWorkflow.transitions = workflow.transitions.filter(
        transition => transition.id !== elementId
      );
      // Remove arcs connected to this transition
      updatedWorkflow.arcs = workflow.arcs.filter(arc =>
        arc.from !== elementId && arc.to !== elementId
      );
    }

    setWorkflow(updatedWorkflow);
    if (onWorkflowChange) {
      onWorkflowChange(updatedWorkflow);
    }

    setSelectedElement(null);
    setShowProperties(false);
  }, [workflow, onWorkflowChange]);

  // Save workflow
  const saveWorkflow = useCallback(async () => {
    try {
      const blob = new Blob([JSON.stringify(workflow, null, 2)], {
        type: 'application/json'
      });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `${workflow.name.replace(/[^a-z0-9]/gi, '_').toLowerCase()}.json`;
      link.click();
      URL.revokeObjectURL(link.href);

      toast.success('Workflow saved successfully!');
    } catch (error) {
      console.error('Save error:', error);
      toast.error('Failed to save workflow');
    }
  }, [workflow]);

  // Load workflow
  const loadWorkflow = useCallback((event) => {
    const file = event.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const loadedWorkflow = JSON.parse(e.target.result);
        setWorkflow(loadedWorkflow);
        if (onWorkflowChange) {
          onWorkflowChange(loadedWorkflow);
        }
        toast.success('Workflow loaded successfully!');
      } catch (error) {
        console.error('Load error:', error);
        toast.error('Failed to load workflow file');
      }
    };
    reader.readAsText(file);
  }, [onWorkflowChange]);

  // Handle element selection from visualizer
  const handleElementClick = useCallback((element) => {
    setSelectedElement(element);
    setShowProperties(true);
  }, []);

  const tools = [
    { id: 'select', icon: Edit3, label: 'Select' },
    { id: 'place', icon: Circle, label: 'Add Place' },
    { id: 'transition', icon: Square, label: 'Add Transition' },
    { id: 'arc', icon: GitBranch, label: 'Add Arc' }
  ];

  // Simple Circle and Square components for tools
  const Circle = ({ className }) => (
    <div className={`w-4 h-4 rounded-full border-2 border-current ${className}`} />
  );

  const Square = ({ className }) => (
    <div className={`w-4 h-4 border-2 border-current ${className}`} />
  );

  return (
    <div className={`bg-white rounded-xl border border-gray-200 shadow-lg ${className}`}>
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-100">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-xl font-semibold text-gray-900">Interactive Workflow Builder</h2>
            <p className="text-sm text-gray-600 mt-1">
              Build P3Net workflows visually or from natural language
            </p>
          </div>
          <div className="flex items-center space-x-2">
            {isConnected && (
              <div className="flex items-center space-x-2 px-3 py-1 bg-green-100 text-green-700 rounded-full text-sm">
                <CheckCircle className="h-3 w-3" />
                <span>Connected</span>
              </div>
            )}
          </div>
        </div>
      </div>

      <div className="flex">
        {/* Left Panel - Natural Language Input & Tools */}
        <div className="w-80 border-r border-gray-100 flex flex-col">
          {/* Natural Language Input */}
          <div className="p-4 border-b border-gray-100">
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Describe your workflow in natural language:
            </label>
            <textarea
              value={naturalLanguageInput}
              onChange={(e) => setNaturalLanguageInput(e.target.value)}
              placeholder="e.g., Send an email to john@example.com, then create a file called report.txt, then upload it to Google Drive"
              className="w-full h-20 px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <motion.button
              onClick={handleNaturalLanguageProcess}
              disabled={isProcessingNL || !naturalLanguageInput.trim()}
              className="mt-2 w-full flex items-center justify-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
            >
              {isProcessingNL ? (
                <>
                  <Loader className="h-4 w-4 animate-spin" />
                  <span>Processing...</span>
                </>
              ) : (
                <>
                  <Zap className="h-4 w-4" />
                  <span>Generate P3Net</span>
                </>
              )}
            </motion.button>
          </div>

          {/* Building Tools */}
          <div className="p-4 border-b border-gray-100">
            <h3 className="text-sm font-medium text-gray-900 mb-3">Building Tools</h3>
            <div className="grid grid-cols-2 gap-2">
              {tools.map(({ id, icon: Icon, label }) => (
                <motion.button
                  key={id}
                  onClick={() => setSelectedTool(id)}
                  className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-sm transition-colors ${
                    selectedTool === id
                      ? 'bg-blue-100 text-blue-700 border border-blue-300'
                      : 'bg-gray-50 text-gray-700 hover:bg-gray-100'
                  }`}
                  whileHover={{ scale: 1.02 }}
                  whileTap={{ scale: 0.98 }}
                >
                  <Icon className="h-4 w-4" />
                  <span>{label}</span>
                </motion.button>
              ))}
            </div>
          </div>

          {/* Quick Actions */}
          <div className="p-4 border-b border-gray-100">
            <h3 className="text-sm font-medium text-gray-900 mb-3">Quick Actions</h3>
            <div className="space-y-2">
              <motion.button
                onClick={addPlace}
                className="w-full flex items-center space-x-2 px-3 py-2 bg-green-50 text-green-700 rounded-lg text-sm hover:bg-green-100 transition-colors"
                whileHover={{ scale: 1.02 }}
              >
                <Plus className="h-4 w-4" />
                <span>Add Place</span>
              </motion.button>

              <motion.button
                onClick={addTransition}
                className="w-full flex items-center space-x-2 px-3 py-2 bg-yellow-50 text-yellow-700 rounded-lg text-sm hover:bg-yellow-100 transition-colors"
                whileHover={{ scale: 1.02 }}
              >
                <Plus className="h-4 w-4" />
                <span>Add Transition</span>
              </motion.button>

              <motion.button
                onClick={() => startRealTimeValidation()}
                disabled={!isConnected || isValidating}
                className="w-full flex items-center space-x-2 px-3 py-2 bg-purple-50 text-purple-700 rounded-lg text-sm hover:bg-purple-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                whileHover={{ scale: 1.02 }}
              >
                {isValidating ? (
                  <Loader className="h-4 w-4 animate-spin" />
                ) : (
                  <CheckCircle className="h-4 w-4" />
                )}
                <span>Validate</span>
              </motion.button>
            </div>
          </div>

          {/* File Operations */}
          <div className="p-4">
            <h3 className="text-sm font-medium text-gray-900 mb-3">File Operations</h3>
            <div className="space-y-2">
              <motion.button
                onClick={saveWorkflow}
                className="w-full flex items-center space-x-2 px-3 py-2 bg-gray-50 text-gray-700 rounded-lg text-sm hover:bg-gray-100 transition-colors"
                whileHover={{ scale: 1.02 }}
              >
                <Download className="h-4 w-4" />
                <span>Save Workflow</span>
              </motion.button>

              <label className="w-full flex items-center space-x-2 px-3 py-2 bg-gray-50 text-gray-700 rounded-lg text-sm hover:bg-gray-100 transition-colors cursor-pointer">
                <Upload className="h-4 w-4" />
                <span>Load Workflow</span>
                <input
                  type="file"
                  accept=".json"
                  onChange={loadWorkflow}
                  className="hidden"
                />
              </label>
            </div>
          </div>
        </div>

        {/* Main Visualization Area */}
        <div className="flex-1 flex flex-col">
          {/* Workflow Info */}
          <div className="p-4 border-b border-gray-100">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="font-medium text-gray-900">{workflow.name}</h3>
                <p className="text-sm text-gray-600">{workflow.description}</p>
              </div>
              <div className="flex items-center space-x-4 text-sm text-gray-500">
                <span>{workflow.places?.length || 0} Places</span>
                <span>{workflow.transitions?.length || 0} Transitions</span>
                <span>{workflow.arcs?.length || 0} Arcs</span>
              </div>
            </div>
          </div>

          {/* Real-time Visualizer */}
          <div className="flex-1 p-4">
            <RealTimePetriNetVisualizer
              petriNet={workflow}
              onPetriNetChange={setWorkflow}
              onElementClick={handleElementClick}
              enableRealTime={true}
              showControls={true}
              className="h-full"
            />
          </div>
        </div>

        {/* Right Panel - Properties */}
        <AnimatePresence>
          {showProperties && selectedElement && (
            <motion.div
              initial={{ width: 0, opacity: 0 }}
              animate={{ width: 320, opacity: 1 }}
              exit={{ width: 0, opacity: 0 }}
              className="border-l border-gray-100 bg-gray-50"
            >
              <ElementPropertiesPanel
                element={selectedElement}
                onUpdate={(updates) => updateElementProperties(
                  selectedElement.type,
                  selectedElement.data.id || selectedElement.id,
                  updates
                )}
                onDelete={() => deleteElement(
                  selectedElement.type,
                  selectedElement.data.id || selectedElement.id
                )}
                onClose={() => {
                  setShowProperties(false);
                  setSelectedElement(null);
                }}
              />
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Validation Results Modal */}
      <AnimatePresence>
        {showValidationPanel && validationResult && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50"
            onClick={() => setShowValidationPanel(false)}
          >
            <motion.div
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white rounded-xl shadow-2xl max-w-2xl w-full m-4 max-h-[80vh] overflow-y-auto"
              onClick={(e) => e.stopPropagation()}
            >
              <ValidationResultsPanel
                result={validationResult}
                onClose={() => setShowValidationPanel(false)}
              />
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

// Element Properties Panel Component
const ElementPropertiesPanel = ({ element, onUpdate, onDelete, onClose }) => {
  const [localData, setLocalData] = useState(element.data || element);

  const handleUpdate = (field, value) => {
    const updated = { ...localData, [field]: value };
    setLocalData(updated);
    onUpdate(updated);
  };

  return (
    <div className="h-full flex flex-col">
      <div className="p-4 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h3 className="font-medium text-gray-900">
            {element.type.charAt(0).toUpperCase() + element.type.slice(1)} Properties
          </h3>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600">
            ×
          </button>
        </div>
      </div>

      <div className="flex-1 p-4 space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Name
          </label>
          <input
            type="text"
            value={localData.name || ''}
            onChange={(e) => handleUpdate('name', e.target.value)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            Description
          </label>
          <textarea
            value={localData.description || ''}
            onChange={(e) => handleUpdate('description', e.target.value)}
            className="w-full h-16 px-3 py-2 border border-gray-300 rounded-lg text-sm resize-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        {element.type === 'place' && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Initial Tokens
            </label>
            <input
              type="number"
              min="0"
              value={localData.initialTokens || 0}
              onChange={(e) => handleUpdate('initialTokens', parseInt(e.target.value) || 0)}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
        )}

        <div className="pt-4 border-t border-gray-200">
          <motion.button
            onClick={() => onDelete()}
            className="w-full flex items-center justify-center space-x-2 px-4 py-2 bg-red-600 text-white rounded-lg font-medium hover:bg-red-700 transition-colors"
            whileHover={{ scale: 1.02 }}
            whileTap={{ scale: 0.98 }}
          >
            <Trash2 className="h-4 w-4" />
            <span>Delete Element</span>
          </motion.button>
        </div>
      </div>
    </div>
  );
};

// Validation Results Panel Component
const ValidationResultsPanel = ({ result, onClose }) => {
  const isValid = result?.isValid;

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center space-x-3">
          {isValid ? (
            <CheckCircle className="h-6 w-6 text-green-600" />
          ) : (
            <AlertTriangle className="h-6 w-6 text-red-600" />
          )}
          <h2 className="text-xl font-semibold text-gray-900">
            Validation {isValid ? 'Passed' : 'Failed'}
          </h2>
        </div>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-gray-600"
        >
          ×
        </button>
      </div>

      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-gray-50 p-3 rounded-lg">
            <div className="text-sm font-medium text-gray-700">Status</div>
            <div className={`text-lg font-semibold ${isValid ? 'text-green-600' : 'text-red-600'}`}>
              {result.status || (isValid ? 'VALID' : 'INVALID')}
            </div>
          </div>
          <div className="bg-gray-50 p-3 rounded-lg">
            <div className="text-sm font-medium text-gray-700">States Explored</div>
            <div className="text-lg font-semibold text-gray-900">
              {result.statesExplored || 0}
            </div>
          </div>
        </div>

        {result.checks && (
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-3">Check Results</h3>
            <div className="space-y-2">
              {Object.entries(result.checks).map(([check, status]) => (
                <div key={check} className="flex items-center justify-between p-2 bg-gray-50 rounded-lg">
                  <span className="text-sm text-gray-700 capitalize">
                    {check.replace(/([A-Z])/g, ' $1').toLowerCase()}
                  </span>
                  <span className={`text-sm font-medium ${
                    status === 'PASSED' ? 'text-green-600' : 'text-red-600'
                  }`}>
                    {status}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}

        {result.hints && result.hints.length > 0 && (
          <div>
            <h3 className="text-lg font-medium text-gray-900 mb-3">Suggestions</h3>
            <ul className="space-y-2">
              {result.hints.map((hint, index) => (
                <li key={index} className="text-sm text-gray-600 bg-blue-50 p-2 rounded-lg">
                  {hint}
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="mt-6 flex justify-end">
        <motion.button
          onClick={onClose}
          className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition-colors"
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          Close
        </motion.button>
      </div>
    </div>
  );
};

export default InteractiveWorkflowBuilder;