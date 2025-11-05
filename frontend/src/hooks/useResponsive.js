import { useState, useEffect } from 'react';

/**
 * Responsive breakpoints following mobile-first approach
 * - Mobile: 320px - 767px
 * - Tablet: 768px - 1023px
 * - Desktop: 1024px - 1439px
 * - Large Desktop: 1440px+
 */
const breakpoints = {
  mobile: 0,      // 0px - 767px
  tablet: 768,    // 768px - 1023px
  desktop: 1024,  // 1024px - 1439px
  large: 1440     // 1440px+
};

/**
 * Get current breakpoint based on window width
 */
const getBreakpoint = (width) => {
  if (width >= breakpoints.large) return 'large';
  if (width >= breakpoints.desktop) return 'desktop';
  if (width >= breakpoints.tablet) return 'tablet';
  return 'mobile';
};

/**
 * Check if current width matches specific breakpoint
 */
const matchesBreakpoint = (width, breakpoint) => {
  switch (breakpoint) {
    case 'mobile':
      return width < breakpoints.tablet;
    case 'tablet':
      return width >= breakpoints.tablet && width < breakpoints.desktop;
    case 'desktop':
      return width >= breakpoints.desktop && width < breakpoints.large;
    case 'large':
      return width >= breakpoints.large;
    default:
      return false;
  }
};

/**
 * Custom hook for responsive design detection and utilities
 * Provides breakpoint detection, device type, and responsive utilities
 */
export const useResponsive = () => {
  const [windowSize, setWindowSize] = useState({
    width: typeof window !== 'undefined' ? window.innerWidth : 1024,
    height: typeof window !== 'undefined' ? window.innerHeight : 768,
  });

  const [orientation, setOrientation] = useState('landscape');

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const handleResize = () => {
      const newWidth = window.innerWidth;
      const newHeight = window.innerHeight;

      setWindowSize({
        width: newWidth,
        height: newHeight,
      });

      // Update orientation
      setOrientation(newWidth > newHeight ? 'landscape' : 'portrait');
    };

    // Set initial orientation
    setOrientation(window.innerWidth > window.innerHeight ? 'landscape' : 'portrait');

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const currentBreakpoint = getBreakpoint(windowSize.width);

  // Device type detection
  const isMobile = currentBreakpoint === 'mobile';
  const isTablet = currentBreakpoint === 'tablet';
  const isDesktop = currentBreakpoint === 'desktop' || currentBreakpoint === 'large';
  const isLarge = currentBreakpoint === 'large';

  // Touch device detection
  const isTouchDevice = typeof window !== 'undefined' && (
    'ontouchstart' in window ||
    navigator.maxTouchPoints > 0 ||
    navigator.msMaxTouchPoints > 0
  );

  // Utility functions
  const isBreakpointUp = (breakpoint) => {
    return windowSize.width >= breakpoints[breakpoint];
  };

  const isBreakpointDown = (breakpoint) => {
    const nextBreakpoint = {
      mobile: 'tablet',
      tablet: 'desktop',
      desktop: 'large'
    }[breakpoint];

    return nextBreakpoint ? windowSize.width < breakpoints[nextBreakpoint] : true;
  };

  const isBreakpointOnly = (breakpoint) => {
    return matchesBreakpoint(windowSize.width, breakpoint);
  };

  // Layout utilities
  const getResponsiveValue = (values) => {
    if (typeof values === 'object') {
      if (isLarge && values.large !== undefined) return values.large;
      if (isDesktop && values.desktop !== undefined) return values.desktop;
      if (isTablet && values.tablet !== undefined) return values.tablet;
      if (isMobile && values.mobile !== undefined) return values.mobile;

      // Fallback logic - use the most appropriate available value
      return values.desktop || values.tablet || values.mobile || values.large;
    }

    return values;
  };

  const getContainerClasses = () => {
    const baseClasses = 'mx-auto px-4';
    if (isLarge) return `${baseClasses} max-w-7xl px-8`;
    if (isDesktop) return `${baseClasses} max-w-6xl px-6`;
    if (isTablet) return `${baseClasses} max-w-4xl px-6`;
    return `${baseClasses} max-w-full px-4`;
  };

  const getGridClasses = (columns) => {
    const cols = getResponsiveValue(columns);
    if (typeof cols === 'number') {
      return `grid-cols-${cols}`;
    }

    // Default responsive grid
    if (isMobile) return 'grid-cols-1';
    if (isTablet) return 'grid-cols-2';
    if (isDesktop) return 'grid-cols-3';
    return 'grid-cols-4';
  };

  const getSpacingClasses = (spacing) => {
    const space = getResponsiveValue(spacing) || {
      mobile: 4,
      tablet: 6,
      desktop: 8,
      large: 10
    };

    if (typeof space === 'number') {
      return `space-y-${space}`;
    }

    if (isLarge) return `space-y-${space.large || space.desktop || 8}`;
    if (isDesktop) return `space-y-${space.desktop || 8}`;
    if (isTablet) return `space-y-${space.tablet || 6}`;
    return `space-y-${space.mobile || 4}`;
  };

  // Touch gesture utilities
  const getTouchTargetClasses = () => {
    if (isTouchDevice) {
      return 'min-h-[44px] min-w-[44px] touch-manipulation';
    }
    return '';
  };

  const getTextSizeClasses = (textConfig) => {
    const sizes = getResponsiveValue(textConfig) || {
      mobile: 'text-sm',
      tablet: 'text-base',
      desktop: 'text-lg',
      large: 'text-xl'
    };

    if (typeof sizes === 'string') return sizes;

    if (isLarge && sizes.large) return sizes.large;
    if (isDesktop && sizes.desktop) return sizes.desktop;
    if (isTablet && sizes.tablet) return sizes.tablet;
    return sizes.mobile || 'text-sm';
  };

  return {
    // Current state
    width: windowSize.width,
    height: windowSize.height,
    breakpoint: currentBreakpoint,
    orientation,

    // Device detection
    isMobile,
    isTablet,
    isDesktop,
    isLarge,
    isTouchDevice,

    // Breakpoint utilities
    isBreakpointUp,
    isBreakpointDown,
    isBreakpointOnly,

    // Layout utilities
    getResponsiveValue,
    getContainerClasses,
    getGridClasses,
    getSpacingClasses,
    getTouchTargetClasses,
    getTextSizeClasses,

    // Breakpoint constants for direct use
    breakpoints,
  };
};

