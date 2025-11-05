import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Key,
  Info,
  ExternalLink,
  Copy,
  CheckCircle,
  AlertCircle,
  Loader,
  Eye,
  EyeOff,
  HelpCircle,
  BookOpen,
  Video,
  Shield,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import apiService from '../services/api';
import toast from 'react-hot-toast';

/**
 * OAuth Credential Setup Component
 * Allows users to configure their own OAuth credentials for plugins
 */
const OAuthCredentialSetup = ({ plugin, onCredentialsSaved, onSkip }) => {
  const [credentials, setCredentials] = useState({
    clientId: '',
    clientSecret: ''
  });
  const [showSecret, setShowSecret] = useState(false);
  const [loading, setLoading] = useState(false);
  const [instructions, setInstructions] = useState(null);
  const [showInstructions, setShowInstructions] = useState(true);
  const [existingCredentials, setExistingCredentials] = useState(null);
  const [copied, setCopied] = useState('');

  useEffect(() => {
    loadInstructions();
    checkExistingCredentials();
  }, [plugin]);

  const loadInstructions = async () => {
    try {
      const response = await fetch(`/api/oauth/setup-instructions?pluginId=${plugin.id}`);
      const data = await response.json();
      setInstructions(data);
    } catch (error) {
      console.error('Failed to load setup instructions:', error);
    }
  };

  const checkExistingCredentials = async () => {
    try {
      const response = await fetch(`/api/oauth/credentials?pluginId=${plugin.id}&userId=demo-user`);
      const data = await response.json();
      if (data.isConfigured) {
        setExistingCredentials(data);
        setCredentials({
          clientId: data.clientId || '',
          clientSecret: '' // Never show existing secret
        });
      }
    } catch (error) {
      console.error('Failed to check existing credentials:', error);
    }
  };

  const handleSaveCredentials = async () => {
    if (!credentials.clientId) {
      toast.error('Client ID is required');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch('/api/oauth/credentials', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          pluginId: plugin.id,
          userId: 'demo-user',
          clientId: credentials.clientId,
          clientSecret: credentials.clientSecret
        })
      });

      const data = await response.json();
      if (data.success) {
        toast.success('OAuth credentials saved successfully!');
        onCredentialsSaved && onCredentialsSaved(credentials);
      } else {
        toast.error(data.message || 'Failed to save credentials');
      }
    } catch (error) {
      toast.error('Failed to save credentials');
      console.error('Save error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCopy = (text, type) => {
    navigator.clipboard.writeText(text);
    setCopied(type);
    toast.success('Copied to clipboard!');
    setTimeout(() => setCopied(''), 2000);
  };

  const getPluginIcon = () => {
    const icons = {
      google: '🔷',
      slack: '💬',
      github: '🐙',
      stripe: '💳',
      default: '🔌'
    };
    return icons[plugin.id] || icons.default;
  };

  const getPluginColor = () => {
    const colors = {
      google: 'from-blue-500 to-blue-600',
      slack: 'from-purple-500 to-purple-600',
      github: 'from-gray-700 to-gray-900',
      stripe: 'from-indigo-500 to-indigo-600',
      default: 'from-primary-500 to-primary-600'
    };
    return colors[plugin.id] || colors.default;
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="text-center space-y-3">
        <div className={`mx-auto w-20 h-20 bg-gradient-to-br ${getPluginColor()} rounded-2xl flex items-center justify-center text-3xl shadow-lg`}>
          {getPluginIcon()}
        </div>
        <h3 className="text-2xl font-bold text-gray-900">
          Set Up {plugin.name} OAuth
        </h3>
        <p className="text-gray-600 max-w-md mx-auto">
          To connect {plugin.name}, you'll need to create OAuth credentials in their developer console
        </p>
      </div>

      {/* Existing Credentials Alert */}
      {existingCredentials?.isConfigured && (
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
          <div className="flex items-start space-x-3">
            <Shield className="h-5 w-5 text-blue-600 mt-0.5" />
            <div className="flex-1">
              <h4 className="font-medium text-blue-900">Credentials Already Configured</h4>
              <p className="text-sm text-blue-700 mt-1">
                You have existing OAuth credentials for {plugin.name}. 
                Enter new credentials below to update them.
              </p>
            </div>
          </div>
        </div>
      )}

      {/* Setup Instructions */}
      {instructions && (
        <div className="border border-gray-200 rounded-lg overflow-hidden">
          <button
            onClick={() => setShowInstructions(!showInstructions)}
            className="w-full px-4 py-3 bg-gray-50 hover:bg-gray-100 transition-colors flex items-center justify-between"
          >
            <div className="flex items-center space-x-2">
              <BookOpen className="h-5 w-5 text-gray-600" />
              <span className="font-medium text-gray-900">Setup Instructions</span>
            </div>
            {showInstructions ? (
              <ChevronUp className="h-5 w-5 text-gray-600" />
            ) : (
              <ChevronDown className="h-5 w-5 text-gray-600" />
            )}
          </button>

          <AnimatePresence>
            {showInstructions && (
              <motion.div
                initial={{ height: 0, opacity: 0 }}
                animate={{ height: 'auto', opacity: 1 }}
                exit={{ height: 0, opacity: 0 }}
                className="border-t border-gray-200"
              >
                <div className="p-4 space-y-4">
                  {/* Step-by-step instructions */}
                  <ol className="space-y-3">
                    {instructions.instructions?.map((step, index) => (
                      <li key={index} className="flex space-x-3">
                        <span className="flex-shrink-0 w-7 h-7 bg-primary-100 text-primary-700 rounded-full flex items-center justify-center text-sm font-medium">
                          {index + 1}
                        </span>
                        <span className="text-sm text-gray-700">{step}</span>
                      </li>
                    ))}
                  </ol>

                  {/* Important Values to Copy */}
                  <div className="bg-amber-50 border border-amber-200 rounded-lg p-3">
                    <h5 className="font-medium text-amber-900 mb-2 flex items-center">
                      <Info className="h-4 w-4 mr-2" />
                      Important Values
                    </h5>
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-gray-700">Redirect URI:</span>
                        <div className="flex items-center space-x-2">
                          <code className="text-xs bg-white px-2 py-1 rounded border border-amber-200">
                            {window.location.origin}/plugins
                          </code>
                          <button
                            onClick={() => handleCopy(`${window.location.origin}/plugins`, 'redirect')}
                            className="p-1 hover:bg-amber-100 rounded transition-colors"
                          >
                            {copied === 'redirect' ? (
                              <CheckCircle className="h-4 w-4 text-green-600" />
                            ) : (
                              <Copy className="h-4 w-4 text-amber-700" />
                            )}
                          </button>
                        </div>
                      </div>
                    </div>
                  </div>

                  {/* External Links */}
                  <div className="flex flex-wrap gap-2">
                    {instructions.docLink && (
                      <a
                        href={instructions.docLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center space-x-1 text-sm text-primary-600 hover:text-primary-700"
                      >
                        <BookOpen className="h-4 w-4" />
                        <span>Documentation</span>
                        <ExternalLink className="h-3 w-3" />
                      </a>
                    )}
                    {instructions.videoTutorial && (
                      <a
                        href={instructions.videoTutorial}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center space-x-1 text-sm text-primary-600 hover:text-primary-700"
                      >
                        <Video className="h-4 w-4" />
                        <span>Video Tutorial</span>
                        <ExternalLink className="h-3 w-3" />
                      </a>
                    )}
                    {plugin.id === 'google' && (
                      <a
                        href="https://console.cloud.google.com"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center space-x-1 text-sm text-primary-600 hover:text-primary-700"
                      >
                        <span>Google Cloud Console</span>
                        <ExternalLink className="h-3 w-3" />
                      </a>
                    )}
                    {plugin.id === 'slack' && (
                      <a
                        href="https://api.slack.com/apps"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center space-x-1 text-sm text-primary-600 hover:text-primary-700"
                      >
                        <span>Slack Apps</span>
                        <ExternalLink className="h-3 w-3" />
                      </a>
                    )}
                    {plugin.id === 'github' && (
                      <a
                        href="https://github.com/settings/developers"
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center space-x-1 text-sm text-primary-600 hover:text-primary-700"
                      >
                        <span>GitHub OAuth Apps</span>
                        <ExternalLink className="h-3 w-3" />
                      </a>
                    )}
                  </div>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      )}

      {/* Credential Input Form */}
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Client ID
            <span className="text-red-500 ml-1">*</span>
          </label>
          <div className="relative">
            <input
              type="text"
              value={credentials.clientId}
              onChange={(e) => setCredentials({ ...credentials, clientId: e.target.value })}
              className="w-full px-4 py-2 pr-10 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              placeholder={plugin.id === 'google' ? 'xxxxx.apps.googleusercontent.com' : 'Your Client ID'}
            />
            <Key className="absolute right-3 top-2.5 h-5 w-5 text-gray-400" />
          </div>
          <p className="text-xs text-gray-500 mt-1">
            This is public and safe to share
          </p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Client Secret
            {plugin.id !== 'github' && <span className="text-gray-500 text-xs ml-2">(Optional for some providers)</span>}
          </label>
          <div className="relative">
            <input
              type={showSecret ? 'text' : 'password'}
              value={credentials.clientSecret}
              onChange={(e) => setCredentials({ ...credentials, clientSecret: e.target.value })}
              className="w-full px-4 py-2 pr-20 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
              placeholder="Your Client Secret"
            />
            <div className="absolute right-3 top-2.5 flex items-center space-x-2">
              <button
                type="button"
                onClick={() => setShowSecret(!showSecret)}
                className="text-gray-400 hover:text-gray-600"
              >
                {showSecret ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
              </button>
              <Shield className="h-5 w-5 text-gray-400" />
            </div>
          </div>
          <p className="text-xs text-gray-500 mt-1">
            Keep this secret and never share it publicly
          </p>
        </div>
      </div>

      {/* Security Notice */}
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-4">
        <div className="flex items-start space-x-3">
          <Shield className="h-5 w-5 text-gray-600 mt-0.5" />
          <div>
            <h4 className="font-medium text-gray-900">Your credentials are secure</h4>
            <p className="text-sm text-gray-600 mt-1">
              OAuth credentials are encrypted and stored securely. They are only used to authenticate 
              with {plugin.name} on your behalf and are never shared with third parties.
            </p>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      <div className="flex space-x-3">
        <button
          onClick={onSkip}
          className="flex-1 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
        >
          Skip for Now
        </button>
        <button
          onClick={handleSaveCredentials}
          disabled={loading || !credentials.clientId}
          className="flex-1 btn-primary flex items-center justify-center space-x-2"
        >
          {loading ? (
            <>
              <Loader className="h-5 w-5 animate-spin" />
              <span>Saving...</span>
            </>
          ) : (
            <>
              <CheckCircle className="h-5 w-5" />
              <span>Save Credentials</span>
            </>
          )}
        </button>
      </div>

      {/* Help Link */}
      <div className="text-center">
        <button className="inline-flex items-center space-x-1 text-sm text-gray-500 hover:text-gray-700">
          <HelpCircle className="h-4 w-4" />
          <span>Need help setting up OAuth?</span>
        </button>
      </div>
    </div>
  );
};

export default OAuthCredentialSetup;