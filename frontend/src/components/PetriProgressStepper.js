import React from 'react';
import { motion } from 'framer-motion';
import {
  MessageSquare,
  Network,
  CheckCircle,
  Play,
  ArrowRight,
  Check,
  Clock,
  AlertCircle,
  Loader,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import { useResponsive } from '../hooks/useResponsive';
import { useSwipe } from '../hooks/useTouch';

/**
 * PetriProgressStepper - Mobile-first responsive progress indicator for P3Net pipeline
 *
 * Displays the 4-step workflow with comprehensive responsive design:
 * 1. Parse Intent - Natural language processing
 * 2. Build PetriNet - Formal model construction
 * 3. Validate - Formal verification and deadlock detection
 * 4. Simulate - Token simulation and execution
 *
 * Responsive Features:
 * - Mobile: Vertical compact layout with collapsible details
 * - Tablet: Horizontal with adaptive sizing
 * - Desktop: Full horizontal with performance metrics
 * - Touch-friendly interactions with swipe support
 * - Adaptive font sizes and spacing
 * - Performance-optimized animations
 */
const PetriProgressStepper = ({
  currentStep,
  stepStatuses = {},
  processingTimes = {},
  onStepClick,
  disabled = false,
  showTimings = true,
  orientation = 'auto', // 'auto', 'horizontal', 'vertical'
  size = 'auto', // 'auto', 'small', 'medium', 'large'
  enableSwipeNavigation = true
}) => {
  const responsive = useResponsive();

  // Responsive configuration
  const [showDetails, setShowDetails] = React.useState(!responsive.isMobile);
  const [currentStepIndex, setCurrentStepIndex] = React.useState(0);

  // Auto-determine orientation and size based on breakpoint
  const effectiveOrientation = orientation === 'auto'
    ? (responsive.isMobile ? 'vertical' : 'horizontal')
    : orientation;

  const effectiveSize = size === 'auto'
    ? (responsive.isMobile ? 'small' : responsive.isTablet ? 'medium' : 'large')
    : size;

  const effectiveShowTimings = showTimings && !responsive.isMobile;

  // Step definitions with metadata
  const steps = [
    {
      id: 'parse',
      label: 'Parse Intent',
      shortLabel: 'Parse',
      description: 'Transform natural language into structured intent specification',
      icon: MessageSquare,
      color: 'blue',
      targetTime: 500, // ms
      examples: ['DevOps patterns', 'Football workflows', 'Generic sequences']
    },
    {
      id: 'build',
      label: 'Build PetriNet',
      shortLabel: 'Build',
      description: 'Construct formal Petri net with places, transitions, and arcs',
      icon: Network,
      color: 'purple',
      targetTime: 300,
      examples: ['Place creation', 'Transition mapping', 'Arc generation']
    },
    {
      id: 'validate',
      label: 'Validate',
      shortLabel: 'Validate',
      description: 'Perform formal verification including deadlock detection',
      icon: CheckCircle,
      color: 'green',
      targetTime: 1000,
      examples: ['Deadlock check', 'Reachability analysis', 'Liveness verification']
    },
    {
      id: 'simulate',
      label: 'Simulate',
      shortLabel: 'Simulate',
      description: 'Execute token simulation with real-time visualization',
      icon: Play,
      color: 'orange',
      targetTime: 800,
      examples: ['Token animation', 'Step execution', 'Trace generation']
    }
  ];

  // Swipe navigation for mobile
  const swipeHandlers = useSwipe({
    onSwipeLeft: () => {
      if (enableSwipeNavigation && responsive.isMobile) {
        const nextIndex = Math.min(currentStepIndex + 1, steps.length - 1);
        if (nextIndex !== currentStepIndex) {
          setCurrentStepIndex(nextIndex);
          onStepClick?.(steps[nextIndex].id);
        }
      }
    },
    onSwipeRight: () => {
      if (enableSwipeNavigation && responsive.isMobile) {
        const prevIndex = Math.max(currentStepIndex - 1, 0);
        if (prevIndex !== currentStepIndex) {
          setCurrentStepIndex(prevIndex);
          onStepClick?.(steps[prevIndex].id);
        }
      }
    }
  });

  // Update current step index when currentStep changes
  React.useEffect(() => {
    const index = steps.findIndex(step => step.id === currentStep);
    if (index >= 0) {
      setCurrentStepIndex(index);
    }
  }, [currentStep]);

  // Get status for a specific step
  const getStepStatus = (stepId) => {
    return stepStatuses[stepId] || 'pending';
  };

  // Get step index
  const getStepIndex = (stepId) => {
    return steps.findIndex(step => step.id === stepId);
  };

  const getCurrentStepIndex = () => {
    return getStepIndex(currentStep);
  };

  // Responsive status-based styling
  const getStepStyles = (step, status) => {
    const baseClasses = 'transition-all duration-300 ease-in-out';
    const touchTargetClasses = responsive.isTouchDevice ? 'min-h-[44px] touch-manipulation' : '';

    const colorMap = {
      blue: 'bg-blue-500 text-white border-blue-500',
      purple: 'bg-purple-500 text-white border-purple-500',
      green: 'bg-green-500 text-white border-green-500',
      orange: 'bg-orange-500 text-white border-orange-500'
    };

    const statusStyles = {
      pending: 'bg-gray-100 text-gray-400 border-gray-200 hover:bg-gray-200',
      active: `${colorMap[step.color]} shadow-lg ${responsive.isMobile ? 'ring-2 ring-offset-2' : 'ring-2'} ring-${step.color}-200`,
      processing: `${colorMap[step.color]} animate-pulse shadow-lg ${responsive.isMobile ? 'ring-2 ring-offset-2' : 'ring-2'} ring-${step.color}-200`,
      completed: 'bg-green-100 text-green-800 border-green-300 hover:bg-green-200',
      error: 'bg-red-100 text-red-800 border-red-300 hover:bg-red-200'
    };

    return `${baseClasses} ${touchTargetClasses} ${statusStyles[status] || statusStyles.pending}`;
  };

  // Click handler with validation
  const handleStepClick = (step) => {
    if (disabled || !onStepClick) return;

    const stepIndex = getStepIndex(step.id);
    const currentIndex = getCurrentStepIndex();

    // Allow clicking on completed steps or next step
    if (stepIndex <= currentIndex + 1 || getStepStatus(step.id) === 'completed') {
      onStepClick(step.id);
    }
  };

  // Check if step is clickable
  const isStepClickable = (step) => {
    if (disabled) return false;

    const stepIndex = getStepIndex(step.id);
    const currentIndex = getCurrentStepIndex();
    const status = getStepStatus(step.id);

    return stepIndex <= currentIndex + 1 || status === 'completed';
  };

  // Status icon component with responsive sizing
  const StatusIcon = ({ step, status, size: iconSize }) => {
    const IconComponent = step.icon;
    const finalSize = iconSize || classes.icon;

    switch (status) {
      case 'processing':
        return <Loader size={finalSize} className="animate-spin" />;
      case 'completed':
        return <Check size={finalSize} />;
      case 'error':
        return <AlertCircle size={finalSize} />;
      default:
        return <IconComponent size={finalSize} />;
    }
  };

  // Performance indicator with responsive design
  const PerformanceIndicator = ({ step, actualTime }) => {
    if (!effectiveShowTimings || !actualTime) return null;

    const isSlowerthanTarget = actualTime > step.targetTime;
    const percentage = (actualTime / step.targetTime) * 100;

    return (
      <div className={`text-xs mt-1 ${isSlowerthanTarget ? 'text-red-600' : 'text-green-600'}`}>
        <div className="flex items-center space-x-1">
          <span>{actualTime}ms</span>
          {!responsive.isMobile && (
            <span className="opacity-70">(target: {step.targetTime}ms)</span>
          )}
        </div>
        {responsive.isDesktop && (
          <div className="w-full bg-gray-200 rounded-full h-1 mt-1">
            <div
              className={`h-1 rounded-full transition-all duration-300 ${
                isSlowerthanTarget ? 'bg-red-500' : 'bg-green-500'
              }`}
              style={{ width: `${Math.min(percentage, 100)}%` }}
            />
          </div>
        )}
      </div>
    );
  };

  // Mobile compact step indicator
  const MobileStepIndicator = () => (
    <div className="flex items-center justify-between p-4 bg-white border-b border-gray-200">
      <div className="flex items-center space-x-3">
        <div className={`
          w-8 h-8 rounded-full border-2 border-white flex items-center justify-center font-bold text-sm
          ${getStepStyles(steps[currentStepIndex], getStepStatus(steps[currentStepIndex].id))}
        `}>
          <StatusIcon step={steps[currentStepIndex]} status={getStepStatus(steps[currentStepIndex].id)} size={16} />
        </div>
        <div>
          <div className="font-semibold text-gray-900">
            {steps[currentStepIndex].label}
          </div>
          <div className="text-sm text-gray-600">
            Step {currentStepIndex + 1} of {steps.length}
          </div>
        </div>
      </div>

      <button
        onClick={() => setShowDetails(!showDetails)}
        className="p-2 rounded-lg hover:bg-gray-100 transition-colors touch-manipulation min-h-[44px] min-w-[44px]"
        aria-label={showDetails ? "Hide details" : "Show details"}
      >
        {showDetails ? <ChevronUp size={20} /> : <ChevronDown size={20} />}
      </button>
    </div>
  );

  // Mobile step navigation dots
  const MobileStepDots = () => (
    <div className="flex justify-center space-x-2 p-4">
      {steps.map((step, index) => {
        const status = getStepStatus(step.id);
        const isActive = index === currentStepIndex;

        return (
          <button
            key={step.id}
            onClick={() => {
              setCurrentStepIndex(index);
              onStepClick?.(step.id);
            }}
            disabled={disabled || !isStepClickable(step)}
            className={`
              w-3 h-3 rounded-full transition-all duration-200 touch-manipulation
              ${isActive ? 'w-6' : ''}
              ${status === 'completed' ? 'bg-green-500' :
                status === 'error' ? 'bg-red-500' :
                status === 'active' || status === 'processing' ? `bg-${step.color}-500` :
                'bg-gray-300'}
              ${isStepClickable(step) ? 'hover:scale-110' : 'opacity-50'}
            `}
            aria-label={`Go to ${step.label}`}
          />
        );
      })}
    </div>
  );

  // Responsive sizing classes with mobile optimizations
  const sizeClasses = {
    small: {
      step: responsive.isMobile ? 'px-3 py-3' : 'px-3 py-2',
      text: responsive.isMobile ? 'text-xs' : 'text-sm',
      connector: 'w-6',
      icon: responsive.isMobile ? 14 : 16,
      badge: 'w-5 h-5 text-xs'
    },
    medium: {
      step: responsive.isMobile ? 'px-4 py-4' : 'px-4 py-3',
      text: responsive.isMobile ? 'text-sm' : 'text-base',
      connector: responsive.isMobile ? 'w-8' : 'w-12',
      icon: responsive.isMobile ? 16 : 20,
      badge: 'w-6 h-6 text-xs'
    },
    large: {
      step: responsive.isMobile ? 'px-4 py-4' : 'px-6 py-4',
      text: responsive.isMobile ? 'text-base' : 'text-lg',
      connector: responsive.isMobile ? 'w-10' : 'w-16',
      icon: responsive.isMobile ? 18 : 24,
      badge: 'w-7 h-7 text-sm'
    }
  };

  const classes = sizeClasses[effectiveSize];

  // Render horizontal layout with responsive adaptations
  const renderHorizontal = () => {
    // On mobile, show simplified horizontal dots instead of full stepper
    if (responsive.isMobile) {
      return (
        <div className="space-y-4">
          <MobileStepIndicator />
          {showDetails && (
            <div className="px-4 pb-4">
              <div className="text-sm text-gray-600 mb-2">
                {steps[currentStepIndex].description}
              </div>
              <PerformanceIndicator
                step={steps[currentStepIndex]}
                actualTime={processingTimes[steps[currentStepIndex].id]}
              />
            </div>
          )}
          <MobileStepDots />
        </div>
      );
    }

    // Tablet and desktop horizontal layout
    return (
      <div className={`flex items-center ${responsive.isTablet ? 'space-x-2' : 'space-x-4'} overflow-x-auto pb-2`}>
        {steps.map((step, index) => {
          const status = getStepStatus(step.id);
          const isClickable = isStepClickable(step);
          const actualTime = processingTimes[step.id];

          return (
            <React.Fragment key={step.id}>
              <motion.div
                className={`
                  relative flex items-center space-x-3 border rounded-lg ${classes.step} flex-shrink-0
                  ${getStepStyles(step, status)}
                  ${isClickable ? 'cursor-pointer hover:scale-105' : 'cursor-not-allowed'}
                `}
                onClick={() => handleStepClick(step)}
                whileHover={isClickable ? { scale: responsive.isTablet ? 1.01 : 1.02 } : {}}
                whileTap={isClickable ? { scale: 0.98 } : {}}
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
              >
                {/* Status Icon */}
                <StatusIcon step={step} status={status} />

                {/* Step Content */}
                <div className="flex-1 min-w-0">
                  <div className={`font-medium ${classes.text} truncate`}>
                    {effectiveSize === 'small' || responsive.isTablet ? step.shortLabel : step.label}
                  </div>

                  {effectiveSize !== 'small' && responsive.isDesktop && (
                    <div className="text-xs opacity-70 mt-1 line-clamp-2">
                      {step.description}
                    </div>
                  )}

                  <PerformanceIndicator step={step} actualTime={actualTime} />
                </div>

                {/* Step Number Badge */}
                <div className={`
                  absolute -top-2 -right-2 ${classes.badge} rounded-full border-2 border-white
                  flex items-center justify-center font-bold
                  ${status === 'completed' ? 'bg-green-500 text-white' :
                    status === 'error' ? 'bg-red-500 text-white' :
                    status === 'active' || status === 'processing' ? `bg-${step.color}-500 text-white` :
                    'bg-gray-300 text-gray-600'}
                `}>
                  {status === 'completed' ? <Check size={12} /> : index + 1}
                </div>
              </motion.div>

              {/* Connector Arrow */}
              {index < steps.length - 1 && (
                <div className={`flex items-center justify-center ${classes.connector} flex-shrink-0`}>
                  <ArrowRight
                    size={responsive.isTablet ? 16 : 20}
                    className={`
                      ${getCurrentStepIndex() > index ? 'text-green-500' : 'text-gray-300'}
                      transition-colors duration-300
                    `}
                  />
                </div>
              )}
            </React.Fragment>
          );
        })}
      </div>
    );
  };

  // Render vertical layout
  const renderVertical = () => (
    <div className="space-y-4">
      {steps.map((step, index) => {
        const status = getStepStatus(step.id);
        const isClickable = isStepClickable(step);
        const actualTime = processingTimes[step.id];

        return (
          <motion.div
            key={step.id}
            className="relative"
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: index * 0.1 }}
          >
            <div
              className={`
                flex items-start space-x-4 p-4 border rounded-lg
                ${getStepStyles(step, status)}
                ${isClickable ? 'cursor-pointer hover:scale-105' : 'cursor-not-allowed'}
              `}
              onClick={() => handleStepClick(step)}
            >
              {/* Step Number and Icon */}
              <div className="flex-shrink-0">
                <div className={`
                  w-10 h-10 rounded-full border-2 border-white
                  flex items-center justify-center font-bold
                  ${status === 'completed' ? 'bg-green-500 text-white' :
                    status === 'error' ? 'bg-red-500 text-white' :
                    status === 'active' || status === 'processing' ? `bg-${step.color}-500 text-white` :
                    'bg-gray-300 text-gray-600'}
                `}>
                  {status === 'completed' ? <Check size={20} /> :
                   status === 'processing' ? <Loader size={20} className="animate-spin" /> :
                   index + 1}
                </div>
              </div>

              {/* Step Content */}
              <div className="flex-1 min-w-0">
                <h3 className="font-semibold text-lg">{step.label}</h3>
                <p className="text-sm opacity-70 mt-1">{step.description}</p>

                {step.examples && size === 'large' && (
                  <div className="mt-2">
                    <div className="text-xs opacity-60">Examples:</div>
                    <div className="flex flex-wrap gap-1 mt-1">
                      {step.examples.map((example, i) => (
                        <span key={i} className="text-xs bg-white/20 px-2 py-1 rounded">
                          {example}
                        </span>
                      ))}
                    </div>
                  </div>
                )}

                <PerformanceIndicator step={step} actualTime={actualTime} />
              </div>

              {/* Status Icon */}
              <div className="flex-shrink-0">
                <StatusIcon step={step} status={status} />
              </div>
            </div>

            {/* Vertical Connector */}
            {index < steps.length - 1 && (
              <div className="flex justify-center py-2">
                <div className={`
                  w-0.5 h-6
                  ${getCurrentStepIndex() > index ? 'bg-green-500' : 'bg-gray-300'}
                  transition-colors duration-300
                `} />
              </div>
            )}
          </motion.div>
        );
      })}
    </div>
  );

  // Main render with swipe support for mobile
  return (
    <div
      className={`petri-progress-stepper ${responsive.isMobile ? 'select-none' : ''}`}
      {...(responsive.isMobile && enableSwipeNavigation ? swipeHandlers : {})}
    >
      {effectiveOrientation === 'vertical' ? renderVertical() : renderHorizontal()}

      {/* Mobile swipe hint */}
      {responsive.isMobile && enableSwipeNavigation && currentStepIndex > 0 && (
        <div className="text-xs text-gray-500 text-center py-2">
          Swipe left or right to navigate steps
        </div>
      )}
    </div>
  );
};

export default PetriProgressStepper;