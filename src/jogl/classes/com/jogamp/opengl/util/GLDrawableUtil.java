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

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLBase;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLRunnable;

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
     * Return a heuristic value whether switching the {@link GLContext} is safe between {@lin GLAutoDrawable}s,
     * i.e. via {@link #swapGLContext(GLAutoDrawable, GLAutoDrawable)} or {@link #swapGLContextAndAllGLEventListener(GLAutoDrawable, GLAutoDrawable)}.
     * <p>
     * Method currently returns <code>false</code> if:
     * <ul>
     *   <li>Switching between on- and offscreen and one of the following is <code>true</code>:
     *     <ul>
     *       <li>{@link GLCapabilitiesImmutable#getSampleBuffers() MSAA is used}[1], or
     *       <li>{@link GLCapabilitiesImmutable#getStereo() Stereo is used}
     *     </ul></li>
     * </ul>
     * Otherwise method returns <code>true</code>
     * </p>
     * <p>
     * [1] See Bug 830: swapGLContextAndAllGLEventListener and onscreen MSAA w/ NV/GLX
     * </p>
     */
    public static boolean isSwapGLContextSafe(final GLCapabilitiesImmutable a, final GLCapabilitiesImmutable b) {
        if( ( a.isOnscreen() && !b.isOnscreen() || !a.isOnscreen() && b.isOnscreen() ) && // switching between on- and offscreen
            (
              ( a.getSampleBuffers() || b.getSampleBuffers() ) ||  // MSAA involved
              ( a.getStereo() || b.getStereo() )                   // Stereo involved
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
     * see <a href="../../../../javax/media/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.
     * </p>
     * @param a
     * @param b
     * @throws GLException if the {@link AbstractGraphicsDevice} are incompatible w/ each other.
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
     * see <a href="../../../../javax/media/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.
     * </p>
     * @param a
     * @param b
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
     * However, <i>multisampling</i> offscreen {@link javax.media.opengl.GLFBODrawable}s
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
