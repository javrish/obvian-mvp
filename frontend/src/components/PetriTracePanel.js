import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Activity,
  Clock,
  Download,
  Filter,
  Search,
  ChevronDown,
  ChevronRight,
  Circle,
  Square,
  ArrowRight,
  Trash2,
  Eye,
  EyeOff,
  Play,
  Pause
} from 'lucide-react';
import toast from 'react-hot-toast';

const PetriTracePanel = ({
  trace = [],
  petriNet,
  onEventClick,
  onEventHover,
  className = "",
  showRealTime = true,
  maxEvents = 1000,
  autoScroll = true
}) => {
  const [filteredTrace, setFilteredTrace] = useState(trace);
  const [searchTerm, setSearchTerm] = useState('');
  const [expandedEvents, setExpandedEvents] = useState(new Set());
  const [filterType, setFilterType] = useState('all');
  const [showTimestamps, setShowTimestamps] = useState(true);
  const [isFollowing, setIsFollowing] = useState(autoScroll);
  const traceEndRef = useRef(null);
  const containerRef = useRef(null);

  // Auto-scroll to bottom when new events arrive
  useEffect(() => {
    if (isFollowing && traceEndRef.current) {
      traceEndRef.current.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest'
      });
    }
  }, [trace, isFollowing]);

  // Filter trace events
  useEffect(() => {
    let filtered = [...trace];

    // Apply text search
    if (searchTerm.trim()) {
      const term = searchTerm.toLowerCase();
      filtered = filtered.filter(event =>
        event.transitionName?.toLowerCase().includes(term) ||
        event.transitionId?.toLowerCase().includes(term) ||
        event.eventType?.toLowerCase().includes(term)
      );
    }

    // Apply type filter
    if (filterType !== 'all') {
      filtered = filtered.filter(event => event.eventType === filterType);
    }

    // Limit number of events
    if (filtered.length > maxEvents) {
      filtered = filtered.slice(-maxEvents);
    }

    setFilteredTrace(filtered);
  }, [trace, searchTerm, filterType, maxEvents]);

  const toggleEventExpansion = (eventId) => {
    const newExpanded = new Set(expandedEvents);
    if (newExpanded.has(eventId)) {
      newExpanded.delete(eventId);
    } else {
      newExpanded.add(eventId);
    }
    setExpandedEvents(newExpanded);
  };

  const clearTrace = () => {
    setFilteredTrace([]);
    setExpandedEvents(new Set());
    toast.success('Trace cleared');
  };

  const exportTrace = useCallback((format = 'ndjson') => {
    if (filteredTrace.length === 0) {
      toast.error('No trace events to export');
      return;
    }

    let content;
    let fileName;
    let mimeType;

    switch (format) {
      case 'ndjson':
        content = filteredTrace.map(event => JSON.stringify(event)).join('\n');
        fileName = `petri-trace-${Date.now()}.ndjson`;
        mimeType = 'application/x-ndjson';
        break;
      case 'json':
        content = JSON.stringify(filteredTrace, null, 2);
        fileName = `petri-trace-${Date.now()}.json`;
        mimeType = 'application/json';
        break;
      case 'csv':
        const headers = ['timestamp', 'stepNumber', 'eventType', 'transitionId', 'transitionName'];
        const csvRows = [
          headers.join(','),
          ...filteredTrace.map(event => [
            new Date(event.timestamp).toISOString(),
            event.stepNumber,
            event.eventType,
            event.transitionId,
            `"${event.transitionName || ''}"`
          ].join(','))
        ];
        content = csvRows.join('\n');
        fileName = `petri-trace-${Date.now()}.csv`;
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

    toast.success(`Trace exported as ${format.toUpperCase()}`);
  }, [filteredTrace]);

  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      fractionalSecondDigits: 3
    });
  };

  const renderTokenMovement = (movement, type) => {
    const isRemoval = type === 'removed';
    const color = isRemoval ? 'text-red-600 bg-red-50' : 'text-green-600 bg-green-50';

    return (
      <div className={`flex items-center space-x-2 px-2 py-1 rounded text-xs ${color}`}>
        <div className="flex items-center space-x-1">
          <Circle className={`h-3 w-3 ${isRemoval ? 'text-red-400' : 'text-green-400'}`} />
          <span className="font-mono">{movement.placeId}</span>
        </div>
        <span className="font-semibold">
          {isRemoval ? '-' : '+'}{movement.tokens}
        </span>
      </div>
    );
  };

  const getEventIcon = (eventType) => {
    switch (eventType) {
      case 'transition_fired':
        return <Square className="h-4 w-4 text-yellow-600" />;
      case 'marking_changed':
        return <Circle className="h-4 w-4 text-blue-600" />;
      default:
        return <Activity className="h-4 w-4 text-gray-600" />;
    }
  };

  const getEventTypeColor = (eventType) => {
    switch (eventType) {
      case 'transition_fired':
        return 'text-yellow-700 bg-yellow-50 border-yellow-200';
      case 'marking_changed':
        return 'text-blue-700 bg-blue-50 border-blue-200';
      default:
        return 'text-gray-700 bg-gray-50 border-gray-200';
    }
  };

  // Get unique event types for filter
  const eventTypes = [...new Set(trace.map(event => event.eventType))];

  return (
    <motion.div
      initial={{ opacity: 0, x: 20 }}
      animate={{ opacity: 1, x: 0 }}
      className={`bg-white rounded-xl border border-gray-200 shadow-sm flex flex-col ${className}`}
    >
      {/* Header */}
      <div className="px-6 py-4 border-b border-gray-100">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-primary-100 rounded-lg">
              <Activity className="h-5 w-5 text-primary-600" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-gray-900">Execution Trace</h3>
              <p className="text-sm text-gray-500">
                {filteredTrace.length} event{filteredTrace.length !== 1 ? 's' : ''}
                {trace.length !== filteredTrace.length && ` (filtered from ${trace.length})`}
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            {/* Auto-scroll toggle */}
            <motion.button
              onClick={() => setIsFollowing(!isFollowing)}
              className={`p-2 rounded-lg border transition-colors ${
                isFollowing
                  ? 'bg-green-50 border-green-200 text-green-700'
                  : 'bg-gray-50 border-gray-200 text-gray-600 hover:bg-gray-100'
              }`}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              title={isFollowing ? 'Auto-scroll enabled' : 'Auto-scroll disabled'}
            >
              {isFollowing ? <Play className="h-4 w-4" /> : <Pause className="h-4 w-4" />}
            </motion.button>

            {/* Export dropdown */}
            <div className="relative group">
              <motion.button
                className="flex items-center space-x-2 px-3 py-2 bg-primary-600 text-white rounded-lg font-medium hover:bg-primary-700 transition-colors"
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
              >
                <Download className="h-4 w-4" />
                <span>Export</span>
                <ChevronDown className="h-4 w-4" />
              </motion.button>

              <div className="absolute right-0 mt-2 w-48 bg-white border border-gray-200 rounded-lg shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 z-10">
                <div className="py-2">
                  <button
                    onClick={() => exportTrace('ndjson')}
                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                  >
                    ND-JSON (.ndjson)
                  </button>
                  <button
                    onClick={() => exportTrace('json')}
                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                  >
                    JSON (.json)
                  </button>
                  <button
                    onClick={() => exportTrace('csv')}
                    className="block w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                  >
                    CSV (.csv)
                  </button>
                </div>
              </div>
            </div>

            {/* Clear button */}
            <motion.button
              onClick={clearTrace}
              disabled={filteredTrace.length === 0}
              className="p-2 text-gray-600 hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              title="Clear trace"
            >
              <Trash2 className="h-4 w-4" />
            </motion.button>
          </div>
        </div>

        {/* Filters */}
        <div className="mt-4 flex items-center space-x-4">
          {/* Search */}
          <div className="flex-1 max-w-md relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
            <input
              type="text"
              placeholder="Search events..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500 text-sm"
            />
          </div>

          {/* Event type filter */}
          <div className="relative">
            <select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value)}
              className="appearance-none bg-white border border-gray-300 rounded-lg px-3 py-2 pr-8 text-sm focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
            >
              <option value="all">All Events</option>
              {eventTypes.map(type => (
                <option key={type} value={type}>
                  {type.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase())}
                </option>
              ))}
            </select>
            <Filter className="absolute right-2 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400 pointer-events-none" />
          </div>

          {/* Toggle timestamps */}
          <motion.button
            onClick={() => setShowTimestamps(!showTimestamps)}
            className={`flex items-center space-x-2 px-3 py-2 rounded-lg text-sm transition-colors ${
              showTimestamps
                ? 'bg-primary-100 text-primary-700 border border-primary-300'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            {showTimestamps ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
            <span>Time</span>
          </motion.button>
        </div>
      </div>

      {/* Events List */}
      <div
        ref={containerRef}
        className="flex-1 overflow-y-auto max-h-96 p-4 space-y-3"
      >
        <AnimatePresence>
          {filteredTrace.length === 0 ? (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="text-center py-8"
            >
              <Activity className="h-12 w-12 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-600">
                {trace.length === 0 ? 'No trace events yet' : 'No events match your filters'}
              </p>
              <p className="text-sm text-gray-500 mt-1">
                {trace.length === 0
                  ? 'Start the simulation to see events here'
                  : 'Try adjusting your search or filter criteria'}
              </p>
            </motion.div>
          ) : (
            filteredTrace.map((event, index) => {
              const isExpanded = expandedEvents.has(event.timestamp);
              const eventId = `event-${event.timestamp}-${index}`;

              return (
                <motion.div
                  key={eventId}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  exit={{ opacity: 0, x: 20 }}
                  transition={{ duration: 0.2, delay: index * 0.02 }}
                  className="bg-gray-50 rounded-lg border border-gray-200 hover:shadow-md transition-all duration-200"
                  onMouseEnter={() => onEventHover && onEventHover(event, 'enter')}
                  onMouseLeave={() => onEventHover && onEventHover(event, 'leave')}
                >
                  {/* Event Header */}
                  <motion.div
                    className="p-4 cursor-pointer"
                    onClick={() => {
                      toggleEventExpansion(event.timestamp);
                      onEventClick && onEventClick(event);
                    }}
                    whileHover={{ backgroundColor: "rgb(243 244 246)" }}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex items-start space-x-3 flex-1">
                        {/* Event Icon and Type */}
                        <div className="flex items-center space-x-2 flex-shrink-0">
                          {getEventIcon(event.eventType)}
                          <span className={`px-2 py-1 rounded text-xs font-medium border ${getEventTypeColor(event.eventType)}`}>
                            {event.eventType.replace('_', ' ').replace(/\b\w/g, l => l.toUpperCase())}
                          </span>
                        </div>

                        {/* Event Details */}
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center space-x-2">
                            <span className="text-sm font-medium text-gray-900 truncate">
                              Step {event.stepNumber}
                            </span>
                            {event.transitionName && (
                              <>
                                <ArrowRight className="h-3 w-3 text-gray-400 flex-shrink-0" />
                                <span className="text-sm text-gray-700 truncate font-mono">
                                  {event.transitionName}
                                </span>
                              </>
                            )}
                          </div>

                          {showTimestamps && (
                            <div className="flex items-center space-x-2 text-xs text-gray-500 mt-1">
                              <Clock className="h-3 w-3" />
                              <span className="font-mono">
                                {formatTimestamp(event.timestamp)}
                              </span>
                            </div>
                          )}

                          {/* Token movements preview */}
                          {event.tokenMovements && (
                            <div className="flex items-center space-x-2 mt-2">
                              {event.tokenMovements.removed?.length > 0 && (
                                <div className="flex items-center space-x-1 text-xs">
                                  <span className="text-red-600">-{event.tokenMovements.removed.reduce((sum, m) => sum + m.tokens, 0)}</span>
                                  <Circle className="h-2 w-2 text-red-400" />
                                </div>
                              )}
                              {event.tokenMovements.added?.length > 0 && (
                                <div className="flex items-center space-x-1 text-xs">
                                  <span className="text-green-600">+{event.tokenMovements.added.reduce((sum, m) => sum + m.tokens, 0)}</span>
                                  <Circle className="h-2 w-2 text-green-400" />
                                </div>
                              )}
                            </div>
                          )}
                        </div>

                        {/* Expand/Collapse */}
                        <motion.div
                          animate={{ rotate: isExpanded ? 90 : 0 }}
                          transition={{ duration: 0.2 }}
                          className="flex-shrink-0"
                        >
                          <ChevronRight className="h-4 w-4 text-gray-400" />
                        </motion.div>
                      </div>
                    </div>
                  </motion.div>

                  {/* Expanded Details */}
                  <AnimatePresence>
                    {isExpanded && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2 }}
                        className="border-t border-gray-200 overflow-hidden"
                      >
                        <div className="p-4 space-y-4">
                          {/* Transition Details */}
                          {event.transitionId && (
                            <div>
                              <h4 className="text-sm font-medium text-gray-900 mb-2">Transition</h4>
                              <div className="bg-white rounded-lg p-3 border">
                                <div className="flex items-center space-x-2">
                                  <Square className="h-4 w-4 text-yellow-600" />
                                  <span className="font-mono text-sm">{event.transitionId}</span>
                                  {event.transitionName && event.transitionName !== event.transitionId && (
                                    <span className="text-sm text-gray-600">({event.transitionName})</span>
                                  )}
                                </div>
                              </div>
                            </div>
                          )}

                          {/* Token Movements */}
                          {event.tokenMovements && (event.tokenMovements.removed?.length > 0 || event.tokenMovements.added?.length > 0) && (
                            <div>
                              <h4 className="text-sm font-medium text-gray-900 mb-2">Token Movements</h4>
                              <div className="space-y-2">
                                {event.tokenMovements.removed?.length > 0 && (
                                  <div>
                                    <p className="text-xs text-gray-600 mb-1">Removed from places:</p>
                                    <div className="flex flex-wrap gap-2">
                                      {event.tokenMovements.removed.map((movement, idx) => (
                                        <div key={idx}>
                                          {renderTokenMovement(movement, 'removed')}
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}

                                {event.tokenMovements.added?.length > 0 && (
                                  <div>
                                    <p className="text-xs text-gray-600 mb-1">Added to places:</p>
                                    <div className="flex flex-wrap gap-2">
                                      {event.tokenMovements.added.map((movement, idx) => (
                                        <div key={idx}>
                                          {renderTokenMovement(movement, 'added')}
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                )}
                              </div>
                            </div>
                          )}

                          {/* Markings */}
                          {event.previousMarking && event.newMarking && (
                            <div>
                              <h4 className="text-sm font-medium text-gray-900 mb-2">Marking Changes</h4>
                              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                                <div>
                                  <p className="text-xs text-gray-600 mb-1">Before:</p>
                                  <div className="bg-red-50 border border-red-200 rounded p-2 max-h-20 overflow-y-auto">
                                    <div className="text-xs font-mono">
                                      {Object.entries(event.previousMarking)
                                        .filter(([_, tokens]) => tokens > 0)
                                        .map(([placeId, tokens]) => (
                                          <div key={placeId} className="flex justify-between">
                                            <span>{placeId}:</span>
                                            <span>{tokens}</span>
                                          </div>
                                        ))
                                      }
                                    </div>
                                  </div>
                                </div>

                                <div>
                                  <p className="text-xs text-gray-600 mb-1">After:</p>
                                  <div className="bg-green-50 border border-green-200 rounded p-2 max-h-20 overflow-y-auto">
                                    <div className="text-xs font-mono">
                                      {Object.entries(event.newMarking)
                                        .filter(([_, tokens]) => tokens > 0)
                                        .map(([placeId, tokens]) => (
                                          <div key={placeId} className="flex justify-between">
                                            <span>{placeId}:</span>
                                            <span>{tokens}</span>
                                          </div>
                                        ))
                                      }
                                    </div>
                                  </div>
                                </div>
                              </div>
                            </div>
                          )}
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </motion.div>
              );
            })
          )}
        </AnimatePresence>

        {/* Auto-scroll anchor */}
        <div ref={traceEndRef} />
      </div>
    </motion.div>
  );
};

export default PetriTracePanel;