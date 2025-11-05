import React, { useState, useEffect } from 'react';
import { 
  Settings as SettingsIcon, 
  Brain, 
  Bell, 
  Shield, 
  User,
  Save,
  Info,
  Check
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import toast from 'react-hot-toast';
import apiService from '../services/api';

const Settings = () => {
  const [settings, setSettings] = useState({
    consciousness: {
      mode: 'OFF',
      consent: {
        ENHANCED: false,
        FULL: false
      }
    },
    notifications: {
      email: false,
      push: false,
      executionComplete: false,
      executionFailed: false
    },
    security: {
      twoFactor: false,
      sessionTimeout: 30
    },
    profile: {
      name: '',
      email: '',
      company: ''
    }
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [activeTab, setActiveTab] = useState('consciousness');

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    try {
      setLoading(true);
      
      // Get real settings from backend
      const [consciousnessSettings, userProfile] = await Promise.all([
        apiService.getSettings().catch(() => ({ consciousnessMode: 'OFF' })),
        apiService.getUserProfile().catch(() => ({ name: 'User', email: '' }))
      ]);
      
      setSettings(prev => ({
        ...prev,
        consciousness: {
          mode: consciousnessSettings.consciousnessMode || consciousnessSettings.mode || 'OFF',
          consent: consciousnessSettings.consent || {
            ENHANCED: false,
            FULL: false
          }
        },
        profile: {
          name: userProfile.name || '',
          email: userProfile.email || '',
          company: userProfile.company || ''
        }
      }));
    } catch (error) {
      console.error('Failed to load settings:', error);
      toast.error('Failed to load settings from server');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveSettings = async () => {
    try {
      setSaving(true);
      
      // Save to backend
      await apiService.updateSettings({
        consciousnessMode: settings.consciousness.mode,
        consent: settings.consciousness.consent,
        notifications: settings.notifications
      });
      
      // Also save to localStorage as backup
      localStorage.setItem('obvian-settings', JSON.stringify(settings));
      
      setSaveSuccess(true);
      toast.success('Settings saved successfully!');
      
      // Reset success state after 3 seconds
      setTimeout(() => setSaveSuccess(false), 3000);
      
    } catch (error) {
      console.error('Failed to save settings:', error);
      toast.error('Failed to save settings to server');
    } finally {
      setSaving(false);
    }
  };

  const consciousnessModes = [
    { id: 'OFF', name: 'Off', description: 'No AI assistance' },
    { id: 'BASIC', name: 'Basic', description: 'Template suggestions and error explanations' },
    { id: 'ENHANCED', name: 'Enhanced', description: 'Pattern learning and optimization' },
    { id: 'FULL', name: 'Full', description: 'Proactive AI co-pilot' }
  ];

  const tabs = [
    { id: 'consciousness', name: 'Consciousness', icon: Brain },
    { id: 'notifications', name: 'Notifications', icon: Bell },
    { id: 'security', name: 'Security', icon: Shield },
    { id: 'profile', name: 'Profile', icon: User }
  ];

  // Custom toggle component
  const Toggle = ({ checked, onChange, disabled = false }) => (
    <motion.button
      type="button"
      onClick={() => !disabled && onChange(!checked)}
      className={`
        relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200 ease-in-out
        ${checked 
          ? 'bg-gradient-to-r from-purple-500 to-blue-500' 
          : 'bg-gray-200'
        }
        ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
        focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2
      `}
      whileTap={{ scale: disabled ? 1 : 0.95 }}
    >
      <motion.span
        className={`
          inline-block h-4 w-4 transform rounded-full bg-white shadow-lg transition-transform duration-200 ease-in-out
          ${checked ? 'translate-x-6' : 'translate-x-1'}
        `}
        layout
        transition={{ type: "spring", stiffness: 500, damping: 30 }}
      />
    </motion.button>
  );

  // Loading skeleton component
  const LoadingSkeleton = () => (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="space-y-2">
        <div className="h-8 bg-gray-200 rounded-lg animate-pulse w-1/3"></div>
        <div className="h-4 bg-gray-200 rounded-lg animate-pulse w-1/2"></div>
      </div>
      
      <div className="border-b border-gray-200">
        <div className="flex space-x-8">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="flex items-center space-x-2 py-3">
              <div className="h-5 w-5 bg-gray-200 rounded animate-pulse"></div>
              <div className="h-4 w-20 bg-gray-200 rounded animate-pulse"></div>
            </div>
          ))}
        </div>
      </div>
      
      <div className="card">
        <div className="space-y-6">
          <div className="h-6 bg-gray-200 rounded-lg animate-pulse w-1/4"></div>
          <div className="space-y-4">
            {[1, 2, 3].map(i => (
              <div key={i} className="flex items-center justify-between">
                <div className="space-y-2">
                  <div className="h-4 bg-gray-200 rounded animate-pulse w-32"></div>
                  <div className="h-3 bg-gray-200 rounded animate-pulse w-48"></div>
                </div>
                <div className="h-6 w-11 bg-gray-200 rounded-full animate-pulse"></div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );

  if (loading) {
    return <LoadingSkeleton />;
  }

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="min-h-screen p-6"
    >
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <motion.div 
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.1 }}
          className="mb-8"
        >
          <div className="flex items-center space-x-4 mb-2">
            <div className="p-3 bg-gradient-to-r from-purple-500 to-blue-500 rounded-2xl">
              <SettingsIcon className="h-6 w-6 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold gradient-text">Settings</h1>
              <p className="text-gray-600 mt-1">Configure your Obvian experience</p>
            </div>
          </div>
        </motion.div>

        {/* Tabs */}
        <motion.div 
          initial={{ opacity: 0, x: -20 }}
          animate={{ opacity: 1, x: 0 }}
          transition={{ duration: 0.6, delay: 0.2 }}
          className="mb-8"
        >
          <div className="bg-white/70 backdrop-blur-xl rounded-2xl border border-white/50 shadow-xl p-2">
            <nav className="flex space-x-2">
              {tabs.map((tab, index) => {
                const Icon = tab.icon;
                return (
                  <motion.button
                    key={tab.id}
                    onClick={() => setActiveTab(tab.id)}
                    className={`
                      flex items-center space-x-3 px-6 py-4 rounded-xl font-medium transition-all duration-300 flex-1 justify-center
                      ${activeTab === tab.id 
                        ? 'bg-gradient-to-r from-purple-500 to-blue-500 text-white shadow-lg' 
                        : 'text-gray-600 hover:text-gray-900 hover:bg-white/50'
                      }
                    `}
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    initial={false}
                    animate={{
                      scale: activeTab === tab.id ? 1.02 : 1
                    }}
                    transition={{ duration: 0.2 }}
                  >
                    <Icon className="h-5 w-5" />
                    <span>{tab.name}</span>
                  </motion.button>
                );
              })}
            </nav>
          </div>
        </motion.div>

        {/* Tab Content */}
        <AnimatePresence mode="wait">
          <motion.div
            key={activeTab}
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -20 }}
            transition={{ duration: 0.3 }}
            className="bg-white/80 backdrop-blur-xl rounded-3xl border border-white/50 shadow-2xl p-8"
          >
            {activeTab === 'consciousness' && (
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.1 }}
                className="space-y-8"
              >
                <div className="flex items-center space-x-4 mb-6">
                  <div className="p-3 bg-gradient-to-r from-purple-100 to-blue-100 rounded-xl">
                    <Brain className="h-6 w-6 text-purple-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-gray-900">AI Consciousness Mode</h2>
                    <p className="text-gray-600">Choose your level of AI assistance and automation</p>
                  </div>
                </div>

                <div className="grid gap-4">
                  {consciousnessModes.map((mode, index) => (
                    <motion.label 
                      key={mode.id}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.1 }}
                      className={`
                        group relative flex items-start space-x-4 p-6 rounded-2xl cursor-pointer transition-all duration-300 border-2
                        ${settings.consciousness.mode === mode.id 
                          ? 'border-purple-300 bg-gradient-to-r from-purple-50 to-blue-50 shadow-lg' 
                          : 'border-gray-200 bg-white/50 hover:border-purple-200 hover:bg-purple-50/50'
                        }
                      `}
                      whileHover={{ scale: 1.02, y: -2 }}
                      whileTap={{ scale: 0.98 }}
                    >
                      <div className="relative mt-1">
                        <input
                          type="radio"
                          name="consciousness-mode"
                          value={mode.id}
                          checked={settings.consciousness.mode === mode.id}
                          onChange={(e) => setSettings({
                            ...settings,
                            consciousness: {
                              ...settings.consciousness,
                              mode: e.target.value
                            }
                          })}
                          className="sr-only"
                        />
                        <div className={`
                          w-5 h-5 rounded-full border-2 transition-all duration-200
                          ${settings.consciousness.mode === mode.id 
                            ? 'border-purple-500 bg-purple-500' 
                            : 'border-gray-300 bg-white group-hover:border-purple-300'
                          }
                        `}>
                          {settings.consciousness.mode === mode.id && (
                            <motion.div
                              initial={{ scale: 0 }}
                              animate={{ scale: 1 }}
                              className="w-2 h-2 bg-white rounded-full mx-auto mt-1.5"
                            />
                          )}
                        </div>
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center space-x-2 mb-2">
                          <span className="text-lg font-semibold text-gray-900">{mode.name}</span>
                          {settings.consciousness.mode === mode.id && (
                            <motion.div
                              initial={{ scale: 0 }}
                              animate={{ scale: 1 }}
                              className="px-2 py-1 bg-gradient-to-r from-purple-500 to-blue-500 text-white text-xs rounded-full font-medium"
                            >
                              Active
                            </motion.div>
                          )}
                        </div>
                        <p className="text-gray-600">{mode.description}</p>
                      </div>
                    </motion.label>
                  ))}
                </div>

                <AnimatePresence>
                  {(settings.consciousness.mode === 'ENHANCED' || settings.consciousness.mode === 'FULL') && (
                    <motion.div
                      initial={{ opacity: 0, height: 0 }}
                      animate={{ opacity: 1, height: 'auto' }}
                      exit={{ opacity: 0, height: 0 }}
                      transition={{ duration: 0.3 }}
                      className="bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200 rounded-2xl p-6"
                    >
                      <div className="flex items-start space-x-4">
                        <div className="p-2 bg-blue-100 rounded-lg">
                          <Info className="h-5 w-5 text-blue-600" />
                        </div>
                        <div className="flex-1">
                          <h4 className="text-lg font-semibold text-blue-900 mb-2">Consent Required</h4>
                          <p className="text-blue-800 mb-4">
                            {settings.consciousness.mode} mode requires explicit consent for advanced AI features including pattern learning and data analysis.
                          </p>
                          <motion.label 
                            className="flex items-start space-x-3 cursor-pointer group"
                            whileHover={{ scale: 1.01 }}
                          >
                            <div className="relative mt-1">
                              <input
                                type="checkbox"
                                checked={settings.consciousness.consent[settings.consciousness.mode]}
                                onChange={(e) => setSettings({
                                  ...settings,
                                  consciousness: {
                                    ...settings.consciousness,
                                    consent: {
                                      ...settings.consciousness.consent,
                                      [settings.consciousness.mode]: e.target.checked
                                    }
                                  }
                                })}
                                className="sr-only"
                              />
                              <div className={`
                                w-5 h-5 rounded border-2 transition-all duration-200 flex items-center justify-center
                                ${settings.consciousness.consent[settings.consciousness.mode]
                                  ? 'bg-blue-500 border-blue-500' 
                                  : 'bg-white border-gray-300 group-hover:border-blue-300'
                                }
                              `}>
                                {settings.consciousness.consent[settings.consciousness.mode] && (
                                  <motion.div
                                    initial={{ scale: 0 }}
                                    animate={{ scale: 1 }}
                                  >
                                    <Check className="h-3 w-3 text-white" />
                                  </motion.div>
                                )}
                              </div>
                            </div>
                            <span className="text-blue-900 font-medium">
                              I consent to AI-enhanced features for {settings.consciousness.mode.toLowerCase()} mode
                            </span>
                          </motion.label>
                        </div>
                      </div>
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.div>
            )}

            {activeTab === 'notifications' && (
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.1 }}
                className="space-y-8"
              >
                <div className="flex items-center space-x-4 mb-6">
                  <div className="p-3 bg-gradient-to-r from-yellow-100 to-orange-100 rounded-xl">
                    <Bell className="h-6 w-6 text-orange-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-gray-900">Notification Preferences</h2>
                    <p className="text-gray-600">Control how you receive updates and alerts</p>
                  </div>
                </div>
                
                <div className="grid gap-6">
                  {[
                    { key: 'email', title: 'Email Notifications', description: 'Receive updates and summaries via email' },
                    { key: 'push', title: 'Push Notifications', description: 'Browser notifications for important updates' },
                    { key: 'executionComplete', title: 'Workflow Complete', description: 'Get notified when workflows finish successfully' },
                    { key: 'executionFailed', title: 'Workflow Failed', description: 'Immediate alerts when workflows encounter errors' }
                  ].map((item, index) => (
                    <motion.label 
                      key={item.key}
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: index * 0.1 }}
                      className="group flex items-center justify-between p-6 bg-white/60 rounded-2xl border border-gray-200 hover:border-orange-200 hover:bg-orange-50/50 transition-all duration-300 cursor-pointer"
                    >
                      <div className="flex-1">
                        <div className="text-lg font-semibold text-gray-900 group-hover:text-orange-900 transition-colors">
                          {item.title}
                        </div>
                        <div className="text-gray-600 mt-1">{item.description}</div>
                      </div>
                      <Toggle
                        checked={settings.notifications[item.key]}
                        onChange={(checked) => setSettings({
                          ...settings,
                          notifications: {
                            ...settings.notifications,
                            [item.key]: checked
                          }
                        })}
                      />
                    </motion.label>
                  ))}
                </div>
              </motion.div>
            )}

            {activeTab === 'security' && (
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.1 }}
                className="space-y-8"
              >
                <div className="flex items-center space-x-4 mb-6">
                  <div className="p-3 bg-gradient-to-r from-green-100 to-emerald-100 rounded-xl">
                    <Shield className="h-6 w-6 text-green-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-gray-900">Security Settings</h2>
                    <p className="text-gray-600">Protect your account and data</p>
                  </div>
                </div>
                
                <div className="space-y-6">
                  <motion.label 
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.1 }}
                    className="group flex items-center justify-between p-6 bg-white/60 rounded-2xl border border-gray-200 hover:border-green-200 hover:bg-green-50/50 transition-all duration-300 cursor-pointer"
                  >
                    <div className="flex-1">
                      <div className="text-lg font-semibold text-gray-900 group-hover:text-green-900 transition-colors">
                        Two-Factor Authentication
                      </div>
                      <div className="text-gray-600 mt-1">Add an extra layer of security to your account</div>
                    </div>
                    <Toggle
                      checked={settings.security.twoFactor}
                      onChange={(checked) => setSettings({
                        ...settings,
                        security: {
                          ...settings.security,
                          twoFactor: checked
                        }
                      })}
                    />
                  </motion.label>

                  <motion.div
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.2 }}
                    className="p-6 bg-white/60 rounded-2xl border border-gray-200"
                  >
                    <div className="space-y-4">
                      <div>
                        <label className="block text-lg font-semibold text-gray-900 mb-2">
                          Session Timeout
                        </label>
                        <p className="text-gray-600 mb-4">
                          Automatically sign out after a period of inactivity
                        </p>
                      </div>
                      
                      <div className="flex items-center space-x-4">
                        <div className="flex-1">
                          <input
                            type="range"
                            min="5"
                            max="120"
                            step="5"
                            value={settings.security.sessionTimeout}
                            onChange={(e) => setSettings({
                              ...settings,
                              security: {
                                ...settings.security,
                                sessionTimeout: parseInt(e.target.value)
                              }
                            })}
                            className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer slider"
                          />
                          <div className="flex justify-between text-xs text-gray-500 mt-1">
                            <span>5 min</span>
                            <span>60 min</span>
                            <span>120 min</span>
                          </div>
                        </div>
                        
                        <div className="bg-gradient-to-r from-green-500 to-emerald-500 text-white px-4 py-2 rounded-xl font-bold min-w-[80px] text-center">
                          {settings.security.sessionTimeout}m
                        </div>
                      </div>
                    </div>
                  </motion.div>
                </div>
              </motion.div>
            )}

            {activeTab === 'profile' && (
              <motion.div 
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.1 }}
                className="space-y-8"
              >
                <div className="flex items-center space-x-4 mb-6">
                  <div className="p-3 bg-gradient-to-r from-blue-100 to-indigo-100 rounded-xl">
                    <User className="h-6 w-6 text-blue-600" />
                  </div>
                  <div>
                    <h2 className="text-2xl font-bold text-gray-900">Profile Information</h2>
                    <p className="text-gray-600">Manage your personal details</p>
                  </div>
                </div>
                
                <div className="grid gap-6">
                  {[
                    { key: 'name', label: 'Full Name', type: 'text', placeholder: 'Enter your full name' },
                    { key: 'email', label: 'Email Address', type: 'email', placeholder: 'Enter your email' },
                    { key: 'company', label: 'Company', type: 'text', placeholder: 'Enter your company name' }
                  ].map((field, index) => (
                    <motion.div
                      key={field.key}
                      initial={{ opacity: 0, y: 20 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: index * 0.1 }}
                      className="space-y-2"
                    >
                      <label className="block text-lg font-semibold text-gray-900">
                        {field.label}
                      </label>
                      <input
                        type={field.type}
                        value={settings.profile[field.key]}
                        onChange={(e) => setSettings({
                          ...settings,
                          profile: {
                            ...settings.profile,
                            [field.key]: e.target.value
                          }
                        })}
                        placeholder={field.placeholder}
                        className="w-full px-6 py-4 bg-white/70 backdrop-blur border border-gray-200 rounded-2xl focus:outline-none focus:ring-4 focus:ring-blue-500/20 focus:border-blue-500 transition-all duration-300 text-lg"
                      />
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            )}
          </motion.div>
        </AnimatePresence>

        {/* Save Button */}
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.6, delay: 0.4 }}
          className="flex justify-center mt-12"
        >
          <motion.button
            onClick={handleSaveSettings}
            disabled={saving}
            className={`
              relative px-8 py-4 rounded-2xl font-bold text-lg transition-all duration-300
              ${saveSuccess 
                ? 'bg-gradient-to-r from-green-500 to-emerald-500 text-white' 
                : 'bg-gradient-to-r from-purple-600 to-blue-600 text-white hover:from-purple-700 hover:to-blue-700'
              }
              shadow-2xl hover:shadow-3xl transform hover:-translate-y-1 
              ${saving ? 'cursor-not-allowed opacity-75' : 'cursor-pointer'}
              focus:outline-none focus:ring-4 focus:ring-purple-500/20
            `}
            whileHover={!saving ? { scale: 1.05, y: -2 } : {}}
            whileTap={!saving ? { scale: 0.95 } : {}}
            animate={saveSuccess ? { scale: [1, 1.1, 1] } : {}}
            transition={{ duration: 0.3 }}
          >
            <div className="flex items-center space-x-3">
              <AnimatePresence mode="wait">
                {saving ? (
                  <motion.div
                    key="saving"
                    initial={{ opacity: 0, rotate: 0 }}
                    animate={{ opacity: 1, rotate: 360 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                  >
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full" />
                  </motion.div>
                ) : saveSuccess ? (
                  <motion.div
                    key="success"
                    initial={{ opacity: 0, scale: 0 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0 }}
                    transition={{ duration: 0.3, type: "spring", stiffness: 500 }}
                  >
                    <Check className="h-5 w-5" />
                  </motion.div>
                ) : (
                  <motion.div
                    key="save"
                    initial={{ opacity: 0, scale: 0 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0 }}
                    transition={{ duration: 0.2 }}
                  >
                    <Save className="h-5 w-5" />
                  </motion.div>
                )}
              </AnimatePresence>
              
              <span>
                {saving ? 'Saving...' : saveSuccess ? 'Saved!' : 'Save Settings'}
              </span>
            </div>
            
            {/* Ripple effect for save success */}
            {saveSuccess && (
              <motion.div
                className="absolute inset-0 bg-white/20 rounded-2xl"
                initial={{ scale: 0, opacity: 1 }}
                animate={{ scale: 1, opacity: 0 }}
                transition={{ duration: 0.6 }}
              />
            )}
          </motion.button>
        </motion.div>
      </div>
    </motion.div>
  );
};

export default Settings;