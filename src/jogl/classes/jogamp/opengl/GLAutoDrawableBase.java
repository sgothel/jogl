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

package jogamp.opengl;

import java.io.PrintStream;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.WindowClosingProtocol;
import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.GLSharedContextSetter;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.GLEventListenerState;
import com.jogamp.opengl.GLStateKeeper;


/**
 * Abstract common code for GLAutoDrawable implementations
 * utilizing multithreading, i.e. {@link #isThreadGLCapable()} always returns <code>true</code>.
 *
 * @see GLAutoDrawable
 * @see GLAutoDrawable#getThreadingMode()
 * @see GLAutoDrawableDelegate
 * @see GLOffscreenAutoDrawable
 * @see GLOffscreenAutoDrawableImpl
 * @see GLPBufferImpl
 * @see com.jogamp.newt.opengl.GLWindow
 */
public abstract class GLAutoDrawableBase implements GLAutoDrawable, GLStateKeeper, FPSCounter, GLSharedContextSetter {
    public static final boolean DEBUG = GLDrawableImpl.DEBUG;
    protected final GLDrawableHelper helper = new GLDrawableHelper();
    protected final FPSCounterImpl fpsCounter = new FPSCounterImpl();

    protected volatile GLDrawableImpl drawable; // volatile: avoid locking for read-only access
    protected volatile GLContextImpl context;
    protected boolean preserveGLELSAtDestroy;
    protected GLEventListenerState glels;
    protected GLStateKeeper.Listener glStateKeeperListener;
    protected final boolean ownsDevice;
    protected int additionalCtxCreationFlags = 0;
    protected volatile boolean sendReshape = false; // volatile: maybe written by WindowManager thread w/o locking
    protected volatile boolean sendDestroy = false; // volatile: maybe written by WindowManager thread w/o locking

    /**
     * <p>
     * The {@link GLContext} can be assigned later manually via {@link GLAutoDrawable#setContext(GLContext, boolean) setContext(ctx)}
     * <i>or</i> it will be created <i>lazily</i> at the 1st {@link GLAutoDrawable#display() display()} method call.<br>
     * <i>Lazy</i> {@link GLContext} creation will take a shared {@link GLContext} into account
     * which has been set {@link #setSharedContext(GLContext) directly}
     * or {@link #setSharedAutoDrawable(GLAutoDrawable) via another GLAutoDrawable}.
     * </p>
     * @param drawable upstream {@link GLDrawableImpl} instance,
     *                 may be <code>null</code> for lazy initialization
     * @param context upstream {@link GLContextImpl} instance,
     *                may not have been made current (created) yet,
     *                may not be associated w/ <code>drawable<code> yet,
     *                may be <code>null</code> for lazy initialization at 1st {@link #display()}.
     * @param ownsDevice pass <code>true</code> if {@link AbstractGraphicsDevice#close()} shall be issued,
     *                   otherwise pass <code>false</code>. Closing the device is required in case
     *                   the drawable is created w/ it's own new instance, e.g. offscreen drawables,
     *                   and no further lifecycle handling is applied.
     */
    public GLAutoDrawableBase(final GLDrawableImpl drawable, final GLContextImpl context, final boolean ownsDevice) {
        this.drawable = drawable;
        this.context = context;
        this.preserveGLELSAtDestroy = false;
        this.glels = null;
        this.glStateKeeperListener = null;
        this.ownsDevice = ownsDevice;
        if(null != context && null != drawable) {
            context.setGLDrawable(drawable, false);
        }
        resetFPSCounter();
    }

    @Override
    public final void setSharedContext(final GLContext sharedContext) throws IllegalStateException {
        helper.setSharedContext(this.context, sharedContext);
    }

    @Override
    public final void setSharedAutoDrawable(final GLAutoDrawable sharedAutoDrawable) throws IllegalStateException {
        helper.setSharedAutoDrawable(this, sharedAutoDrawable);
    }

    @Override
    public final GLStateKeeper.Listener setGLStateKeeperListener(final Listener l) {
        final GLStateKeeper.Listener pre = glStateKeeperListener;
        glStateKeeperListener = l;
        return pre;
    }

