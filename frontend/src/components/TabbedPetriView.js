import React, { useState } from 'react';
import { CheckCircle, GitBranch, Activity, List } from 'lucide-react';
import DualGraphView from './DualGraphView';

/**
 * Tabbed view wrapper for P3Net results
 *
 * Tabs:
 * 1. Workflow - Simple, user-friendly task list
 * 2. Petri Net - Full formal model visualization
 * 3. Execution - Step-by-step trace
 */
const TabbedPetriView = ({
  petriNet,
  dag,
  validationResult,
  simulationResult,
  selectedElement,
  onElementSelect,
  layout = 'horizontal',
  simulationSpeed = 1,
  isSimulationPlaying = false
}) => {
  const [activeTab, setActiveTab] = useState('workflow');

  const tabs = [
    { id: 'workflow', label: 'Your Workflow', icon: List },
    { id: 'petri', label: 'Formal Model', icon: GitBranch },
    { id: 'execution', label: 'Execution Trace', icon: Activity }
  ];

  return (
    <div className="flex flex-col h-full bg-white dark:bg-gray-900 rounded-lg shadow-sm">
      {/* Tab Navigation */}
      <div className="flex border-b border-gray-200 dark:border-gray-700">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.id;
          return (
            <button
              key={tab.id}
              onClick={() => setActiveTab(tab.id)}
              className={`
                flex-1 flex items-center justify-center gap-2 px-4 py-3 text-sm font-medium
                transition-all duration-200
                ${isActive
                  ? 'text-blue-600 dark:text-blue-400 border-b-2 border-blue-600 dark:border-blue-400 bg-blue-50 dark:bg-blue-900/20'
                  : 'text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-200 hover:bg-gray-50 dark:hover:bg-gray-800'
                }
              `}
            >
              <Icon size={18} />
              <span className="hidden sm:inline">{tab.label}</span>
              <span className="sm:hidden">{tab.label.split(' ')[0]}</span>
            </button>
          );
        })}
      </div>

      {/* Tab Content */}
      <div className="flex-1 overflow-auto">
        {activeTab === 'workflow' && (
          <WorkflowView petriNet={petriNet} validationResult={validationResult} />
        )}
        {activeTab === 'petri' && (
          <PetriNetView
            petriNet={petriNet}
            dag={dag}
            selectedElement={selectedElement}
            onElementSelect={onElementSelect}
            layout={layout}
            simulationSpeed={simulationSpeed}
            isSimulationPlaying={isSimulationPlaying}
            validationResult={validationResult}
          />
        )}
        {activeTab === 'execution' && (
          <ExecutionView simulationResult={simulationResult} />
        )}
      </div>
    </div>
  );
};

/**
 * Tab 1: Simplified Workflow View
 * Shows just the user-facing tasks without Petri net theory
 */
