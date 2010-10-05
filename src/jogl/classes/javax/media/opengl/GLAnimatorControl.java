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

package javax.media.opengl;

/**
 * An animator control interface, 
 * which implementation may drive a {@link javax.media.opengl.GLAutoDrawable} animation.
 * <P>
 * Note that the methods {@link #start()}, {@link #stop()}, {@link #pause()} and {@link #resume()}
 * shall be implemented to fail-fast, ie {@link #start()} fails if not started, etc.
 * This way an implementation can find implementation errors faster.
 */
public interface GLAnimatorControl {

    /**
     * @return Time of the first display call in milliseconds.
     *         This value is reset if started or resumed.
     *
     * @see #start()
     * @see #resume()
     */
    long getStartTime();

    /**
     * @return Time of the last display call in milliseconds.
     *         This value is reset if started or resumed.
     *
     * @see #start()
     * @see #resume()
     */
    long getCurrentTime();

    /**
     * @return Duration <code>getCurrentTime() - getStartTime()</code>.
     *
     * @see #getStartTime()
     * @see #getCurrentTime()
     */
    long getDuration();


    /**
     * @return Number of frames issued to all registered GLAutoDrawables registered
     */
    /**
     * @return Number of frame cycles displayed by all registered {@link javax.media.opengl.GLAutoDrawable}
     *         since the first display call, ie <code>getStartTime()</code>.
     *         This value is reset if started or resumed.
     *
     * @see #start()
     * @see #resume()
     */
    int getTotalFrames();

    /**
     * Indicates whether this animator is currently running, ie started.
     *
     * @see #start()
     * @see #stop()
     * @see #pause()
     * @see #resume()
     */
    boolean isStarted();

    /**
     * Indicates whether this animator is currently running and not paused.
     *
     * @see #start()
     * @see #stop()
     * @see #pause()
     * @see #resume()
     */
    boolean isAnimating();

    /**
     * Indicates whether this animator is currently running and paused.
     *
     * @see #start()
     * @see #stop()
     * @see #pause()
     * @see #resume()
     */
    boolean isPaused();

    /**
     * @return The animation thread if started, ie running.
     *
     * @see #start()
     * @see #stop()
     */
    Thread getThread();

    /**
     * Starts this animator, if not running.
     * <P>
     * In most situations this method blocks until
     * completion, except when called from the animation thread itself
     * or in some cases from an implementation-internal thread like the
     * AWT event queue thread.
     * <P>
     * If started, all counters (time, frames, ..) are reset to zero.
     *
     * @see #stop()
     * @see #isAnimating()
     * @see #getThread()
     * @throws GLException if started and animating already
     */
    void start();

    /**
     * Stops this animator.
     * <P>
     * In most situations this method blocks until
     * completion, except when called from the animation thread itself
     * or in some cases from an implementation-internal thread like the
     * AWT event queue thread.
     *
     * @see #start()
     * @see #isAnimating()
     * @see #getThread()
     * @throws GLException if not started or not animating
     */
    void stop();

    /**
     * Pauses this animator.
     * <P>
     * In most situations this method blocks until
     * completion, except when called from the animation thread itself
     * or in some cases from an implementation-internal thread like the
     * AWT event queue thread.
     *
     * @see #resume()
     * @see #isAnimating()
     * @throws GLException if not started or not animating or already paused
     */
    void pause();

    /**
     * Resumes animation if paused.
     * <P>
     * In most situations this method blocks until
     * completion, except when called from the animation thread itself
     * or in some cases from an implementation-internal thread like the
     * AWT event queue thread.
     * <P>
     * If resumed, all counters (time, frames, ..) are reset to zero.
     *
     * @see #pause()
     * @see #isAnimating()
     * @throws GLException if not started or not paused
     */
    void resume();
}
