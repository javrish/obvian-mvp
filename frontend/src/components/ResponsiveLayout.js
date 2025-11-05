import React, { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import {
  Menu,
  X,
  ChevronLeft,
  ChevronRight,
  RotateCcw,
  Maximize2,
  Minimize2,
  Smartphone,
  Tablet,
  Monitor
} from 'lucide-react';
import { useResponsiveLayout } from '../hooks/useResponsive';

/**
 * ResponsiveLayout - Mobile-first responsive wrapper component
 *
 * Provides adaptive layout management across all breakpoints:
 * - Mobile: Single-panel with navigation drawer
 * - Tablet: Flexible dual-panel or stacked layout
 * - Desktop: Full dual-panel with sidebars
 * - Large Desktop: Enhanced multi-panel with optimized spacing
 *
 * Features:
 * - Touch-friendly controls on mobile devices
 * - Swipe gestures for panel switching
 * - Collapsible navigation and controls
 * - Breakpoint detection and adaptive layouts
 * - Performance optimizations for mobile
 */
const ResponsiveLayout = ({
  children,
  header,
  sidebar,
  footer,
  className = '',
  enableSwipeGestures = true,
  enableBreakpointIndicator = false, // For development/debugging
  customBreakpoints,
  onBreakpointChange
}) => {
  const responsive = useResponsiveLayout();

  // Layout state management
  const [sidebarOpen, setSidebarOpen] = useState(!responsive.isMobile);
  const [panelMode, setPanelMode] = useState(responsive.layout.panelLayout);
  const [isFullscreen, setIsFullscreen] = useState(false);

  // Touch gesture state
  const [touchStart, setTouchStart] = useState({ x: 0, y: 0, time: 0 });
  const [isDragging, setIsDragging] = useState(false);

  // Update layout when breakpoint changes
  useEffect(() => {
    const newLayout = responsive.layout;
    setPanelMode(newLayout.panelLayout);

    // Auto-close sidebar on mobile, open on desktop
    if (responsive.isMobile) {
      setSidebarOpen(false);
    } else if (responsive.isDesktop) {
      setSidebarOpen(newLayout.showSidebar);
    }

    // Notify parent of breakpoint change
    if (onBreakpointChange) {
      onBreakpointChange({
        breakpoint: responsive.breakpoint,
        layout: newLayout,
        isMobile: responsive.isMobile,
        isTablet: responsive.isTablet,
        isDesktop: responsive.isDesktop
      });
    }
  }, [responsive.breakpoint, onBreakpointChange]);

  // Touch gesture handlers for mobile swipe navigation
  const handleTouchStart = (e) => {
    if (!enableSwipeGestures || !responsive.isTouchDevice) return;

    const touch = e.touches[0];
    setTouchStart({
      x: touch.clientX,
      y: touch.clientY,
      time: Date.now()
    });
    setIsDragging(false);
  };

  const handleTouchMove = (e) => {
    if (!enableSwipeGestures || !responsive.isTouchDevice) return;

    const touch = e.touches[0];
    const deltaX = Math.abs(touch.clientX - touchStart.x);
    const deltaY = Math.abs(touch.clientY - touchStart.y);

    // Detect horizontal swipe gesture
    if (deltaX > 10 && deltaX > deltaY) {
      setIsDragging(true);
    }
  };

  const handleTouchEnd = (e) => {
    if (!enableSwipeGestures || !responsive.isTouchDevice || !isDragging) return;

    const touch = e.changedTouches[0];
    const deltaX = touch.clientX - touchStart.x;
    const deltaY = Math.abs(touch.clientY - touchStart.y);
    const deltaTime = Date.now() - touchStart.time;

    // Swipe gesture thresholds
    const minSwipeDistance = 50;
    const maxSwipeTime = 300;
    const maxVerticalDeviation = 100;

    if (
      Math.abs(deltaX) > minSwipeDistance &&
      deltaTime < maxSwipeTime &&
      deltaY < maxVerticalDeviation
    ) {
      if (deltaX > 0) {
        // Swipe right - open sidebar
        if (responsive.isMobile) {
          setSidebarOpen(true);
        }
      } else {
        // Swipe left - close sidebar or switch panels
        if (responsive.isMobile) {
          setSidebarOpen(false);
        }
      }
    }

    setIsDragging(false);
  };

  // Panel mode switching
  const cyclePanelMode = () => {
    const modes = responsive.isMobile
      ? ['single']
      : responsive.isTablet
        ? ['single', 'stacked', 'split']
        : ['split', 'stacked', 'single'];

    const currentIndex = modes.indexOf(panelMode);
    const nextIndex = (currentIndex + 1) % modes.length;
    setPanelMode(modes[nextIndex]);
  };

  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen);
  };

  const toggleFullscreen = () => {
    setIsFullscreen(!isFullscreen);
  };

  // Responsive container classes
  const containerClasses = [
    'responsive-layout',
    'min-h-screen',
    'flex flex-col',
    responsive.getContainerClasses(),
    isFullscreen ? 'fixed inset-0 z-50 bg-white' : '',
    className
  ].filter(Boolean).join(' ');

  // Sidebar classes based on breakpoint and state
  const sidebarClasses = [
    'responsive-sidebar',
    'transition-transform duration-300 ease-in-out',
    'border-r border-gray-200 bg-white',
    responsive.isMobile ? [
      'fixed inset-y-0 left-0 z-40',
      'w-80 max-w-[80vw]',
      sidebarOpen ? 'translate-x-0' : '-translate-x-full'
    ] : [
      'relative',
      responsive.isTablet ? 'w-72' : 'w-80',
      sidebarOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'
    ]
  ].flat().filter(Boolean).join(' ');

  // Main content classes based on layout mode and breakpoint
  const mainContentClasses = [
    'responsive-main',
    'flex-1 flex flex-col overflow-hidden',
    responsive.isMobile ? 'w-full' : '',
    responsive.isTablet && panelMode === 'stacked' ? 'space-y-4' : '',
    responsive.isDesktop && panelMode === 'split' ? 'lg:flex-row lg:space-x-6 lg:space-y-0' : ''
  ].filter(Boolean).join(' ');

  // Breakpoint indicator for development
  const BreakpointIndicator = () => (
    <div className="fixed top-4 right-4 z-50 bg-black/80 text-white px-3 py-1 rounded-full text-xs font-mono">
      <div className="flex items-center space-x-2">
        {responsive.isMobile && <Smartphone size={12} />}
        {responsive.isTablet && <Tablet size={12} />}
        {responsive.isDesktop && <Monitor size={12} />}
        <span>{responsive.breakpoint}</span>
        <span>({responsive.width}px)</span>
        {responsive.orientation === 'portrait' && <span>📱</span>}
      </div>
    </div>
  );

  return (
    <div
      className={containerClasses}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
    >
      {/* Development breakpoint indicator */}
      {enableBreakpointIndicator && <BreakpointIndicator />}

      {/* Mobile overlay for sidebar */}
      {responsive.isMobile && sidebarOpen && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="fixed inset-0 z-30 bg-black/50"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Header */}
      {header && (
        <motion.header
          className={`
            responsive-header border-b border-gray-200 bg-white z-20
            ${responsive.shouldUseCompactHeader() ? 'px-4 py-3' : 'px-6 py-4'}
          `}
          initial={{ y: -20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.3 }}
        >
          <div className="flex items-center justify-between">
            {/* Mobile menu button */}
            {responsive.isMobile && (
              <button
                onClick={toggleSidebar}
                className={`
                  p-2 rounded-lg hover:bg-gray-100 transition-colors
                  ${responsive.getTouchTargetClasses()}
                `}
                aria-label="Toggle menu"
              >
                <Menu size={20} />
              </button>
            )}

            {/* Header content */}
            <div className="flex-1 flex items-center justify-between">
              {header}
            </div>

            {/* Layout controls */}
            <div className="flex items-center space-x-2">
              {!responsive.isMobile && (
                <button
                  onClick={cyclePanelMode}
                  className={`
                    p-2 rounded-lg hover:bg-gray-100 transition-colors
                    ${responsive.getTouchTargetClasses()}
                  `}
                  title={`Current layout: ${panelMode}`}
                >
                  {panelMode === 'split' ? <ChevronRight size={16} /> :
                   panelMode === 'stacked' ? <RotateCcw size={16} /> :
                   <Maximize2 size={16} />}
                </button>
              )}

              <button
                onClick={toggleFullscreen}
                className={`
                  p-2 rounded-lg hover:bg-gray-100 transition-colors
                  ${responsive.getTouchTargetClasses()}
                `}
                title={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
              >
                {isFullscreen ? <Minimize2 size={16} /> : <Maximize2 size={16} />}
              </button>
            </div>
          </div>
        </motion.header>
      )}

      {/* Main layout container */}
      <div className="flex flex-1 overflow-hidden">
        {/* Sidebar */}
        {sidebar && (
          <motion.aside
            className={sidebarClasses}
            initial={responsive.isMobile ? { x: -320 } : { x: 0 }}
            animate={responsive.isMobile ?
              { x: sidebarOpen ? 0 : -320 } :
              { x: sidebarOpen ? 0 : -320 }
            }
            transition={{ type: "spring", stiffness: 300, damping: 30 }}
          >
            {/* Mobile sidebar header */}
            {responsive.isMobile && (
              <div className="flex items-center justify-between p-4 border-b border-gray-200">
                <h2 className="font-semibold text-gray-900">Menu</h2>
                <button
                  onClick={() => setSidebarOpen(false)}
                  className={`
                    p-2 rounded-lg hover:bg-gray-100 transition-colors
                    ${responsive.getTouchTargetClasses()}
                  `}
                >
                  <X size={20} />
                </button>
              </div>
            )}

            <div className="flex-1 overflow-y-auto">
              {sidebar}
            </div>
          </motion.aside>
        )}

        {/* Main content area */}
        <main className={mainContentClasses}>
          {children}
        </main>
      </div>

      {/* Footer */}
      {footer && (
        <motion.footer
          className={`
            responsive-footer border-t border-gray-200 bg-white
            ${responsive.isMobile ? 'px-4 py-3' : 'px-6 py-4'}
          `}
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ duration: 0.3 }}
        >
          {footer}
        </motion.footer>
      )}
    </div>
  );
};

