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
 
package javax.media.opengl;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

import jogamp.opengl.Debug;
import jogamp.opengl.GLAutoDrawableBase;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;


/**
 * Fully functional {@link GLAutoDrawable} implementation
 * utilizing already created created {@link GLDrawable} and {@link GLContext} instances.
 * <p>
 * Since no native windowing system events are being processed, it is recommended
 * to handle at least:
 * <ul>
 *   <li>{@link com.jogamp.newt.event.WindowListener#windowRepaint(com.jogamp.newt.event.WindowUpdateEvent) repaint} using {@link #defaultWindowRepaintOp()}</li>
 *   <li>{@link com.jogamp.newt.event.WindowListener#windowResized(com.jogamp.newt.event.WindowEvent) resize} using {@link #defaultWindowResizedOp()}</li>
 *   <li>{@link com.jogamp.newt.event.WindowListener#windowDestroyNotify(com.jogamp.newt.event.WindowEvent) destroy-notify}  using {@link #defaultWindowDestroyNotifyOp()}</li> 
 * </ul> 
 * </p>
 * <p> 
 * See example {@link com.jogamp.opengl.test.junit.jogl.acore.TestGLAutoDrawableDelegateNEWT TestGLAutoDrawableDelegateNEWT}.
 * </p>
 */
public class GLAutoDrawableDelegate extends GLAutoDrawableBase {
    public static final boolean DEBUG = Debug.debug("GLAutoDrawableDelegate");
    
    public GLAutoDrawableDelegate(GLDrawable drawable, GLContext context) {
        super((GLDrawableImpl)drawable, (GLContextImpl)context);
    }
    
    //
    // make protected methods accessible
    //
    
    public void defaultWindowRepaintOp() {
        super.defaultWindowRepaintOp();
    }
    
    public void defaultWindowResizedOp() {
        super.defaultWindowResizedOp();
    }
    
    public void defaultWindowDestroyNotifyOp() {
        super.defaultWindowDestroyNotifyOp();
    }
    
    //
    // Complete GLAutoDrawable
    //
    
    private RecursiveLock lock = LockFactory.createRecursiveLock();  // instance wide lock

    /**
     * {@inheritDoc}
     * <p>
     * This implementation calls {@link #defaultDestroyOp()}.
     * </p>
     * <p>
     * User still needs to destroy the upstream window, which details are hidden from this aspect.
     * </p>
     */
    @Override
    public void destroy() {
        lock.lock();
        try {
            defaultDestroyOp();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void display() {
        if( sendDestroy ) {
            sendDestroy=false;
            destroy();
            return;
        }
        
        lock.lock(); // sync: context/drawable could been recreated/destroyed while animating
        try {
            if( null != drawable && drawable.isRealized() && null != context ) {
                // surface is locked/unlocked implicit by context's makeCurrent/release
                helper.invokeGL(drawable, context, defaultDisplayAction, defaultInitAction);
            }
        } finally {
            lock.unlock();
        }
    }
    
    //
    // GLDrawable delegation
    //
    
    @Override
    public final GLDrawableFactory getFactory() {
        return drawable.getFactory();
    }
    
    @Override
    public final void setRealized(boolean realized) {
    }

}
