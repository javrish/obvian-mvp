# P3Net UI - Comprehensive Responsive Design Implementation

## Overview

This document outlines the comprehensive responsive design system implemented for the P3Net UI components, ensuring seamless functionality across desktop, tablet, and mobile devices.

## Architecture

### Mobile-First Approach
- **Breakpoints**: 320px (mobile) → 768px (tablet) → 1024px (desktop) → 1440px+ (large desktop)
- **Progressive Enhancement**: Start with mobile constraints, progressively add features for larger screens
- **Performance Optimization**: Reduced animations, lazy loading, and optimized touch interactions on mobile

### Core Components

#### 1. Responsive Utilities (`/hooks/useResponsive.js`)
```javascript
// Breakpoint detection and responsive utilities
const responsive = useResponsive();
// Returns: isMobile, isTablet, isDesktop, width, height, etc.
```

**Features:**
- Real-time breakpoint detection
- Device capability detection (touch, orientation)
- Utility functions for responsive styling
- Grid, spacing, and text sizing helpers

#### 2. Touch Gesture Support (`/hooks/useTouch.js`)
```javascript
// Swipe, pinch, tap, and long press detection
const swipeHandlers = useSwipe({
  onSwipeLeft: () => navigateNext(),
  onSwipeRight: () => navigatePrev()
});
```

**Gestures Supported:**
- **Swipe**: Left/right navigation with velocity and distance thresholds
- **Pinch**: Zoom in/out with scale detection
- **Tap**: Single tap, double tap, long press
- **Touch Drag**: Drag and drop with touch optimization

#### 3. Responsive Layout System (`/components/ResponsiveLayout.js`)
```javascript
// Adaptive layout wrapper with sidebar, header, footer support
<ResponsiveLayout
  header={<HeaderComponent />}
  sidebar={<MobileSidebar />}
  enableSwipeGestures={true}
>
  <MainContent />
</ResponsiveLayout>
```

**Features:**
- Automatic layout adaptation based on breakpoint
- Mobile drawer navigation with overlay
- Touch gesture integration
- Breakpoint change notifications

## Component-Specific Responsive Implementation

### PetriProgressStepper
**Mobile (320px - 767px):**
- Vertical compact layout with collapsible details
- Touch-friendly navigation dots
- Simplified performance indicators
- Swipe navigation between steps

**Tablet (768px - 1023px):**
- Horizontal layout with adaptive sizing
- Medium-sized touch targets
- Condensed descriptions

**Desktop (1024px+):**
- Full horizontal layout with performance metrics
- Detailed step descriptions
- Hover effects and animations

### PetriWorkspaceContainer
**Mobile:**
- Single-panel workflow with navigation drawer
- Floating action button for step execution
- Panel switching with swipe gestures
- Collapsible controls and simplified UI

**Tablet:**
- Dual-panel with flexible stacking
- Touch-optimized controls
- Adaptive content prioritization

**Desktop:**
- Full dual-panel layout (30% controls, 70% visualization)
- Enhanced controls and sidebars
- Real-time cross-highlighting
- Advanced settings and diagnostics

### DualGraphView
**Mobile:**
- Single-panel view with toggle between Petri Net and DAG
- Swipe navigation between views
- Simplified controls
- Touch-optimized visualization size (h-64)

**Tablet:**
- Stacked vertical panels or side-by-side
- Medium visualization size (h-72 to h-80)
- Reduced advanced features

**Desktop:**
- Side-by-side dual visualization
- Full cross-highlighting capabilities
- Advanced mapping statistics
- Large visualization size (h-80 to h-96)

## State Management

### Responsive State Hook (`/hooks/useResponsiveState.js`)
```javascript
const { state, setState, toggleValue, cycleValue } = useResponsiveState(
  initialState,
  {
    persist: true,
    mobileDefaults: { compactMode: true },
    tabletDefaults: { panelLayout: 'stacked' },
    desktopDefaults: { panelLayout: 'split' }
  }
);
```

**Features:**
- Breakpoint-aware state management
- Automatic state adaptation on breakpoint changes
- localStorage persistence
- Transition animations

### Panel Management
```javascript
const {
  currentPanel,
  setCurrentPanel,
  navigatePanel,
  cyclePanelLayout
} = useResponsivePanels(panels);
```

**Capabilities:**
- Mobile: Single panel with navigation
- Tablet/Desktop: Multiple panels with flexibility
- Swipe navigation integration
- State persistence across sessions

## Mobile Interaction Patterns (`/utils/mobileInteractions.js`)

### Touch Target Optimization
- **Minimum**: 44px × 44px (iOS HIG compliance)
- **Comfortable**: 48px × 48px
- **Spacious**: 56px × 56px

### Performance Optimizations
```javascript
// Reduced animations for mobile
const animation = getResponsiveAnimation('slide', isMobile);
// 30% faster animations on mobile devices
```

### Virtual Keyboard Handling
```javascript
const keyboardHandler = new VirtualKeyboardHandler({
  threshold: 150,
  adjustViewport: true
});
```

### Pull-to-Refresh
```javascript
const pullToRefresh = new PullToRefresh(element, onRefresh, {
  threshold: 100,
  maxPullDistance: 150
});
```

## Accessibility Features

### Screen Reader Support
- Proper ARIA labels for responsive state changes
- Screen reader announcements for layout changes
- Semantic HTML structure maintained across breakpoints

### Keyboard Navigation
- Tab order preserved across responsive layouts
- Keyboard shortcuts work across all breakpoints
- Focus management for modal/drawer states

