/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl;

/**
 * An animator control interface,
 * which implementation may drive a {@link com.jogamp.opengl.GLAutoDrawable} animation.
 */
public interface GLAnimatorControl extends FPSCounter {
    /**
     * A {@link GLAnimatorControl#setUncaughtExceptionHandler(UncaughtExceptionHandler) registered}
     * {@link UncaughtExceptionHandler} instance is invoked when an {@link GLAnimatorControl animator} abruptly {@link #stop() stops}
     * due to an uncaught exception from one of its {@link GLAutoDrawable}s.
     * @see #uncaughtException(GLAnimatorControl, GLAutoDrawable, Throwable)
     * @see GLAnimatorControl#setUncaughtExceptionHandler(UncaughtExceptionHandler)
     * @since 2.2
     */
    public static interface UncaughtExceptionHandler {
        /**
         * Method invoked when the given {@link GLAnimatorControl} is {@link GLAnimatorControl#stop() stopped} due to the
         * given uncaught exception happened on the given {@link GLAutoDrawable}.
         * <p>
         * The animator thread can still be retrieved via {@link GLAnimatorControl#getThread()}.
         * </p>
         * <p>
         * All {@link GLAnimatorControl} states already reflect its stopped state.
         * </p>
         * <p>
         * After this handler method is called, the {@link GLAnimatorControl} is stopped.
         * </p>
         * <p>
         * Any exception thrown by this method will be ignored.
         * </p>
         * @param animator the {@link GLAnimatorControl}
         * @param drawable the causing {@link GLAutoDrawable},
         *        may be {@code null} in case {@link Throwable} caused unrelated to any {@link GLAutoDrawable}.
         * @param cause the uncaught exception
         * @see GLAnimatorControl#setUncaughtExceptionHandler(UncaughtExceptionHandler)
         * @since 2.2
         */
        void uncaughtException(final GLAnimatorControl animator, final GLAutoDrawable drawable, final Throwable cause);
    }

    /**
     * Indicates whether this animator has been {@link #start() started}.
     *
     * @see #start()
     * @see #stop()
     * @see #isPaused()
     * @see #pause()
     * @see #resume()
     */
    boolean isStarted();

    /**
     * Indicates whether this animator {@link #isStarted() is started} and {@link #isPaused() is not paused}.
     *
     * @see #start()
     * @see #stop()
     * @see #pause()
     * @see #resume()
     */
    boolean isAnimating();

    /**
     * Indicates whether this animator {@link #isStarted() is started}
     * and either {@link #pause() manually paused} or paused
     * automatically due to no {@link #add(GLAutoDrawable) added} {@link GLAutoDrawable}s.
     *
     * @see #start()
     * @see #stop()
     * @see #pause()
     * @see #resume()
     */
    boolean isPaused();

    /**
     * @return The animation thread if running, otherwise null.
     *
     * @see #start()
     * @see #stop()
     */
    Thread getThread();

    /**
     * Starts this animator, if not running.
     * <p>
     * In most situations this method blocks until
     * completion, except when called from the animation thread itself
     * or in some cases from an implementation-internal thread like the
     * AWT event queue thread.
     * </p>
     * <p>
     * Note that an animator w/o {@link #add(GLAutoDrawable) added drawables}
     * will be paused automatically.
     * </p>
     * <p>
     * If started, all counters (time, frames, ..) are reset to zero.
     * </p>
     *
     * @return true is started due to this call,
     *         otherwise false, ie started already or unable to start.
     *
     * @see #stop()
     * @see #isAnimating()
     * @see #isPaused()
     * @see #getThread()
     */
    boolean start();

    /**
     * Stops this animator.
     * <p>
     * In most situations this method blocks until
     * completion, except when called from the animation thread itself
     * or in some cases from an implementation-internal thread like the
     * AWT event queue thread.
     * </p>
     *
     * @return true is stopped due to this call,
     *         otherwise false, ie not started or unable to stop.
     *
     * @see #start()
     * @see #isAnimating()
     * @see #getThread()
     */
    boolean stop();

    /**
     * Pauses this animator.
     * <p>
     * In most situations this method blocks until
     * completion, except when called from the animation thread itself
     * or in some cases from an implementation-internal thread like the
     * AWT event queue thread.
     * </p>
     *
     * @return  false if not started, already paused or failed to pause, otherwise true
     *
     * @see #resume()
     * @see #isAnimating()
     */
    boolean pause();

    /**
     * Resumes animation if paused.
     * <p>
     * In most situations this method blocks until
     * completion, except when called from the animation thread itself
     * or in some cases from an implementation-internal thread like the
     * AWT event queue thread.
     * </p>
     * <p>
     * If resumed, all counters (time, frames, ..) are reset to zero.
     * </p>
     *
     * @return false if not started, not paused or unable to resume, otherwise true
     *
     * @see #pause()
     * @see #isAnimating()
     */
    boolean resume();

    /**
     * Adds a drawable to this animator's list of rendering drawables.
     * <p>
     * This allows the animator thread to become {@link #isAnimating() animating},
     * in case the first drawable is added and the animator {@link #isStarted() is started}.
     * </p>
     *
     * @param drawable the drawable to be added
     * @throws IllegalArgumentException if drawable was already added to this animator
     */
    void add(GLAutoDrawable drawable);

    /**
     * Removes a drawable from the animator's list of rendering drawables.
     * <p>
     * This method should get called in case a drawable becomes invalid,
     * and will not be recovered.
     * </p>
     * <p>
     * This allows the animator thread to become {@link #isAnimating() not animating},
     * in case the last drawable has been removed.
     * </p>
     *
     * @param drawable the drawable to be removed
     * @throws IllegalArgumentException if drawable was not added to this animator
     */
    void remove(GLAutoDrawable drawable);

    /**
     * Returns the {@link UncaughtExceptionHandler} invoked when this {@link GLAnimatorControl animator} abruptly {@link #stop() stops}
     * due to an uncaught exception from one of its {@link GLAutoDrawable}s.
     * <p>
     * Default is <code>null</code>.
     * </p>
     * @since 2.2
     */
    UncaughtExceptionHandler getUncaughtExceptionHandler();

    /**
     * Set the handler invoked when this {@link GLAnimatorControl animator} abruptly {@link #stop() stops}
     * due to an uncaught exception from one of its {@link GLAutoDrawable}s.
     * @param handler the {@link UncaughtExceptionHandler} to use as this {@link GLAnimatorControl animator}'s uncaught exception
     * handler. Pass <code>null</code> to unset the handler.
     * @see UncaughtExceptionHandler#uncaughtException(GLAnimatorControl, GLAutoDrawable, Throwable)
     * @since 2.2
     */
    void setUncaughtExceptionHandler(final UncaughtExceptionHandler handler);
}
