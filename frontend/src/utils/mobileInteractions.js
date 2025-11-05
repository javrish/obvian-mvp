/**
 * Mobile interaction patterns and utilities for P3Net UI
 * Provides touch-friendly interactions, gestures, and mobile optimizations
 */

/**
 * Touch target size utilities
 */
export const TOUCH_TARGET = {
  MINIMUM: 44, // iOS HIG and Material Design minimum
  COMFORTABLE: 48,
  SPACIOUS: 56
};

export const getTouchTargetClasses = (size = 'comfortable') => {
  const sizeMap = {
    minimum: `min-h-[${TOUCH_TARGET.MINIMUM}px] min-w-[${TOUCH_TARGET.MINIMUM}px]`,
    comfortable: `min-h-[${TOUCH_TARGET.COMFORTABLE}px] min-w-[${TOUCH_TARGET.COMFORTABLE}px]`,
    spacious: `min-h-[${TOUCH_TARGET.SPACIOUS}px] min-w-[${TOUCH_TARGET.SPACIOUS}px]`
  };

  return `${sizeMap[size] || sizeMap.comfortable} touch-manipulation`;
};

/**
 * Mobile-optimized animation configurations
 */
export const MOBILE_ANIMATIONS = {
  // Reduced motion for mobile performance
  slide: {
    initial: { x: 20, opacity: 0 },
    animate: { x: 0, opacity: 1 },
    exit: { x: -20, opacity: 0 },
    transition: { duration: 0.2, ease: 'easeOut' }
  },

  fade: {
    initial: { opacity: 0 },
    animate: { opacity: 1 },
    exit: { opacity: 0 },
    transition: { duration: 0.15 }
  },

  scale: {
    initial: { scale: 0.95, opacity: 0 },
    animate: { scale: 1, opacity: 1 },
    exit: { scale: 0.95, opacity: 0 },
    transition: { duration: 0.2, ease: 'easeOut' }
  },

  // Modal/drawer animations
  slideUp: {
    initial: { y: '100%', opacity: 0 },
    animate: { y: 0, opacity: 1 },
    exit: { y: '100%', opacity: 0 },
    transition: { duration: 0.3, ease: 'easeOut' }
  },

  slideDown: {
    initial: { y: '-100%', opacity: 0 },
    animate: { y: 0, opacity: 1 },
    exit: { y: '-100%', opacity: 0 },
    transition: { duration: 0.3, ease: 'easeOut' }
  }
};

/**
 * Get animation config based on device capabilities
 */
export const getResponsiveAnimation = (animationType, isMobile = false, reducedMotion = false) => {
  if (reducedMotion) {
    return {
      initial: { opacity: 0 },
      animate: { opacity: 1 },
      exit: { opacity: 0 },
      transition: { duration: 0.1 }
    };
  }

  const config = MOBILE_ANIMATIONS[animationType] || MOBILE_ANIMATIONS.fade;

  // Reduce animation complexity on mobile
  if (isMobile) {
    return {
      ...config,
      transition: {
        ...config.transition,
        duration: (config.transition?.duration || 0.2) * 0.7 // 30% faster on mobile
      }
    };
  }

  return config;
};

/**
 * Mobile-optimized spacing and sizing utilities
 */
export const MOBILE_SPACING = {
  xs: 'p-2',      // 8px
  sm: 'p-3',      // 12px
  md: 'p-4',      // 16px
  lg: 'p-6',      // 24px
  xl: 'p-8'       // 32px
};

export const getMobileSpacing = (size = 'md', isMobile = false) => {
  if (isMobile && size === 'lg') return MOBILE_SPACING.md;
  if (isMobile && size === 'xl') return MOBILE_SPACING.lg;
  return MOBILE_SPACING[size] || MOBILE_SPACING.md;
};

/**
 * Mobile text sizing with optimal readability
 */
export const MOBILE_TEXT_SIZES = {
  xs: { mobile: 'text-xs', desktop: 'text-xs' },
  sm: { mobile: 'text-sm', desktop: 'text-sm' },
  base: { mobile: 'text-base', desktop: 'text-base' },
  lg: { mobile: 'text-base', desktop: 'text-lg' },
  xl: { mobile: 'text-lg', desktop: 'text-xl' },
  '2xl': { mobile: 'text-xl', desktop: 'text-2xl' },
  '3xl': { mobile: 'text-2xl', desktop: 'text-3xl' }
};

export const getResponsiveTextSize = (size = 'base', isMobile = false) => {
  const config = MOBILE_TEXT_SIZES[size] || MOBILE_TEXT_SIZES.base;
  return isMobile ? config.mobile : config.desktop;
};

/**
 * Pull-to-refresh utility
 */
