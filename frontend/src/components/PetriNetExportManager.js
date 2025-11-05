import React, { useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Download,
  FileImage,
  FileText,
  Database,
  Code,
  Package,
  ChevronDown,
  Check,
  X,
  Loader2,
  ExternalLink
} from 'lucide-react';
import toast from 'react-hot-toast';
import apiService from '../services/api';

const PetriNetExportManager = ({
  petriNet,
  validationReport,
  simulationTrace,
  dagData,
  executionState,
  className = ""
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [exportingItems, setExportingItems] = useState(new Set());
  const [completedItems, setCompletedItems] = useState(new Set());

  // Export formats configuration
  const exportFormats = [
    {
      id: 'petri-json',
      title: 'Petri Net (JSON)',
      description: 'Complete Petri net structure in JSON format',
      icon: Database,
      category: 'Structure',
      format: 'json',
      available: !!petriNet
    },
    {
      id: 'petri-png',
      title: 'Petri Net Diagram (PNG)',
      description: 'Visual representation as high-resolution image',
      icon: FileImage,
      category: 'Visual',
      format: 'png',
      available: !!petriNet
    },
    {
      id: 'petri-svg',
      title: 'Petri Net Diagram (SVG)',
      description: 'Scalable vector graphics format',
      icon: FileImage,
      category: 'Visual',
      format: 'svg',
      available: !!petriNet
    },
    {
      id: 'validation-json',
      title: 'Validation Report (JSON)',
      description: 'Complete validation results and analysis',
      icon: FileText,
      category: 'Analysis',
      format: 'json',
      available: !!validationReport
    },
    {
      id: 'validation-pdf',
      title: 'Validation Report (PDF)',
      description: 'Formatted validation report for sharing',
      icon: FileText,
      category: 'Analysis',
      format: 'pdf',
      available: !!validationReport
    },
    {
      id: 'trace-ndjson',
      title: 'Simulation Trace (ND-JSON)',
      description: 'Newline-delimited JSON for streaming analysis',
      icon: Code,
      category: 'Simulation',
      format: 'ndjson',
      available: !!simulationTrace && simulationTrace.length > 0
    },
    {
      id: 'trace-csv',
      title: 'Simulation Trace (CSV)',
      description: 'Tabular format for spreadsheet analysis',
      icon: FileText,
      category: 'Simulation',
      format: 'csv',
      available: !!simulationTrace && simulationTrace.length > 0
    },
    {
      id: 'dag-json',
      title: 'DAG Structure (JSON)',
      description: 'Original DAG data structure',
      icon: Database,
      category: 'Structure',
      format: 'json',
      available: !!dagData
    },
    {
      id: 'workflow-bundle',
      title: 'Complete Workflow Bundle',
      description: 'All artifacts in a single ZIP archive',
      icon: Package,
      category: 'Bundle',
      format: 'zip',
      available: !!(petriNet && dagData)
    },
    {
      id: 'mermaid-diagram',
      title: 'Mermaid Diagram Code',
      description: 'Textual diagram representation',
      icon: Code,
      category: 'Code',
      format: 'md',
      available: !!petriNet
    }
  ];

  // Group exports by category
  const exportsByCategory = exportFormats.reduce((acc, format) => {
    if (!acc[format.category]) {
      acc[format.category] = [];
    }
    acc[format.category].push(format);
    return acc;
  }, {});

  // Export handlers
  const exportPetriNetJSON = useCallback(async () => {
    const data = {
      petriNet,
      metadata: {
        exportedAt: new Date().toISOString(),
        version: '1.0',
        type: 'petri-net'
      }
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `petri-net-${Date.now()}.json`;
    link.click();
    URL.revokeObjectURL(link.href);
  }, [petriNet]);

  const exportValidationReport = useCallback(async (format) => {
    const data = {
      validationReport,
      metadata: {
        exportedAt: new Date().toISOString(),
        version: '1.0',
        type: 'validation-report'
      }
    };

    if (format === 'json') {
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `validation-report-${Date.now()}.json`;
      link.click();
      URL.revokeObjectURL(link.href);
    } else if (format === 'pdf') {
      // Generate PDF report (would require PDF generation library)
      toast.error('PDF export not yet implemented');
    }
  }, [validationReport]);

  const exportSimulationTrace = useCallback(async (format) => {
    if (!simulationTrace || simulationTrace.length === 0) return;

    let content, fileName, mimeType;

    switch (format) {
      case 'ndjson':
        content = simulationTrace.map(event => JSON.stringify(event)).join('\n');
        fileName = `simulation-trace-${Date.now()}.ndjson`;
        mimeType = 'application/x-ndjson';
        break;
      case 'csv':
        const headers = ['timestamp', 'stepNumber', 'eventType', 'transitionId', 'transitionName'];
        const csvRows = [
          headers.join(','),
          ...simulationTrace.map(event => [
            new Date(event.timestamp).toISOString(),
            event.stepNumber,
            event.eventType,
            event.transitionId,
            `"${event.transitionName || ''}"`
          ].join(','))
        ];
        content = csvRows.join('\n');
        fileName = `simulation-trace-${Date.now()}.csv`;
        mimeType = 'text/csv';
        break;
      default:
        return;
    }

    const blob = new Blob([content], { type: mimeType });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(link.href);
  }, [simulationTrace]);

  const exportDAGJSON = useCallback(async () => {
    const data = {
      dag: dagData,
      metadata: {
        exportedAt: new Date().toISOString(),
        version: '1.0',
        type: 'dag-structure'
      }
    };

    const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `dag-structure-${Date.now()}.json`;
    link.click();
    URL.revokeObjectURL(link.href);
  }, [dagData]);

  const exportMermaidDiagram = useCallback(async () => {
    if (!petriNet) return;

    // Convert Petri net to Mermaid format
    let mermaidCode = 'graph TD\n';

    // Add places
    if (petriNet.places) {
      petriNet.places.forEach(place => {
        const shape = place.tokens > 0 ? '(((' + place.name + ')))' : '((' + place.name + '))';
        mermaidCode += `    ${place.id}${shape}\n`;
      });
    }

    // Add transitions
    if (petriNet.transitions) {
      petriNet.transitions.forEach(transition => {
        mermaidCode += `    ${transition.id}[${transition.name}]\n`;
      });
    }

    // Add arcs
    if (petriNet.arcs) {
      petriNet.arcs.forEach((arc, index) => {
        const weight = arc.weight > 1 ? `|${arc.weight}|` : '';
        mermaidCode += `    ${arc.from} -->${weight} ${arc.to}\n`;
      });
    }

    const content = `# Petri Net Mermaid Diagram

\`\`\`mermaid
${mermaidCode}
\`\`\`

Generated on: ${new Date().toISOString()}
`;

    const blob = new Blob([content], { type: 'text/markdown' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `petri-net-diagram-${Date.now()}.md`;
    link.click();
    URL.revokeObjectURL(link.href);
  }, [petriNet]);

  const exportWorkflowBundle = useCallback(async () => {
    // This would require a ZIP library like JSZip
    toast.error('Workflow bundle export not yet implemented');
  }, []);

  // Main export handler
  const handleExport = useCallback(async (exportId) => {
    setExportingItems(prev => new Set(prev).add(exportId));

    try {
      switch (exportId) {
        case 'petri-json':
          await exportPetriNetJSON();
          break;
        case 'petri-png':
        case 'petri-svg':
          toast.error('Visual export not yet implemented');
          break;
        case 'validation-json':
          await exportValidationReport('json');
          break;
        case 'validation-pdf':
          await exportValidationReport('pdf');
          break;
        case 'trace-ndjson':
          await exportSimulationTrace('ndjson');
          break;
        case 'trace-csv':
          await exportSimulationTrace('csv');
          break;
        case 'dag-json':
          await exportDAGJSON();
          break;
        case 'workflow-bundle':
          await exportWorkflowBundle();
          break;
        case 'mermaid-diagram':
          await exportMermaidDiagram();
          break;
        default:
          toast.error('Unknown export format');
          return;
      }

      setCompletedItems(prev => new Set(prev).add(exportId));
      toast.success('Export completed successfully');

      // Clear completed status after delay
      setTimeout(() => {
        setCompletedItems(prev => {
          const newSet = new Set(prev);
          newSet.delete(exportId);
          return newSet;
        });
      }, 3000);
    } catch (error) {
      console.error('Export failed:', error);
      toast.error('Export failed');
    } finally {
      setExportingItems(prev => {
        const newSet = new Set(prev);
        newSet.delete(exportId);
        return newSet;
      });
    }
  }, [exportPetriNetJSON, exportValidationReport, exportSimulationTrace, exportDAGJSON, exportMermaidDiagram, exportWorkflowBundle]);

  const getItemIcon = (item) => {
    const ItemIcon = item.icon;
    const isExporting = exportingItems.has(item.id);
    const isCompleted = completedItems.has(item.id);

    if (isExporting) {
      return <Loader2 className="h-4 w-4 animate-spin text-blue-600" />;
    }

    if (isCompleted) {
      return <Check className="h-4 w-4 text-green-600" />;
    }

    return <ItemIcon className="h-4 w-4 text-gray-600" />;
  };

  return (
    <div className={`relative ${className}`}>
      <motion.button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center space-x-2 px-4 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 transition-colors"
        whileHover={{ scale: 1.02 }}
        whileTap={{ scale: 0.98 }}
      >
        <Download className="h-4 w-4" />
        <span>Export Artifacts</span>
        <motion.div
          animate={{ rotate: isOpen ? 180 : 0 }}
          transition={{ duration: 0.2 }}
        >
          <ChevronDown className="h-4 w-4" />
        </motion.div>
      </motion.button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            className="absolute right-0 mt-2 w-96 bg-white border border-gray-200 rounded-xl shadow-lg z-10"
          >
            {/* Header */}
            <div className="px-6 py-4 border-b border-gray-100">
              <div className="flex items-center justify-between">
                <div>
                  <h3 className="text-lg font-semibold text-gray-900">Export Artifacts</h3>
                  <p className="text-sm text-gray-600">Download workflow components</p>
                </div>
                <motion.button
                  onClick={() => setIsOpen(false)}
                  className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  <X className="h-4 w-4 text-gray-600" />
                </motion.button>
              </div>
            </div>

            {/* Export Options */}
            <div className="max-h-96 overflow-y-auto">
              {Object.entries(exportsByCategory).map(([category, items]) => (
                <div key={category} className="p-4 border-b border-gray-100 last:border-b-0">
                  <h4 className="text-sm font-semibold text-gray-800 mb-3 uppercase tracking-wide">
                    {category}
                  </h4>
                  <div className="space-y-2">
                    {items.map(item => (
                      <motion.button
                        key={item.id}
                        onClick={() => item.available && handleExport(item.id)}
                        disabled={!item.available || exportingItems.has(item.id)}
                        className={`w-full flex items-start space-x-3 p-3 rounded-lg text-left transition-colors ${
                          item.available
                            ? 'hover:bg-gray-50 border border-transparent hover:border-gray-200'
                            : 'opacity-50 cursor-not-allowed'
                        }`}
                        whileHover={item.available ? { scale: 1.01 } : {}}
                        whileTap={item.available ? { scale: 0.99 } : {}}
                      >
                        <div className="flex-shrink-0 mt-0.5">
                          {getItemIcon(item)}
                        </div>

                        <div className="flex-1 min-w-0">
                          <div className="flex items-center justify-between">
                            <h5 className="text-sm font-medium text-gray-900">
                              {item.title}
                            </h5>
                            <span className={`text-xs font-medium px-2 py-1 rounded ${
                              item.available
                                ? 'text-green-700 bg-green-100'
                                : 'text-gray-500 bg-gray-100'
                            }`}>
                              {item.format.toUpperCase()}
                            </span>
                          </div>
                          <p className="text-xs text-gray-600 mt-1">
                            {item.description}
                          </p>
                          {!item.available && (
                            <p className="text-xs text-red-600 mt-1">
                              No data available for this export
                            </p>
                          )}
                        </div>

                        {item.available && (
                          <ExternalLink className="h-3 w-3 text-gray-400 flex-shrink-0 mt-0.5" />
                        )}
                      </motion.button>
                    ))}
                  </div>
                </div>
              ))}
            </div>

            {/* Footer */}
            <div className="px-6 py-4 bg-gray-50 rounded-b-xl">
              <div className="flex items-center justify-between text-sm">
                <span className="text-gray-600">
                  {exportFormats.filter(f => f.available).length} of {exportFormats.length} formats available
                </span>
                <motion.button
                  onClick={() => {
                    const availableFormats = exportFormats.filter(f => f.available);
                    availableFormats.forEach(format => {
                      setTimeout(() => handleExport(format.id), Math.random() * 1000);
                    });
                  }}
                  className="text-primary-600 hover:text-primary-700 font-medium"
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                >
                  Export All
                </motion.button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export default PetriNetExportManager;