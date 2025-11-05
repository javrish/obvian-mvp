import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Dashboard from './components/Dashboard';
import Templates from './components/Templates';
import Plugins from './components/Plugins';
import Executions from './components/Executions';
import Settings from './components/Settings';
import PetriNetWorkflow from './components/PetriNetWorkflow';
import Layout from './components/Layout';
import Login from './components/Login';
import GoogleCallback from './components/GoogleCallback';
import GoogleAuthHandler from './components/GoogleAuthHandler';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user is authenticated
    const token = localStorage.getItem('token');
    const googleSession = localStorage.getItem('google_session');
    setIsAuthenticated(!!(token || googleSession));
    setLoading(false);
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  return (
    <Router>
      <Toaster position="top-right" />
      <Routes>
        <Route path="/login" element={
          isAuthenticated ? <Navigate to="/" /> : <Login setIsAuthenticated={setIsAuthenticated} />
        } />
        
        <Route path="/auth/google/callback" element={
          <GoogleCallback setIsAuthenticated={setIsAuthenticated} />
        } />
        
        <Route path="/auth/success" element={<GoogleAuthHandler />} />
        <Route path="/auth/failed" element={<GoogleAuthHandler />} />
        
        <Route path="/" element={
          isAuthenticated ? <Layout /> : <Navigate to="/login" />
        }>
          <Route index element={<Dashboard />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="templates" element={<Templates />} />
          <Route path="plugins" element={<Plugins />} />
          <Route path="executions" element={<Executions />} />
          <Route path="petri-nets" element={<PetriNetWorkflow />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;