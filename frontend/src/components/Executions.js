import React, { useState, useEffect } from 'react';
import {
  Activity,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
  RefreshCw,
  Eye,
  Download,
  X,
  PlayCircle,
  PauseCircle,
  Calendar,
  Users,
  Smartphone,
  Network,
  BarChart3
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import apiService from '../services/api';
import toast from 'react-hot-toast';
import PetriNetSimulation from './PetriNetSimulation';

const Executions = () => {
  const [executions, setExecutions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedExecution, setSelectedExecution] = useState(null);
  const [filter, setFilter] = useState('all');
  const [activeTab, setActiveTab] = useState('details'); // 'details', 'simulation'
  const [petriNetData, setPetriNetData] = useState(null);
  const [simulationActive, setSimulationActive] = useState(false);

  useEffect(() => {
    loadExecutions();
    const interval = setInterval(loadExecutions, 5000); // Refresh every 5 seconds
    return () => clearInterval(interval);
  }, []);

  const loadExecutions = async () => {
    try {
      // Get real executions from backend
      const response = await apiService.getExecutions();
      const executionsData = response.executions || [];
      
      // Map backend data to UI format
      const formattedExecutions = executionsData.map(exec => ({
        id: exec.id || exec.executionId,
        templateName: exec.name || exec.templateName || exec.dagId || 'Unnamed Execution',
        status: mapExecutionStatus(exec.status),
        startedAt: exec.startedAt || exec.startTime || new Date().toISOString(),
        completedAt: exec.completedAt || exec.endTime || null,
        duration: calculateDuration(exec.startedAt || exec.startTime, exec.completedAt || exec.endTime),
        nodeProgress: {
          total: exec.totalNodes || exec.nodeCount || 0,
          completed: exec.completedNodes || exec.successfulNodes || 0,
          failed: exec.failedNodes || 0
        },
        message: exec.message || exec.result || ''
      }));
      
      setExecutions(formattedExecutions);
      setLoading(false);
    } catch (error) {
      console.error('Failed to load executions:', error);
      toast.error('Failed to load executions from server');
      setExecutions([]); // Set empty array on error
      setLoading(false);
    }
  };
  
  const mapExecutionStatus = (status) => {
    if (!status) return 'pending';
    const statusLower = status.toLowerCase();
    if (statusLower === 'success' || statusLower === 'succeeded' || statusLower === 'complete') return 'completed';
    if (statusLower === 'running' || statusLower === 'in_progress' || statusLower === 'processing') return 'running';
    if (statusLower === 'failed' || statusLower === 'error' || statusLower === 'failure') return 'failed';
    return statusLower;
  };
  
  const calculateDuration = (start, end) => {
    if (!start) return '--';
    const startTime = new Date(start).getTime();
    const endTime = end ? new Date(end).getTime() : Date.now();
    const diff = endTime - startTime;
    
    const minutes = Math.floor(diff / 60000);
    const seconds = Math.floor((diff % 60000) / 1000);
    
    if (minutes > 0) {
      return `${minutes}m ${seconds}s`;
    }
    return `${seconds}s`;
  };

  const handleCancelExecution = async (executionId) => {
    try {
      await apiService.cancelExecution(executionId);
      toast.success('Execution cancelled');
      loadExecutions();
    } catch (error) {
      toast.error('Failed to cancel execution');
    }
  };

  const loadPetriNetData = async (executionId) => {
    try {
      // Try to load Petri net data for the execution
      const response = await apiService.get(`/api/executions/${executionId}/petri-net`);
      setPetriNetData(response.data);
    } catch (error) {
      console.warn('No Petri net data available for execution:', executionId);
      // Generate mock Petri net data for demonstration
      const mockPetriNet = {
        places: [
          { id: 'start', name: 'Start', capacity: 1 },
          { id: 'processing', name: 'Processing', capacity: 1 },
          { id: 'complete', name: 'Complete', capacity: 1 }
        ],
        transitions: [
          { id: 'begin', name: 'Begin Processing' },
          { id: 'finish', name: 'Finish Processing' }
        ],
        arcs: [
          { from: 'start', to: 'begin', weight: 1 },
          { from: 'begin', to: 'processing', weight: 1 },
          { from: 'processing', to: 'finish', weight: 1 },
          { from: 'finish', to: 'complete', weight: 1 }
        ]
      };

      const mockInitialMarking = { start: 1, processing: 0, complete: 0 };

      setPetriNetData({
        petriNet: mockPetriNet,
        initialMarking: mockInitialMarking,
        dagData: {
          nodes: [
            { id: 'node1', name: 'Start Task', type: 'start' },
            { id: 'node2', name: 'Process Task', type: 'task' },
            { id: 'node3', name: 'End Task', type: 'end' }
          ],
          edges: [
            { from: 'node1', to: 'node2' },
            { from: 'node2', to: 'node3' }
          ]
        }
      });
    }
  };

  const handleViewExecution = async (execution) => {
    setSelectedExecution(execution);
    setActiveTab('details');

    // Load Petri net data if available
    await loadPetriNetData(execution.id);
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'completed':
        return <CheckCircle className="h-5 w-5 text-green-600" />;
      case 'running':
        return <RefreshCw className="h-5 w-5 text-blue-600 animate-spin" />;
      case 'failed':
        return <XCircle className="h-5 w-5 text-red-600" />;
      default:
        return <AlertCircle className="h-5 w-5 text-yellow-600" />;
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'completed':
        return 'bg-green-100 text-green-800 border-green-200';
      case 'running':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'failed':
        return 'bg-red-100 text-red-800 border-red-200';
      default:
        return 'bg-yellow-100 text-yellow-800 border-yellow-200';
    }
  };

  const formatTime = (timestamp) => {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;
    
    if (diff < 60000) {
      return 'Just now';
    } else if (diff < 3600000) {
      return `${Math.floor(diff / 60000)} minutes ago`;
    } else if (diff < 86400000) {
      return `${Math.floor(diff / 3600000)} hours ago`;
    } else {
      return date.toLocaleDateString();
    }
  };

  const filteredExecutions = executions.filter(exec => {
    if (filter === 'all') return true;
    return exec.status === filter;
  });

  // Loading skeleton component
  const LoadingSkeleton = () => (
    <div className="space-y-6">
      <div className="animate-pulse">
        <div className="flex justify-between items-center mb-6">
          <div>
            <div className="h-8 bg-gray-200 rounded w-48 mb-2"></div>
            <div className="h-4 bg-gray-200 rounded w-64"></div>
          </div>
          <div className="h-10 bg-gray-200 rounded w-24"></div>
        </div>
        
        <div className="flex space-x-2 mb-6">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-10 bg-gray-200 rounded w-20"></div>
          ))}
        </div>
        
        <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
          <div className="bg-gray-50 px-6 py-3">
            <div className="grid grid-cols-6 gap-4">
              {[1, 2, 3, 4, 5, 6].map(i => (
                <div key={i} className="h-4 bg-gray-200 rounded"></div>
              ))}
            </div>
          </div>
          {[1, 2, 3, 4, 5].map(i => (
            <div key={i} className="px-6 py-4 border-t border-gray-100">
              <div className="grid grid-cols-6 gap-4">
                {[1, 2, 3, 4, 5, 6].map(j => (
                  <div key={j} className="h-4 bg-gray-200 rounded"></div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );

  if (loading) {
    return <LoadingSkeleton />;
  }

  return (
    <motion.div 
      className="space-y-6"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
    >
      {/* Header */}
      <motion.div 
        className="flex flex-col sm:flex-row sm:items-center justify-between gap-4"
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.1 }}
      >
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Executions</h2>
          <p className="text-gray-600 mt-1">Monitor and manage workflow executions</p>
          <div className="flex items-center space-x-4 mt-2 text-sm text-gray-500">
            <div className="flex items-center space-x-1">
              <Activity className="h-4 w-4" />
              <span>{executions.length} total executions</span>
            </div>
            <div className="flex items-center space-x-1">
              <PlayCircle className="h-4 w-4 text-blue-600" />
              <span>{executions.filter(e => e.status === 'running').length} running</span>
            </div>
          </div>
        </div>
        
        <motion.button
          onClick={loadExecutions}
          className="btn-secondary flex items-center space-x-2 hover:shadow-lg transition-all duration-200"
          whileHover={{ scale: 1.02 }}
          whileTap={{ scale: 0.98 }}
        >
          <RefreshCw className="h-4 w-4" />
          <span>Refresh</span>
        </motion.button>
      </motion.div>

      {/* Filters */}
      <motion.div 
        className="flex flex-wrap gap-2"
        initial={{ opacity: 0, y: -10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.5, delay: 0.2 }}
      >
        {['all', 'running', 'completed', 'failed'].map(status => {
          const count = status === 'all' 
            ? executions.length 
            : executions.filter(e => e.status === status).length;
          
          return (
            <motion.button
              key={status}
              onClick={() => setFilter(status)}
              className={`
                px-4 py-2 rounded-lg capitalize transition-all duration-200 relative
                flex items-center space-x-2 hover:shadow-md
                ${filter === status 
                  ? 'bg-primary-600 text-white shadow-lg' 
                  : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-200'
                }
              `}
              whileHover={{ scale: 1.02, y: -1 }}
              whileTap={{ scale: 0.98 }}
            >
              <span>{status}</span>
              {count > 0 && (
                <span className={`
                  px-2 py-0.5 rounded-full text-xs font-medium
                  ${filter === status 
                    ? 'bg-white bg-opacity-20 text-white' 
                    : 'bg-gray-100 text-gray-600'
                  }
                `}>
                  {count}
                </span>
              )}
            </motion.button>
          );
        })}
      </motion.div>

      {/* Executions Content */}
      <AnimatePresence mode="wait">
        {filteredExecutions.length === 0 ? (
          <motion.div 
            key="empty-state"
            className="text-center py-16 bg-gradient-to-br from-gray-50 to-gray-100 rounded-2xl border-2 border-dashed border-gray-300"
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            exit={{ opacity: 0, scale: 0.95 }}
            transition={{ duration: 0.3 }}
          >
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
            >
              <Activity className="h-16 w-16 text-gray-400 mx-auto mb-6" />
              <h3 className="text-xl font-semibold text-gray-900 mb-3">
                {filter !== 'all' ? `No ${filter} executions` : 'No executions yet'}
              </h3>
              <p className="text-gray-600 max-w-md mx-auto leading-relaxed">
                {filter !== 'all' 
                  ? 'Try changing the filter to see other executions, or execute a new template to get started.'
                  : 'Execute a template to see your workflow history here. Your executions will appear with real-time status updates.'}
              </p>
            </motion.div>
          </motion.div>
        ) : (
          <motion.div 
            key="executions-table"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            transition={{ duration: 0.5, delay: 0.3 }}
          >
            {/* Desktop Table View */}
            <div className="hidden lg:block bg-white rounded-2xl border border-gray-200 shadow-sm overflow-hidden">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-gray-50 border-b border-gray-200">
                    <tr>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                        Status
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                        Template
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                        Progress
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                        Started
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                        Duration
                      </th>
                      <th className="px-6 py-4 text-left text-xs font-semibold text-gray-600 uppercase tracking-wider">
                        Actions
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-100">
                    <AnimatePresence>
                      {filteredExecutions.map((execution, index) => (
                        <motion.tr 
                          key={execution.id} 
                          className="hover:bg-gray-50 transition-colors duration-150"
                          initial={{ opacity: 0, x: -20 }}
                          animate={{ opacity: 1, x: 0 }}
                          exit={{ opacity: 0, x: 20 }}
                          transition={{ duration: 0.3, delay: index * 0.05 }}
                          whileHover={{ backgroundColor: "rgb(249 250 251)" }}
                        >
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center space-x-3">
                              <motion.div
                                whileHover={{ scale: 1.1 }}
                                transition={{ duration: 0.2 }}
                              >
                                {getStatusIcon(execution.status)}
                              </motion.div>
                              <span className={`
                                px-3 py-1.5 text-xs font-semibold rounded-full border
                                ${getStatusColor(execution.status)}
                              `}>
                                {execution.status.toUpperCase()}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex flex-col">
                              <div className="text-sm font-semibold text-gray-900 truncate max-w-xs">
                                {execution.templateName}
                              </div>
                              <div className="text-xs text-gray-500 font-mono mt-1">
                                {execution.id.slice(0, 8)}...
                              </div>
                            </div>
                          </td>
                          <td className="px-6 py-4">
                            <div className="flex items-center space-x-3">
                              <div className="flex-1 bg-gray-200 rounded-full h-2.5 min-w-[80px]">
                                <motion.div 
                                  className={`h-2.5 rounded-full transition-all duration-300 ${
                                    execution.status === 'failed' 
                                      ? 'bg-gradient-to-r from-red-500 to-red-600' 
                                      : 'bg-gradient-to-r from-green-500 to-green-600'
                                  }`}
                                  initial={{ width: 0 }}
                                  animate={{ 
                                    width: `${(execution.nodeProgress.completed / execution.nodeProgress.total) * 100}%` 
                                  }}
                                  transition={{ duration: 0.8, delay: 0.2 }}
                                />
                              </div>
                              <span className="text-xs text-gray-600 font-medium whitespace-nowrap">
                                {execution.nodeProgress.completed}/{execution.nodeProgress.total}
                              </span>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center space-x-2 text-sm text-gray-600">
                              <Calendar className="h-4 w-4" />
                              <span>{formatTime(execution.startedAt)}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center space-x-2 text-sm text-gray-600">
                              <Clock className="h-4 w-4" />
                              <span className="font-mono">{execution.duration}</span>
                            </div>
                          </td>
                          <td className="px-6 py-4 whitespace-nowrap">
                            <div className="flex items-center space-x-2">
                              <motion.button
                                onClick={() => handleViewExecution(execution)}
                                className="p-2 hover:bg-blue-50 rounded-lg transition-all duration-200"
                                title="View details"
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.95 }}
                              >
                                <Eye className="h-4 w-4 text-blue-600" />
                              </motion.button>
                              
                              {execution.status === 'running' && (
                                <motion.button
                                  onClick={() => handleCancelExecution(execution.id)}
                                  className="p-2 hover:bg-red-50 rounded-lg transition-all duration-200"
                                  title="Cancel execution"
                                  whileHover={{ scale: 1.1 }}
                                  whileTap={{ scale: 0.95 }}
                                >
                                  <X className="h-4 w-4 text-red-600" />
                                </motion.button>
                              )}
                              
                              <motion.button
                                className="p-2 hover:bg-gray-100 rounded-lg transition-all duration-200"
                                title="Download logs"
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.95 }}
                              >
                                <Download className="h-4 w-4 text-gray-600" />
                              </motion.button>
                            </div>
                          </td>
                        </motion.tr>
                      ))}
                    </AnimatePresence>
                  </tbody>
                </table>
              </div>
            </div>

            {/* Mobile Card View */}
            <div className="lg:hidden space-y-4">
              <AnimatePresence>
                {filteredExecutions.map((execution, index) => (
                  <motion.div
                    key={execution.id}
                    className="bg-white rounded-2xl border border-gray-200 shadow-sm p-6 hover:shadow-md transition-all duration-200"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                    transition={{ duration: 0.3, delay: index * 0.1 }}
                    whileHover={{ y: -2 }}
                  >
                    {/* Status and Template Info */}
                    <div className="flex items-start justify-between mb-4">
                      <div className="flex-1">
                        <div className="flex items-center space-x-3 mb-2">
                          {getStatusIcon(execution.status)}
                          <span className={`
                            px-3 py-1.5 text-xs font-semibold rounded-full border
                            ${getStatusColor(execution.status)}
                          `}>
                            {execution.status.toUpperCase()}
                          </span>
                        </div>
                        <h3 className="text-lg font-semibold text-gray-900 mb-1">
                          {execution.templateName}
                        </h3>
                        <p className="text-sm text-gray-500 font-mono">
                          {execution.id.slice(0, 12)}...
                        </p>
                      </div>
                      
                      <div className="flex items-center space-x-2">
                        <motion.button
                          onClick={() => handleViewExecution(execution)}
                          className="p-2 hover:bg-blue-50 rounded-lg transition-all duration-200"
                          whileHover={{ scale: 1.1 }}
                          whileTap={{ scale: 0.95 }}
                        >
                          <Eye className="h-5 w-5 text-blue-600" />
                        </motion.button>
                        
                        {execution.status === 'running' && (
                          <motion.button
                            onClick={() => handleCancelExecution(execution.id)}
                            className="p-2 hover:bg-red-50 rounded-lg transition-all duration-200"
                            whileHover={{ scale: 1.1 }}
                            whileTap={{ scale: 0.95 }}
                          >
                            <X className="h-5 w-5 text-red-600" />
                          </motion.button>
                        )}
                      </div>
                    </div>

                    {/* Progress Bar */}
                    <div className="mb-4">
                      <div className="flex justify-between items-center mb-2">
                        <span className="text-sm font-medium text-gray-700">Progress</span>
                        <span className="text-sm text-gray-600">
                          {execution.nodeProgress.completed}/{execution.nodeProgress.total} nodes
                        </span>
                      </div>
                      <div className="bg-gray-200 rounded-full h-3">
                        <motion.div 
                          className={`h-3 rounded-full ${
                            execution.status === 'failed' 
                              ? 'bg-gradient-to-r from-red-500 to-red-600' 
                              : 'bg-gradient-to-r from-green-500 to-green-600'
                          }`}
                          initial={{ width: 0 }}
                          animate={{ 
                            width: `${(execution.nodeProgress.completed / execution.nodeProgress.total) * 100}%` 
                          }}
                          transition={{ duration: 0.8 }}
                        />
                      </div>
                    </div>

                    {/* Time Information */}
                    <div className="flex flex-wrap gap-4 text-sm text-gray-600">
                      <div className="flex items-center space-x-2">
                        <Calendar className="h-4 w-4" />
                        <span>{formatTime(execution.startedAt)}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <Clock className="h-4 w-4" />
                        <span className="font-mono">{execution.duration}</span>
                      </div>
                    </div>
                  </motion.div>
                ))}
              </AnimatePresence>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Execution Details Modal */}
      <AnimatePresence>
        {selectedExecution && (
          <motion.div 
            className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.2 }}
            onClick={() => setSelectedExecution(null)}
          >
            <motion.div 
              className="bg-white rounded-2xl max-w-2xl w-full max-h-[90vh] overflow-hidden shadow-2xl"
              initial={{ opacity: 0, scale: 0.95, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.95, y: 20 }}
              transition={{ duration: 0.3 }}
              onClick={(e) => e.stopPropagation()}
            >
              {/* Modal Header */}
              <div className="bg-gradient-to-r from-primary-600 to-primary-700 px-6 py-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-xl font-bold text-white">
                      Execution Details
                    </h3>
                    <p className="text-primary-100 text-sm mt-1">
                      {selectedExecution.templateName}
                    </p>
                  </div>
                  <motion.button
                    onClick={() => {
                      setSelectedExecution(null);
                      setActiveTab('details');
                      setPetriNetData(null);
                      setSimulationActive(false);
                    }}
                    className="p-2 hover:bg-white hover:bg-opacity-10 rounded-lg transition-all duration-200"
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.95 }}
                  >
                    <X className="h-6 w-6 text-white" />
                  </motion.button>
                </div>

                {/* Tabs */}
                <div className="flex space-x-4 mt-4">
                  <motion.button
                    onClick={() => setActiveTab('details')}
                    className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                      activeTab === 'details'
                        ? 'bg-white bg-opacity-20 text-white'
                        : 'text-primary-100 hover:bg-white hover:bg-opacity-10'
                    }`}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                  >
                    <BarChart3 className="h-4 w-4" />
                    <span>Details</span>
                  </motion.button>

                  {petriNetData && (
                    <motion.button
                      onClick={() => setActiveTab('simulation')}
                      className={`flex items-center space-x-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                        activeTab === 'simulation'
                          ? 'bg-white bg-opacity-20 text-white'
                          : 'text-primary-100 hover:bg-white hover:bg-opacity-10'
                      }`}
                      whileHover={{ scale: 1.02 }}
                      whileTap={{ scale: 0.98 }}
                    >
                      <Network className="h-4 w-4" />
                      <span>Simulation</span>
                      {simulationActive && (
                        <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse"></div>
                      )}
                    </motion.button>
                  )}
                </div>
              </div>

              {/* Modal Body */}
              <div className="overflow-y-auto max-h-[calc(90vh-180px)]">
                {activeTab === 'details' ? (
                  <div className="p-6 space-y-6">
                    {/* Status Card */}
                <motion.div 
                  className="bg-gray-50 rounded-xl p-4"
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  transition={{ delay: 0.1 }}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-sm font-medium text-gray-500 mb-2">Current Status</p>
                      <div className="flex items-center space-x-3">
                        <motion.div
                          whileHover={{ scale: 1.1 }}
                          transition={{ duration: 0.2 }}
                        >
                          {getStatusIcon(selectedExecution.status)}
                        </motion.div>
                        <span className={`
                          px-3 py-1.5 text-sm font-semibold rounded-full border
                          ${getStatusColor(selectedExecution.status)}
                        `}>
                          {selectedExecution.status.toUpperCase()}
                        </span>
                      </div>
                    </div>
                    
                    <div className="text-right">
                      <p className="text-sm font-medium text-gray-500 mb-2">Duration</p>
                      <div className="flex items-center space-x-2 text-gray-700">
                        <Clock className="h-4 w-4" />
                        <span className="font-mono font-semibold">{selectedExecution.duration}</span>
                      </div>
                    </div>
                  </div>
                </motion.div>

                {/* Execution Info Grid */}
                <div className="grid md:grid-cols-2 gap-4">
                  <motion.div 
                    className="space-y-4"
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.2 }}
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-500 mb-2">Execution ID</p>
                      <div className="bg-white border border-gray-200 rounded-lg p-3">
                        <p className="text-gray-900 font-mono text-sm break-all">{selectedExecution.id}</p>
                      </div>
                    </div>
                    
                    <div>
                      <p className="text-sm font-medium text-gray-500 mb-2">Started</p>
                      <div className="bg-white border border-gray-200 rounded-lg p-3">
                        <div className="flex items-center space-x-2 text-gray-700">
                          <Calendar className="h-4 w-4" />
                          <span>{formatTime(selectedExecution.startedAt)}</span>
                        </div>
                      </div>
                    </div>
                  </motion.div>

                  <motion.div 
                    className="space-y-4"
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.3 }}
                  >
                    <div>
                      <p className="text-sm font-medium text-gray-500 mb-2">Template Name</p>
                      <div className="bg-white border border-gray-200 rounded-lg p-3">
                        <p className="text-gray-900 font-medium">{selectedExecution.templateName}</p>
                      </div>
                    </div>
                    
                    {selectedExecution.message && (
                      <div>
                        <p className="text-sm font-medium text-gray-500 mb-2">Message</p>
                        <div className="bg-white border border-gray-200 rounded-lg p-3">
                          <p className="text-gray-700 text-sm">{selectedExecution.message}</p>
                        </div>
                      </div>
                    )}
                  </motion.div>
                </div>

                {/* Progress Section */}
                <motion.div 
                  className="bg-white border border-gray-200 rounded-xl p-4"
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.4 }}
                >
                  <p className="text-sm font-medium text-gray-500 mb-4">Node Execution Progress</p>
                  
                  <div className="space-y-4">
                    <div className="flex justify-between items-center text-sm">
                      <div className="flex items-center space-x-2">
                        <div className="w-3 h-3 bg-green-500 rounded-full"></div>
                        <span className="font-medium">Completed: {selectedExecution.nodeProgress.completed}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <div className="w-3 h-3 bg-red-500 rounded-full"></div>
                        <span className="font-medium">Failed: {selectedExecution.nodeProgress.failed}</span>
                      </div>
                      <div className="flex items-center space-x-2">
                        <div className="w-3 h-3 bg-gray-400 rounded-full"></div>
                        <span className="font-medium">Total: {selectedExecution.nodeProgress.total}</span>
                      </div>
                    </div>
                    
                    <div className="bg-gray-200 rounded-full h-3">
                      <motion.div 
                        className={`h-3 rounded-full ${
                          selectedExecution.status === 'failed' 
                            ? 'bg-gradient-to-r from-red-500 to-red-600' 
                            : 'bg-gradient-to-r from-green-500 to-green-600'
                        }`}
                        initial={{ width: 0 }}
                        animate={{ 
                          width: `${(selectedExecution.nodeProgress.completed / selectedExecution.nodeProgress.total) * 100}%` 
                        }}
                        transition={{ duration: 1, delay: 0.5 }}
                      />
                    </div>
                    
                    <div className="text-center text-sm text-gray-600">
                      {Math.round((selectedExecution.nodeProgress.completed / selectedExecution.nodeProgress.total) * 100)}% Complete
                    </div>
                  </div>
                </motion.div>

                    {/* Actions */}
                    <motion.div
                      className="flex flex-col sm:flex-row gap-3 pt-4"
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.5 }}
                    >
                      {selectedExecution.status === 'running' && (
                        <motion.button
                          onClick={() => {
                            handleCancelExecution(selectedExecution.id);
                            setSelectedExecution(null);
                          }}
                          className="flex-1 bg-red-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-red-700 transition-all duration-200 flex items-center justify-center space-x-2"
                          whileHover={{ scale: 1.02 }}
                          whileTap={{ scale: 0.98 }}
                        >
                          <X className="h-4 w-4" />
                          <span>Cancel Execution</span>
                        </motion.button>
                      )}

                      <motion.button
                        className="flex-1 bg-gray-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-gray-700 transition-all duration-200 flex items-center justify-center space-x-2"
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                      >
                        <Download className="h-4 w-4" />
                        <span>Download Logs</span>
                      </motion.button>

                      <motion.button
                        onClick={() => {
                          setSelectedExecution(null);
                          setActiveTab('details');
                          setPetriNetData(null);
                          setSimulationActive(false);
                        }}
                        className="flex-1 bg-primary-600 text-white px-4 py-2 rounded-lg font-medium hover:bg-primary-700 transition-all duration-200"
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                      >
                        Close
                      </motion.button>
                    </motion.div>
                  </div>
                ) : activeTab === 'simulation' && petriNetData ? (
                  <div className="p-4">
                    <PetriNetSimulation
                      petriNet={petriNetData.petriNet}
                      dagData={petriNetData.dagData}
                      initialMarking={petriNetData.initialMarking}
                      onSimulationComplete={(result) => {
                        setSimulationActive(false);
                        toast.success(`Simulation ${result.state}: ${result.totalSteps} steps completed`);
                      }}
                      className="max-h-[calc(90vh-240px)]"
                    />
                  </div>
                ) : (
                  <div className="p-6 text-center">
                    <Network className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-600">No simulation data available for this execution</p>
                  </div>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default Executions;