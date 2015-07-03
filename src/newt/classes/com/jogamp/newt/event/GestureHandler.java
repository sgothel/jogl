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

import jogamp.newt.Debug;

/**
 * Generic gesture handler interface designed to allow pass-through
 * filtering of {@link InputEvent}s.
 * <p>
 * To avoid negative impact on event processing,
 * implementation shall restrict computation as much as possible
 * and only within it's appropriate gesture states.
 * </p>
 * <p>
 * To allow custom user events, other than the <i>normal</i> {@link InputEvent}s,
 * a user may return a {@link GestureEvent} in it's implementation.
 * </p>
 */
public interface GestureHandler {
    public static final boolean DEBUG = Debug.debug("Window.MouseEvent");

    /** A custom gesture event */
    @SuppressWarnings("serial")
    public static class GestureEvent extends InputEvent {
        /** A gesture has been detected. */
        public static final short EVENT_GESTURE_DETECTED = 400;

        private final GestureHandler handler;
        private final InputEvent ie;

        /**
         * Creates a gesture event with default type {@link #EVENT_GESTURE_DETECTED}.
         *
         * @param source
         * @param when
         * @param modifiers
         * @param handler
         * @param trigger TODO
         */
        public GestureEvent(final Object source, final long when, final int modifiers, final GestureHandler handler, final InputEvent trigger) {
            super(EVENT_GESTURE_DETECTED, source, when, modifiers);
            this.handler = handler;
            this.ie = trigger;
        }

        /**
         * Creates a gesture event with custom <i>event_type</i> !
         * @param event_type must lie within [400..599]
         * @param source
         * @param when
         * @param modifiers
         * @param handler
         * @param trigger TODO
         */
        public GestureEvent(final short event_type, final Object source, final long when, final int modifiers, final GestureHandler handler, final InputEvent trigger) {
            super(event_type, source, when, modifiers);
            this.handler = handler;
            this.ie = trigger;
        }

        /** Return the {@link GestureHandler}, which produced the event. */
        public final GestureHandler getHandler() { return handler; }

        /** Triggering {@link InputEvent} */
        public final InputEvent getTrigger() { return ie; }

        public String toString() {
            return "GestureEvent[handler "+handler+"]";
        }
    }

    /**
     * Listener for {@link GestureEvent}s.
     *
     * @see GestureEvent
     */
    public static interface GestureListener extends NEWTEventListener
    {
        /** {@link GestureHandler} {@link GestureHandler#hasGesture() has detected} the gesture. */
        public void gestureDetected(GestureEvent gh);
    }

    /**
     * Clears state of handler, i.e. resets all states incl. previous detected gesture.
     * @param clearStarted if true, also clears {@link #isWithinGesture() started} state,
     *                     otherwise stay within gesture - if appropriate.
     *                     Staying within a gesture allows fluent continuous gesture sequence,
     *                     e.g. for scrolling.
     */
    public void clear(boolean clearStarted);

    /**
     * Returns true if a previous {@link #process(InputEvent)} command produced a gesture,
     * which has not been {@link #clear(boolean) cleared}.
     * Otherwise returns false.
     */
    public boolean hasGesture();

    /**
     * Returns the corresponding {@link InputEvent} for the gesture as detected by
     * a previous {@link #process(InputEvent)}, which has not been {@link #clear(boolean) cleared}.
     * Otherwise returns null.
     * <p>
     * Only implemented for gestures mapping to {@link InputEvent}s.
     * </p>
     */
    public InputEvent getGestureEvent();

    /**
     * Returns true if within a gesture as detected by a previous {@link #process(InputEvent)} command,
     * which has not been {@link #clear(boolean) cleared}.
     * Otherwise returns false.
     */
    public boolean isWithinGesture();

    /**
     * Process the given {@link InputEvent} and returns true if it produced the gesture.
     * Otherwise returns false.
     * <p>
     * If a gesture was already detected previously and has not been cleared,
     * method does not process the event and returns true.
     * </p>
     * <p>
     * Besides validation of the event's details,
     * the handler may also validate the {@link InputEvent.InputClass} and/or {@link InputEvent.InputType}.
     * </p>
     */
    public boolean process(InputEvent e);
}
