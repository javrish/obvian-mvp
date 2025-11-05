import { useState, useEffect, useRef, useCallback } from 'react';

/**
 * Touch gesture detection and handling utilities for mobile interactions
 * Provides swipe, pinch, tap, and long press gesture recognition
 */

const defaultSwipeThreshold = {
  distance: 50,        // Minimum distance for swipe
  velocity: 0.3,       // Minimum velocity (pixels/ms)
  maxTime: 300,        // Maximum time for swipe gesture
  maxVerticalDeviation: 100  // Maximum vertical deviation for horizontal swipes
};

const defaultPinchThreshold = {
  scale: 0.1,          // Minimum scale change to register pinch
  minDistance: 20      // Minimum distance between fingers
};

const defaultTapThreshold = {
  maxTime: 200,        // Maximum time for tap
  maxDistance: 10      // Maximum movement for tap
};

const defaultLongPressThreshold = {
  time: 500,           // Time for long press
  maxDistance: 10      // Maximum movement during long press
};

/**
 * Calculate distance between two points
 */
const getDistance = (point1, point2) => {
  return Math.sqrt(
    Math.pow(point2.x - point1.x, 2) + Math.pow(point2.y - point1.y, 2)
  );
};

/**
 * Calculate angle between two points
 */
const getAngle = (point1, point2) => {
  return Math.atan2(point2.y - point1.y, point2.x - point1.x) * 180 / Math.PI;
};

/**
 * Get touch position from event
 */
const getTouchPosition = (touch) => ({
  x: touch.clientX,
  y: touch.clientY
});

/**
 * Hook for swipe gesture detection
 */
export const useSwipe = (callbacks = {}, options = {}) => {
  const {
    onSwipeLeft,
    onSwipeRight,
    onSwipeUp,
    onSwipeDown,
    onSwipeStart,
    onSwipeMove,
    onSwipeEnd
  } = callbacks;

  const threshold = { ...defaultSwipeThreshold, ...options };
  const [swipeData, setSwipeData] = useState(null);

  const handleTouchStart = useCallback((e) => {
    const touch = e.touches[0];
    const startData = {
      x: touch.clientX,
      y: touch.clientY,
      time: Date.now()
    };

    setSwipeData(startData);

    if (onSwipeStart) {
      onSwipeStart(startData);
    }
  }, [onSwipeStart]);

  const handleTouchMove = useCallback((e) => {
    if (!swipeData) return;

    const touch = e.touches[0];
    const currentData = {
      x: touch.clientX,
      y: touch.clientY,
      deltaX: touch.clientX - swipeData.x,
      deltaY: touch.clientY - swipeData.y,
      time: Date.now()
    };

    if (onSwipeMove) {
      onSwipeMove(currentData);
    }
  }, [swipeData, onSwipeMove]);

  const handleTouchEnd = useCallback((e) => {
    if (!swipeData) return;

    const touch = e.changedTouches[0];
    const endTime = Date.now();
    const deltaTime = endTime - swipeData.time;
    const deltaX = touch.clientX - swipeData.x;
    const deltaY = touch.clientY - swipeData.y;

    const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    const velocity = distance / deltaTime;

    const endData = {
      deltaX,
      deltaY,
      deltaTime,
      distance,
      velocity
    };

    // Check if it's a valid swipe
    if (
      distance >= threshold.distance &&
      velocity >= threshold.velocity &&
      deltaTime <= threshold.maxTime
    ) {
      // Determine swipe direction
      const absX = Math.abs(deltaX);
      const absY = Math.abs(deltaY);

      if (absX > absY) {
        // Horizontal swipe
        if (absY <= threshold.maxVerticalDeviation) {
          if (deltaX > 0 && onSwipeRight) {
            onSwipeRight(endData);
          } else if (deltaX < 0 && onSwipeLeft) {
            onSwipeLeft(endData);
          }
        }
      } else {
        // Vertical swipe
        if (deltaY > 0 && onSwipeDown) {
          onSwipeDown(endData);
        } else if (deltaY < 0 && onSwipeUp) {
          onSwipeUp(endData);
        }
      }
    }

    if (onSwipeEnd) {
      onSwipeEnd(endData);
    }

    setSwipeData(null);
  }, [swipeData, threshold, onSwipeLeft, onSwipeRight, onSwipeUp, onSwipeDown, onSwipeEnd]);

  return {
    onTouchStart: handleTouchStart,
    onTouchMove: handleTouchMove,
    onTouchEnd: handleTouchEnd,
    swipeData
  };
};

/**
 * Hook for pinch/zoom gesture detection
 */
