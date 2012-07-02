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

import jogamp.opengl.Debug;
import jogamp.opengl.GLAutoDrawableBase;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableImpl;


/**
 * Fully functional {@link GLAutoDrawable} implementation
 * utilizing already created created {@link GLDrawable} and {@link GLContext} instances.
 * <p>
 * Since no native windowing system events are being processed, it is recommended
 * to handle at least {@link com.jogamp.newt.event.WindowListener#windowResized(com.jogamp.newt.event.WindowEvent) resize},
 * {@link com.jogamp.newt.event.WindowListener#windowDestroyNotify(com.jogamp.newt.event.WindowEvent) destroy-notify} 
 * and maybe {@link com.jogamp.newt.event.WindowListener#windowRepaint(com.jogamp.newt.event.WindowUpdateEvent) repaint}. 
 * The latter is only required if no {@link GLAnimatorControl} is being used.
 * </p> 
 */
public class GLAutoDrawableDelegate extends GLAutoDrawableBase {
    public static final boolean DEBUG = Debug.debug("GLAutoDrawableDelegate");
    
    public GLAutoDrawableDelegate(GLDrawable drawable, GLContext context) {
        super((GLDrawableImpl)drawable, (GLContextImpl)context);
    }
    
    public void defaultRepaintOp() {
        super.defaultRepaintOp();
    }
    
    public void defaultReshapeOp() {
        super.defaultReshapeOp();
    }
    
    //
    // Complete GLAutoDrawable
    //
    
    /**
     * {@inheritDoc}
     * <p>
     * This implementation simply removes references to drawable and context.
     * </p>
     */
    @Override
    public void destroy() {
        drawable = null;
        context = null;
    }

    @Override
    public void display() {
        if( null == drawable || !drawable.isRealized() || null == context ) { return; }

        // surface is locked/unlocked implicit by context's makeCurrent/release
        helper.invokeGL(drawable, context, defaultDisplayAction, defaultInitAction);
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