export class PullToRefresh {
  constructor(element, onRefresh, options = {}) {
    this.element = element;
    this.onRefresh = onRefresh;
    this.options = {
      threshold: 100,
      maxPullDistance: 150,
      resistance: 0.6,
      ...options
    };

    this.startY = 0;
    this.currentY = 0;
    this.pulling = false;
    this.refreshing = false;

    this.init();
  }

  init() {
    if (!this.element) return;

    this.element.addEventListener('touchstart', this.handleTouchStart.bind(this), { passive: false });
    this.element.addEventListener('touchmove', this.handleTouchMove.bind(this), { passive: false });
    this.element.addEventListener('touchend', this.handleTouchEnd.bind(this), { passive: false });
  }

  handleTouchStart(e) {
    if (this.element.scrollTop > 0 || this.refreshing) return;

    this.startY = e.touches[0].clientY;
    this.pulling = true;
  }

  handleTouchMove(e) {
    if (!this.pulling || this.refreshing) return;

    this.currentY = e.touches[0].clientY;
    const deltaY = this.currentY - this.startY;

    if (deltaY > 0) {
      e.preventDefault();
      const pullDistance = Math.min(
        deltaY * this.options.resistance,
        this.options.maxPullDistance
      );

      this.updatePullIndicator(pullDistance);
    }
  }

  handleTouchEnd() {
    if (!this.pulling || this.refreshing) return;

    const deltaY = this.currentY - this.startY;
    const pullDistance = deltaY * this.options.resistance;

    if (pullDistance >= this.options.threshold) {
      this.triggerRefresh();
    } else {
      this.resetPull();
    }

    this.pulling = false;
  }

  updatePullIndicator(distance) {
    // Override this method to update your pull indicator UI
    const opacity = Math.min(distance / this.options.threshold, 1);
    const rotation = (distance / this.options.threshold) * 180;

    // Example implementation - customize as needed
    this.element.style.transform = `translateY(${distance}px)`;
    this.element.style.setProperty('--pull-opacity', opacity);
    this.element.style.setProperty('--pull-rotation', `${rotation}deg`);
  }

  async triggerRefresh() {
    if (this.refreshing) return;

    this.refreshing = true;
    this.updateRefreshingState(true);

    try {
      await this.onRefresh();
    } catch (error) {
      console.error('Pull to refresh error:', error);
    } finally {
      this.refreshing = false;
      this.resetPull();
    }
  }

  updateRefreshingState(refreshing) {
    // Override this method to update refreshing UI state
    this.element.classList.toggle('refreshing', refreshing);
  }

  resetPull() {
    this.element.style.transform = '';
    this.element.style.removeProperty('--pull-opacity');
    this.element.style.removeProperty('--pull-rotation');
    this.updateRefreshingState(false);
  }

  destroy() {
    if (!this.element) return;

    this.element.removeEventListener('touchstart', this.handleTouchStart);
    this.element.removeEventListener('touchmove', this.handleTouchMove);
    this.element.removeEventListener('touchend', this.handleTouchEnd);
  }
}

/**
 * Virtual keyboard detection and handling
 */
export class VirtualKeyboardHandler {
  constructor(options = {}) {
    this.options = {
      threshold: 150, // Minimum height change to consider keyboard open
      adjustViewport: true,
      ...options
    };

    this.initialViewportHeight = window.innerHeight;
    this.keyboardOpen = false;
    this.callbacks = {
      onShow: [],
      onHide: []
    };

    this.init();
  }

  init() {
    // Visual Viewport API (modern browsers)
    if ('visualViewport' in window) {
      window.visualViewport.addEventListener('resize', this.handleViewportResize.bind(this));
    } else {
      // Fallback for older browsers
      window.addEventListener('resize', this.handleWindowResize.bind(this));
    }
  }

  handleViewportResize() {
    const currentHeight = window.visualViewport.height;
    const heightDiff = this.initialViewportHeight - currentHeight;

    if (heightDiff > this.options.threshold) {
      if (!this.keyboardOpen) {
        this.keyboardOpen = true;
        this.onKeyboardShow(currentHeight);
      }
    } else if (this.keyboardOpen) {
      this.keyboardOpen = false;
      this.onKeyboardHide(currentHeight);
    }
  }

  handleWindowResize() {
    const currentHeight = window.innerHeight;
    const heightDiff = this.initialViewportHeight - currentHeight;

    if (heightDiff > this.options.threshold) {
      if (!this.keyboardOpen) {
        this.keyboardOpen = true;
        this.onKeyboardShow(currentHeight);
      }
    } else if (this.keyboardOpen) {
      this.keyboardOpen = false;
      this.onKeyboardHide(currentHeight);
    }
  }

  onKeyboardShow(viewportHeight) {
    document.body.classList.add('keyboard-open');

    if (this.options.adjustViewport) {
      document.documentElement.style.setProperty('--viewport-height', `${viewportHeight}px`);
    }

    this.callbacks.onShow.forEach(callback => callback(viewportHeight));
  }