/**
 * ResponsivePanel - Adaptive panel component for content organization
 */
export const ResponsivePanel = ({
  children,
  className = '',
  title,
  collapsible = false,
  defaultCollapsed = false,
  priority = 'normal' // 'high', 'normal', 'low' - affects mobile visibility
}) => {
  const responsive = useResponsiveLayout();
  const [isCollapsed, setIsCollapsed] = useState(defaultCollapsed);

  // Auto-collapse low priority panels on mobile
  useEffect(() => {
    if (responsive.isMobile && priority === 'low') {
      setIsCollapsed(true);
    }
  }, [responsive.isMobile, priority]);

  const panelClasses = [
    'responsive-panel',
    'bg-white rounded-lg border border-gray-200',
    responsive.isMobile ? 'mx-4 mb-4' : 'mb-6',
    className
  ].filter(Boolean).join(' ');

  const headerClasses = [
    'flex items-center justify-between',
    responsive.isMobile ? 'p-4' : 'p-6',
    title || collapsible ? 'border-b border-gray-200' : ''
  ].filter(Boolean).join(' ');

  const contentClasses = [
    'responsive-panel-content',
    responsive.isMobile ? 'p-4' : 'p-6',
    isCollapsed ? 'hidden' : 'block'
  ].filter(Boolean).join(' ');

  return (
    <motion.div
      className={panelClasses}
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      {(title || collapsible) && (
        <div className={headerClasses}>
          {title && (
            <h3 className={`font-semibold text-gray-900 ${responsive.getTextSizeClasses({
              mobile: 'text-base',
              tablet: 'text-lg',
              desktop: 'text-xl'
            })}`}>
              {title}
            </h3>
          )}

          {collapsible && (
            <button
              onClick={() => setIsCollapsed(!isCollapsed)}
              className={`
                p-1 rounded hover:bg-gray-100 transition-colors
                ${responsive.getTouchTargetClasses()}
              `}
              aria-label={isCollapsed ? "Expand panel" : "Collapse panel"}
            >
              {isCollapsed ? <ChevronRight size={16} /> : <ChevronLeft size={16} />}
            </button>
          )}
        </div>
      )}

      <div className={contentClasses}>
        {children}
      </div>
    </motion.div>
  );
};

/**
 * ResponsiveGrid - Adaptive grid layout component
 */
export const ResponsiveGrid = ({
  children,
  columns,
  gap,
  className = ''
}) => {
  const responsive = useResponsiveLayout();

  const gridClasses = [
    'responsive-grid grid',
    responsive.getGridClasses(columns),
    responsive.getSpacingClasses(gap),
    className
  ].filter(Boolean).join(' ');

  return (
    <div className={gridClasses}>
      {children}
    </div>
  );
};

export default ResponsiveLayout;