import axios from 'axios';

// Create axios instance with base configuration
const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for auth
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for error handling
api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', {
      url: error.config?.url,
      method: error.config?.method,
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      requestData: error.config?.data
    });

    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// API service methods
const apiService = {
  // Home
  getApiInfo: () => api.get('/'),
  getHealth: () => api.get('/health'),

  // Templates
  getTemplates: (params) => api.get('/api/templates', { params }),
  getTemplate: (id) => api.get(`/api/templates/${id}`),
  executeTemplate: (id, data) => api.post(`/api/templates/${id}/execute`, data),
  validateTemplate: (id, params) => api.post(`/api/templates/${id}/validate`, params),
  previewTemplate: (id, params) => api.post(`/api/templates/${id}/preview`, params),

  // Plugins
  getPlugins: (params) => api.get('/api/plugins', { params }),
  getPlugin: (id) => api.get(`/api/plugins/${id}`),
  configurePlugin: (id, config) => api.post(`/api/plugins/${id}/configure`, config),
  testPlugin: (id, data) => api.post(`/api/plugins/${id}/test`, data),

  // Execution
  execute: (data) => api.post('/api/execute', data),
  getExecutions: (params) => api.get('/api/executions', { params }),
  getExecution: (id) => api.get(`/api/executions/${id}`),
  cancelExecution: (id) => api.post(`/api/executions/${id}/cancel`),

  // Consciousness
  getConsciousnessStatus: () => api.get('/api/consciousness/status'),
  updateConsciousnessMode: (mode) => api.put('/api/consciousness/mode', mode),
  getSuggestions: (context) => api.post('/api/consciousness/suggest', context),
  
  // Settings
  getSettings: () => api.get('/api/settings'),
  updateSettings: (settings) => api.put('/api/settings', settings),
  getUserProfile: () => api.get('/api/user/profile'),
  updateUserProfile: (profile) => api.put('/api/user/profile', profile),

  // Auth
  login: async (credentials) => {
    try {
      // Try real auth endpoint first
      return await api.post('/api/auth/login', credentials);
    } catch (error) {
      // Fallback for demo mode only
      if (credentials.username === 'demo@obvian.io' || credentials.username === 'demo') {
        const token = 'demo-jwt-token-' + Date.now();
        localStorage.setItem('token', token);
        return {
          success: true,
          token,
          user: {
            id: 'demo-user',
            email: 'demo@obvian.io',
            name: 'Demo User',
            role: 'user'
          }
        };
      }
      throw error;
    }
  },
  logout: async () => {
    console.log('[API] Logout called');
    try {
      // Call backend logout endpoint
      const token = localStorage.getItem('token');
      console.log('[API] Current token:', token ? 'exists' : 'none');
      if (token) {
        await api.post('/api/auth/logout', {});
        console.log('[API] Backend logout call succeeded');
      }
    } catch (error) {
      console.log('[API] Logout endpoint call failed:', error);
    } finally {
      // Always clear all auth-related storage
      localStorage.removeItem('token');
      localStorage.removeItem('google_session');
      localStorage.removeItem('user_email');
      localStorage.removeItem('oauth_state');
      // Clear session storage too
      sessionStorage.clear();
      console.log('[API] All auth data cleared from storage');
      return { success: true };
    }
  },

  // Generic plugin configuration endpoints
  getPluginRequirements: (pluginId) => api.get(`/api/plugins/configure/${pluginId}/requirements`),
  initiatePluginOAuth: (pluginId, data) => api.post(`/api/plugins/configure/${pluginId}/oauth/initiate`, data),
  savePluginConfiguration: (pluginId, config) => api.post(`/api/plugins/configure/${pluginId}/save`, config),
  testPluginConfiguration: (pluginId) => api.post(`/api/plugins/configure/${pluginId}/test`),
  getPluginConfiguration: (pluginId) => api.get(`/api/plugins/configure/${pluginId}/current`),
  deletePluginConfiguration: (pluginId) => api.delete(`/api/plugins/configure/${pluginId}`),
  
  // Legacy Slack endpoints (can be removed later)
  initiateSlackOAuth: (data) => api.post('/api/slack/oauth/initiate', data),
  getSlackConfig: () => api.get('/api/slack/config?userId=demo-user'),
  testSlackConnection: (data) => api.post('/api/slack/test', data),
  sendSlackTestMessage: (data) => api.post('/api/slack/send-test', data),
  disconnectSlack: (data) => api.post('/api/slack/disconnect', data),
  getSlackChannels: () => api.get('/api/slack/channels?userId=demo-user'),

  // Petri Net endpoints
  petri: {
    parse: (data) => api.post('/api/v1/petri/parse', data),
    build: (data) => api.post('/api/v1/petri/build', data),
    validate: (data) => api.post('/api/v1/petri/validate', data),
    simulate: (data) => api.post('/api/v1/petri/simulate', data),
    dag: (data) => api.post('/api/v1/petri/dag', data),
    health: () => api.get('/api/v1/petri/health'),

    // Real-time WebSocket connection for live simulation and validation
    createWebSocketConnection: (callbacks = {}) => {
      const wsUrl = process.env.REACT_APP_WS_URL || 'ws://localhost:8080';
      const socket = new WebSocket(`${wsUrl}/ws/petri`);

      socket.onopen = (event) => {
        console.log('P3Net WebSocket connected');
        if (callbacks.onConnect) callbacks.onConnect(event);
      };

      socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          console.log('P3Net WebSocket message:', message);

          // Route messages to appropriate callbacks
          switch (message.type) {
            case 'CONNECTION_ESTABLISHED':
              if (callbacks.onConnectionEstablished) callbacks.onConnectionEstablished(message);
              break;
            case 'SIMULATION_STARTED':
              if (callbacks.onSimulationStarted) callbacks.onSimulationStarted(message);
              break;
            case 'TOKEN_MOVEMENT':
              if (callbacks.onTokenMovement) callbacks.onTokenMovement(message);
              break;
            case 'MARKING_UPDATE':
              if (callbacks.onMarkingUpdate) callbacks.onMarkingUpdate(message);
              break;
            case 'SIMULATION_COMPLETED':
              if (callbacks.onSimulationCompleted) callbacks.onSimulationCompleted(message);
              break;
            case 'VALIDATION_STARTED':
              if (callbacks.onValidationStarted) callbacks.onValidationStarted(message);
              break;
            case 'VALIDATION_PROGRESS':
              if (callbacks.onValidationProgress) callbacks.onValidationProgress(message);
              break;
            case 'VALIDATION_COMPLETED':
              if (callbacks.onValidationCompleted) callbacks.onValidationCompleted(message);
              break;
            case 'ERROR':
              if (callbacks.onError) callbacks.onError(message);
              break;
            default:
              if (callbacks.onMessage) callbacks.onMessage(message);
          }
        } catch (error) {
          console.error('Error parsing P3Net WebSocket message:', error);
          if (callbacks.onError) callbacks.onError({ error: 'Message parsing failed', raw: event.data });
        }
      };

      socket.onerror = (error) => {
        console.error('P3Net WebSocket error:', error);
        if (callbacks.onError) callbacks.onError(error);
      };

      socket.onclose = (event) => {
        console.log('P3Net WebSocket disconnected:', event.code, event.reason);
        if (callbacks.onClose) callbacks.onClose(event);
      };

      // Expose methods for sending messages
      socket.sendMessage = (type, data = {}) => {
        if (socket.readyState === WebSocket.OPEN) {
          const message = { type, timestamp: Date.now(), ...data };
          socket.send(JSON.stringify(message));
        } else {
          console.warn('P3Net WebSocket not ready, message not sent:', type);
        }
      };

      // Convenience methods for common operations
      socket.startSimulation = (petriNet, config = {}) => {
        socket.sendMessage('SIMULATION_START', { petriNet, config });
      };

      socket.stepSimulation = () => {
        socket.sendMessage('SIMULATION_STEP');
      };

      socket.pauseSimulation = () => {
        socket.sendMessage('SIMULATION_PAUSE');
      };

      socket.resetSimulation = () => {
        socket.sendMessage('SIMULATION_RESET');
      };

      socket.startValidation = (config = {}) => {
        socket.sendMessage('VALIDATION_START', { config });
      };

      socket.subscribe = (eventTypes) => {
        socket.sendMessage('SUBSCRIPTION', { eventTypes });
      };

      socket.ping = () => {
        socket.sendMessage('PING');
      };

      return socket;
    }
  }
};

export default apiService;