  onKeyboardHide(viewportHeight) {
    document.body.classList.remove('keyboard-open');

    if (this.options.adjustViewport) {
      document.documentElement.style.removeProperty('--viewport-height');
    }

    this.callbacks.onHide.forEach(callback => callback(viewportHeight));
  }

  addEventListener(type, callback) {
    if (this.callbacks[type]) {
      this.callbacks[type].push(callback);
    }
  }

  removeEventListener(type, callback) {
    if (this.callbacks[type]) {
      const index = this.callbacks[type].indexOf(callback);
      if (index > -1) {
        this.callbacks[type].splice(index, 1);
      }
    }
  }

  destroy() {
    if ('visualViewport' in window) {
      window.visualViewport.removeEventListener('resize', this.handleViewportResize);
    } else {
      window.removeEventListener('resize', this.handleWindowResize);
    }

    this.callbacks = { onShow: [], onHide: [] };
  }
}

/**
 * Mobile-optimized scroll utilities
 */
export const scrollToElement = (element, options = {}) => {
  if (!element) return;

  const {
    behavior = 'smooth',
    block = 'center',
    inline = 'nearest',
    offset = 0,
    isMobile = false
  } = options;

  // On mobile, add extra offset to account for virtual keyboard
  const mobileOffset = isMobile ? 100 : 0;
  const totalOffset = offset + mobileOffset;

  if (totalOffset !== 0) {
    const elementPosition = element.getBoundingClientRect().top + window.pageYOffset - totalOffset;
    window.scrollTo({
      top: elementPosition,
      behavior
    });
  } else {
    element.scrollIntoView({
      behavior,
      block,
      inline
    });
  }
};

/**
 * Prevent body scroll during modal/overlay display
 */
export const bodyScrollLock = {
  locked: false,
  scrollPosition: 0,

  lock() {
    if (this.locked) return;

    this.scrollPosition = window.pageYOffset;
    document.body.style.position = 'fixed';
    document.body.style.top = `-${this.scrollPosition}px`;
    document.body.style.width = '100%';
    document.body.classList.add('scroll-locked');

    this.locked = true;
  },

  unlock() {
    if (!this.locked) return;

    document.body.style.position = '';
    document.body.style.top = '';
    document.body.style.width = '';
    document.body.classList.remove('scroll-locked');

    window.scrollTo(0, this.scrollPosition);

    this.locked = false;
  }
};

/**
 * Mobile-safe focus management
 */
export const mobileFocus = {
  focusedElement: null,

  focus(element, options = {}) {
    if (!element) return;

    const { preventScroll = false, delay = 100 } = options;

    this.focusedElement = element;

    // Delay focus on mobile to avoid issues with virtual keyboard
    if (delay > 0) {
      setTimeout(() => {
        element.focus({ preventScroll });
      }, delay);
    } else {
      element.focus({ preventScroll });
    }
  },

  blur() {
    if (this.focusedElement) {
      this.focusedElement.blur();
      this.focusedElement = null;
    }
  },

  // Auto-scroll to focused element with mobile considerations
  scrollToFocused(offset = 20) {
    if (this.focusedElement) {
      scrollToElement(this.focusedElement, {
        offset,
        isMobile: true
      });
    }
  }
};

/**
 * Performance monitoring for mobile
 */
export const mobilePerformance = {
  // Frame rate monitoring
  measureFPS(duration = 1000) {
    return new Promise((resolve) => {
      let frames = 0;
      const startTime = performance.now();

      const countFrame = () => {
        frames++;
        const currentTime = performance.now();

        if (currentTime - startTime < duration) {
          requestAnimationFrame(countFrame);
        } else {
          const fps = Math.round((frames * 1000) / (currentTime - startTime));
          resolve(fps);
        }
      };

      requestAnimationFrame(countFrame);
    });
  },

  // Memory usage (if available)
  getMemoryUsage() {
    if ('memory' in performance) {
      return {
        used: Math.round(performance.memory.usedJSHeapSize / 1048576), // MB
        total: Math.round(performance.memory.totalJSHeapSize / 1048576), // MB
        limit: Math.round(performance.memory.jsHeapSizeLimit / 1048576) // MB
      };
    }
    return null;
  },

  // Connection quality
  getConnectionInfo() {
    if ('connection' in navigator) {
      return {
        effectiveType: navigator.connection.effectiveType,
        downlink: navigator.connection.downlink,
        rtt: navigator.connection.rtt,
        saveData: navigator.connection.saveData
      };
    }
    return null;
  }
};

export default {
  TOUCH_TARGET,
  getTouchTargetClasses,
  MOBILE_ANIMATIONS,
  getResponsiveAnimation,
  MOBILE_SPACING,
  getMobileSpacing,
  getResponsiveTextSize,
  PullToRefresh,
  VirtualKeyboardHandler,
  scrollToElement,
  bodyScrollLock,
  mobileFocus,
  mobilePerformance
};