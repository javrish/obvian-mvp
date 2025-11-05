// Demo templates for solopreneurs and businesses
export const demoTemplates = [
  // Business Operations
  {
    id: 'weekly-report',
    name: 'Weekly Business Report',
    description: 'Automatically compile and send weekly business reports with key metrics',
    category: 'Business Operations',
    icon: '📊',
    difficulty: 'beginner',
    estimatedTime: '2 mins',
    nodeCount: 4,
    tags: ['reporting', 'analytics', 'email'],
    usageCount: 1247,
    rating: 4.8,
    parameters: [
      { name: 'recipient_email', type: 'email', required: true, description: 'Email to send report to' },
      { name: 'include_charts', type: 'boolean', defaultValue: true, description: 'Include visual charts' },
      { name: 'report_type', type: 'select', options: ['summary', 'detailed', 'executive'], defaultValue: 'summary' }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Fetch sales data', plugin: 'database', icon: '📈' },
        { id: 2, name: 'Generate insights', plugin: 'ai', icon: '🤖' },
        { id: 3, name: 'Create report', plugin: 'document', icon: '📄' },
        { id: 4, name: 'Email report', plugin: 'email', icon: '📧' }
      ]
    }
  },
  {
    id: 'invoice-automation',
    name: 'Invoice Generation & Sending',
    description: 'Generate professional invoices and send them automatically to clients',
    category: 'Business Operations',
    icon: '💰',
    difficulty: 'intermediate',
    estimatedTime: '3 mins',
    nodeCount: 5,
    tags: ['finance', 'billing', 'automation'],
    usageCount: 892,
    rating: 4.9,
    parameters: [
      { name: 'client_email', type: 'email', required: true },
      { name: 'amount', type: 'number', required: true },
      { name: 'due_days', type: 'number', defaultValue: 30 },
      { name: 'include_logo', type: 'boolean', defaultValue: true }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Load client data', plugin: 'database', icon: '👤' },
        { id: 2, name: 'Calculate totals', plugin: 'calculation', icon: '🧮' },
        { id: 3, name: 'Generate PDF', plugin: 'document', icon: '📑' },
        { id: 4, name: 'Send invoice', plugin: 'email', icon: '📬' },
        { id: 5, name: 'Update records', plugin: 'database', icon: '💾' }
      ]
    }
  },

  // Marketing & Growth
  {
    id: 'social-scheduler',
    name: 'Social Media Content Scheduler',
    description: 'Post content across multiple social platforms at optimal times',
    category: 'Marketing',
    icon: '📱',
    difficulty: 'beginner',
    estimatedTime: '1 min',
    nodeCount: 3,
    tags: ['social', 'marketing', 'content'],
    usageCount: 2156,
    rating: 4.7,
    parameters: [
      { name: 'content', type: 'text', required: true, description: 'Content to post' },
      { name: 'platforms', type: 'multiselect', options: ['twitter', 'linkedin', 'facebook'], required: true },
      { name: 'schedule_time', type: 'datetime', required: true }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Optimize content', plugin: 'ai', icon: '✨' },
        { id: 2, name: 'Schedule posts', plugin: 'social', icon: '⏰' },
        { id: 3, name: 'Track engagement', plugin: 'analytics', icon: '📊' }
      ]
    }
  },
  {
    id: 'email-campaign',
    name: 'Email Campaign Automation',
    description: 'Send personalized email campaigns with automated follow-ups',
    category: 'Marketing',
    icon: '📧',
    difficulty: 'intermediate',
    estimatedTime: '5 mins',
    nodeCount: 6,
    tags: ['email', 'marketing', 'automation'],
    usageCount: 1678,
    rating: 4.6,
    parameters: [
      { name: 'campaign_name', type: 'text', required: true },
      { name: 'recipient_list', type: 'file', required: true },
      { name: 'follow_up_days', type: 'number', defaultValue: 3 },
      { name: 'personalize', type: 'boolean', defaultValue: true }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Import contacts', plugin: 'file', icon: '📋' },
        { id: 2, name: 'Segment audience', plugin: 'ai', icon: '🎯' },
        { id: 3, name: 'Personalize content', plugin: 'ai', icon: '✏️' },
        { id: 4, name: 'Send emails', plugin: 'email', icon: '📤' },
        { id: 5, name: 'Schedule follow-ups', plugin: 'scheduler', icon: '🔄' },
        { id: 6, name: 'Track opens', plugin: 'analytics', icon: '👁️' }
      ]
    }
  },

  // Personal Productivity
  {
    id: 'daily-briefing',
    name: 'Daily Briefing Generator',
    description: 'Get your personalized daily briefing with weather, calendar, and priorities',
    category: 'Productivity',
    icon: '☀️',
    difficulty: 'beginner',
    estimatedTime: '30 secs',
    nodeCount: 4,
    tags: ['daily', 'personal', 'productivity'],
    usageCount: 3421,
    rating: 4.9,
    parameters: [
      { name: 'delivery_time', type: 'time', defaultValue: '08:00', required: true },
      { name: 'include_weather', type: 'boolean', defaultValue: true },
      { name: 'include_news', type: 'boolean', defaultValue: true },
      { name: 'motivational_quote', type: 'boolean', defaultValue: true }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Fetch weather', plugin: 'weather', icon: '🌤️' },
        { id: 2, name: 'Get calendar', plugin: 'calendar', icon: '📅' },
        { id: 3, name: 'Compile briefing', plugin: 'ai', icon: '📝' },
        { id: 4, name: 'Send notification', plugin: 'notification', icon: '🔔' }
      ]
    }
  },
  {
    id: 'expense-tracker',
    name: 'Expense Tracking & Categorization',
    description: 'Automatically categorize and track business expenses from receipts',
    category: 'Productivity',
    icon: '💳',
    difficulty: 'intermediate',
    estimatedTime: '2 mins',
    nodeCount: 5,
    tags: ['finance', 'tracking', 'automation'],
    usageCount: 1123,
    rating: 4.7,
    parameters: [
      { name: 'receipt_source', type: 'select', options: ['email', 'upload', 'scan'], required: true },
      { name: 'auto_categorize', type: 'boolean', defaultValue: true },
      { name: 'spreadsheet_id', type: 'text', required: true }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Extract receipt data', plugin: 'ocr', icon: '📸' },
        { id: 2, name: 'Categorize expense', plugin: 'ai', icon: '🏷️' },
        { id: 3, name: 'Validate amount', plugin: 'calculation', icon: '✅' },
        { id: 4, name: 'Update spreadsheet', plugin: 'sheets', icon: '📊' },
        { id: 5, name: 'Generate summary', plugin: 'document', icon: '📈' }
      ]
    }
  },

  // Customer Support
  {
    id: 'support-automation',
    name: 'Support Ticket Auto-Response',
    description: 'Automatically categorize and respond to common support requests',
    category: 'Support',
    icon: '🎫',
    difficulty: 'advanced',
    estimatedTime: '1 min',
    nodeCount: 5,
    tags: ['support', 'automation', 'customer'],
    usageCount: 756,
    rating: 4.5,
    parameters: [
      { name: 'ticket_source', type: 'select', options: ['email', 'form', 'chat'], required: true },
      { name: 'auto_respond', type: 'boolean', defaultValue: true },
      { name: 'escalation_threshold', type: 'select', options: ['low', 'medium', 'high'], defaultValue: 'medium' }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Receive ticket', plugin: 'webhook', icon: '📥' },
        { id: 2, name: 'Analyze sentiment', plugin: 'ai', icon: '😊' },
        { id: 3, name: 'Categorize issue', plugin: 'ai', icon: '📂' },
        { id: 4, name: 'Generate response', plugin: 'ai', icon: '💬' },
        { id: 5, name: 'Send or escalate', plugin: 'router', icon: '🚀' }
      ]
    }
  },
  {
    id: 'customer-onboarding',
    name: 'Customer Onboarding Flow',
    description: 'Automated welcome sequence for new customers with personalized setup',
    category: 'Support',
    icon: '🎉',
    difficulty: 'intermediate',
    estimatedTime: '3 mins',
    nodeCount: 7,
    tags: ['onboarding', 'customer', 'automation'],
    usageCount: 934,
    rating: 4.8,
    parameters: [
      { name: 'customer_email', type: 'email', required: true },
      { name: 'product_type', type: 'select', options: ['starter', 'pro', 'enterprise'], required: true },
      { name: 'send_welcome_gift', type: 'boolean', defaultValue: false }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Send welcome email', plugin: 'email', icon: '👋' },
        { id: 2, name: 'Create account', plugin: 'database', icon: '🔐' },
        { id: 3, name: 'Setup workspace', plugin: 'system', icon: '🏗️' },
        { id: 4, name: 'Schedule tutorial', plugin: 'calendar', icon: '📚' },
        { id: 5, name: 'Assign success manager', plugin: 'crm', icon: '👤' },
        { id: 6, name: 'Send resources', plugin: 'email', icon: '📚' },
        { id: 7, name: 'Schedule check-in', plugin: 'scheduler', icon: '📞' }
      ]
    }
  },

  // Data & Analytics
  {
    id: 'website-monitor',
    name: 'Website Performance Monitor',
    description: 'Monitor website uptime, speed, and SEO metrics with alerts',
    category: 'Analytics',
    icon: '🌐',
    difficulty: 'beginner',
    estimatedTime: '1 min',
    nodeCount: 4,
    tags: ['monitoring', 'website', 'analytics'],
    usageCount: 1876,
    rating: 4.6,
    parameters: [
      { name: 'website_url', type: 'url', required: true },
      { name: 'check_frequency', type: 'select', options: ['5min', '15min', '30min', '1hour'], defaultValue: '15min' },
      { name: 'alert_threshold', type: 'number', defaultValue: 3, description: 'Seconds before alert' }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Check uptime', plugin: 'monitor', icon: '🟢' },
        { id: 2, name: 'Measure speed', plugin: 'performance', icon: '⚡' },
        { id: 3, name: 'Analyze SEO', plugin: 'seo', icon: '🔍' },
        { id: 4, name: 'Send alerts', plugin: 'notification', icon: '🚨' }
      ]
    }
  },
  {
    id: 'data-backup',
    name: 'Automated Data Backup',
    description: 'Backup important data to cloud storage on schedule',
    category: 'Analytics',
    icon: '💾',
    difficulty: 'intermediate',
    estimatedTime: '5 mins',
    nodeCount: 5,
    tags: ['backup', 'data', 'security'],
    usageCount: 2341,
    rating: 4.9,
    parameters: [
      { name: 'backup_source', type: 'text', required: true, description: 'Path or database to backup' },
      { name: 'destination', type: 'select', options: ['s3', 'dropbox', 'gdrive'], required: true },
      { name: 'schedule', type: 'select', options: ['daily', 'weekly', 'monthly'], defaultValue: 'daily' },
      { name: 'encrypt', type: 'boolean', defaultValue: true }
    ],
    workflow: {
      steps: [
        { id: 1, name: 'Collect data', plugin: 'file', icon: '📁' },
        { id: 2, name: 'Compress files', plugin: 'compression', icon: '🗜️' },
        { id: 3, name: 'Encrypt backup', plugin: 'security', icon: '🔒' },
        { id: 4, name: 'Upload to cloud', plugin: 'storage', icon: '☁️' },
        { id: 5, name: 'Verify backup', plugin: 'validation', icon: '✅' }
      ]
    }
  }
];

// Template categories with metadata
export const templateCategories = [
  { id: 'all', name: 'All Templates', icon: '🎯', count: demoTemplates.length },
  { id: 'business-operations', name: 'Business Operations', icon: '💼', count: 2 },
  { id: 'marketing', name: 'Marketing', icon: '📣', count: 2 },
  { id: 'productivity', name: 'Productivity', icon: '⚡', count: 2 },
  { id: 'support', name: 'Support', icon: '🤝', count: 2 },
  { id: 'analytics', name: 'Analytics', icon: '📊', count: 2 }
];

// Get templates by category
export const getTemplatesByCategory = (category) => {
  if (category === 'all' || !category) {
    return demoTemplates;
  }
  return demoTemplates.filter(t => 
    t.category.toLowerCase().replace(/\s+/g, '-') === category
  );
};

// Get template by ID
export const getTemplateById = (id) => {
  return demoTemplates.find(t => t.id === id);
};

// Search templates
export const searchTemplates = (query) => {
  const searchTerm = query.toLowerCase();
  return demoTemplates.filter(t => 
    t.name.toLowerCase().includes(searchTerm) ||
    t.description.toLowerCase().includes(searchTerm) ||
    t.tags.some(tag => tag.toLowerCase().includes(searchTerm))
  );
};