### High Contrast and Reduced Motion
```javascript
const { preferences } = useResponsivePreferences();
// Respects user's motion preferences
// Supports high contrast mode
```

## Performance Considerations

### Mobile Optimizations
- **Lazy Loading**: Heavy visualizations loaded on demand
- **Reduced Animations**: 30% faster, simplified transitions
- **Touch Optimization**: Debounced touch events, passive listeners
- **Bundle Optimization**: Conditional feature loading

### Memory Management
- Component unmounting on breakpoint changes
- Event listener cleanup
- State persistence with size limits

### Network Awareness
```javascript
const connectionInfo = mobilePerformance.getConnectionInfo();
// Adapts features based on connection quality
```

## Testing Strategy

### Responsive Testing
1. **Breakpoint Testing**: All major breakpoints (320px, 768px, 1024px, 1440px)
2. **Device Testing**: iOS Safari, Chrome Mobile, Samsung Internet
3. **Orientation Testing**: Portrait and landscape modes
4. **Touch Testing**: Gesture accuracy and performance
5. **Performance Testing**: Frame rate, memory usage, network usage

### Cross-Browser Compatibility
- **Mobile**: iOS Safari 14+, Chrome Mobile 90+, Samsung Internet 14+
- **Desktop**: Chrome 90+, Firefox 88+, Safari 14+, Edge 90+

## Usage Examples

### Basic Responsive Component
```javascript
import { useResponsive } from '../hooks/useResponsive';
import { getTouchTargetClasses } from '../utils/mobileInteractions';

const MyComponent = () => {
  const responsive = useResponsive();

  return (
    <div className={responsive.getContainerClasses()}>
      <button
        className={`
          ${getTouchTargetClasses('comfortable')}
          ${responsive.getTextSizeClasses({
            mobile: 'text-sm',
            desktop: 'text-base'
          })}
        `}
      >
        {responsive.isMobile ? 'Tap' : 'Click'} Me
      </button>
    </div>
  );
};
```

### Gesture-Enabled Navigation
```javascript
import { useSwipe } from '../hooks/useTouch';

const SwipeablePanel = ({ onNext, onPrev }) => {
  const swipeHandlers = useSwipe({
    onSwipeLeft: onNext,
    onSwipeRight: onPrev
  });

  return (
    <div {...swipeHandlers} className="touch-manipulation">
      {/* Panel content */}
    </div>
  );
};
```

### Responsive State Management
```javascript
import { useResponsiveState } from '../hooks/useResponsiveState';

const ResponsiveComponent = () => {
  const { state, setState, toggleValue } = useResponsiveState(
    { sidebarOpen: false },
    {
      mobileDefaults: { sidebarOpen: false },
      desktopDefaults: { sidebarOpen: true }
    }
  );

  return (
    <div>
      {/* Component adapts automatically to breakpoint changes */}
    </div>
  );
};
```

## Best Practices

### Development Guidelines
1. **Mobile First**: Always start with mobile constraints
2. **Progressive Enhancement**: Add features for larger screens
3. **Touch Targets**: Minimum 44px for interactive elements
4. **Performance**: Monitor frame rates and memory usage
5. **Accessibility**: Test with screen readers and keyboard navigation

### Design Guidelines
1. **Content Priority**: Most important content first on mobile
2. **Gesture Discovery**: Provide visual hints for swipe interactions
3. **Feedback**: Immediate visual feedback for touch interactions
4. **Error Handling**: Graceful degradation for unsupported features

### Testing Guidelines
1. **Real Devices**: Test on actual mobile devices, not just emulators
2. **Network Conditions**: Test on various network speeds
3. **Usage Patterns**: Consider one-handed mobile usage
4. **Battery Impact**: Monitor battery usage during intensive operations

## File Structure
```
frontend/src/
├── hooks/
│   ├── useResponsive.js        # Breakpoint detection and utilities
│   ├── useTouch.js             # Touch gesture handling
│   └── useResponsiveState.js   # Responsive state management
├── components/
│   ├── ResponsiveLayout.js     # Layout wrapper with mobile support
│   ├── PetriProgressStepper.js # Responsive progress indicator
│   ├── PetriWorkspaceContainer.js # Main responsive container
│   ├── PetriNetWorkflow.js     # Responsive workflow component
│   └── DualGraphView.js        # Responsive dual visualization
├── utils/
│   └── mobileInteractions.js   # Mobile interaction patterns
└── docs/
    └── RESPONSIVE_DESIGN.md    # This documentation
```

## Migration Guide

### Updating Existing Components
1. Import responsive utilities: `import { useResponsive } from '../hooks/useResponsive'`
2. Add breakpoint detection: `const responsive = useResponsive()`
3. Apply conditional classes: `className={responsive.isMobile ? 'mobile-class' : 'desktop-class'}`
4. Add touch targets: `className={getTouchTargetClasses()}`
5. Implement gesture support where appropriate

### Common Patterns
```javascript
// Conditional rendering based on breakpoint
{responsive.isMobile ? <MobileComponent /> : <DesktopComponent />}

// Responsive styling
className={`base-classes ${
  responsive.isMobile ? 'mobile-specific' :
  responsive.isTablet ? 'tablet-specific' :
  'desktop-specific'
}`}

// Touch-optimized interactions
<button
  className={getTouchTargetClasses()}
  onClick={handleClick}
  onTouchStart={responsive.isTouchDevice ? handleTouchStart : undefined}
>
```

This comprehensive responsive design system ensures that P3Net UI components provide an optimal user experience across all device types and screen sizes, with particular attention to mobile usability and performance.