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

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.FPSCounter;
import javax.media.opengl.GL;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.util.Animator;


/**
 * Abstract common code for GLAutoDrawable implementations.
 * 
 * @see GLAutoDrawable
 * @see GLAutoDrawableDelegate
 * @see GLPBufferImpl
 * @see GLWindow
 */
public abstract class GLAutoDrawableBase implements GLAutoDrawable, FPSCounter {
    public static final boolean DEBUG = GLDrawableImpl.DEBUG;
    
    protected final GLDrawableHelper helper = new GLDrawableHelper();
    protected final FPSCounterImpl fpsCounter = new FPSCounterImpl();
    
    protected volatile GLDrawableImpl drawable; // volatile: avoid locking for read-only access
    protected GLContextImpl context;
    protected final boolean ownsDevice;
    protected int additionalCtxCreationFlags = 0;
    protected volatile boolean sendReshape = false; // volatile: maybe written by WindowManager thread w/o locking
    protected volatile boolean sendDestroy = false; // volatile: maybe written by WindowManager thread w/o locking

    /**
     * @param drawable upstream {@link GLDrawableImpl} instance, may be null for lazy initialization
     * @param context upstream {@link GLContextImpl} instance, may be null for lazy initialization
     * @param ownsDevice pass <code>true</code> if {@link AbstractGraphicsDevice#close()} shall be issued,
     *                   otherwise pass <code>false</code>. Closing the device is required in case
     *                   the drawable is created w/ it's own new instance, e.g. offscreen drawables,
     *                   and no further lifecycle handling is applied.
     */
    public GLAutoDrawableBase(GLDrawableImpl drawable, GLContextImpl context, boolean ownsDevice) {
        this.drawable = drawable;
        this.context = context;
        this.ownsDevice = ownsDevice;
        resetFPSCounter();        
    }
   
    /** Returns the recursive lock object of the upstream implementation, which synchronizes multithreaded access. */ 
    protected abstract RecursiveLock getLock();
    
    /** Default implementation to handle repaint events from the windowing system */
    protected final void defaultWindowRepaintOp() {
        final GLDrawable _drawable = drawable;
        if( null != _drawable && _drawable.isRealized() ) {
            if( !_drawable.getNativeSurface().isSurfaceLockedByOtherThread() && !helper.isExternalAnimatorAnimating() ) {
                display();
            }
        }
    }
    
