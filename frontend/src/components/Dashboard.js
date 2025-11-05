import React, { useState, useEffect } from 'react';
import { 
  Sparkles, 
  TrendingUp,
  Heart,
  Brain,
  Target,
  Zap,
  Coffee,
  Moon,
  Sun,
  Calendar,
  Award,
  Rocket,
  Users,
  BookOpen,
  Briefcase
} from 'lucide-react';
import { Link } from 'react-router-dom';
import apiService from '../services/api';
import toast from 'react-hot-toast';
import { motion } from 'framer-motion';

const Dashboard = () => {
  const [stats, setStats] = useState({
    dailyProgress: 0,
    weeklyGoals: 0,
    activeWorkflows: 0,
    productivityScore: 0,
    wellnessScore: 0,
    creativityBoost: 0
  });
  const [timeOfDay, setTimeOfDay] = useState('morning');
  const [userName, setUserName] = useState('');
  const [dailyFocus, setDailyFocus] = useState('');
  const [executionHistory, setExecutionHistory] = useState([]);
  const [activePlugins, setActivePlugins] = useState([]);
  const [consciousnessMode, setConsciousnessMode] = useState('OFF');
  const [apiInfo, setApiInfo] = useState(null);
  const [realTemplates, setRealTemplates] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDashboardData();
    determineTimeOfDay();
    generateDailyFocus();
  }, []);

  const loadDashboardData = async () => {
    try {
      setLoading(true);
      
      // Get API info
      const info = await apiService.getApiInfo();
      setApiInfo(info);

      // Get real data from backend
      const [templates, plugins, executions, settings] = await Promise.all([
        apiService.getTemplates().catch(() => ({ templates: [] })),
        apiService.getPlugins().catch(() => ({ plugins: [] })),
        apiService.getExecutions().catch(() => ({ executions: [] })),
        apiService.getSettings().catch(() => ({ consciousnessMode: 'OFF' }))
      ]);
      
      // Store real templates
      setRealTemplates(templates.templates || []);
      setActivePlugins(plugins.plugins || []);
      setExecutionHistory(executions.executions || []);
      setConsciousnessMode(settings.consciousnessMode || 'OFF');
      
      // Get user info
      const userInfo = await apiService.getUserProfile().catch(() => ({ name: 'User' }));
      setUserName(userInfo.name || 'User');
      
      // Calculate real stats based on actual data
      const completedExecutions = (executions.executions || []).filter(e => e.status === 'completed').length;
      const totalExecutions = (executions.executions || []).length;
      const successRate = totalExecutions > 0 ? Math.round((completedExecutions / totalExecutions) * 100) : 0;
      
      // Update stats with real data
      setStats({
        dailyProgress: successRate,
        weeklyGoals: templates.templates?.length || 0,
        activeWorkflows: (executions.executions || []).filter(e => e.status === 'running').length,
        productivityScore: successRate,
        wellnessScore: settings.consciousnessMode === 'FULL' ? 90 : 
                       settings.consciousnessMode === 'ENHANCED' ? 70 : 
                       settings.consciousnessMode === 'BASIC' ? 50 : 30,
        creativityBoost: (plugins.plugins || []).filter(p => p.enabled).length * 10
      });

    } catch (error) {
      console.error('Failed to load dashboard data:', error);
      toast.error('Failed to load dashboard data');
    } finally {
      setLoading(false);
    }
  };

  const determineTimeOfDay = () => {
    const hour = new Date().getHours();
    if (hour >= 5 && hour < 12) setTimeOfDay('morning');
    else if (hour >= 12 && hour < 17) setTimeOfDay('afternoon');
    else if (hour >= 17 && hour < 21) setTimeOfDay('evening');
    else setTimeOfDay('night');
  };

  const generateDailyFocus = () => {
    const focuses = [
      "🎯 Launch your next big idea",
      "📈 Scale your business effortlessly",
      "🧘 Balance work and wellness",
      "🚀 Automate repetitive tasks",
      "💡 Innovate and create",
      "🌟 Achieve your dreams"
    ];
    setDailyFocus(focuses[Math.floor(Math.random() * focuses.length)]);
  };

  const getGreeting = () => {
    const greetings = {
      morning: "Good morning",
      afternoon: "Good afternoon",
      evening: "Good evening",
      night: "Good night"
    };
    return greetings[timeOfDay];
  };

  const getTimeIcon = () => {
    const icons = {
      morning: <Sun className="h-6 w-6 text-yellow-500" />,
      afternoon: <Sun className="h-6 w-6 text-orange-500" />,
      evening: <Moon className="h-6 w-6 text-purple-500" />,
      night: <Moon className="h-6 w-6 text-indigo-600" />
    };
    return icons[timeOfDay];
  };

  const lifestyleCards = [
    {
      title: "Work Flow",
      subtitle: "Automate your business",
      icon: Briefcase,
      color: "from-blue-500 to-purple-600",
      bgColor: "from-blue-50 to-purple-50",
      value: `${stats.activeWorkflows} active`,
      detail: `${realTemplates.length} templates`,
      link: "/templates",
      emoji: "💼"
    },
    {
      title: "Life Balance",
      subtitle: "Wellness & productivity",
      icon: Heart,
      color: "from-pink-500 to-red-500",
      bgColor: "from-pink-50 to-red-50",
      value: `${stats.wellnessScore}% balanced`,
      link: "/settings",
      emoji: "❤️"
    },
    {
      title: "AI Assistant",
      subtitle: "Your smart companion",
      icon: Brain,
      color: "from-purple-500 to-indigo-600",
      bgColor: "from-purple-50 to-indigo-50",
      value: consciousnessMode + " mode",
      link: "/settings",
      emoji: "🧠"
    },
    {
      title: "Growth Tracker",
      subtitle: "Monitor your progress",
      icon: TrendingUp,
      color: "from-green-500 to-teal-600",
      bgColor: "from-green-50 to-teal-50",
      value: `${stats.productivityScore}% success`,
      detail: `${executionHistory.length} total runs`,
      link: "/executions",
      emoji: "📈"
    }
  ];

  const quickActions = [
    {
      title: "Morning Routine",
      description: "Start your day right",
      icon: Coffee,
      gradient: "from-yellow-400 to-orange-500",
      action: "morning-routine",
      emoji: "☕"
    },
    {
      title: "Deep Work",
      description: "Focus mode activated",
      icon: Target,
      gradient: "from-blue-500 to-indigo-600",
      action: "deep-work",
      emoji: "🎯"
    },
    {
      title: "Creative Sprint",
      description: "Unleash creativity",
      icon: Sparkles,
      gradient: "from-purple-500 to-pink-500",
      action: "creative-sprint",
      emoji: "✨"
    },
    {
      title: "Network & Grow",
      description: "Connect with others",
      icon: Users,
      gradient: "from-green-500 to-teal-500",
      action: "network",
      emoji: "🤝"
    }
  ];

  // Map real execution history to tasks
  const todaysTasks = executionHistory.slice(0, 4).map((exec, idx) => ({
    time: new Date(exec.startTime || Date.now()).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }),
    task: exec.name || exec.templateId || `Execution ${exec.id}`,
    status: exec.status === 'SUCCESS' ? 'completed' : 
            exec.status === 'RUNNING' ? 'in-progress' : 
            exec.status === 'FAILED' ? 'failed' : 'upcoming',
    emoji: exec.status === 'SUCCESS' ? '✅' : 
           exec.status === 'RUNNING' ? '⚡' : 
           exec.status === 'FAILED' ? '❌' : '⏰'
  })).concat(
    // Add placeholder tasks if we have less than 4 executions
    Array(Math.max(0, 4 - executionHistory.length)).fill(null).map((_, idx) => ({
      time: '--:-- --',
      task: 'No execution yet',
      status: 'upcoming',
      emoji: '📋'
    }))
  );

  if (loading) {
    return (
      <div className="flex items-center justify-center h-screen">
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
        >
          <Sparkles className="h-12 w-12 text-purple-600" />
        </motion.div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 to-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6 md:space-y-8">
      {/* Welcome Section with Lifestyle Focus */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-3xl bg-gradient-to-r from-purple-600 via-pink-600 to-orange-500 p-6 md:p-8 lg:p-10 text-white shadow-2xl"
      >
        <div className="relative z-10">
          <div className="flex items-center justify-between mb-6">
            <div>
              <div className="flex items-center space-x-3 mb-2">
                {getTimeIcon()}
                <h1 className="text-3xl font-bold">
                  {getGreeting()}, {userName}! ✨
                </h1>
              </div>
              <p className="text-xl text-white/90 mb-4">
                Ready to make today extraordinary?
              </p>
              <div className="flex items-center space-x-2 bg-white/20 backdrop-blur rounded-full px-4 py-2 inline-flex">
                <Sparkles className="h-5 w-5" />
                <span className="font-medium">Today's Focus: {dailyFocus}</span>
              </div>
            </div>
            <div className="text-right">
              <div className="text-5xl mb-2 emoji-bounce">🚀</div>
              <div className="text-2xl font-bold">{stats.dailyProgress}%</div>
              <div className="text-sm text-white/80">Daily Progress</div>
            </div>
          </div>
        </div>
        
        {/* Animated background elements */}
        <div className="absolute top-0 right-0 w-96 h-96 bg-white/10 rounded-full blur-3xl -mr-48 -mt-48 float-animation"></div>
        <div className="absolute bottom-0 left-0 w-64 h-64 bg-white/10 rounded-full blur-2xl -ml-32 -mb-32"></div>
      </motion.div>

      {/* Lifestyle Metrics Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 md:gap-5 lg:gap-6">
        {lifestyleCards.map((card, idx) => {
          const Icon = card.icon;
          return (
            <motion.div
              key={idx}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
            >
              <Link to={card.link}>
                <div className={`card-lifestyle lifestyle-card group cursor-pointer transform transition-all duration-300 hover:scale-105 hover:-translate-y-1`}>
                  <div className={`absolute inset-0 bg-gradient-to-br ${card.bgColor} opacity-50 rounded-3xl transition-opacity duration-300 group-hover:opacity-70`}></div>
                  <div className="relative">
                    <div className="flex items-start justify-between mb-4">
                      <motion.div 
                        whileHover={{ rotate: 5 }}
                        className={`p-3 bg-gradient-to-r ${card.color} rounded-2xl text-white shadow-lg transition-all duration-300`}
                      >
                        <Icon className="h-6 w-6" />
                      </motion.div>
                      <motion.span 
                        animate={{ scale: [1, 1.1, 1] }}
                        transition={{ duration: 2, repeat: Infinity }}
                        className="text-3xl"
                      >
                        {card.emoji}
                      </motion.span>
                    </div>
                    <h3 className="text-lg font-semibold text-gray-900 mb-1">{card.title}</h3>
                    <p className="text-sm text-gray-600 mb-3">{card.subtitle}</p>
                    <div className="text-2xl font-bold gradient-text">{card.value}</div>
                    {card.detail && <div className="text-xs text-gray-500 mt-1">{card.detail}</div>}
                  </div>
                </div>
              </Link>
            </motion.div>
          );
        })}
      </div>

      {/* Quick Actions - Lifestyle Focused */}
      <div>
        <h2 className="text-2xl font-bold mb-4 md:mb-6 gradient-text">Quick Life Actions</h2>
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 md:gap-4">
          {quickActions.map((action, idx) => {
            const Icon = action.icon;
            return (
              <motion.button
                key={idx}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="p-4 md:p-5 lg:p-6 rounded-2xl bg-white shadow-md hover:shadow-xl transition-all duration-300 group"
                onClick={() => toast.success(`Starting ${action.title}...`)}
              >
                <div className={`w-14 h-14 bg-gradient-to-r ${action.gradient} rounded-xl flex items-center justify-center text-white mb-4 group-hover:scale-110 transition-transform`}>
                  <Icon className="h-7 w-7" />
                </div>
                <div className="text-left">
                  <h3 className="font-semibold text-gray-900 mb-1">{action.title}</h3>
                  <p className="text-sm text-gray-600">{action.description}</p>
                </div>
              </motion.button>
            );
          })}
        </div>
      </div>

      {/* Today's Journey Timeline */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4 md:gap-6">
        <div className="card-lifestyle">
          <h3 className="text-xl font-bold mb-6 gradient-text">Today's Journey</h3>
          <div className="space-y-4">
            {todaysTasks.map((task, idx) => (
              <motion.div
                key={idx}
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: idx * 0.1 }}
                className="flex items-center space-x-4 p-4 rounded-xl bg-gradient-to-r from-purple-50 to-pink-50 hover:from-purple-100 hover:to-pink-100 transition-colors"
              >
                <span className="text-2xl">{task.emoji}</span>
                <div className="flex-1">
                  <div className="font-medium text-gray-900">{task.task}</div>
                  <div className="text-sm text-gray-600">{task.time}</div>
                </div>
                <div className={`px-3 py-1 rounded-full text-xs font-medium ${
                  task.status === 'completed' ? 'bg-green-100 text-green-700' :
                  task.status === 'in-progress' ? 'bg-blue-100 text-blue-700' :
                  'bg-gray-100 text-gray-600'
                }`}>
                  {task.status}
                </div>
              </motion.div>
            ))}
          </div>
        </div>

        {/* Inspiration & Motivation */}
        <div className="card-lifestyle">
          <h3 className="text-xl font-bold mb-6 gradient-text">Daily Inspiration</h3>
          <div className="space-y-6">
            <div className="p-6 rounded-2xl wellness-gradient text-white">
              <Award className="h-8 w-8 mb-3" />
              <h4 className="text-lg font-semibold mb-2">Achievement Unlocked!</h4>
              <p className="text-white/90">You've run {executionHistory.length} workflows! 🎉</p>
            </div>
            
            <div className="p-6 rounded-2xl bg-gradient-to-r from-orange-100 to-yellow-100">
              <div className="flex items-start space-x-3">
                <span className="text-3xl">💡</span>
                <div>
                  <h4 className="font-semibold text-gray-900 mb-1">Pro Tip</h4>
                  <p className="text-sm text-gray-700">
                    {realTemplates.length > 0 
                      ? `You have ${realTemplates.length} templates available. Try one to boost productivity!`
                      : 'Create your first template to start automating your workflow!'}
                  </p>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between p-4 rounded-xl bg-gradient-to-r from-blue-50 to-indigo-50">
              <div className="flex items-center space-x-3">
                <Rocket className="h-6 w-6 text-blue-600" />
                <span className="font-medium text-gray-900">Ready to launch?</span>
              </div>
              <Link to="/templates" className="btn-lifestyle text-sm px-4 py-2">
                Explore Workflows
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Wellness & Productivity Balance */}
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.5 }}
        className="card-lifestyle"
      >
        <h3 className="text-xl font-bold mb-6 gradient-text">Life Balance Meter</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="text-center">
            <div className="relative w-32 h-32 mx-auto mb-4">
              <svg className="transform -rotate-90 w-32 h-32">
                <circle cx="64" cy="64" r="56" stroke="#e5e7eb" strokeWidth="12" fill="none" />
                <circle
                  cx="64" cy="64" r="56"
                  stroke="url(#productivityGradient)"
                  strokeWidth="12"
                  fill="none"
                  strokeDasharray={`${stats.productivityScore * 3.52} 352`}
                  strokeLinecap="round"
                />
                <defs>
                  <linearGradient id="productivityGradient">
                    <stop offset="0%" stopColor="#f093fb" />
                    <stop offset="100%" stopColor="#f5576c" />
                  </linearGradient>
                </defs>
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-2xl font-bold">{stats.productivityScore}%</span>
              </div>
            </div>
            <h4 className="font-semibold text-gray-900">Productivity</h4>
            <p className="text-sm text-gray-600">You're on fire today! 🔥</p>
          </div>

          <div className="text-center">
            <div className="relative w-32 h-32 mx-auto mb-4">
              <svg className="transform -rotate-90 w-32 h-32">
                <circle cx="64" cy="64" r="56" stroke="#e5e7eb" strokeWidth="12" fill="none" />
                <circle
                  cx="64" cy="64" r="56"
                  stroke="url(#wellnessGradient)"
                  strokeWidth="12"
                  fill="none"
                  strokeDasharray={`${stats.wellnessScore * 3.52} 352`}
                  strokeLinecap="round"
                />
                <defs>
                  <linearGradient id="wellnessGradient">
                    <stop offset="0%" stopColor="#667eea" />
                    <stop offset="100%" stopColor="#764ba2" />
                  </linearGradient>
                </defs>
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-2xl font-bold">{stats.wellnessScore}%</span>
              </div>
            </div>
            <h4 className="font-semibold text-gray-900">Wellness</h4>
            <p className="text-sm text-gray-600">Great balance! 🧘</p>
          </div>

          <div className="text-center">
            <div className="relative w-32 h-32 mx-auto mb-4">
              <svg className="transform -rotate-90 w-32 h-32">
                <circle cx="64" cy="64" r="56" stroke="#e5e7eb" strokeWidth="12" fill="none" />
                <circle
                  cx="64" cy="64" r="56"
                  stroke="url(#creativityGradient)"
                  strokeWidth="12"
                  fill="none"
                  strokeDasharray={`${stats.creativityBoost * 3.52} 352`}
                  strokeLinecap="round"
                />
                <defs>
                  <linearGradient id="creativityGradient">
                    <stop offset="0%" stopColor="#fa709a" />
                    <stop offset="100%" stopColor="#fee140" />
                  </linearGradient>
                </defs>
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className="text-2xl font-bold">{stats.creativityBoost}%</span>
              </div>
            </div>
            <h4 className="font-semibold text-gray-900">Creativity</h4>
            <p className="text-sm text-gray-600">Inspiration flowing! ✨</p>
          </div>
        </div>
      </motion.div>
      </div>
    </div>
  );
};

export default Dashboard;