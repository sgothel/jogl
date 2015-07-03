/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLBase;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.Threading;

import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.opengl.GLEventListenerState;

import jogamp.opengl.Debug;

/**
 * Providing utility functions dealing w/ {@link GLDrawable}s, {@link GLAutoDrawable} and their {@link GLEventListener}.
 */
public class GLDrawableUtil {
    protected static final boolean DEBUG = Debug.debug("GLDrawable");

    public static final boolean isAnimatorStartedOnOtherThread(final GLAnimatorControl animatorCtrl) {
        return ( null != animatorCtrl ) ? animatorCtrl.isStarted() && animatorCtrl.getThread() != Thread.currentThread() : false ;
    }

    public static final boolean isAnimatorStarted(final GLAnimatorControl animatorCtrl) {
        return ( null != animatorCtrl ) ? animatorCtrl.isStarted() : false ;
    }

    public static final boolean isAnimatorAnimatingOnOtherThread(final GLAnimatorControl animatorCtrl) {
        return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() && animatorCtrl.getThread() != Thread.currentThread() : false ;
    }

    public static final boolean isAnimatorAnimating(final GLAnimatorControl animatorCtrl) {
        return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() : false ;
    }

    /**
     * {@link GLRunnable} to issue {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int)},
     * returning <code>true</code> on {@link GLRunnable#run(GLAutoDrawable)}.
     */
    public static class ReshapeGLEventListener implements GLRunnable {
        private final GLEventListener listener;
        private final boolean displayAfterReshape;
        /**
         *
         * @param listener
         * @param displayAfterReshape <code>true</code> to issue {@link GLEventListener#display(GLAutoDrawable)}
         *                            after {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int)},
         *                            otherwise false.
         */
        public ReshapeGLEventListener(final GLEventListener listener, final boolean displayAfterReshape) {
            this.listener = listener;
            this.displayAfterReshape = displayAfterReshape;
        }
        @Override
        public boolean run(final GLAutoDrawable drawable) {
            listener.reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
            if( displayAfterReshape ) {
                listener.display(drawable);
            }
            return true;
        }
    }

    /**
     * Moves the designated {@link GLEventListener} from {@link GLAutoDrawable} <code>src</code> to <code>dest</code>.
     * If <code>preserveInitState</code> is <code>true</code>, it's initialized state is preserved
     * and {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape(..)} issued w/ the next {@link GLAutoDrawable#display()} call.
     * <p>
     * Note that it is only legal to pass <code>preserveInitState := true</code>,
     * if the {@link GLContext} of both <code>src</code> and <code>dest</code> are shared, or has itself moved from <code>src</code> to <code>dest</code>.
     * </p>
     * <p>
     * Also note that the caller is encouraged to pause an attached {@link GLAnimatorControl}.
     * </p>
     * @param src
     * @param dest
     * @param listener
     * @param preserveInitState
     */
    public static final void moveGLEventListener(final GLAutoDrawable src, final GLAutoDrawable dest, final GLEventListener listener, final boolean preserveInitState) {
        final boolean initialized = src.getGLEventListenerInitState(listener);
        if( preserveInitState ) {
            src.removeGLEventListener(listener);
            dest.addGLEventListener(listener);
            if( initialized ) {
                dest.setGLEventListenerInitState(listener, true);
                dest.invoke(false, new ReshapeGLEventListener(listener, true));
            }
        } else {
            src.disposeGLEventListener(listener, true);
            dest.addGLEventListener(listener);
        }
    }

