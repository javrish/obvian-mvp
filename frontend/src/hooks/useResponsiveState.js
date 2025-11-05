import { useState, useEffect, useCallback, useRef } from 'react';
import { useResponsive } from './useResponsive';

/**
 * Enhanced responsive state management hook
 * Provides breakpoint-aware state management with persistence and mobile optimizations
 */
export const useResponsiveState = (initialState = {}, options = {}) => {
  const {
    persist = false,
    storageKey = 'responsiveState',
    mobileDefaults = {},
    tabletDefaults = {},
    desktopDefaults = {},
    enableTransitions = true
  } = options;

  const responsive = useResponsive();
  const prevBreakpoint = useRef(responsive.breakpoint);

  // Initialize state with breakpoint-specific defaults
  const getInitialState = useCallback(() => {
    let state = { ...initialState };

    // Apply breakpoint-specific defaults
    if (responsive.isMobile && mobileDefaults) {
      state = { ...state, ...mobileDefaults };
    } else if (responsive.isTablet && tabletDefaults) {
      state = { ...state, ...tabletDefaults };
    } else if (responsive.isDesktop && desktopDefaults) {
      state = { ...state, ...desktopDefaults };
    }

    // Load from storage if persistence is enabled
    if (persist && typeof window !== 'undefined') {
      try {
        const stored = localStorage.getItem(storageKey);
        if (stored) {
          const parsedStored = JSON.parse(stored);
          state = { ...state, ...parsedStored };
        }
      } catch (error) {
        console.warn('Failed to load responsive state from storage:', error);
      }
    }

    return state;
  }, [initialState, responsive.breakpoint, persist, storageKey, mobileDefaults, tabletDefaults, desktopDefaults]);

  const [state, setState] = useState(getInitialState);
  const [isTransitioning, setIsTransitioning] = useState(false);

  // Persist state changes to storage
  const persistState = useCallback((newState) => {
    if (persist && typeof window !== 'undefined') {
      try {
        localStorage.setItem(storageKey, JSON.stringify(newState));
      } catch (error) {
        console.warn('Failed to persist responsive state:', error);
      }
    }
  }, [persist, storageKey]);

  // Enhanced setState with responsive behavior
  const setResponsiveState = useCallback((updater, options = {}) => {
    const { breakpointSpecific = false, animate = enableTransitions } = options;

    if (animate && enableTransitions) {
      setIsTransitioning(true);
      setTimeout(() => setIsTransitioning(false), 300);
    }

    setState(prevState => {
      let newState;

      if (typeof updater === 'function') {
        newState = updater(prevState);
      } else {
        newState = { ...prevState, ...updater };
      }

      // Apply breakpoint-specific logic if requested
      if (breakpointSpecific) {
        if (responsive.isMobile && mobileDefaults) {
          newState = { ...newState, ...mobileDefaults };
        } else if (responsive.isTablet && tabletDefaults) {
          newState = { ...newState, ...tabletDefaults };
        } else if (responsive.isDesktop && desktopDefaults) {
          newState = { ...newState, ...desktopDefaults };
        }
      }

      persistState(newState);
      return newState;
    });
  }, [responsive, enableTransitions, persistState, mobileDefaults, tabletDefaults, desktopDefaults]);

  // Handle breakpoint changes
  useEffect(() => {
    if (prevBreakpoint.current !== responsive.breakpoint) {
      setResponsiveState(prevState => {
        let adaptedState = { ...prevState };

        // Apply automatic mobile optimizations
        if (responsive.isMobile) {
          adaptedState = {
            ...adaptedState,
            // Common mobile defaults
            sidebarOpen: false,
            compactMode: true,
            showAdvancedControls: false,
            panelLayout: 'single',
            ...mobileDefaults
          };
        } else if (responsive.isTablet) {
          adaptedState = {
            ...adaptedState,
            sidebarOpen: false,
            compactMode: false,
            showAdvancedControls: true,
            panelLayout: 'stacked',
            ...tabletDefaults
          };
        } else {
          adaptedState = {
            ...adaptedState,
            sidebarOpen: true,
            compactMode: false,
            showAdvancedControls: true,
            panelLayout: 'split',
            ...desktopDefaults
          };
        }

        return adaptedState;
      }, { animate: enableTransitions });

      prevBreakpoint.current = responsive.breakpoint;
    }
  }, [responsive.breakpoint, setResponsiveState, enableTransitions, mobileDefaults, tabletDefaults, desktopDefaults]);

  // Utility functions for common responsive patterns
  const toggleValue = useCallback((key, trueValue = true, falseValue = false) => {
    setResponsiveState(prevState => ({
      ...prevState,
      [key]: prevState[key] === trueValue ? falseValue : trueValue
    }));
  }, [setResponsiveState]);

  const cycleValue = useCallback((key, values = [], options = {}) => {
    const { mobileValues, tabletValues, desktopValues } = options;

    // Use breakpoint-specific values if provided
    let availableValues = values;
    if (responsive.isMobile && mobileValues) {
      availableValues = mobileValues;
    } else if (responsive.isTablet && tabletValues) {
      availableValues = tabletValues;
    } else if (responsive.isDesktop && desktopValues) {
      availableValues = desktopValues;
    }

    setResponsiveState(prevState => {
      const currentIndex = availableValues.indexOf(prevState[key]);
      const nextIndex = (currentIndex + 1) % availableValues.length;
      return {
        ...prevState,
        [key]: availableValues[nextIndex]
      };
    });
  }, [responsive, setResponsiveState]);

  const resetToDefaults = useCallback(() => {
    setResponsiveState(getInitialState(), { animate: enableTransitions });
  }, [getInitialState, setResponsiveState, enableTransitions]);

  const clearPersisted = useCallback(() => {
    if (persist && typeof window !== 'undefined') {
      try {
        localStorage.removeItem(storageKey);
      } catch (error) {
        console.warn('Failed to clear persisted responsive state:', error);
      }
    }
  }, [persist, storageKey]);

  return {
    state,
    setState: setResponsiveState,
    toggleValue,
    cycleValue,
    resetToDefaults,
    clearPersisted,
    isTransitioning,
    responsive
  };
};