export const usePinch = (callbacks = {}, options = {}) => {
  const { onPinchStart, onPinchMove, onPinchEnd } = callbacks;
  const threshold = { ...defaultPinchThreshold, ...options };
  const [pinchData, setPinchData] = useState(null);

  const handleTouchStart = useCallback((e) => {
    if (e.touches.length === 2) {
      const touch1 = getTouchPosition(e.touches[0]);
      const touch2 = getTouchPosition(e.touches[1]);
      const distance = getDistance(touch1, touch2);

      const startData = {
        touch1,
        touch2,
        distance,
        scale: 1,
        center: {
          x: (touch1.x + touch2.x) / 2,
          y: (touch1.y + touch2.y) / 2
        }
      };

      setPinchData(startData);

      if (onPinchStart) {
        onPinchStart(startData);
      }
    }
  }, [onPinchStart]);

  const handleTouchMove = useCallback((e) => {
    if (e.touches.length === 2 && pinchData) {
      const touch1 = getTouchPosition(e.touches[0]);
      const touch2 = getTouchPosition(e.touches[1]);
      const distance = getDistance(touch1, touch2);
      const scale = distance / pinchData.distance;

      if (Math.abs(scale - 1) >= threshold.scale) {
        const moveData = {
          ...pinchData,
          currentDistance: distance,
          scale,
          center: {
            x: (touch1.x + touch2.x) / 2,
            y: (touch1.y + touch2.y) / 2
          }
        };

        if (onPinchMove) {
          onPinchMove(moveData);
        }
      }
    }
  }, [pinchData, threshold.scale, onPinchMove]);

  const handleTouchEnd = useCallback((e) => {
    if (pinchData && e.touches.length < 2) {
      if (onPinchEnd) {
        onPinchEnd(pinchData);
      }
      setPinchData(null);
    }
  }, [pinchData, onPinchEnd]);

  return {
    onTouchStart: handleTouchStart,
    onTouchMove: handleTouchMove,
    onTouchEnd: handleTouchEnd,
    pinchData
  };
};

/**
 * Hook for tap gesture detection (including long press)
 */
export const useTap = (callbacks = {}, options = {}) => {
  const { onTap, onDoubleTap, onLongPress } = callbacks;
  const tapThreshold = { ...defaultTapThreshold, ...options.tap };
  const longPressThreshold = { ...defaultLongPressThreshold, ...options.longPress };

  const [tapData, setTapData] = useState(null);
  const [lastTap, setLastTap] = useState(null);
  const longPressTimer = useRef(null);

  const handleTouchStart = useCallback((e) => {
    const touch = e.touches[0];
    const startData = {
      x: touch.clientX,
      y: touch.clientY,
      time: Date.now()
    };

    setTapData(startData);

    // Start long press timer
    if (onLongPress) {
      longPressTimer.current = setTimeout(() => {
        if (tapData) {
          onLongPress(startData);
          setTapData(null);
        }
      }, longPressThreshold.time);
    }
  }, [onLongPress, longPressThreshold.time, tapData]);

  const handleTouchMove = useCallback((e) => {
    if (!tapData) return;

    const touch = e.touches[0];
    const distance = getDistance(
      { x: tapData.x, y: tapData.y },
      { x: touch.clientX, y: touch.clientY }
    );

    // Cancel long press if moved too much
    if (distance > longPressThreshold.maxDistance) {
      if (longPressTimer.current) {
        clearTimeout(longPressTimer.current);
        longPressTimer.current = null;
      }
    }

    // Cancel tap if moved too much
    if (distance > tapThreshold.maxDistance) {
      setTapData(null);
    }
  }, [tapData, tapThreshold.maxDistance, longPressThreshold.maxDistance]);

  const handleTouchEnd = useCallback((e) => {
    if (!tapData) return;

    // Clear long press timer
    if (longPressTimer.current) {
      clearTimeout(longPressTimer.current);
      longPressTimer.current = null;
    }

    const touch = e.changedTouches[0];
    const endTime = Date.now();
    const deltaTime = endTime - tapData.time;
    const distance = getDistance(
      { x: tapData.x, y: tapData.y },
      { x: touch.clientX, y: touch.clientY }
    );

    // Check if it's a valid tap
    if (deltaTime <= tapThreshold.maxTime && distance <= tapThreshold.maxDistance) {
      const tapInfo = {
        x: touch.clientX,
        y: touch.clientY,
        time: endTime
      };

      // Check for double tap
      if (lastTap && onDoubleTap) {
        const timeDiff = endTime - lastTap.time;
        const distDiff = getDistance(
          { x: lastTap.x, y: lastTap.y },
          { x: touch.clientX, y: touch.clientY }
        );

        if (timeDiff <= 300 && distDiff <= 30) {
          onDoubleTap(tapInfo);
          setLastTap(null);
          setTapData(null);
          return;
        }
      }

      // Single tap
      if (onTap) {
        onTap(tapInfo);
      }

      setLastTap(tapInfo);
    }

    setTapData(null);
  }, [tapData, tapThreshold, lastTap, onTap, onDoubleTap]);

  // Cleanup timer on unmount
  useEffect(() => {
    return () => {
      if (longPressTimer.current) {
        clearTimeout(longPressTimer.current);
      }
    };
  }, []);

  return {
    onTouchStart: handleTouchStart,
    onTouchMove: handleTouchMove,
    onTouchEnd: handleTouchEnd,
    tapData
  };
};