const WorkflowView = ({ petriNet, validationResult }) => {
  if (!petriNet) {
    return (
      <div className="flex items-center justify-center h-full text-gray-500 dark:text-gray-400">
        <p>No workflow to display</p>
      </div>
    );
  }

  const workflowSteps = petriNet.workflowSummary || [];
  const isVerified = validationResult?.validationResult?.petriStatus === 'PASS';

  return (
    <div className="p-6 space-y-6">
      {/* Verification Badge */}
      {validationResult && (
        <div className={`
          flex items-center gap-2 px-4 py-3 rounded-lg
          ${isVerified
            ? 'bg-green-50 dark:bg-green-900/20 text-green-800 dark:text-green-300 border border-green-200 dark:border-green-800'
            : 'bg-yellow-50 dark:bg-yellow-900/20 text-yellow-800 dark:text-yellow-300 border border-yellow-200 dark:border-yellow-800'
          }
        `}>
          <CheckCircle size={20} />
          <div>
            <div className="font-semibold">
              {isVerified ? '✓ Workflow Verified Safe' : '⚠ Verification Issues'}
            </div>
            <div className="text-sm opacity-80">
              {isVerified
                ? 'No deadlocks detected • Will complete successfully'
                : validationResult.validationResult?.summary || 'Check formal model for details'
              }
            </div>
          </div>
        </div>
      )}

      {/* Workflow Steps */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Workflow Steps
        </h3>
        <div className="space-y-3">
          {workflowSteps.map((step, index) => (
            <div
              key={index}
              className="flex items-start gap-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700"
            >
              {/* Step Number */}
              <div className="flex-shrink-0 w-8 h-8 flex items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 font-semibold text-sm">
                {index + 1}
              </div>

              {/* Step Details */}
              <div className="flex-1 min-w-0">
                <div className="font-medium text-gray-900 dark:text-gray-100">
                  {step.name || step.description}
                </div>
                {step.description && step.name !== step.description && (
                  <div className="text-sm text-gray-600 dark:text-gray-400 mt-1">
                    {step.description}
                  </div>
                )}
                {step.stepType && step.stepType !== 'ACTION' && (
                  <div className="text-xs text-gray-500 dark:text-gray-500 mt-1">
                    Type: {step.stepType}
                  </div>
                )}
              </div>

              {/* Plugin Info (if available) */}
              {step.plugin && (
                <div className="flex-shrink-0 px-3 py-1 bg-purple-100 dark:bg-purple-900/30 text-purple-700 dark:text-purple-300 rounded-full text-xs font-medium">
                  {step.plugin}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-4 pt-4 border-t border-gray-200 dark:border-gray-700">
        <div className="text-center">
          <div className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            {workflowSteps.length}
          </div>
          <div className="text-sm text-gray-600 dark:text-gray-400">Steps</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            {petriNet.places?.length || 0}
          </div>
          <div className="text-sm text-gray-600 dark:text-gray-400">States</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-gray-900 dark:text-gray-100">
            {isVerified ? '✓' : '⚠'}
          </div>
          <div className="text-sm text-gray-600 dark:text-gray-400">Status</div>
        </div>
      </div>
    </div>
  );
};

/**
 * Tab 2: Full Petri Net Formal Model
 * Uses the existing DualGraphView visualization
 */
const PetriNetView = ({
  petriNet,
  dag,
  selectedElement,
  onElementSelect,
  layout,
  simulationSpeed,
  isSimulationPlaying,
  validationResult
}) => {
  if (!petriNet || !dag) {
    return (
      <div className="flex items-center justify-center h-full text-gray-500 dark:text-gray-400">
        <p>No Petri net to display</p>
      </div>
    );
  }

  return (
    <div className="h-full">
      <DualGraphView
        petriNet={petriNet}
        dag={dag}
        validationResult={validationResult}
        traceEvents={[]}
        selectedElement={selectedElement}
        onElementSelect={onElementSelect}
        layout={layout}
        simulationSpeed={simulationSpeed}
        isSimulationPlaying={isSimulationPlaying}
      />
    </div>
  );
};

/**
 * Tab 3: Execution Trace
 * Shows step-by-step what happened during simulation
 */
const ExecutionView = ({ simulationResult }) => {
  if (!simulationResult) {
    return (
      <div className="flex items-center justify-center h-full text-gray-500 dark:text-gray-400">
        <p>No execution trace available. Run simulation first.</p>
      </div>
    );
  }

  const trace = simulationResult.trace || [];

  return (
    <div className="p-6 space-y-6">
      {/* Summary */}
      <div className="flex items-center justify-between p-4 bg-blue-50 dark:bg-blue-900/20 rounded-lg border border-blue-200 dark:border-blue-800">
        <div>
          <div className="font-semibold text-blue-900 dark:text-blue-100">
            Simulation {simulationResult.success ? 'Completed' : 'Failed'}
          </div>
          <div className="text-sm text-blue-700 dark:text-blue-300">
            {simulationResult.stepsExecuted} steps executed • Status: {simulationResult.status}
          </div>
        </div>
      </div>

      {/* Execution Steps */}
      <div>
        <h3 className="text-lg font-semibold text-gray-900 dark:text-gray-100 mb-4">
          Execution Trace
        </h3>
        <div className="space-y-3">
          {trace.map((event, index) => (
            <div
              key={index}
              className="relative pl-8 pb-4 border-l-2 border-gray-300 dark:border-gray-600 last:border-l-0 last:pb-0"
            >
              {/* Step Marker */}
              <div className="absolute left-[-9px] top-0 w-4 h-4 rounded-full bg-blue-500 dark:bg-blue-400 border-2 border-white dark:border-gray-900" />

              {/* Step Content */}
              <div className="bg-gray-50 dark:bg-gray-800 rounded-lg p-4 border border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between mb-2">
                  <div className="font-semibold text-gray-900 dark:text-gray-100">
                    Step {event.sequenceNumber}: {event.transition}
                  </div>
                  <div className="text-xs text-gray-500 dark:text-gray-500 font-mono">
                    {new Date(event.timestamp).toLocaleTimeString()}
                  </div>
                </div>

                {event.description && (
                  <div className="text-sm text-gray-600 dark:text-gray-400 mb-2">
                    {event.description}
                  </div>
                )}

                {/* Token Movement */}
                <div className="grid grid-cols-2 gap-3 mt-3 text-xs">
                  <div>
                    <div className="text-gray-500 dark:text-gray-500 mb-1">Before:</div>
                    <div className="font-mono bg-gray-100 dark:bg-gray-700 p-2 rounded">
                      {JSON.stringify(event.markingBefore?.tokens || event.markingBefore || {})}
                    </div>
                  </div>
                  <div>
                    <div className="text-gray-500 dark:text-gray-500 mb-1">After:</div>
                    <div className="font-mono bg-gray-100 dark:bg-gray-700 p-2 rounded">
                      {JSON.stringify(event.markingAfter?.tokens || event.markingAfter || {})}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default TabbedPetriView;