/**
 * Hook for media query matching
 */
export const useMediaQuery = (query) => {
  const [matches, setMatches] = useState(false);

  useEffect(() => {
    if (typeof window === 'undefined') return;

    const mediaQuery = window.matchMedia(query);
    setMatches(mediaQuery.matches);

    const handleChange = (event) => {
      setMatches(event.matches);
    };

    mediaQuery.addEventListener('change', handleChange);
    return () => mediaQuery.removeEventListener('change', handleChange);
  }, [query]);

  return matches;
};

/**
 * Hook for detecting viewport changes and providing layout suggestions
 */
export const useResponsiveLayout = () => {
  const responsive = useResponsive();

  // Layout recommendations based on breakpoint
  const getLayoutSuggestion = () => {
    if (responsive.isMobile) {
      return {
        orientation: 'vertical',
        panelLayout: 'single',
        showSidebar: false,
        collapsibleControls: true,
        compactMode: true
      };
    }

    if (responsive.isTablet) {
      return {
        orientation: responsive.orientation === 'portrait' ? 'vertical' : 'horizontal',
        panelLayout: responsive.orientation === 'portrait' ? 'stacked' : 'split',
        showSidebar: false,
        collapsibleControls: true,
        compactMode: false
      };
    }

    return {
      orientation: 'horizontal',
      panelLayout: 'split',
      showSidebar: true,
      collapsibleControls: false,
      compactMode: false
    };
  };

  const shouldUseCompactHeader = () => {
    return responsive.isMobile || (responsive.isTablet && responsive.orientation === 'portrait');
  };

  const shouldStackPanels = () => {
    return responsive.isMobile || (responsive.isTablet && responsive.orientation === 'portrait');
  };

  const shouldShowFullControls = () => {
    return responsive.isDesktop;
  };

  const getOptimalColumnCount = () => {
    if (responsive.isMobile) return 1;
    if (responsive.isTablet) return 2;
    if (responsive.isDesktop) return 3;
    return 4;
  };

  return {
    ...responsive,
    layout: getLayoutSuggestion(),
    shouldUseCompactHeader,
    shouldStackPanels,
    shouldShowFullControls,
    getOptimalColumnCount,
  };
};

export default useResponsive;