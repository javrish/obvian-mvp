import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Activity, TrendingUp, Clock, Zap, BarChart3,
  Monitor, AlertTriangle, CheckCircle, RefreshCw,
  Download, Settings, Eye, EyeOff, Maximize2, Minimize2
} from 'lucide-react';
import apiService from '../services/api';

const PetriNetPerformanceDashboard = ({
  petriNet,
  className = "",
  refreshInterval = 5000,
  showAdvancedMetrics = true
}) => {
  // Performance metrics state
  const [metrics, setMetrics] = useState({
    parsing: { avgTime: 0, maxTime: 0, successRate: 100, totalOperations: 0 },
    building: { avgTime: 0, maxTime: 0, successRate: 100, totalOperations: 0 },
    validation: { avgTime: 0, maxTime: 0, successRate: 100, totalOperations: 0, avgStatesExplored: 0 },
    simulation: { avgTime: 0, maxTime: 0, successRate: 100, totalOperations: 0, avgStepsExecuted: 0 },
    system: { cpuUsage: 0, memoryUsage: 0, websocketConnections: 0, queuedOperations: 0 }
  });

  // Real-time data state
  const [realTimeData, setRealTimeData] = useState([]);
  const [isMonitoring, setIsMonitoring] = useState(false);
  const [lastUpdate, setLastUpdate] = useState(null);

  // UI state
  const [selectedMetric, setSelectedMetric] = useState('validation');
  const [isExpanded, setIsExpanded] = useState(false);
  const [showRealTimeGraph, setShowRealTimeGraph] = useState(true);
  const [alertThresholds, setAlertThresholds] = useState({
    validationTime: 5000,
    simulationTime: 3000,
    memoryUsage: 80,
    cpuUsage: 90
  });

  // WebSocket and monitoring
  const webSocketRef = useRef(null);
  const intervalRef = useRef(null);
  const metricsHistory = useRef([]);

  // Initialize monitoring
  useEffect(() => {
    startMonitoring();
    return () => stopMonitoring();
  }, [refreshInterval]);

  const startMonitoring = useCallback(() => {
    if (isMonitoring) return;

    setIsMonitoring(true);

    // Setup WebSocket for real-time metrics
    const callbacks = {
      onConnect: () => {
        console.log('Performance monitoring WebSocket connected');
      },

      onMessage: (message) => {
        if (message.type === 'PERFORMANCE_METRICS') {
          updateMetrics(message.data);
        }
      },

      onError: (error) => {
        console.error('Performance monitoring error:', error);
      }
    };

    try {
      webSocketRef.current = apiService.petri.createWebSocketConnection(callbacks);

      // Subscribe to performance metrics
      if (webSocketRef.current) {
        webSocketRef.current.subscribe(['PERFORMANCE_METRICS', 'SYSTEM_STATUS']);
      }
    } catch (error) {
      console.error('Failed to start performance monitoring:', error);
    }

    // Fallback: periodic API polling
    intervalRef.current = setInterval(async () => {
      try {
        await fetchMetrics();
      } catch (error) {
        console.error('Error fetching metrics:', error);
      }
    }, refreshInterval);

  }, [isMonitoring, refreshInterval]);

  const stopMonitoring = useCallback(() => {
    setIsMonitoring(false);

    if (webSocketRef.current) {
      webSocketRef.current.close();
      webSocketRef.current = null;
    }

    if (intervalRef.current) {
      clearInterval(intervalRef.current);
      intervalRef.current = null;
    }
  }, []);

  // Fetch metrics from API
  const fetchMetrics = useCallback(async () => {
    try {
      // Mock API call - in real implementation, this would fetch actual metrics
      const mockMetrics = generateMockMetrics();
      updateMetrics(mockMetrics);
    } catch (error) {
      console.error('Failed to fetch performance metrics:', error);
    }
  }, []);

  // Update metrics and history
  const updateMetrics = useCallback((newMetrics) => {
    setMetrics(newMetrics);
    setLastUpdate(new Date());

    // Add to history for graphs
    const timestamp = Date.now();
    const historyEntry = {
      timestamp,
      ...newMetrics
    };

    metricsHistory.current = [...metricsHistory.current, historyEntry].slice(-100); // Keep last 100 entries

    // Update real-time data for graphs
    setRealTimeData(metricsHistory.current);
  }, []);

  // Generate mock metrics (replace with real API calls)
  const generateMockMetrics = useCallback(() => {
    const now = Date.now();
    return {
      parsing: {
        avgTime: Math.random() * 100 + 50,
        maxTime: Math.random() * 200 + 100,
        successRate: 95 + Math.random() * 5,
        totalOperations: Math.floor(Math.random() * 1000) + 500
      },
      building: {
        avgTime: Math.random() * 300 + 100,
        maxTime: Math.random() * 500 + 200,
        successRate: 90 + Math.random() * 10,
        totalOperations: Math.floor(Math.random() * 800) + 300
      },
      validation: {
        avgTime: Math.random() * 2000 + 1000,
        maxTime: Math.random() * 5000 + 2000,
        successRate: 85 + Math.random() * 15,
        totalOperations: Math.floor(Math.random() * 500) + 200,
        avgStatesExplored: Math.floor(Math.random() * 10000) + 1000
      },
      simulation: {
        avgTime: Math.random() * 1500 + 500,
        maxTime: Math.random() * 3000 + 1000,
        successRate: 90 + Math.random() * 10,
        totalOperations: Math.floor(Math.random() * 600) + 250,
        avgStepsExecuted: Math.floor(Math.random() * 50) + 10
      },
      system: {
        cpuUsage: Math.random() * 100,
        memoryUsage: Math.random() * 100,
        websocketConnections: Math.floor(Math.random() * 20) + 5,
        queuedOperations: Math.floor(Math.random() * 10)
      }
    };
  }, []);

  // Check for alerts
  const getAlerts = useCallback(() => {
    const alerts = [];

    if (metrics.validation.avgTime > alertThresholds.validationTime) {
      alerts.push({
        type: 'warning',
        message: 'Validation taking longer than expected',
        value: `${Math.round(metrics.validation.avgTime)}ms`
      });
    }

    if (metrics.simulation.avgTime > alertThresholds.simulationTime) {
      alerts.push({
        type: 'warning',
        message: 'Simulation performance degraded',
        value: `${Math.round(metrics.simulation.avgTime)}ms`
      });
    }

    if (metrics.system.memoryUsage > alertThresholds.memoryUsage) {
      alerts.push({
        type: 'error',
        message: 'High memory usage detected',
        value: `${Math.round(metrics.system.memoryUsage)}%`
      });
    }

    if (metrics.system.cpuUsage > alertThresholds.cpuUsage) {
      alerts.push({
        type: 'error',
        message: 'High CPU usage detected',
        value: `${Math.round(metrics.system.cpuUsage)}%`
      });
    }

    return alerts;
  }, [metrics, alertThresholds]);

  const alerts = getAlerts();

  // Export metrics data
  const exportMetrics = useCallback(() => {
    const exportData = {
      timestamp: new Date().toISOString(),
      petriNetId: petriNet?.id,
      metrics,
      history: metricsHistory.current,
      alerts
    };

    const blob = new Blob([JSON.stringify(exportData, null, 2)], {
      type: 'application/json'
    });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `petri-net-performance-${Date.now()}.json`;
    link.click();
    URL.revokeObjectURL(link.href);
  }, [metrics, petriNet, alerts]);

  // Metric cards data
  const metricCards = [
    {
      id: 'parsing',
      title: 'Natural Language Parsing',
      icon: Zap,
      color: 'blue',
      value: `${Math.round(metrics.parsing.avgTime)}ms`,
      subtitle: `${metrics.parsing.totalOperations} operations`,
      successRate: metrics.parsing.successRate,
      trend: '+5.2%'
    },
    {
      id: 'building',
      title: 'P3Net Building',
      icon: BarChart3,
      color: 'green',
      value: `${Math.round(metrics.building.avgTime)}ms`,
      subtitle: `${metrics.building.totalOperations} operations`,
      successRate: metrics.building.successRate,
      trend: '-2.1%'
    },
    {
      id: 'validation',
      title: 'Formal Validation',
      icon: CheckCircle,
      color: 'purple',
      value: `${Math.round(metrics.validation.avgTime)}ms`,
      subtitle: `${metrics.validation.avgStatesExplored} avg states`,
      successRate: metrics.validation.successRate,
      trend: '+1.8%'
    },
    {
      id: 'simulation',
      title: 'Token Simulation',
      icon: Activity,
      color: 'orange',
      value: `${Math.round(metrics.simulation.avgTime)}ms`,
      subtitle: `${metrics.simulation.avgStepsExecuted} avg steps`,
      successRate: metrics.simulation.successRate,
      trend: '+0.9%'
    }
  ];

  const getColorClasses = (color) => {
    const colors = {
      blue: { bg: 'bg-blue-50', text: 'text-blue-700', border: 'border-blue-200' },
      green: { bg: 'bg-green-50', text: 'text-green-700', border: 'border-green-200' },
      purple: { bg: 'bg-purple-50', text: 'text-purple-700', border: 'border-purple-200' },
      orange: { bg: 'bg-orange-50', text: 'text-orange-700', border: 'border-orange-200' }
    };
    return colors[color] || colors.blue;
  };

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
            <Monitor className="h-6 w-6 text-blue-600" />
            <div>
              <h2 className="text-xl font-semibold text-gray-900">P3Net Performance Dashboard</h2>
              <p className="text-sm text-gray-600">
                Real-time monitoring of Petri net processing performance
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-3">
            {/* Connection Status */}
            <div className={`flex items-center space-x-2 px-3 py-1 rounded-full text-sm ${
              isMonitoring ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-700'
            }`}>
              <div className={`w-2 h-2 rounded-full ${
                isMonitoring ? 'bg-green-400' : 'bg-gray-400'
              }`} />
              <span>{isMonitoring ? 'Live' : 'Offline'}</span>
            </div>

            {/* Last Update */}
            {lastUpdate && (
              <span className="text-sm text-gray-500">
                Updated {lastUpdate.toLocaleTimeString()}
              </span>
            )}

            {/* Controls */}
            <div className="flex items-center space-x-1">
              <motion.button
                onClick={() => setShowRealTimeGraph(!showRealTimeGraph)}
                className="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                title={showRealTimeGraph ? 'Hide Graph' : 'Show Graph'}
              >
                {showRealTimeGraph ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </motion.button>

              <motion.button
                onClick={() => setIsExpanded(!isExpanded)}
                className="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                title={isExpanded ? 'Collapse' : 'Expand'}
              >
                {isExpanded ? <Minimize2 className="h-4 w-4" /> : <Maximize2 className="h-4 w-4" />}
              </motion.button>

              <motion.button
                onClick={exportMetrics}
                className="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                title="Export Metrics"
              >
                <Download className="h-4 w-4" />
              </motion.button>

              <motion.button
                onClick={fetchMetrics}
                className="p-2 text-gray-400 hover:text-gray-600 rounded-lg hover:bg-gray-100"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                title="Refresh"
              >
                <RefreshCw className="h-4 w-4" />
              </motion.button>
            </div>
          </div>
        </div>
      </div>

      {/* Alerts */}
      <AnimatePresence>
        {alerts.length > 0 && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="px-6 py-3 bg-yellow-50 border-b border-yellow-200"
          >
            <div className="flex items-start space-x-3">
              <AlertTriangle className="h-5 w-5 text-yellow-600 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <h4 className="text-sm font-medium text-yellow-800 mb-1">Performance Alerts</h4>
                <div className="space-y-1">
                  {alerts.map((alert, index) => (
                    <div key={index} className="text-sm text-yellow-700">
                      {alert.message}: <strong>{alert.value}</strong>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Metric Cards */}
      <div className="p-6">
        <div className={`grid gap-6 ${isExpanded ? 'grid-cols-2' : 'grid-cols-4'}`}>
          {metricCards.map((metric) => {
            const colors = getColorClasses(metric.color);
            const Icon = metric.icon;

            return (
              <motion.div
                key={metric.id}
                className={`p-4 rounded-xl border ${colors.border} ${colors.bg} cursor-pointer transition-all ${
                  selectedMetric === metric.id ? 'ring-2 ring-blue-500' : 'hover:shadow-md'
                }`}
                onClick={() => setSelectedMetric(metric.id)}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <div className="flex items-center justify-between mb-3">
                  <Icon className={`h-5 w-5 ${colors.text}`} />
                  <span className={`text-xs font-medium ${colors.text}`}>
                    {metric.trend}
                  </span>
                </div>

                <div className="space-y-1">
                  <h3 className="text-sm font-medium text-gray-900">
                    {metric.title}
                  </h3>
                  <div className="flex items-baseline space-x-2">
                    <span className="text-2xl font-bold text-gray-900">
                      {metric.value}
                    </span>
                  </div>
                  <p className="text-xs text-gray-600">{metric.subtitle}</p>

                  {/* Success Rate Bar */}
                  <div className="mt-2">
                    <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                      <span>Success Rate</span>
                      <span>{Math.round(metric.successRate)}%</span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-1.5">
                      <div
                        className={`h-1.5 rounded-full transition-all duration-300 ${
                          metric.successRate > 95 ? 'bg-green-500' :
                          metric.successRate > 90 ? 'bg-yellow-500' : 'bg-red-500'
                        }`}
                        style={{ width: `${metric.successRate}%` }}
                      />
                    </div>
                  </div>
                </div>
              </motion.div>
            );
          })}
        </div>

        {/* System Status */}
        {showAdvancedMetrics && (
          <div className="mt-6 grid grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-2 mb-2">
                <Activity className="h-4 w-4 text-gray-600" />
                <span className="text-sm font-medium text-gray-700">CPU Usage</span>
              </div>
              <div className="text-xl font-bold text-gray-900">
                {Math.round(metrics.system.cpuUsage)}%
              </div>
              <div className="w-full bg-gray-200 rounded-full h-1.5 mt-2">
                <div
                  className={`h-1.5 rounded-full transition-all duration-300 ${
                    metrics.system.cpuUsage > 90 ? 'bg-red-500' :
                    metrics.system.cpuUsage > 70 ? 'bg-yellow-500' : 'bg-green-500'
                  }`}
                  style={{ width: `${metrics.system.cpuUsage}%` }}
                />
              </div>
            </div>

            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-2 mb-2">
                <BarChart3 className="h-4 w-4 text-gray-600" />
                <span className="text-sm font-medium text-gray-700">Memory Usage</span>
              </div>
              <div className="text-xl font-bold text-gray-900">
                {Math.round(metrics.system.memoryUsage)}%
              </div>
              <div className="w-full bg-gray-200 rounded-full h-1.5 mt-2">
                <div
                  className={`h-1.5 rounded-full transition-all duration-300 ${
                    metrics.system.memoryUsage > 80 ? 'bg-red-500' :
                    metrics.system.memoryUsage > 60 ? 'bg-yellow-500' : 'bg-green-500'
                  }`}
                  style={{ width: `${metrics.system.memoryUsage}%` }}
                />
              </div>
            </div>

            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-2 mb-2">
                <TrendingUp className="h-4 w-4 text-gray-600" />
                <span className="text-sm font-medium text-gray-700">Connections</span>
              </div>
              <div className="text-xl font-bold text-gray-900">
                {metrics.system.websocketConnections}
              </div>
              <div className="text-xs text-gray-500 mt-1">Active WebSocket</div>
            </div>

            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-2 mb-2">
                <Clock className="h-4 w-4 text-gray-600" />
                <span className="text-sm font-medium text-gray-700">Queue</span>
              </div>
              <div className="text-xl font-bold text-gray-900">
                {metrics.system.queuedOperations}
              </div>
              <div className="text-xs text-gray-500 mt-1">Pending Operations</div>
            </div>
          </div>
        )}

        {/* Real-time Graph Placeholder */}
        {showRealTimeGraph && realTimeData.length > 0 && (
          <div className="mt-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Performance Trends - {metricCards.find(m => m.id === selectedMetric)?.title}
            </h3>
            <div className="h-64 bg-gray-50 rounded-lg flex items-center justify-center">
              <div className="text-center text-gray-500">
                <TrendingUp className="h-12 w-12 mx-auto mb-2 opacity-50" />
                <p className="text-sm">Real-time performance graph</p>
                <p className="text-xs">Chart integration would go here</p>
              </div>
            </div>
          </div>
        )}
      </div>
    </motion.div>
  );
};

export default PetriNetPerformanceDashboard;