/**
 * Combined touch gesture hook with all gestures
 */
export const useTouch = (callbacks = {}, options = {}) => {
  const swipeHandlers = useSwipe(callbacks, options.swipe);
  const pinchHandlers = usePinch(callbacks, options.pinch);
  const tapHandlers = useTap(callbacks, options.tap);

  const handleTouchStart = useCallback((e) => {
    swipeHandlers.onTouchStart(e);
    pinchHandlers.onTouchStart(e);
    tapHandlers.onTouchStart(e);
  }, [swipeHandlers.onTouchStart, pinchHandlers.onTouchStart, tapHandlers.onTouchStart]);

  const handleTouchMove = useCallback((e) => {
    swipeHandlers.onTouchMove(e);
    pinchHandlers.onTouchMove(e);
    tapHandlers.onTouchMove(e);
  }, [swipeHandlers.onTouchMove, pinchHandlers.onTouchMove, tapHandlers.onTouchMove]);

  const handleTouchEnd = useCallback((e) => {
    swipeHandlers.onTouchEnd(e);
    pinchHandlers.onTouchEnd(e);
    tapHandlers.onTouchEnd(e);
  }, [swipeHandlers.onTouchEnd, pinchHandlers.onTouchEnd, tapHandlers.onTouchEnd]);

  return {
    onTouchStart: handleTouchStart,
    onTouchMove: handleTouchMove,
    onTouchEnd: handleTouchEnd,
    swipeData: swipeHandlers.swipeData,
    pinchData: pinchHandlers.pinchData,
    tapData: tapHandlers.tapData
  };
};

/**
 * Hook for touch-friendly drag and drop
 */
export const useTouchDrag = (callbacks = {}, options = {}) => {
  const { onDragStart, onDragMove, onDragEnd } = callbacks;
  const [dragData, setDragData] = useState(null);
  const elementRef = useRef(null);

  const threshold = {
    distance: 5,  // Minimum distance to start drag
    ...options
  };

  const handleTouchStart = useCallback((e) => {
    const touch = e.touches[0];
    const startData = {
      startX: touch.clientX,
      startY: touch.clientY,
      currentX: touch.clientX,
      currentY: touch.clientY,
      deltaX: 0,
      deltaY: 0,
      time: Date.now(),
      isDragging: false
    };

    setDragData(startData);
  }, []);

  const handleTouchMove = useCallback((e) => {
    if (!dragData) return;

    e.preventDefault(); // Prevent scrolling

    const touch = e.touches[0];
    const distance = getDistance(
      { x: dragData.startX, y: dragData.startY },
      { x: touch.clientX, y: touch.clientY }
    );

    const moveData = {
      ...dragData,
      currentX: touch.clientX,
      currentY: touch.clientY,
      deltaX: touch.clientX - dragData.startX,
      deltaY: touch.clientY - dragData.startY
    };

    // Start dragging if threshold is met
    if (!dragData.isDragging && distance >= threshold.distance) {
      moveData.isDragging = true;
      if (onDragStart) {
        onDragStart(moveData);
      }
    }

    if (moveData.isDragging && onDragMove) {
      onDragMove(moveData);
    }

    setDragData(moveData);
  }, [dragData, threshold.distance, onDragStart, onDragMove]);

  const handleTouchEnd = useCallback((e) => {
    if (!dragData) return;

    if (dragData.isDragging && onDragEnd) {
      onDragEnd(dragData);
    }

    setDragData(null);
  }, [dragData, onDragEnd]);

  return {
    onTouchStart: handleTouchStart,
    onTouchMove: handleTouchMove,
    onTouchEnd: handleTouchEnd,
    dragData,
    ref: elementRef
  };
};

export default useTouch;