    /**
     * Moves all {@link GLEventListener} from {@link GLAutoDrawable} <code>src</code> to <code>dest</code>.
     * If <code>preserveInitState</code> is <code>true</code>, it's initialized state is preserved
     * and {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape(..)} issued w/ the next {@link GLAutoDrawable#display()} call.
     * <p>
     * Note that it is only legal to pass <code>preserveInitState := true</code>,
     * if the {@link GLContext} of both <code>src</code> and <code>dest</code> are shared, or has itself moved from <code>src</code> to <code>dest</code>.
     * </p>
     * <p>
     * Also note that the caller is encouraged to pause an attached {@link GLAnimatorControl}.
     * </p>
     * @param src
     * @param dest
     * @param listener
     * @param preserveInitState
     */
    public static final void moveAllGLEventListener(final GLAutoDrawable src, final GLAutoDrawable dest, final boolean preserveInitState) {
        for(int count = src.getGLEventListenerCount(); 0<count; count--) {
            final GLEventListener listener = src.getGLEventListener(0);
            moveGLEventListener(src, dest, listener, preserveInitState);
        }
    }

    /**
     * Return a heuristic value whether switching the {@link GLContext} is safe between {@link GLAutoDrawable}s,
     * i.e. via {@link #swapGLContext(GLAutoDrawable, GLAutoDrawable)} or {@link #swapGLContextAndAllGLEventListener(GLAutoDrawable, GLAutoDrawable)}.
     * <p>
     * Method currently returns <code>false</code> if:
     * <ul>
     *   <li>Switching between on- and offscreen and one of the following is <code>true</code>:
     *     <ul>
     *       <li>{@link GLCapabilitiesImmutable#getSampleBuffers() MSAA is <i>used</i>} [1] in <code>chosenCapsA</code> or <code>chosenCapsB</code></li>
     *       <li>{@link GLCapabilitiesImmutable#getStereo() Stereo is <i>used</i>} in <code>chosenCapsA</code> or <code>chosenCapsB</code></li>
     *       <li>{@link GLCapabilitiesImmutable#getAccumAlphaBits() Accumulator Buffer is <i>requested</i>} [2] in <code>requestedCaps</code></li>
     *     </ul></li>
     * </ul>
     * Otherwise method returns <code>true</code>
     * </p>
     * <pre>
     * [1] See Bug 830: swapGLContextAndAllGLEventListener and onscreen MSAA w/ NV/GLX
     *     On NVidia GPUs w/ it's proprietary driver context swapping does not work if MSAA is involved
     *     and when swapping on- to offscreen.
     * </pre>
     * <pre>
     * [2] On AMD GPUs w/ it's proprietary driver, requesting an accumulator buffer leads to receive an accumulator buffer configuration,
     *     for which context swapping does not work when swapping on- to offscreen and vice-versa, i.e. cannot make context current.
     *     With AMD and Mesa drivers we only receive an accumulator buffer if requested,
     *     where on NVidia drivers all configurations contain the accumulator buffer.
     *     On both drivers, NVidia and Mesa, context swapping with accumulator buffer works.
     * </pre>
     * @param requestedCaps requested {@link GLCapabilitiesImmutable} which are intended for usage by both {@link GLAutoDrawable}s A and B
     * @param chosenCapsA chosen {@link GLCapabilitiesImmutable} of {@link GLAutoDrawable} A, which {@link GLContext} is intended to be swapped
     * @param chosenCapsB chosen {@link GLCapabilitiesImmutable} of {@link GLAutoDrawable} B, which {@link GLContext} is intended to be swapped
     * @see #swapGLContext(GLAutoDrawable, GLAutoDrawable)
     * @see #swapGLContextAndAllGLEventListener(GLAutoDrawable, GLAutoDrawable)
     */
    public static boolean isSwapGLContextSafe(final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesImmutable chosenCapsA, final GLCapabilitiesImmutable chosenCapsB) {
        final boolean usingAccumulatorBuffer = requestedCaps.getAccumAlphaBits() > 0 ||
                                               requestedCaps.getAccumRedBits()   > 0 ||
                                               requestedCaps.getAccumGreenBits() > 0 ||
                                               requestedCaps.getAccumBlueBits()  > 0;
        if( ( chosenCapsA.isOnscreen() && !chosenCapsB.isOnscreen() || !chosenCapsA.isOnscreen() && chosenCapsB.isOnscreen() ) && // switching between on- and offscreen
            (
              ( chosenCapsA.getSampleBuffers() || chosenCapsB.getSampleBuffers() ) ||  // MSAA involved
              ( chosenCapsA.getStereo() || chosenCapsB.getStereo() )               ||  // Stereo involved
              usingAccumulatorBuffer                                                   // Using accumulator buffer
            )
          )
        {
            return false;
        } else {
            return true;
        }
    }
    /**
     * Swaps the {@link GLContext} and all {@link GLEventListener} between {@link GLAutoDrawable} <code>a</code> and <code>b</code>,
     * while preserving it's initialized state, resets the GL-Viewport and issuing {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape(..)}.
     * <p>
     * The {@link GLAutoDrawable} to {@link GLAnimatorControl} association
     * is also swapped.
     * </p>
     * <p>
     * If an {@link GLAnimatorControl} is being attached to {@link GLAutoDrawable} <code>a</code> or <code>b</code>
     * and the current thread is different than {@link GLAnimatorControl#getThread() the animator's thread}, it is paused during the operation.
     * </p>
     * <p>
     * During operation, both {@link GLAutoDrawable auto-drawable's}
     * {@link GLAutoDrawable#getUpstreamLock() upstream-locks} and {@link GLAutoDrawable#getNativeSurface() surfaces} are locked,
     * hence atomicity of operation is guaranteed,
     * see <a href="../../../../com/jogamp/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.
     * </p>
     * <p>
     * Because of above mentioned locking, if this method is not performed
     * on {@link GLAutoDrawable#isThreadGLCapable() a OpenGL capable thread} of <i>both</i>
     * {@link GLAutoDrawable}s, it must be invoked on such an OpenGL capable thread,
     * e.g. via {@link Threading#invokeOnOpenGLThread(boolean, Runnable)}.
     * </p>
     * @throws GLException if the {@link AbstractGraphicsDevice} are incompatible w/ each other.
     * @see #isSwapGLContextSafe(GLCapabilitiesImmutable, GLCapabilitiesImmutable, GLCapabilitiesImmutable)
     */
    public static final void swapGLContextAndAllGLEventListener(final GLAutoDrawable a, final GLAutoDrawable b) {
        final GLEventListenerState gllsA = GLEventListenerState.moveFrom(a, true);
        final GLEventListenerState gllsB = GLEventListenerState.moveFrom(b, true);
        final Runnable gllsAUnlockOp = gllsA.getUnlockSurfaceOp();
        final Runnable gllsBUnlockOp = gllsB.getUnlockSurfaceOp();
        try {
            gllsA.moveTo(b, gllsBUnlockOp);
            gllsB.moveTo(a, gllsAUnlockOp);
        } finally {
            // guarantee unlock in case of an exception
            gllsBUnlockOp.run();
            gllsAUnlockOp.run();
        }
    }

