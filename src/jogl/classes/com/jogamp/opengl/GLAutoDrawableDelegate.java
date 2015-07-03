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

package com.jogamp.opengl;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLSharedContextSetter;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

import jogamp.opengl.GLAutoDrawableBase;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;


/**
 * Fully functional {@link GLAutoDrawable} implementation
 * utilizing already created {@link GLDrawable} and {@link GLContext} instances.
 * <p>
 * Since no native windowing system events are being processed, it is recommended
 * to handle at least the {@link com.jogamp.newt.event.WindowEvent window events}:
 * <ul>
 *   <li>{@link com.jogamp.newt.event.WindowListener#windowRepaint(com.jogamp.newt.event.WindowUpdateEvent) repaint} using {@link #windowRepaintOp()}</li>
 *   <li>{@link com.jogamp.newt.event.WindowListener#windowResized(com.jogamp.newt.event.WindowEvent) resize} using {@link #windowResizedOp()}</li>
 * </ul>
 * and setup a {@link com.jogamp.newt.Window#setWindowDestroyNotifyAction(Runnable) custom toolkit destruction} issuing {@link #windowDestroyNotifyOp()}.
 * </p>
 * <p>
 * See example {@link com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableDelegateNEWT TestGLAutoDrawableDelegateNEWT}.
 * </p>
 * <p>
 * <a name="contextSharing"><h5>OpenGL Context Sharing</h5></a>
 * To share a {@link GLContext} see the following note in the documentation overview:
 * <a href="../../../overview-summary.html#SHARING">context sharing</a>
 * as well as {@link GLSharedContextSetter}.
 * </p>
 */
public class GLAutoDrawableDelegate extends GLAutoDrawableBase implements GLAutoDrawable {
    /**
     * <p>
     * The {@link GLContext} can be assigned later manually via {@link GLAutoDrawable#setContext(GLContext, boolean) setContext(ctx)}
     * <i>or</i> it will be created <i>lazily</i> at the 1st {@link GLAutoDrawable#display() display()} method call.<br>
     * <i>Lazy</i> {@link GLContext} creation will take a shared {@link GLContext} into account
     * which has been set {@link #setSharedContext(GLContext) directly}
     * or {@link #setSharedAutoDrawable(GLAutoDrawable) via another GLAutoDrawable}.
     * </p>
     * @param drawable a valid {@link GLDrawable}, may not be {@link GLDrawable#isRealized() realized} yet.
     * @param context a valid {@link GLContext},
     *                may not have been made current (created) yet,
     *                may not be associated w/ <code>drawable<code> yet,
     *                may be <code>null</code> for lazy initialization at 1st {@link #display()}.
     * @param upstreamWidget optional UI element holding this instance, see {@link #getUpstreamWidget()}.
     * @param ownDevice pass <code>true</code> if {@link AbstractGraphicsDevice#close()} shall be issued,
     *                  otherwise pass <code>false</code>. Closing the device is required in case
     *                  the drawable is created w/ it's own new instance, e.g. offscreen drawables,
     *                  and no further lifecycle handling is applied.
     * @param lock optional custom {@link RecursiveLock}.
     */
    public GLAutoDrawableDelegate(final GLDrawable drawable, final GLContext context, final Object upstreamWidget, final boolean ownDevice, final RecursiveLock lock) {
        super((GLDrawableImpl)drawable, (GLContextImpl)context, ownDevice);
        if(null == drawable) {
            throw new IllegalArgumentException("null drawable");
        }
        this.upstreamWidget = upstreamWidget;
        this.lock = ( null != lock ) ? lock : LockFactory.createRecursiveLock() ;
    }

    //
    // expose default methods
    //

    /** Default implementation to handle repaint events from the windowing system */
    public final void windowRepaintOp() {
        super.defaultWindowRepaintOp();
    }

    /**
     * Handling resize events from the windowing system.
     * <p>
     * Implementation:
     * <ul>
     *   <li>resizes {@link #getDelegatedDrawable() the GLDrawable}, if offscreen,</li>
     *   <li>triggers a pending {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape events}, and</li>
     *   <li>issues a {@link #display()} call, if no animator is present.</li>
     * </ul>
     * </p>
     * <p>
     * All required locks are being claimed.
     * </p>
     * @param newWidth new width in pixel units
     * @param newWidth new height in pixel units
     */
    public final void windowResizedOp(final int newWidth, final int newHeight) {
        super.defaultWindowResizedOp(newWidth, newHeight);
    }

    /**
     * Implementation to handle destroy notifications from the windowing system.
     *
     * <p>
     * If the {@link NativeSurface} does not implement {@link WindowClosingProtocol}
     * or {@link WindowClosingMode#DISPOSE_ON_CLOSE} is enabled (default),
     * a thread safe destruction is being induced.
     * </p>
     */
    public final void windowDestroyNotifyOp() {
        super.defaultWindowDestroyNotifyOp();
    }

    //
    // Complete GLAutoDrawable
    //

    private Object upstreamWidget;
    private final RecursiveLock lock;

    @Override
    public final RecursiveLock getUpstreamLock() { return lock; }

    @Override
    public final Object getUpstreamWidget() {
        return upstreamWidget;
    }

    /**
     * Set the upstream UI toolkit object.
     * @see #getUpstreamWidget()
     */
    public final void setUpstreamWidget(final Object newUpstreamWidget) {
        upstreamWidget = newUpstreamWidget;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation calls {@link #defaultDestroy()}.
     * </p>
     * <p>
     * User still needs to destroy the upstream window, which details are hidden from this aspect.
     * This can be performed by overriding {@link #destroyImplInLock()}.
     * </p>
     */
    @Override
    public final void destroy() {
        defaultDestroy();
    }

    @Override
    protected void destroyImplInLock() {
        super.destroyImplInLock();
    }

    @Override
    public void display() {
        defaultDisplay();
    }

    //
    // GLDrawable delegation
    //

    @Override
    public final GLDrawableFactory getFactory() {
        return drawable.getFactory();
    }

    @Override
    public final void swapBuffers() throws GLException {
         defaultSwapBuffers();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[ \n\tHelper: " + helper + ", \n\tDrawable: " + drawable +
               ", \n\tContext: " + context + ", \n\tUpstreamWidget: "+upstreamWidget+ /** ", \n\tFactory: "+factory+ */ "]";
    }
}
