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

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.FPSCounter;
import javax.media.opengl.GL;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLAutoDrawableDelegate;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

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
    public static final boolean DEBUG = Debug.debug("GLAutoDrawable");
    
    protected final GLDrawableHelper helper = new GLDrawableHelper();
    protected final FPSCounterImpl fpsCounter = new FPSCounterImpl();
    
    protected GLDrawableImpl drawable;
    protected GLContextImpl context;
    protected int additionalCtxCreationFlags = 0;
    protected boolean sendReshape = false;
    protected boolean sendDestroy = false;

    public GLAutoDrawableBase(GLDrawableImpl drawable, GLContextImpl context) {
        this.drawable = drawable;
        this.context = context;
        resetFPSCounter();        
    }
   
    /** Returns the delegated GLDrawable */
    public final GLDrawable getDelegatedDrawable() { return drawable; }
    
    protected void defaultRepaintOp() {
        if( null != drawable && drawable.isRealized() ) {
            if( !drawable.getNativeSurface().isSurfaceLockedByOtherThread() && !helper.isAnimatorAnimating() ) {
                display();
            }
        }        
    }
    
    protected void defaultReshapeOp() {
        if( null!=drawable ) {
            if(DEBUG) {
                System.err.println("GLAutoDrawableBase.sizeChanged: ("+Thread.currentThread().getName()+"): "+getWidth()+"x"+getHeight()+" - surfaceHandle 0x"+Long.toHexString(getNativeSurface().getSurfaceHandle()));
            }
            sendReshape = true;
            defaultRepaintOp();
        }
    }
            
    //
    // GLAutoDrawable
    //
    
    protected final Runnable defaultInitAction = new Runnable() {
        @Override
        public final void run() {
            // Lock: Locked Surface/Window by MakeCurrent/Release
            helper.init(GLAutoDrawableBase.this);
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

    @Override
    public final GLContext getContext() {
        return context;
    }

    @Override
    public final GLContext setContext(GLContext newCtx) {
        final GLContext oldCtx = context;
        final boolean newCtxCurrent = helper.switchContext(drawable, oldCtx, newCtx, additionalCtxCreationFlags);
        context=(GLContextImpl)newCtx;
        if(newCtxCurrent) {
            context.makeCurrent();
        }
        return oldCtx;
    }

    @Override
    public final GL getGL() {
        if (context == null) {
            return null;
        }
        return context.getGL();
    }

    @Override
    public final GL setGL(GL gl) {
        if (context != null) {
            context.setGL(gl);
            return gl;
        }
        return null;
    }

    @Override
    public final void addGLEventListener(GLEventListener listener) {
        helper.addGLEventListener(listener);
    }

    @Override
    public final void addGLEventListener(int index, GLEventListener listener)
            throws IndexOutOfBoundsException {
        helper.addGLEventListener(index, listener);        
    }

    @Override
    public final void removeGLEventListener(GLEventListener listener) {
        helper.removeGLEventListener(listener);        
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
    public final void invoke(boolean wait, GLRunnable glRunnable) {
        helper.invoke(this, wait, glRunnable);        
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
        if(null != context) {
            context.setContextCreationFlags(additionalCtxCreationFlags);
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
        if(drawable != null) {
            final GLContext _ctx = drawable.createContext(shareWith);
            _ctx.setContextCreationFlags(additionalCtxCreationFlags);
            return _ctx;
        }
        return null;
    }

    @Override
    public final boolean isRealized() {
        return null != drawable ? drawable.isRealized() : false;
    }

    @Override
    public int getWidth() {
        return null != drawable ? drawable.getWidth() : 0;
    }

    @Override
    public int getHeight() {
        return null != drawable ? drawable.getHeight() : 0;
    }

    /**
     * @param t the thread for which context release shall be skipped, usually the animation thread,
     *          ie. {@link Animator#getThread()}.
     * @deprecated this is an experimental feature,
     *             intended for measuring performance in regards to GL context switch
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
    public final void swapBuffers() throws GLException {
        if(drawable!=null && context != null) {
            drawable.swapBuffers();
        }
    }

    @Override
    public final GLCapabilitiesImmutable getChosenGLCapabilities() {
        return null != drawable ? drawable.getChosenGLCapabilities() : null;
    }

    @Override
    public final GLProfile getGLProfile() {
        return null != drawable ? drawable.getGLProfile() : null;
    }

    @Override
    public final NativeSurface getNativeSurface() {
        return null != drawable ? drawable.getNativeSurface() : null;
    }

    @Override
    public final long getHandle() {
        return null != drawable ? drawable.getHandle() : 0;
    }
}