    /**
     * Swaps the {@link GLContext} of given {@link GLAutoDrawable}
     * and {@link GLAutoDrawable#disposeGLEventListener(GLEventListener, boolean) disposes}
     * each {@link GLEventListener} w/o removing it.
     * <p>
     * The GL-Viewport is reset and {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape(..)} issued implicit.
     * </p>
     * <p>
     * If an {@link GLAnimatorControl} is being attached to GLAutoDrawable src or dest and the current thread is different
     * than {@link GLAnimatorControl#getThread() the animator's thread}, it is paused during the operation.
     * </p>
     * <p>
     * During operation, both {@link GLAutoDrawable auto-drawable's}
     * {@link GLAutoDrawable#getUpstreamLock() upstream-locks} and {@link GLAutoDrawable#getNativeSurface() surfaces} are locked,
     * hence atomicity of operation is guaranteed,
     * see <a href="../../../../com/jogamp/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.
     * </p>
     * <p>
     * Because of above mentioned locking, if this method is not performed
     * on {@link GLAutoDrawable#isThreadGLCapable() a OpenGL capable thread} of <i>both</i>
     * {@link GLAutoDrawable}s, it must be invoked on such an OpenGL capable thread,
     * e.g. via {@link Threading#invokeOnOpenGLThread(boolean, Runnable)}.
     * </p>
     * @param a
     * @param b
     * @see #isSwapGLContextSafe(GLCapabilitiesImmutable, GLCapabilitiesImmutable, GLCapabilitiesImmutable)
     */
    public static final void swapGLContext(final GLAutoDrawable a, final GLAutoDrawable b) {
        final GLAnimatorControl aAnim = a.getAnimator();
        final GLAnimatorControl bAnim = b.getAnimator();
        final boolean aIsPaused = isAnimatorAnimatingOnOtherThread(aAnim) && aAnim.pause();
        final boolean bIsPaused = isAnimatorAnimatingOnOtherThread(bAnim) && bAnim.pause();

        final RecursiveLock aUpstreamLock = a.getUpstreamLock();
        final RecursiveLock bUpstreamLock = b.getUpstreamLock();
        aUpstreamLock.lock();
        bUpstreamLock.lock();
        try {
            final NativeSurface aSurface = a.getNativeSurface();
            final boolean aSurfaceLocked = NativeSurface.LOCK_SURFACE_NOT_READY < aSurface.lockSurface();
            if( a.isRealized() && !aSurfaceLocked ) {
                throw new GLException("Could not lock realized a surface "+a);
            }
            final NativeSurface bSurface = b.getNativeSurface();
            final boolean bSurfaceLocked = NativeSurface.LOCK_SURFACE_NOT_READY < bSurface.lockSurface();
            if( b.isRealized() && !bSurfaceLocked ) {
                throw new GLException("Could not lock realized b surface "+b);
            }
            try {
                for(int i = a.getGLEventListenerCount() - 1; 0 <= i; i--) {
                    a.disposeGLEventListener(a.getGLEventListener(i), false);
                }
                for(int i = b.getGLEventListenerCount() - 1; 0 <= i; i--) {
                    b.disposeGLEventListener(b.getGLEventListener(i), false);
                }
                b.setContext( a.setContext( b.getContext(), false ), false );

            } finally {
                if( bSurfaceLocked ) {
                    bSurface.unlockSurface();
                }
                if( aSurfaceLocked ) {
                    aSurface.unlockSurface();
                }
            }
        } finally {
            bUpstreamLock.unlock();
            aUpstreamLock.unlock();
        }
        a.invoke(true, setViewport);
        b.invoke(true, setViewport);
        if(aIsPaused) { aAnim.resume(); }
        if(bIsPaused) { bAnim.resume(); }
    }

