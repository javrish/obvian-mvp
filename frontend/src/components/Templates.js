import React, { useState, useEffect } from 'react';
import { 
  FileText, 
  Play, 
  Search, 
  Filter,
  Clock,
  Zap,
  ChevronRight,
  X,
  Check,
  AlertCircle,
  Sparkles,
  Plus,
  Star,
  TrendingUp,
  Users,
  ArrowRight,
  Loader,
  Eye,
  Edit3,
  Copy,
  Settings,
  RefreshCw,
  MessageSquare
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import apiService from '../services/api';
import toast from 'react-hot-toast';
import { demoTemplates, templateCategories, getTemplatesByCategory, searchTemplates } from '../data/demoTemplates';

const Templates = () => {
  const [templates, setTemplates] = useState(demoTemplates);
  const [loading, setLoading] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [executing, setExecuting] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('all');
  const [showExecuteModal, setShowExecuteModal] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [parameters, setParameters] = useState({});
  const [executionResult, setExecutionResult] = useState(null);
  const [executionProgress, setExecutionProgress] = useState(null);
  const [naturalLanguagePrompt, setNaturalLanguagePrompt] = useState('');
  const [generatingTemplate, setGeneratingTemplate] = useState(false);
  const [viewMode, setViewMode] = useState('grid'); // grid or list
  const [sortBy, setSortBy] = useState('popularity'); // popularity, recent, alphabetical
  
  // Memory persistence
  const [executionHistory, setExecutionHistory] = useState([]);
  const [favoriteTemplates, setFavoriteTemplates] = useState([]);
  
  useEffect(() => {
    loadUserPreferences();
    loadExecutionHistory();
  }, []);

  const loadUserPreferences = () => {
    const savedFavorites = localStorage.getItem('favoriteTemplates');
    const savedViewMode = localStorage.getItem('templateViewMode');
    const savedCategory = localStorage.getItem('lastSelectedCategory');
    
    if (savedFavorites) setFavoriteTemplates(JSON.parse(savedFavorites));
    if (savedViewMode) setViewMode(savedViewMode);
    if (savedCategory) setSelectedCategory(savedCategory);
  };

  const loadExecutionHistory = () => {
    const history = localStorage.getItem('executionHistory');
    if (history) {
      setExecutionHistory(JSON.parse(history));
    }
  };

  const saveExecutionToHistory = (template, params, result) => {
    const execution = {
      id: Date.now().toString(),
      templateId: template.id,
      templateName: template.name,
      parameters: params,
      result: result,
      timestamp: new Date().toISOString()
    };
    
    const newHistory = [execution, ...executionHistory].slice(0, 50); // Keep last 50
    setExecutionHistory(newHistory);
    localStorage.setItem('executionHistory', JSON.stringify(newHistory));
  };

  const toggleFavorite = (templateId) => {
    const newFavorites = favoriteTemplates.includes(templateId)
      ? favoriteTemplates.filter(id => id !== templateId)
      : [...favoriteTemplates, templateId];
    
    setFavoriteTemplates(newFavorites);
    localStorage.setItem('favoriteTemplates', JSON.stringify(newFavorites));
    
    toast.success(
      favoriteTemplates.includes(templateId) 
        ? 'Removed from favorites' 
        : 'Added to favorites'
    );
  };

  const handleExecuteTemplate = async () => {
    if (!selectedTemplate) return;

    try {
      setExecuting(true);
      setExecutionProgress({ step: 0, total: selectedTemplate.workflow.steps.length, currentStep: 'Initializing...' });
      
      // Simulate execution progress
      for (let i = 0; i < selectedTemplate.workflow.steps.length; i++) {
        const step = selectedTemplate.workflow.steps[i];
        setExecutionProgress({
          step: i + 1,
          total: selectedTemplate.workflow.steps.length,
          currentStep: step.name,
          percentage: ((i + 1) / selectedTemplate.workflow.steps.length) * 100
        });
        
        // Simulate step execution
        await new Promise(resolve => setTimeout(resolve, 1000));
      }
      
      const result = {
        success: true,
        executionId: 'exec-' + Date.now(),
        message: `${selectedTemplate.name} executed successfully!`,
        duration: '2.5s',
        output: {
          affectedItems: Math.floor(Math.random() * 100) + 1,
          nextRun: new Date(Date.now() + 86400000).toISOString()
        }
      };
      
      setExecutionResult(result);
      saveExecutionToHistory(selectedTemplate, parameters, result);
      
      // Update template usage count
      const updatedTemplates = templates.map(t => 
        t.id === selectedTemplate.id 
          ? { ...t, usageCount: (t.usageCount || 0) + 1 }
          : t
      );
      setTemplates(updatedTemplates);
      
      toast.success('Template executed successfully!');
      
      // Learn from execution (simulate memory update)
      const memoryUpdate = {
        templateId: selectedTemplate.id,
        successfulParameters: parameters,
        timestamp: new Date().toISOString()
      };
      localStorage.setItem(`template_memory_${selectedTemplate.id}`, JSON.stringify(memoryUpdate));
      
    } catch (error) {
      console.error('Failed to execute template:', error);
      toast.error('Failed to execute template');
      setExecutionResult({
        success: false,
        message: error.message || 'Execution failed'
      });
    } finally {
      setExecuting(false);
      setExecutionProgress(null);
    }
  };

  const handleCreateFromPrompt = async () => {
    if (!naturalLanguagePrompt.trim()) {
      toast.error('Please describe what you want to automate');
      return;
    }
    
    setGeneratingTemplate(true);
    
    try {
      // Simulate AI template generation
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      const generatedTemplate = {
        id: 'custom-' + Date.now(),
        name: 'Custom Workflow',
        description: naturalLanguagePrompt,
        category: 'Custom',
        icon: '🤖',
        difficulty: 'custom',
        estimatedTime: '2 mins',
        nodeCount: 3,
        tags: ['custom', 'ai-generated'],
        usageCount: 0,
        rating: 0,
        parameters: [
          { name: 'input', type: 'text', required: true, description: 'Input data' }
        ],
        workflow: {
          steps: [
            { id: 1, name: 'Process input', plugin: 'ai', icon: '🔄' },
            { id: 2, name: 'Execute action', plugin: 'system', icon: '⚡' },
            { id: 3, name: 'Return result', plugin: 'output', icon: '✅' }
          ]
        }
      };
      
      setTemplates([generatedTemplate, ...templates]);
      setSelectedTemplate(generatedTemplate);
      setShowCreateModal(false);
      setShowExecuteModal(true);
      toast.success('Template created from your description!');
      
    } catch (error) {
      toast.error('Failed to generate template');
    } finally {
      setGeneratingTemplate(false);
      setNaturalLanguagePrompt('');
    }
  };

  const openExecuteModal = (template) => {
    setSelectedTemplate(template);
    setShowExecuteModal(true);
    setExecutionResult(null);
    
    // Load saved parameters from memory
    const savedMemory = localStorage.getItem(`template_memory_${template.id}`);
    if (savedMemory) {
      const memory = JSON.parse(savedMemory);
      setParameters(memory.successfulParameters || {});
    } else {
      // Initialize with default values
      const initialParams = {};
      template.parameters?.forEach(param => {
        initialParams[param.name] = param.defaultValue || '';
      });
      setParameters(initialParams);
    }
  };

  // Filter and sort templates
  const getFilteredTemplates = () => {
    let filtered = templates;
    
    // Category filter
    if (selectedCategory !== 'all') {
      filtered = filtered.filter(t => 
        t.category.toLowerCase().replace(/\s+/g, '-') === selectedCategory
      );
    }
    
    // Search filter
    if (searchTerm) {
      const search = searchTerm.toLowerCase();
      filtered = filtered.filter(t => 
        t.name.toLowerCase().includes(search) ||
        t.description.toLowerCase().includes(search) ||
        t.tags?.some(tag => tag.toLowerCase().includes(search))
      );
    }
    
    // Sorting
    switch (sortBy) {
      case 'recent':
        filtered = [...filtered].sort((a, b) => b.usageCount - a.usageCount);
        break;
      case 'alphabetical':
        filtered = [...filtered].sort((a, b) => a.name.localeCompare(b.name));
        break;
      case 'popularity':
      default:
        filtered = [...filtered].sort((a, b) => (b.rating * b.usageCount) - (a.rating * a.usageCount));
    }
    
    return filtered;
  };

  const filteredTemplates = getFilteredTemplates();

  return (
    <motion.div 
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8"
    >
      {/* Header with search and actions */}
      <div className="bg-gradient-to-r from-purple-600 to-blue-600 rounded-2xl p-8 text-white">
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-6">
          <div>
            <h1 className="text-4xl font-bold mb-3">Template Gallery</h1>
            <p className="text-xl text-white/90">
              Automate your workflow in seconds with pre-built templates
            </p>
          </div>
          <div className="flex flex-col sm:flex-row gap-3">
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => setShowCreateModal(true)}
              className="flex items-center space-x-2 px-6 py-3 bg-white text-purple-600 rounded-lg font-medium shadow-lg hover:shadow-xl transition-all"
            >
              <Sparkles className="h-5 w-5" />
              <span>Create from Description</span>
            </motion.button>
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              className="flex items-center space-x-2 px-6 py-3 bg-white/20 backdrop-blur text-white rounded-lg font-medium hover:bg-white/30 transition-all"
            >
              <Plus className="h-5 w-5" />
              <span>New Template</span>
            </motion.button>
          </div>
        </div>
        
        {/* Search bar */}
        <div className="mt-6 relative">
          <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-gray-400" />
          <input
            type="text"
            placeholder="Search templates... Try 'email' or 'invoice'"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-12 pr-4 py-4 bg-white text-gray-900 rounded-xl text-lg placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-white shadow-lg"
          />
        </div>
      </div>

      {/* Quick stats */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <motion.div 
          whileHover={{ scale: 1.02 }}
          className="bg-white rounded-lg p-4 shadow-sm border border-gray-200"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Total Templates</p>
              <p className="text-2xl font-bold text-gray-900">{templates.length}</p>
            </div>
            <FileText className="h-8 w-8 text-purple-600" />
          </div>
        </motion.div>
        
        <motion.div 
          whileHover={{ scale: 1.02 }}
          className="bg-white rounded-lg p-4 shadow-sm border border-gray-200"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Executions Today</p>
              <p className="text-2xl font-bold text-gray-900">
                {executionHistory.filter(e => 
                  new Date(e.timestamp).toDateString() === new Date().toDateString()
                ).length}
              </p>
            </div>
            <TrendingUp className="h-8 w-8 text-green-600" />
          </div>
        </motion.div>
        
        <motion.div 
          whileHover={{ scale: 1.02 }}
          className="bg-white rounded-lg p-4 shadow-sm border border-gray-200"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Time Saved</p>
              <p className="text-2xl font-bold text-gray-900">12.5 hrs</p>
            </div>
            <Clock className="h-8 w-8 text-blue-600" />
          </div>
        </motion.div>
        
        <motion.div 
          whileHover={{ scale: 1.02 }}
          className="bg-white rounded-lg p-4 shadow-sm border border-gray-200"
        >
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-600">Favorites</p>
              <p className="text-2xl font-bold text-gray-900">{favoriteTemplates.length}</p>
            </div>
            <Star className="h-8 w-8 text-yellow-500" />
          </div>
        </motion.div>
      </div>

      {/* Category filters and view controls */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div className="flex items-center gap-2 overflow-x-auto pb-2">
          {templateCategories.map(category => (
            <motion.button
              key={category.id}
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => {
                setSelectedCategory(category.id);
                localStorage.setItem('lastSelectedCategory', category.id);
              }}
              className={`
                flex items-center space-x-2 px-4 py-2 rounded-lg whitespace-nowrap transition-all
                ${selectedCategory === category.id 
                  ? 'bg-purple-100 text-purple-700 border-2 border-purple-300' 
                  : 'bg-white text-gray-700 hover:bg-gray-50 border border-gray-200'
                }
              `}
            >
              <span className="text-lg">{category.icon}</span>
              <span className="font-medium">{category.name}</span>
              <span className="text-sm bg-gray-100 px-2 py-0.5 rounded-full">
                {category.count}
              </span>
            </motion.button>
          ))}
        </div>
        
        <div className="flex items-center gap-3">
          <select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-purple-500"
          >
            <option value="popularity">Most Popular</option>
            <option value="recent">Recently Used</option>
            <option value="alphabetical">Alphabetical</option>
          </select>
          
          <div className="flex items-center bg-white border border-gray-300 rounded-lg">
            <button
              onClick={() => setViewMode('grid')}
              className={`p-2 ${viewMode === 'grid' ? 'bg-purple-100 text-purple-700' : 'text-gray-600'}`}
            >
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <rect x="3" y="3" width="7" height="7" rx="1" />
                <rect x="14" y="3" width="7" height="7" rx="1" />
                <rect x="3" y="14" width="7" height="7" rx="1" />
                <rect x="14" y="14" width="7" height="7" rx="1" />
              </svg>
            </button>
            <button
              onClick={() => setViewMode('list')}
              className={`p-2 ${viewMode === 'list' ? 'bg-purple-100 text-purple-700' : 'text-gray-600'}`}
            >
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </svg>
            </button>
          </div>
        </div>
      </div>

      {/* Templates Grid/List */}
      {filteredTemplates.length === 0 ? (
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center py-20 bg-gray-50 rounded-2xl border-2 border-dashed border-gray-300"
        >
          <FileText className="h-16 w-16 text-gray-400 mx-auto mb-6" />
          <h3 className="text-xl font-semibold text-gray-900 mb-3">No templates found</h3>
          <p className="text-gray-600 mb-6 max-w-md mx-auto">
            Try adjusting your search or filters, or create a new template from scratch
          </p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="btn-primary"
          >
            Create New Template
          </button>
        </motion.div>
      ) : (
        <div className={
          viewMode === 'grid' 
            ? "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6"
            : "space-y-4"
        }>
          {filteredTemplates.map((template, index) => (
            <motion.div
              key={template.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
              whileHover={{ y: -5 }}
              className={
                viewMode === 'grid'
                  ? "bg-white rounded-xl border border-gray-200 p-6 hover:shadow-xl transition-all cursor-pointer relative group"
                  : "bg-white rounded-xl border border-gray-200 p-6 hover:shadow-lg transition-all cursor-pointer flex items-center justify-between"
              }
              onClick={() => openExecuteModal(template)}
            >
              {/* Favorite button */}
              <motion.button
                whileHover={{ scale: 1.2 }}
                whileTap={{ scale: 0.9 }}
                onClick={(e) => {
                  e.stopPropagation();
                  toggleFavorite(template.id);
                }}
                className={`absolute top-4 right-4 p-2 rounded-lg transition-colors ${
                  favoriteTemplates.includes(template.id)
                    ? 'text-yellow-500 bg-yellow-50'
                    : 'text-gray-400 hover:text-yellow-500 hover:bg-yellow-50'
                }`}
              >
                <Star className={`h-5 w-5 ${favoriteTemplates.includes(template.id) ? 'fill-current' : ''}`} />
              </motion.button>
              
              {viewMode === 'grid' ? (
                <>
                  <div className="flex items-start justify-between mb-4">
                    <div className="p-3 bg-gradient-to-br from-purple-100 to-blue-100 rounded-xl">
                      <span className="text-2xl">{template.icon}</span>
                    </div>
                    <span className={`
                      text-xs px-2 py-1 rounded-full font-medium
                      ${template.difficulty === 'beginner' ? 'bg-green-100 text-green-700' :
                        template.difficulty === 'intermediate' ? 'bg-yellow-100 text-yellow-700' :
                        'bg-red-100 text-red-700'}
                    `}>
                      {template.difficulty}
                    </span>
                  </div>
                  
                  <h3 className="text-lg font-semibold text-gray-900 mb-2 line-clamp-1">
                    {template.name}
                  </h3>
                  <p className="text-sm text-gray-600 mb-4 line-clamp-2">
                    {template.description}
                  </p>
                  
                  <div className="flex items-center justify-between text-sm text-gray-500 mb-4">
                    <span className="flex items-center space-x-1">
                      <Zap className="h-4 w-4" />
                      <span>{template.nodeCount} steps</span>
                    </span>
                    <span className="flex items-center space-x-1">
                      <Clock className="h-4 w-4" />
                      <span>{template.estimatedTime}</span>
                    </span>
                  </div>
                  
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center space-x-1">
                      <div className="flex -space-x-1">
                        {[...Array(5)].map((_, i) => (
                          <Star
                            key={i}
                            className={`h-4 w-4 ${
                              i < Math.floor(template.rating)
                                ? 'text-yellow-400 fill-current'
                                : 'text-gray-300'
                            }`}
                          />
                        ))}
                      </div>
                      <span className="text-sm text-gray-600">({template.usageCount})</span>
                    </div>
                  </div>
                  
                  <div className="flex flex-wrap gap-2 mb-4">
                    {template.tags?.slice(0, 3).map(tag => (
                      <span key={tag} className="text-xs px-2 py-1 bg-gray-100 text-gray-600 rounded-md">
                        {tag}
                      </span>
                    ))}
                  </div>
                  
                  <motion.button
                    whileHover={{ scale: 1.05 }}
                    whileTap={{ scale: 0.95 }}
                    onClick={(e) => {
                      e.stopPropagation();
                      openExecuteModal(template);
                    }}
                    className="w-full flex items-center justify-center space-x-2 px-4 py-2 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-lg font-medium hover:shadow-lg transition-all"
                  >
                    <Play className="h-4 w-4" />
                    <span>Use Template</span>
                  </motion.button>
                </>
              ) : (
                <>
                  <div className="flex items-center space-x-4 flex-1">
                    <div className="p-3 bg-gradient-to-br from-purple-100 to-blue-100 rounded-xl">
                      <span className="text-2xl">{template.icon}</span>
                    </div>
                    <div className="flex-1">
                      <h3 className="text-lg font-semibold text-gray-900 mb-1">
                        {template.name}
                      </h3>
                      <p className="text-sm text-gray-600 mb-2">
                        {template.description}
                      </p>
                      <div className="flex items-center space-x-4 text-sm text-gray-500">
                        <span className="flex items-center space-x-1">
                          <Zap className="h-4 w-4" />
                          <span>{template.nodeCount} steps</span>
                        </span>
                        <span className="flex items-center space-x-1">
                          <Clock className="h-4 w-4" />
                          <span>{template.estimatedTime}</span>
                        </span>
                        <div className="flex items-center space-x-1">
                          {[...Array(5)].map((_, i) => (
                            <Star
                              key={i}
                              className={`h-4 w-4 ${
                                i < Math.floor(template.rating)
                                  ? 'text-yellow-400 fill-current'
                                  : 'text-gray-300'
                              }`}
                            />
                          ))}
                          <span>({template.usageCount})</span>
                        </div>
                      </div>
                    </div>
                    <ChevronRight className="h-5 w-5 text-gray-400 group-hover:text-purple-600 transition-colors" />
                  </div>
                </>
              )}
            </motion.div>
          ))}
        </div>
      )}

      {/* Execute Modal */}
      <AnimatePresence>
        {showExecuteModal && selectedTemplate && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
            onClick={() => setShowExecuteModal(false)}
          >
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto shadow-2xl"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="p-6 border-b border-gray-200 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-t-2xl">
                <div className="flex items-start justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="p-3 bg-white/20 backdrop-blur rounded-xl">
                      <span className="text-2xl">{selectedTemplate.icon}</span>
                    </div>
                    <div>
                      <h3 className="text-2xl font-bold">
                        {selectedTemplate.name}
                      </h3>
                      <p className="text-white/90 mt-1">{selectedTemplate.description}</p>
                    </div>
                  </div>
                  <button
                    onClick={() => setShowExecuteModal(false)}
                    className="p-2 hover:bg-white/20 rounded-lg transition-colors"
                  >
                    <X className="h-5 w-5" />
                  </button>
                </div>
              </div>

              <div className="p-6">
                {!executionResult ? (
                  <>
                    {/* Workflow Preview */}
                    <div className="mb-8">
                      <h4 className="text-lg font-semibold text-gray-900 mb-4">Workflow Steps</h4>
                      <div className="space-y-3">
                        {selectedTemplate.workflow.steps.map((step, index) => (
                          <motion.div
                            key={step.id}
                            initial={{ opacity: 0, x: -20 }}
                            animate={{ opacity: 1, x: 0 }}
                            transition={{ delay: index * 0.1 }}
                            className={`
                              flex items-center space-x-3 p-3 rounded-lg border
                              ${executionProgress && executionProgress.step > index
                                ? 'bg-green-50 border-green-200'
                                : executionProgress && executionProgress.step === index + 1
                                ? 'bg-blue-50 border-blue-200'
                                : 'bg-gray-50 border-gray-200'
                              }
                            `}
                          >
                            <div className="flex-shrink-0">
                              {executionProgress && executionProgress.step > index ? (
                                <div className="w-8 h-8 bg-green-500 rounded-full flex items-center justify-center">
                                  <Check className="h-5 w-5 text-white" />
                                </div>
                              ) : executionProgress && executionProgress.step === index + 1 ? (
                                <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center">
                                  <Loader className="h-5 w-5 text-white animate-spin" />
                                </div>
                              ) : (
                                <div className="w-8 h-8 bg-gray-300 rounded-full flex items-center justify-center">
                                  <span className="text-gray-600 font-medium">{index + 1}</span>
                                </div>
                              )}
                            </div>
                            <div className="flex-1">
                              <div className="flex items-center space-x-2">
                                <span>{step.icon}</span>
                                <span className="font-medium text-gray-900">{step.name}</span>
                              </div>
                            </div>
                            {index < selectedTemplate.workflow.steps.length - 1 && (
                              <ArrowRight className="h-4 w-4 text-gray-400" />
                            )}
                          </motion.div>
                        ))}
                      </div>
                    </div>
                    
                    {/* Progress Bar */}
                    {executionProgress && (
                      <div className="mb-6">
                        <div className="flex items-center justify-between mb-2">
                          <span className="text-sm font-medium text-gray-700">
                            {executionProgress.currentStep}
                          </span>
                          <span className="text-sm text-gray-500">
                            {executionProgress.step} / {executionProgress.total}
                          </span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <motion.div
                            initial={{ width: 0 }}
                            animate={{ width: `${executionProgress.percentage}%` }}
                            className="bg-gradient-to-r from-purple-600 to-blue-600 h-2 rounded-full"
                          />
                        </div>
                      </div>
                    )}
                    
                    {/* Parameters Form */}
                    {selectedTemplate.parameters && selectedTemplate.parameters.length > 0 && !executing && (
                      <div className="mb-8">
                        <h4 className="text-lg font-semibold text-gray-900 mb-4">Configure Parameters</h4>
                        <div className="space-y-4">
                          {selectedTemplate.parameters.map(param => (
                            <div key={param.name}>
                              <label className="block text-sm font-medium text-gray-700 mb-1">
                                {param.description || param.name}
                                {param.required && <span className="text-red-500 ml-1">*</span>}
                              </label>
                              {param.type === 'select' ? (
                                <select
                                  value={parameters[param.name] || param.defaultValue || ''}
                                  onChange={(e) => setParameters({
                                    ...parameters,
                                    [param.name]: e.target.value
                                  })}
                                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                                >
                                  <option value="">Select...</option>
                                  {param.options?.map(option => (
                                    <option key={option} value={option}>{option}</option>
                                  ))}
                                </select>
                              ) : param.type === 'boolean' ? (
                                <label className="flex items-center space-x-2">
                                  <input
                                    type="checkbox"
                                    checked={parameters[param.name] || param.defaultValue || false}
                                    onChange={(e) => setParameters({
                                      ...parameters,
                                      [param.name]: e.target.checked
                                    })}
                                    className="w-4 h-4 text-purple-600 border-gray-300 rounded focus:ring-purple-500"
                                  />
                                  <span className="text-sm text-gray-700">Enable</span>
                                </label>
                              ) : (
                                <input
                                  type={param.type === 'number' ? 'number' : 'text'}
                                  value={parameters[param.name] || param.defaultValue || ''}
                                  onChange={(e) => setParameters({
                                    ...parameters,
                                    [param.name]: e.target.value
                                  })}
                                  placeholder={param.placeholder || `Enter ${param.name}`}
                                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500"
                                />
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                    
                    <div className="flex space-x-3">
                      <motion.button
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={handleExecuteTemplate}
                        disabled={executing}
                        className="flex-1 flex items-center justify-center space-x-2 px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-lg transition-all"
                      >
                        {executing ? (
                          <>
                            <Loader className="h-5 w-5 animate-spin" />
                            <span>Executing...</span>
                          </>
                        ) : (
                          <>
                            <Play className="h-5 w-5" />
                            <span>Execute Template</span>
                          </>
                        )}
                      </motion.button>
                      <button
                        onClick={() => setShowExecuteModal(false)}
                        className="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg font-medium hover:bg-gray-200 transition-colors"
                        disabled={executing}
                      >
                        Cancel
                      </button>
                    </div>
                  </>
                ) : (
                  /* Execution Result */
                  <motion.div 
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="text-center py-12"
                  >
                    {executionResult.success ? (
                      <>
                        <motion.div 
                          initial={{ scale: 0 }}
                          animate={{ scale: 1 }}
                          transition={{ type: "spring", stiffness: 200 }}
                          className="mb-6"
                        >
                          <div className="inline-flex items-center justify-center w-20 h-20 bg-green-100 rounded-full">
                            <Check className="h-10 w-10 text-green-600" />
                          </div>
                        </motion.div>
                        <h4 className="text-2xl font-semibold text-gray-900 mb-3">
                          Execution Successful!
                        </h4>
                        <p className="text-gray-600 mb-6">{executionResult.message}</p>
                        
                        {executionResult.output && (
                          <div className="bg-gray-50 rounded-lg p-4 mb-6 text-left max-w-md mx-auto">
                            <h5 className="font-semibold text-gray-900 mb-2">Results:</h5>
                            <div className="space-y-2 text-sm">
                              <div className="flex justify-between">
                                <span className="text-gray-600">Items Processed:</span>
                                <span className="font-medium">{executionResult.output.affectedItems}</span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-gray-600">Duration:</span>
                                <span className="font-medium">{executionResult.duration}</span>
                              </div>
                              <div className="flex justify-between">
                                <span className="text-gray-600">Next Run:</span>
                                <span className="font-medium">
                                  {new Date(executionResult.output.nextRun).toLocaleDateString()}
                                </span>
                              </div>
                            </div>
                          </div>
                        )}
                      </>
                    ) : (
                      <>
                        <div className="inline-flex items-center justify-center w-20 h-20 bg-red-100 rounded-full mb-6">
                          <AlertCircle className="h-10 w-10 text-red-600" />
                        </div>
                        <h4 className="text-2xl font-semibold text-gray-900 mb-3">
                          Execution Failed
                        </h4>
                        <p className="text-red-600 mb-6">{executionResult.message}</p>
                      </>
                    )}
                    
                    <div className="flex space-x-3 justify-center">
                      <button
                        onClick={() => setShowExecuteModal(false)}
                        className="px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-lg font-medium hover:shadow-lg transition-all"
                      >
                        Done
                      </button>
                      {executionResult.success && (
                        <button
                          onClick={() => {
                            setExecutionResult(null);
                            setParameters({});
                          }}
                          className="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg font-medium hover:bg-gray-200 transition-colors"
                        >
                          Run Again
                        </button>
                      )}
                    </div>
                  </motion.div>
                )}
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Create from Natural Language Modal */}
      <AnimatePresence>
        {showCreateModal && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50"
            onClick={() => setShowCreateModal(false)}
          >
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-white rounded-2xl max-w-2xl w-full shadow-2xl"
              onClick={(e) => e.stopPropagation()}
            >
              <div className="p-6 border-b border-gray-200">
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <Sparkles className="h-8 w-8 text-purple-600" />
                    <div>
                      <h3 className="text-xl font-bold text-gray-900">
                        Create Template from Description
                      </h3>
                      <p className="text-sm text-gray-600">
                        Describe what you want to automate in plain English
                      </p>
                    </div>
                  </div>
                  <button
                    onClick={() => setShowCreateModal(false)}
                    className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                  >
                    <X className="h-5 w-5 text-gray-500" />
                  </button>
                </div>
              </div>

              <div className="p-6">
                <div className="mb-6">
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    What would you like to automate?
                  </label>
                  <textarea
                    value={naturalLanguagePrompt}
                    onChange={(e) => setNaturalLanguagePrompt(e.target.value)}
                    placeholder="Example: Every Friday, collect all customer support tickets from this week, analyze sentiment, and email a summary to the team"
                    className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-purple-500 h-32 resize-none"
                  />
                </div>

                <div className="bg-purple-50 border border-purple-200 rounded-lg p-4 mb-6">
                  <h4 className="font-medium text-purple-900 mb-2">Examples to try:</h4>
                  <ul className="space-y-2 text-sm text-purple-700">
                    <li className="flex items-start space-x-2">
                      <MessageSquare className="h-4 w-4 mt-0.5 flex-shrink-0" />
                      <span>"Send me a daily summary of my calendar events and weather"</span>
                    </li>
                    <li className="flex items-start space-x-2">
                      <MessageSquare className="h-4 w-4 mt-0.5 flex-shrink-0" />
                      <span>"Monitor my website and alert me if it goes down"</span>
                    </li>
                    <li className="flex items-start space-x-2">
                      <MessageSquare className="h-4 w-4 mt-0.5 flex-shrink-0" />
                      <span>"Backup my important files to cloud storage every week"</span>
                    </li>
                  </ul>
                </div>

                <div className="flex space-x-3">
                  <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={handleCreateFromPrompt}
                    disabled={generatingTemplate || !naturalLanguagePrompt.trim()}
                    className="flex-1 flex items-center justify-center space-x-2 px-6 py-3 bg-gradient-to-r from-purple-600 to-blue-600 text-white rounded-lg font-medium disabled:opacity-50 disabled:cursor-not-allowed hover:shadow-lg transition-all"
                  >
                    {generatingTemplate ? (
                      <>
                        <Loader className="h-5 w-5 animate-spin" />
                        <span>Creating Template...</span>
                      </>
                    ) : (
                      <>
                        <Sparkles className="h-5 w-5" />
                        <span>Create Template</span>
                      </>
                    )}
                  </motion.button>
                  <button
                    onClick={() => setShowCreateModal(false)}
                    className="px-6 py-3 bg-gray-100 text-gray-700 rounded-lg font-medium hover:bg-gray-200 transition-colors"
                  >
                    Cancel
                  </button>
                </div>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </motion.div>
  );
};

export default Templates;