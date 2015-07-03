/**
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.newt.event;

import com.jogamp.nativewindow.NativeSurface;

import com.jogamp.newt.event.MouseEvent.PointerClass;

import jogamp.newt.Debug;

/**
 * 2 pointer zoom, a.k.a. <i>pinch to zoom</i>, gesture handler processing {@link MouseEvent}s
 * while producing {@link ZoomEvent}s if gesture is completed.
 * <p>
 * Zoom value lies within [0..2], with 1 as <i>1:1</i>.
 * </p>
 * <pre>
 *   - choosing the smallest surface edge (width/height -> x/y)
 *   - tolerating other fingers to be pressed and hence user to add functionality (scale, ..)
 * </pre>
 */
public class PinchToZoomGesture implements GestureHandler {
    public static final boolean DEBUG = Debug.debug("Window.MouseEvent");

    /** A {@link GestureHandler.GestureEvent} denominating zoom. */
    @SuppressWarnings("serial")
    public static class ZoomEvent extends GestureEvent {
        private final float zoom;
        private final float delta;
        private final float scale;
        public ZoomEvent(final Object source, final long when, final int modifiers, final GestureHandler handler, final MouseEvent pe,
                         final float zoom, final float delta, final float scale) {
            super(source, when, modifiers, handler, pe);
            this.zoom = zoom;
            this.delta = delta;
            this.scale = scale;
        }
        /** Zoom value lies within [0..2], with 1 as <i>1:1</i>. */
        public final float getZoom() { return zoom; }
        /** Delta to last zoom value lies within [-1..1]. */
        public final float getDelta() { return delta; }
        /**
         * Returns the scale used to determine the {@link #getZoom() zoom}
         * and hence it's {@link #getDelta() delta} value,
         * which semantics depends on the {@link #getPointerType() pointer type's} {@link PointerClass}.
         * <p>
         * For {@link PointerClass#Offscreen}, the scale is usually <code>1.0f</code> and denominates
         * an abstract value without association to a physical value.
         * </p>
         * <p>
         * For {@link PointerClass#Onscreen}, the scale varies and denominates
         * the divisor of the distance the finger[s] have moved on the screen.
         * Hence <code>scale * delta</code> reproduces the screen distance in pixels the finger[s] have moved.
         * </p>
         */
        public final float getScale() { return scale; }

        public final String toString() {
            return "ZoomEvent[zoom "+zoom+", delta "+delta+", scale "+scale+", trigger "+getTrigger()+", handler "+getHandler()+"]";
        }
    }

    private final NativeSurface surface;
    private final boolean allowMorePointer;
    private float zoom;
    private int zoomLastEdgeDist;
    private boolean zoomFirstTouch;
    private boolean zoomMode;
    private ZoomEvent zoomEvent;
    private final short[] pIds = new short[] { -1, -1 };

    /**
     * @param surface the {@link NativeSurface}, which size is used to compute the relative zoom factor
     * @param allowMorePointer if false, allow only 2 pressed pointers (safe and recommended), otherwise accept other pointer to be pressed.
     */
    public PinchToZoomGesture(final NativeSurface surface, final boolean allowMorePointer) {
        clear(true);
        this.surface = surface;
        this.allowMorePointer = allowMorePointer;
        this.zoom = 1f;
    }

    @Override
    public String toString() {
        return "PinchZoom[1stTouch "+zoomFirstTouch+", in "+isWithinGesture()+", has "+(null!=zoomEvent)+", zoom "+zoom+"]";
    }

    private int gesturePointers(final MouseEvent e, final int excludeIndex) {
        int j = 0;
        for(int i=e.getPointerCount()-1; i>=0; i--) {
            if( excludeIndex != i ) {
                final int id = e.getPointerId(i);
                if( pIds[0] == id || pIds[1] == id ) {
                    j++;
                }
            }
        }
        return j;
    }

    @Override
    public void clear(final boolean clearStarted) {
        zoomEvent = null;
        if( clearStarted ) {
            zoomLastEdgeDist = 0;
            zoomFirstTouch = true;
            zoomMode = false;
            pIds[0] = -1;
            pIds[1] = -1;
        }
    }

    @Override
    public boolean isWithinGesture() {
        return zoomMode;
    }

    @Override
    public boolean hasGesture() {
        return null != zoomEvent;
    }

    @Override
    public InputEvent getGestureEvent() {
        return zoomEvent;
    }