    /** Default implementation to handle resize events from the windowing system. All required locks are being claimed. */
    protected final void defaultWindowResizedOp(int newWidth, int newHeight) throws NativeWindowException, GLException {
        GLDrawableImpl _drawable = drawable;
        if( null!=_drawable ) {
            if(DEBUG) {
                System.err.println("GLAutoDrawableBase.sizeChanged: ("+Thread.currentThread().getName()+"): "+newWidth+"x"+newHeight+" - surfaceHandle 0x"+Long.toHexString(getNativeSurface().getSurfaceHandle()));
            }
            if( ! _drawable.getChosenGLCapabilities().isOnscreen() ) {
                final RecursiveLock _lock = getLock();
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
                if( !_drawable.getNativeSurface().isSurfaceLockedByOtherThread() && !helper.isExternalAnimatorAnimating() ) {
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
            destroyAvoidAwareOfLocking();
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
        if ( helper.isAnimatorRunningOnOtherThread() ) {
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
        final RecursiveLock lock = getLock();
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
        final GLContext _context = context;
        final GLDrawable _drawable = drawable;
        if( null != _drawable ) {
            if( _drawable.isRealized() ) {
                if( null != _context && _context.isCreated() ) {
                    // Catch dispose GLExceptions by GLEventListener, just 'print' them
                    // so we can continue with the destruction.
                    try {
                        helper.disposeGL(this, _drawable, _context, null);
                    } catch (GLException gle) {
                        gle.printStackTrace();
                    }
                }
                _drawable.setRealized(false);
            }
            if( ownsDevice ) {
                _drawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice().close();
            }
        }
        context = null;
        drawable = null;        
    }
    
    public final void defaultSwapBuffers() throws GLException {
        final RecursiveLock _lock = getLock();
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
                helper.reshape(GLAutoDrawableBase.this, 0, 0, getWidth(), getHeight());
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
        final RecursiveLock _lock = getLock();
        _lock.lock();
        try {
            if( null != context ) {
                // surface is locked/unlocked implicit by context's makeCurrent/release
                helper.invokeGL(drawable, context, defaultDisplayAction, defaultInitAction);
            }
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
    public final GLContext setContext(GLContext newCtx) {
        final RecursiveLock lock = getLock();
        lock.lock();
        try {
            final GLContext oldCtx = context;
            final boolean newCtxCurrent = GLDrawableHelper.switchContext(drawable, oldCtx, newCtx, additionalCtxCreationFlags);
            context=(GLContextImpl)newCtx;
            if(newCtxCurrent) {
                context.makeCurrent();
            }
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
    public final GL setGL(GL gl) {
        final GLContext _context = context;
        if (_context != null) {
            _context.setGL(gl);
            return gl;
        }
        return null;
    }

    @Override
    public final void addGLEventListener(GLEventListener listener) {
        helper.addGLEventListener(listener);
    }

    @Override
    public final void addGLEventListener(int index, GLEventListener listener) throws IndexOutOfBoundsException {
        helper.addGLEventListener(index, listener);        
    }

    @Override
    public final void removeGLEventListener(GLEventListener listener) {
        helper.removeGLEventListener(listener);        
    }
    
    @Override
    public GLEventListener removeGLEventListener(int index) throws IndexOutOfBoundsException {
        return helper.removeGLEventListener(index);
    }
    
    @Override
    public final void setAnimator(GLAnimatorControl animatorControl)
            throws GLException {
        helper.setAnimator(animatorControl);        
    }

    @Override
    public final GLAnimatorControl getAnimator() {
        return helper.getAnimator();
    }

    @Override
    public final boolean invoke(boolean wait, GLRunnable glRunnable) {
        return helper.invoke(this, wait, glRunnable);        
    }

    @Override
    public final void setAutoSwapBufferMode(boolean enable) {
        helper.setAutoSwapBufferMode(enable);        
    }

    @Override
    public final boolean getAutoSwapBufferMode() {
        return helper.getAutoSwapBufferMode();
    }

    @Override
    public final void setContextCreationFlags(int flags) {
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

    //
    // FPSCounter
    //
    
    @Override
    public final void setUpdateFPSFrames(int frames, PrintStream out) {
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
        final RecursiveLock lock = getLock();
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
    public final boolean isRealized() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.isRealized() : false;
    }

    @Override
    public int getWidth() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getWidth() : 0;
    }

    @Override
    public int getHeight() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getHeight() : 0;
    }

    /**
     * @param t the thread for which context release shall be skipped, usually the animation thread,
     *          ie. {@link Animator#getThread()}.
     * @deprecated This is an experimental feature,
     *             intended for measuring performance in regards to GL context switch.
     */
    @Deprecated
    public void setSkipContextReleaseThread(Thread t) {
        helper.setSkipContextReleaseThread(t);
    }

    /**
     * @deprecated see {@link #setSkipContextReleaseThread(Thread)}
     */
    @Deprecated
    public Thread getSkipContextReleaseThread() {
        return helper.getSkipContextReleaseThread();
    }

    @Override
    public final GLCapabilitiesImmutable getChosenGLCapabilities() {
        final GLDrawable _drawable = drawable;
        return null != _drawable ? _drawable.getChosenGLCapabilities() : null;
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
    
    @Override
    public String toString() {
        return getClass().getSimpleName()+"[ \n\tHelper: " + helper + ", \n\tDrawable: " + drawable +
               ", \n\tContext: " + context + /** ", \n\tWindow: "+window+ ", \n\tFactory: "+factory+ */ "]";
    }
}