    @Override
    public final boolean preserveGLStateAtDestroy(final boolean value) {
        final boolean res = isGLStatePreservationSupported() ? true : false;
        if( res ) {
            if( DEBUG ) {
                final long surfaceHandle = null != getNativeSurface() ? getNativeSurface().getSurfaceHandle() : 0;
                System.err.println("GLAutoDrawableBase.setPreserveGLStateAtDestroy: ("+getThreadName()+"): "+preserveGLELSAtDestroy+" -> "+value+" - surfaceHandle 0x"+Long.toHexString(surfaceHandle));
            }
            preserveGLELSAtDestroy = value;
        }
        return res;
    }

    @Override
    public boolean isGLStatePreservationSupported() { return false; }

    @Override
    public final GLEventListenerState getPreservedGLState() {
        return glels;
    }

    @Override
    public final GLEventListenerState clearPreservedGLState() {
        final GLEventListenerState r = glels;
        glels = null;
        return r;
    }

    /**
     * Preserves the {@link GLEventListenerState} from this {@link GLAutoDrawable}.
     *
     * @return <code>true</code> if the {@link GLEventListenerState} is preserved successfully from this {@link GLAutoDrawable},
     *         otherwise <code>false</code>.
     *
     * @throws IllegalStateException if the {@link GLEventListenerState} is already preserved
     *
     * @see #restoreGLEventListenerState()
     */
    protected final boolean preserveGLEventListenerState() throws IllegalStateException {
        if( null != glels ) {
            throw new IllegalStateException("GLEventListenerState already pulled");
        }
        if( null != context && context.isCreated() ) {
            if( null!= glStateKeeperListener) {
                glStateKeeperListener.glStatePreserveNotify(this);
            }
            glels = GLEventListenerState.moveFrom(this);
            return null != glels;
        }
        return false;
    }

    /**
     * Restores a previously {@link #preserveGLEventListenerState() preserved} {@link GLEventListenerState} to this {@link GLAutoDrawable}.
     *
     * @return <code>true</code> if the {@link GLEventListenerState} was previously {@link #preserveGLEventListenerState() preserved}
     *         and is moved successfully to this {@link GLAutoDrawable},
     *         otherwise <code>false</code>.
     *
     * @see #preserveGLEventListenerState()
     */
    protected final boolean restoreGLEventListenerState() {
        if( null != glels ) {
            glels.moveTo(this);
            glels = null;
            if( null!= glStateKeeperListener) {
                glStateKeeperListener.glStateRestored(this);
            }
            return true;
        }
        return false;
    }