    /** Zoom value lies within [0..2], with 1 as <i>1:1</i>. */
    public final float getZoom() {
        return zoom;
    }
    /** Set zoom value within [0..2], with 1 as <i>1:1</i>. */
    public final void setZoom(final float zoom) {
        this.zoom=zoom;
    }

    @Override
    public boolean process(final InputEvent in) {
        if( null != zoomEvent || !(in instanceof MouseEvent) ) {
            return true;
        }
        final MouseEvent pe = (MouseEvent)in;
        final int pointerDownCount = pe.getPointerCount();

        if( pe.getPointerType(0).getPointerClass() != MouseEvent.PointerClass.Onscreen ||
            ( !allowMorePointer && pointerDownCount > 2 ) ) {
            return false;
        }

        final int eventType = pe.getEventType();
        final boolean useY = surface.getSurfaceWidth() >= surface.getSurfaceHeight(); // use smallest dimension
        switch ( eventType ) {
            case MouseEvent.EVENT_MOUSE_PRESSED: {
                if( 1 == pointerDownCount ) {
                    pIds[0] = pe.getPointerId(0);
                    pIds[1] = -1;
                } else if ( 2 <= pointerDownCount ) { // && 1 == gesturePointers(pe, 0) /* w/o pressed pointer */) {
                    pIds[0] = pe.getPointerId(0);
                    pIds[1] = pe.getPointerId(1);
                }
                if(DEBUG) {
                    System.err.println("XXX1: id0 "+pIds[0]+" -> idx0 "+0+", id1 "+pIds[1]+" -> idx1 "+1);
                    System.err.println(this+".pressed: down "+pointerDownCount+", gPtr "+gesturePointers(pe, -1)+", event "+pe);
                }
            } break;

            case MouseEvent.EVENT_MOUSE_RELEASED: {
                final int gPtr = gesturePointers(pe, 0); // w/o lifted pointer
                if ( 1 == gPtr ) {
                    zoomFirstTouch = true;
                    zoomMode = false;
                } else if( 0 == gPtr ) {
                    // all lifted
                    clear(true);
                }
                if(DEBUG) {
                    System.err.println(this+".released: down "+pointerDownCount+", gPtr "+gPtr+", event "+pe);
                }
            } break;

            case MouseEvent.EVENT_MOUSE_DRAGGED: {
                if( 2 <= pointerDownCount ) {
                    final int gPtr = gesturePointers(pe, -1);
                    if( 2 == gPtr ) {
                        // same pointers
                        final int p0Idx = pe.getPointerIdx(pIds[0]);
                        final int p1Idx = pe.getPointerIdx(pIds[1]);
                        if( 0 <= p0Idx && 0 <= p1Idx ) {
                            final int edge0 = useY ? pe.getY(p0Idx) : pe.getX(p0Idx);
                            final int edge1 = useY ? pe.getY(p1Idx) : pe.getX(p1Idx);
                            // Diff. 1:1 Zoom: finger-distance to screen-coord
                            if(zoomFirstTouch) {
                                zoomLastEdgeDist = Math.abs(edge0-edge1);
                                zoomFirstTouch=false;
                                zoomMode = true;
                            } else if( zoomMode ) {
                                final int d = Math.abs(edge0-edge1);
                                final int dd = d - zoomLastEdgeDist;
                                final float screenEdge = useY ? surface.getSurfaceHeight() : surface.getSurfaceWidth();
                                final float delta = dd / screenEdge; // [-1..1]
                                if(DEBUG) {
                                    System.err.println("XXX2: id0 "+pIds[0]+" -> idx0 "+p0Idx+", id1 "+pIds[1]+" -> idx1 "+p1Idx);
                                    System.err.println("XXX3: d "+d+", ld "+zoomLastEdgeDist+", dd "+dd+", screen "+screenEdge+" -> incr "+delta+", zoom "+zoom+" -> "+(zoom+delta));
                                }
                                zoom += delta;
                                // clip value
                                if( 2f < zoom ) {
                                    zoom = 2f;
                                } else if( 0 > zoom ) {
                                    zoom = 0;
                                }
                                zoomLastEdgeDist = d;
                                zoomEvent = new ZoomEvent(pe.getSource(), pe.getWhen(), pe.getModifiers(), this, pe, zoom, delta, screenEdge);
                            }
                        }
                    }
                    if(DEBUG) {
                        System.err.println(this+".dragged: down "+pointerDownCount+", gPtr "+gPtr+", event "+pe);
                    }
                }
            } break;

            default:
        }
        return null != zoomEvent;
    }
}