    private static final GLRunnable setViewport = new GLRunnable() {
        @Override
        public boolean run(final GLAutoDrawable drawable) {
            drawable.getGL().glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
            return false; // issue re-display w/ new viewport!
        }
    };

    /**
     * Determines whether the chosen {@link GLCapabilitiesImmutable}
     * requires a {@link GLDrawable#swapBuffers() swap-buffers}
     * before reading pixels.
     * <p>
     * Usually one uses the {@link GLBase#getDefaultReadBuffer() default-read-buffer}
     * in which case {@link GLDrawable#swapBuffers() swap-buffers} shall happen <b>after</b> calling reading pixels, the default.
     * </p>
     * <p>
     * However, <i>multisampling</i> offscreen {@link com.jogamp.opengl.GLFBODrawable}s
     * utilize {@link GLDrawable#swapBuffers() swap-buffers} to <i>downsample</i>
     * the multisamples into the readable sampling sink.
     * In this case, we require {@link GLDrawable#swapBuffers() swap-buffers} <b>before</b> reading pixels.
     * </p>
     * @return chosenCaps.isFBO() && chosenCaps.getSampleBuffers()
     */
    public static final boolean swapBuffersBeforeRead(final GLCapabilitiesImmutable chosenCaps) {
        return chosenCaps.isFBO() && chosenCaps.getSampleBuffers();
    }
}