    /** Default implementation to handle repaint events from the windowing system */
    protected final void defaultWindowRepaintOp() {
        final GLDrawable _drawable = drawable;
        if( null != _drawable && _drawable.isRealized() ) {
            if( !_drawable.getNativeSurface().isSurfaceLockedByOtherThread() && !helper.isAnimatorAnimatingOnOtherThread() ) {
                display();
            }
        }
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
    protected final void defaultWindowResizedOp(final int newWidth, final int newHeight) throws NativeWindowException, GLException {
        GLDrawableImpl _drawable = drawable;
        if( null!=_drawable ) {
            if(DEBUG) {
                final long surfaceHandle = null != getNativeSurface() ? getNativeSurface().getSurfaceHandle() : 0;
                System.err.println("GLAutoDrawableBase.sizeChanged: ("+getThreadName()+"): "+newWidth+"x"+newHeight+" - surfaceHandle 0x"+Long.toHexString(surfaceHandle));
            }
            if( ! _drawable.getChosenGLCapabilities().isOnscreen() ) {
                final RecursiveLock _lock = getUpstreamLock();
                _lock.lock();
                try {
                    final GLDrawableImpl _drawableNew = GLDrawableHelper.resizeOffscreenDrawable(_drawable, context, newWidth, newHeight);
                    if(_drawable != _drawableNew) {
                        // write back
                        _drawable = _drawableNew;
                        drawable = _drawableNew;
                    }
                } finally {
                    _lock.unlock();
                }
            }
            sendReshape = true; // async if display() doesn't get called below, but avoiding deadlock
            if( _drawable.isRealized() ) {
                if( !_drawable.getNativeSurface().isSurfaceLockedByOtherThread() && !helper.isAnimatorAnimatingOnOtherThread() ) {
                    display();
                }
            }
        }
    }

    /**
     * Default implementation to handle destroy notifications from the windowing system.
     *
     * <p>
     * If the {@link NativeSurface} does not implement {@link WindowClosingProtocol}
     * or {@link WindowClosingMode#DISPOSE_ON_CLOSE} is enabled (default),
     * a thread safe destruction is being induced.
     * </p>
     */
    protected final void defaultWindowDestroyNotifyOp() {
        final NativeSurface ns = getNativeSurface();
        final boolean shallClose;
        if(ns instanceof WindowClosingProtocol) {
            shallClose = WindowClosingMode.DISPOSE_ON_CLOSE == ((WindowClosingProtocol)ns).getDefaultCloseOperation();
        } else {
            shallClose = true;
        }
        if( shallClose ) {
            try {
                destroyAvoidAwareOfLocking();
            } catch( final Throwable t ) {
                // Intentionally catch and ignore exception,
                // so the destroy mechanism of the native windowing system is not corrupted!
                ExceptionUtils.dumpThrowable("ignored", t);
            }
        }
    }

    /**
     * Calls {@link #destroy()}
     * directly if the following requirements are met:
     * <ul>
     *   <li>An {@link GLAnimatorControl} is bound (see {@link #getAnimator()}) and running on another thread.
     *       Here we pause the animation while issuing the destruction.</li>
     *   <li>Surface is not locked by another thread (considered anonymous).</li>
     * </ul>
     * <p>
     * Otherwise destroy is being flagged to be called within the next
     * call of display().
     * </p>
     * <p>
     * This method is being used to avoid deadlock if
     * destruction is desired by <i>other</i> threads, e.g. the window manager.
     * </p>
     * @see #defaultWindowDestroyNotifyOp()
     * @see #defaultDisplay()
     */
    protected final void destroyAvoidAwareOfLocking() {
        final NativeSurface ns = getNativeSurface();

        final GLAnimatorControl ctrl = helper.getAnimator();

        // Is an animator thread perform rendering?
        if ( helper.isAnimatorStartedOnOtherThread() ) {
            // Pause animations before initiating safe destroy.
            final boolean isPaused = ctrl.pause();
            destroy();
            if(isPaused) {
                ctrl.resume();
            }
        } else if (null != ns && ns.isSurfaceLockedByOtherThread()) {
            // Surface is locked by another thread.
            // Flag that destroy should be performed on the next
            // attempt to display.
            sendDestroy = true; // async, but avoiding deadlock
        } else {
            // Without an external thread animating or locking the
            // surface, we are safe.
            destroy();
        }
    }

    /**
     * Calls {@link #destroyImplInLock()} while claiming the lock.
     */
    protected final void defaultDestroy() {
        final RecursiveLock lock = getUpstreamLock();
        lock.lock();
        try {
            destroyImplInLock();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Default implementation to destroys the drawable and context of this GLAutoDrawable:
     * <ul>
     *   <li>issues the GLEventListener dispose call, if drawable and context are valid</li>
     *   <li>destroys the GLContext, if valid</li>
     *   <li>destroys the GLDrawable, if valid</li>
     * </ul>
     * <p>Method assumes the lock is being hold.</p>
     * <p>Override it to extend it to destroy your resources, i.e. the actual window.
     * In such case call <code>super.destroyImplInLock</code> first.</p>
     */
    protected void destroyImplInLock() {
        if( preserveGLELSAtDestroy ) {
            preserveGLStateAtDestroy(false);
            preserveGLEventListenerState();
        }

        GLException exceptionOnDisposeGL = null;
        if( null != context ) {
            if( context.isCreated() ) {
                try {
                    helper.disposeGL(this, context, true);
                } catch (final GLException gle) {
                    exceptionOnDisposeGL = gle;
                }
            }
            context = null;
        }

        Throwable exceptionOnUnrealize = null;
        Throwable exceptionOnDeviceClose = null;
        if( null != drawable ) {
            final AbstractGraphicsDevice device = drawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
            try {
                drawable.setRealized(false);
            } catch( final Throwable re ) {
                exceptionOnUnrealize = re;
            }
            drawable = null;
            try {
                if( ownsDevice ) {
                    device.close();
                }
            } catch (final Throwable re) {
                exceptionOnDeviceClose = re;
            }
        }

        // throw exception in order of occurrence ..
        if( null != exceptionOnDisposeGL ) {
            throw exceptionOnDisposeGL;
        }
        if( null != exceptionOnUnrealize ) {
            throw GLException.newGLException(exceptionOnUnrealize);
        }
        if( null != exceptionOnDeviceClose ) {
            throw GLException.newGLException(exceptionOnDeviceClose);
        }
    }

    public final void defaultSwapBuffers() throws GLException {
        final RecursiveLock _lock = getUpstreamLock();
        _lock.lock();
        try {
            if(null != drawable) {
                drawable.swapBuffers();
            }
        } finally {
            _lock.unlock();
        }
    }

    //
    // GLAutoDrawable
    //

    protected final Runnable defaultInitAction = new Runnable() {
        @Override
        public final void run() {
            // Lock: Locked Surface/Window by MakeCurrent/Release
            helper.init(GLAutoDrawableBase.this, !sendReshape);
            resetFPSCounter();
        } };

    protected final Runnable defaultDisplayAction = new Runnable() {
        @Override
        public final void run() {
            // Lock: Locked Surface/Window by display _and_ MakeCurrent/Release
            if (sendReshape) {
                helper.reshape(GLAutoDrawableBase.this, 0, 0, getSurfaceWidth(), getSurfaceHeight());
                sendReshape = false;
            }
            helper.display(GLAutoDrawableBase.this);
            fpsCounter.tickFPS();
        } };

    protected final void defaultDisplay() {
        if( sendDestroy ) {
            sendDestroy=false;
            destroy();
            return;
        }
        final RecursiveLock _lock = getUpstreamLock();
        _lock.lock();
        try {
            if( null == context ) {
                boolean contextCreated = false;
                final GLDrawableImpl _drawable = drawable;
                if ( null != _drawable && _drawable.isRealized() && 0<_drawable.getSurfaceWidth()*_drawable.getSurfaceHeight() ) {
                    final GLContext[] shareWith = { null };
                    if( !helper.isSharedGLContextPending(shareWith) ) {
                        if( !restoreGLEventListenerState() ) {
                            context = (GLContextImpl) _drawable.createContext(shareWith[0]);
                            context.setContextCreationFlags(additionalCtxCreationFlags);
                            contextCreated = true;
                            // surface is locked/unlocked implicit by context's makeCurrent/release
                            helper.invokeGL(_drawable, context, defaultDisplayAction, defaultInitAction);
                        }
                    }
                }
                if(DEBUG) {
                    System.err.println("GLAutoDrawableBase.defaultDisplay: contextCreated "+contextCreated);
                }
            } else {
                // surface is locked/unlocked implicit by context's makeCurrent/release
                helper.invokeGL(drawable, context, defaultDisplayAction, defaultInitAction);
            }
        } finally {
            _lock.unlock();
        }
    }

    protected final GLEventListener defaultDisposeGLEventListener(final GLEventListener listener, final boolean remove) {
        final RecursiveLock _lock = getUpstreamLock();
        _lock.lock();
        try {
            return helper.disposeGLEventListener(GLAutoDrawableBase.this, drawable, context, listener, remove);
        } finally {
            _lock.unlock();
        }
    }

    @Override
    public final GLDrawable getDelegatedDrawable() {
        return drawable;
    }

    @Override
    public final GLContext getContext() {
        return context;
    }

    @Override
    public final GLContext setContext(final GLContext newCtx, final boolean destroyPrevCtx) {
        final RecursiveLock lock = getUpstreamLock();
        lock.lock();
        try {
            final GLContext oldCtx = context;
            GLDrawableHelper.switchContext(drawable, oldCtx, destroyPrevCtx, newCtx, additionalCtxCreationFlags);
            context=(GLContextImpl)newCtx;
            return oldCtx;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final GL getGL() {
        final GLContext _context = context;
        if (_context == null) {
            return null;
        }
        return _context.getGL();
    }

    @Override
    public final GL setGL(final GL gl) {
        final GLContext _context = context;
        if (_context != null) {
            _context.setGL(gl);
            return gl;
        }
        return null;
    }

    @Override
    public final void addGLEventListener(final GLEventListener listener) {
        helper.addGLEventListener(listener);
    }

    @Override
    public final void addGLEventListener(final int index, final GLEventListener listener) throws IndexOutOfBoundsException {
        helper.addGLEventListener(index, listener);
    }

    @Override
    public int getGLEventListenerCount() {
        return helper.getGLEventListenerCount();
    }

    @Override
    public GLEventListener getGLEventListener(final int index) throws IndexOutOfBoundsException {
        return helper.getGLEventListener(index);
    }

    @Override
    public boolean areAllGLEventListenerInitialized() {
        return helper.areAllGLEventListenerInitialized();
    }

    @Override
    public boolean getGLEventListenerInitState(final GLEventListener listener) {
        return helper.getGLEventListenerInitState(listener);
    }

    @Override
    public void setGLEventListenerInitState(final GLEventListener listener, final boolean initialized) {
        helper.setGLEventListenerInitState(listener, initialized);
    }

    @Override
    public GLEventListener disposeGLEventListener(final GLEventListener listener, final boolean remove) {
        return defaultDisposeGLEventListener(listener, remove);
    }

    @Override
    public final GLEventListener removeGLEventListener(final GLEventListener listener) {
        return helper.removeGLEventListener(listener);
    }

    @Override
    public final void setAnimator(final GLAnimatorControl animatorControl)
            throws GLException {
        helper.setAnimator(animatorControl);
    }

    @Override
    public final GLAnimatorControl getAnimator() {
        return helper.getAnimator();
    }

    @Override
    public final Thread setExclusiveContextThread(final Thread t) throws GLException {
        return helper.setExclusiveContextThread(t, context);
    }

    @Override
    public final Thread getExclusiveContextThread() {
        return helper.getExclusiveContextThread();
    }

    /**
     * Invokes given {@code runnable} on current thread outside of a probable claimed exclusive thread,
     * i.e. releases the exclusive thread, executes the runnable and reclaims it.
     * FIXME: Promote to GLAutoDrawable!
     *
     * @param runnable the {@link Runnable} to execute on the new thread.
     *                 The runnable <b>must exit</b>, i.e. not loop forever.
     *
     * @see #setExclusiveContextThread(Thread, GLContext)
     *
     * @since 2.3.2
     */
    public final void invokeOnCurrentThread(final Runnable runnable) {
        helper.runOutsideOfExclusiveContextThread(context, runnable);
    }
    /**
     * Invokes given {@code runnable} on current thread outside of a probable claimed exclusive thread,
     * i.e. releases the exclusive thread, executes the runnable and reclaims it.
     * FIXME: Promote to GLAutoDrawable!
     *
     * @param tg the {@link ThreadGroup} for the new thread, maybe <code>null</code>
     * @param waitUntilDone if <code>true</code>, waits until <code>runnable</code> execution is completed, otherwise returns immediately.
     * @param runnable the {@link Runnable} to execute on the new thread.
     *                 The runnable <b>must exit</b>, i.e. not loop forever.
     * @return {@link RunnableTask} instance with execution details
     *
     * @see #setExclusiveContextThread(Thread, GLContext)
     *
     * @since 2.3.2
     */
    public final RunnableTask invokeOnNewThread(final ThreadGroup tg, final boolean waitUntilDone, final Runnable runnable) {
        return RunnableTask.invokeOnNewThread(tg, null, waitUntilDone,
                new Runnable() {
                    public final void run() {
                        helper.runOutsideOfExclusiveContextThread(context, runnable);
                    } });
    }

    @Override
    public final boolean invoke(final boolean wait, final GLRunnable glRunnable) throws IllegalStateException {
        return helper.invoke(this, wait, glRunnable);
    }

    @Override
    public boolean invoke(final boolean wait, final List<GLRunnable> glRunnables) throws IllegalStateException {
        return helper.invoke(this, wait, glRunnables);
    }

    @Override
    public void flushGLRunnables() {
        helper.flushGLRunnables();
    }

    @Override
    public final void setAutoSwapBufferMode(final boolean enable) {
        helper.setAutoSwapBufferMode(enable);
    }

    @Override
    public final boolean getAutoSwapBufferMode() {
        return helper.getAutoSwapBufferMode();
    }

    @Override
    public final void setContextCreationFlags(final int flags) {
        additionalCtxCreationFlags = flags;
        final GLContext _context = context;
        if(null != _context) {
            _context.setContextCreationFlags(additionalCtxCreationFlags);
        }
    }

    @Override
    public final int getContextCreationFlags() {
        return additionalCtxCreationFlags;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation always supports multithreading, hence method always returns <code>true</code>.
     * </p>
     */
    @Override
    public final boolean isThreadGLCapable() { return true; }

    //
    // FPSCounter
    //

    @Override
    public final void setUpdateFPSFrames(final int frames, final PrintStream out) {
        fpsCounter.setUpdateFPSFrames(frames, out);
    }

    @Override
    public final void resetFPSCounter() {
        fpsCounter.resetFPSCounter();
    }

    @Override
    public final int getUpdateFPSFrames() {
        return fpsCounter.getUpdateFPSFrames();
    }

    @Override
    public final long getFPSStartTime()   {
        return fpsCounter.getFPSStartTime();
    }

    @Override
    public final long getLastFPSUpdateTime() {
        return fpsCounter.getLastFPSUpdateTime();
    }

    @Override
    public final long getLastFPSPeriod() {
        return fpsCounter.getLastFPSPeriod();
    }

    @Override
    public final float getLastFPS() {
        return fpsCounter.getLastFPS();
    }

    @Override
    public final int getTotalFPSFrames() {
        return fpsCounter.getTotalFPSFrames();
    }

    @Override
    public final long getTotalFPSDuration() {
        return fpsCounter.getTotalFPSDuration();
    }

    @Override
    public final float getTotalFPS() {
        return fpsCounter.getTotalFPS();
    }

    //
    // GLDrawable delegation
    //

    @Override
    public final GLContext createContext(final GLContext shareWith) {
        final RecursiveLock lock = getUpstreamLock();
        lock.lock();
        try {
            if(drawable != null) {
                final GLContext _ctx = drawable.createContext(shareWith);
                _ctx.setContextCreationFlags(additionalCtxCreationFlags);
                return _ctx;
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public final void setRealized(final boolean realized) {
        final RecursiveLock _lock = getUpstreamLock();
        _lock.lock();
        try {
            final GLDrawable _drawable = drawable;
            if( null == _drawable || realized && ( 0 >= _drawable.getSurfaceWidth() || 0 >= _drawable.getSurfaceHeight() ) ) {
                return;
            }
            _drawable.setRealized(realized);
            if( realized && _drawable.isRealized() ) {
                sendReshape=true; // ensure a reshape is being send ..
            }
        } finally {
            _lock.unlock();
        }
    }

    @Override
    public final boolean isRealized() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.isRealized() : false;
    }

    @Override
    public int getSurfaceWidth() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getSurfaceWidth() : 0;
    }

    @Override
    public int getSurfaceHeight() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getSurfaceHeight() : 0;
    }

    @Override
    public boolean isGLOriented() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.isGLOriented() : true;
    }

    @Override
    public final GLCapabilitiesImmutable getChosenGLCapabilities() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getChosenGLCapabilities() : null;
    }

    @Override
    public final GLCapabilitiesImmutable getRequestedGLCapabilities() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getRequestedGLCapabilities() : null;
    }

    @Override
    public final GLProfile getGLProfile() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getGLProfile() : null;
    }

    @Override
    public final NativeSurface getNativeSurface() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getNativeSurface() : null;
    }

    @Override
    public final long getHandle() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getHandle() : 0;
    }

    protected static String getThreadName() { return Thread.currentThread().getName(); }

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[ \n\tHelper: " + helper + ", \n\tDrawable: " + drawable +
               ", \n\tContext: " + context + /** ", \n\tWindow: "+window+ ", \n\tFactory: "+factory+ */ "]";
    }
}
