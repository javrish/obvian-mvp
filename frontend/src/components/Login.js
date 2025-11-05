import React, { useState, useEffect } from 'react';
import { Brain, ArrowRight, Chrome, Shield, Sparkles, CheckCircle } from 'lucide-react';
import apiService from '../services/api';
import toast from 'react-hot-toast';

const Login = ({ setIsAuthenticated }) => {
  const [credentials, setCredentials] = useState({
    username: 'demo@obvian.io',
    password: 'demo123'
  });
  const [loading, setLoading] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);

  useEffect(() => {
    // Check if returning from Google OAuth
    const params = new URLSearchParams(window.location.search);
    const googleAuth = params.get('google_auth');
    const services = params.get('services');
    
    if (googleAuth === 'success' && services) {
      // User successfully authenticated with Google
      const serviceList = services.split(',');
      toast.success(`Welcome! Connected to ${serviceList.length} Google services`, {
        duration: 5000,
        icon: '🎉'
      });
      
      // Auto-login the user
      const token = params.get('token') || 'google-auth-token-' + Date.now();
      localStorage.setItem('token', token);
      setIsAuthenticated(true);
      
      // Show connected services
      serviceList.forEach((service, index) => {
        setTimeout(() => {
          toast.success(`✅ ${service} connected`, {
            duration: 2000,
            position: 'top-right'
          });
        }, index * 300);
      });
      
      // Clean URL
      window.history.replaceState({}, document.title, window.location.pathname);
    }
  }, [setIsAuthenticated]);

  const handleGoogleLogin = async () => {
    setGoogleLoading(true);
    
    try {
      // Get OAuth URL from the server
      const currentPort = window.location.port || '3001';
      const redirectUri = `http://localhost:${currentPort}/auth/google/callback`;
      const response = await fetch(`http://localhost:8080/api/auth/google/login?redirect_uri=${encodeURIComponent(redirectUri)}`);
      
      if (!response.ok) {
        throw new Error('Failed to get OAuth URL');
      }
      
      const data = await response.json();
      
      if (data.authUrl) {
        // Store state for security
        sessionStorage.setItem('oauth_state', data.state);
        // Redirect to the OAuth URL
        window.location.href = data.authUrl;
      } else {
        throw new Error('No auth URL in response');
      }
    } catch (error) {
      console.error('Google login error:', error);
      toast.error('Failed to initiate Google login. Please try again.');
      setGoogleLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      const response = await apiService.login(credentials);
      if (response.success) {
        localStorage.setItem('token', response.token);
        setIsAuthenticated(true);
        toast.success('Welcome to Obvian Labs!');
      }
    } catch (error) {
      toast.error('Login failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleDemoLogin = () => {
    setCredentials({
      username: 'demo@obvian.io',
      password: 'demo123'
    });
    handleSubmit(new Event('submit'));
  };

  return (
    <main className="min-h-screen bg-gradient-to-br from-primary-50 to-primary-100 flex items-center justify-center p-4">
      <div className="max-w-md w-full">
        <div className="bg-white rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <div className="flex justify-center mb-4">
              <div className="p-3 bg-primary-100 rounded-xl">
                <Brain className="h-12 w-12 text-primary-600" />
              </div>
            </div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">Obvian Labs</h1>
            <p className="text-gray-600">AI-Native Operating System for Solopreneurs</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            <div>
              <label htmlFor="email-input" className="block text-sm font-medium text-gray-700 mb-2">
                Email
              </label>
              <input
                id="email-input"
                type="email"
                value={credentials.username}
                onChange={(e) => setCredentials({ ...credentials, username: e.target.value })}
                className="input"
                placeholder="demo@obvian.io"
                aria-label="Email address"
                required
              />
            </div>

            <div>
              <label htmlFor="password-input" className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <input
                id="password-input"
                type="password"
                value={credentials.password}
                onChange={(e) => setCredentials({ ...credentials, password: e.target.value })}
                className="input"
                placeholder="••••••••"
                aria-label="Password"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full btn-primary flex items-center justify-center space-x-2"
            >
              {loading ? (
                <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-white"></div>
              ) : (
                <>
                  <span>Sign In</span>
                  <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </form>

          {/* Divider */}
          <div className="my-6 relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300"></div>
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-4 bg-white text-gray-500">Or continue with</span>
            </div>
          </div>

          {/* Google Sign In Button */}
          <button
            onClick={handleGoogleLogin}
            disabled={googleLoading}
            className="w-full flex items-center justify-center space-x-3 px-4 py-3 border border-gray-300 rounded-lg hover:bg-gray-50 transition-all hover:shadow-md group"
          >
            {googleLoading ? (
              <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-gray-600"></div>
            ) : (
              <>
                <svg className="w-5 h-5" viewBox="0 0 24 24">
                  <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                  <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                  <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                  <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                </svg>
                <span className="font-medium text-gray-700">Sign in with Google</span>
                <Sparkles className="h-4 w-4 text-yellow-500 opacity-0 group-hover:opacity-100 transition-opacity" />
              </>
            )}
          </button>

          {/* Google Benefits - Smaller, inline text */}
          <div className="mt-2 text-center">
            <p className="text-xs text-gray-500">
              <Sparkles className="inline h-3 w-3 text-yellow-500 mr-1" />
              One login connects Calendar, Drive, Gmail, Sheets & Docs
            </p>
          </div>

          <div className="mt-6 text-center">
            <button
              onClick={handleDemoLogin}
              className="text-primary-600 hover:text-primary-700 font-medium text-sm"
            >
              Use Demo Account Instead
            </button>
          </div>
        </div>

        <div className="mt-8 text-center text-sm text-gray-600">
          <p>Experience the power of AI-driven workflow automation</p>
        </div>
      </div>
    </main>
  );
};

export default Login;