/**
 * Hook for managing responsive panel states
 */
export const useResponsivePanels = (panels = [], options = {}) => {
  const responsive = useResponsive();
  const {
    defaultPanel = panels[0]?.id,
    enableSwipeNavigation = true,
    persistSelection = false
  } = options;

  const { state, setState, cycleValue } = useResponsiveState(
    {
      currentPanel: defaultPanel,
      panelsOpen: responsive.isMobile ? [defaultPanel] : panels.map(p => p.id),
      panelLayout: responsive.isMobile ? 'single' : 'multiple'
    },
    {
      persist: persistSelection,
      storageKey: 'responsivePanels',
      mobileDefaults: {
        panelLayout: 'single',
        panelsOpen: [defaultPanel]
      },
      tabletDefaults: {
        panelLayout: 'stacked',
        panelsOpen: panels.slice(0, 2).map(p => p.id)
      },
      desktopDefaults: {
        panelLayout: 'multiple',
        panelsOpen: panels.map(p => p.id)
      }
    }
  );

  const setCurrentPanel = useCallback((panelId) => {
    setState(prevState => ({
      ...prevState,
      currentPanel: panelId,
      panelsOpen: responsive.isMobile ? [panelId] : prevState.panelsOpen
    }));
  }, [setState, responsive.isMobile]);

  const togglePanel = useCallback((panelId) => {
    if (responsive.isMobile) {
      setCurrentPanel(panelId);
    } else {
      setState(prevState => ({
        ...prevState,
        panelsOpen: prevState.panelsOpen.includes(panelId)
          ? prevState.panelsOpen.filter(id => id !== panelId)
          : [...prevState.panelsOpen, panelId]
      }));
    }
  }, [responsive.isMobile, setState, setCurrentPanel]);

  const navigatePanel = useCallback((direction) => {
    const currentIndex = panels.findIndex(p => p.id === state.currentPanel);
    let nextIndex;

    if (direction === 'next') {
      nextIndex = (currentIndex + 1) % panels.length;
    } else {
      nextIndex = currentIndex > 0 ? currentIndex - 1 : panels.length - 1;
    }

    setCurrentPanel(panels[nextIndex].id);
  }, [panels, state.currentPanel, setCurrentPanel]);

  const cyclePanelLayout = useCallback(() => {
    cycleValue('panelLayout', ['single', 'stacked', 'multiple'], {
      mobileValues: ['single'],
      tabletValues: ['single', 'stacked'],
      desktopValues: ['single', 'stacked', 'multiple']
    });
  }, [cycleValue]);

  return {
    ...state,
    setCurrentPanel,
    togglePanel,
    navigatePanel,
    cyclePanelLayout,
    availablePanels: panels,
    currentPanelData: panels.find(p => p.id === state.currentPanel),
    isTransitioning: state.isTransitioning
  };
};

/**
 * Hook for responsive UI preferences
 */
export const useResponsivePreferences = (options = {}) => {
  const responsive = useResponsive();

  const { state, setState, toggleValue, cycleValue } = useResponsiveState(
    {
      // Layout preferences
      compactMode: responsive.isMobile,
      showSidebar: !responsive.isMobile,
      panelLayout: responsive.isMobile ? 'single' : 'split',

      // Interaction preferences
      enableAnimations: !responsive.isMobile, // Reduce animations on mobile for performance
      enableHoverEffects: !responsive.isTouchDevice,
      touchFriendlyControls: responsive.isTouchDevice,

      // Content preferences
      showAdvancedControls: responsive.isDesktop,
      showDetailedInfo: !responsive.isMobile,
      autoCollapse: responsive.isMobile,

      // Performance preferences
      reducedMotion: false,
      lazyLoadImages: responsive.isMobile,
      prefersHighContrast: false
    },
    {
      persist: true,
      storageKey: 'uiPreferences',
      ...options
    }
  );

  const toggleCompactMode = useCallback(() => {
    if (!responsive.isMobile) { // Don't allow toggling compact mode on mobile
      toggleValue('compactMode');
    }
  }, [responsive.isMobile, toggleValue]);

  const toggleSidebar = useCallback(() => {
    toggleValue('showSidebar');
  }, [toggleValue]);

  const cyclePanelLayout = useCallback(() => {
    const layouts = responsive.isMobile ? ['single'] :
                    responsive.isTablet ? ['single', 'stacked'] :
                    ['single', 'stacked', 'split'];

    cycleValue('panelLayout', layouts);
  }, [responsive, cycleValue]);

  const setTheme = useCallback((theme) => {
    setState(prevState => ({
      ...prevState,
      theme,
      prefersHighContrast: theme === 'high-contrast'
    }));
  }, [setState]);

  return {
    preferences: state,
    setPreferences: setState,
    toggleCompactMode,
    toggleSidebar,
    cyclePanelLayout,
    setTheme,
    toggleValue,
    cycleValue,
    responsive
  };
};

export default useResponsiveState;