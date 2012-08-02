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

import javax.media.nativewindow.AbstractGraphicsDevice;

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
    
    /**
     * @param drawable a valid {@link GLDrawable}, may not be realized yet.
     * @param context a valid {@link GLContext}, may not be made current (created) yet.
     * @param upstreamWidget optional UI element holding this instance, see {@link #getUpstreamWidget()}.
     * @param ownDevice pass <code>true</code> if {@link AbstractGraphicsDevice#close()} shall be issued,
     *                  otherwise pass <code>false</code>. Closing the device is required in case
     *                  the drawable is created w/ it's own new instance, e.g. offscreen drawables,
     *                  and no further lifecycle handling is applied.
     */
    public GLAutoDrawableDelegate(GLDrawable drawable, GLContext context, Object upstreamWidget, boolean ownDevice) {
        super((GLDrawableImpl)drawable, (GLContextImpl)context, ownDevice);
        this.upstreamWidget = null;
    }
    
    //
    // expose default methods
    //
    
    public final void windowRepaintOp() {
        super.defaultWindowRepaintOp();
    }
    
    public final void windowResizedOp() {
        super.defaultWindowResizedOp();
    }
    
    public final void windowDestroyNotifyOp() {
        super.defaultWindowDestroyNotifyOp();
    }
    
    //
    // Complete GLAutoDrawable
    //
    
    private final RecursiveLock lock = LockFactory.createRecursiveLock();  // instance wide lock
    private final Object upstreamWidget;
    
    @Override
    protected final RecursiveLock getLock() { return lock; }
    
    @Override
    public final Object getUpstreamWidget() {
        return upstreamWidget;
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
    public final void setRealized(boolean realized) {
    }

    @Override
    public final void swapBuffers() throws GLException {
         defaultSwapBuffers();
    }
        
}
