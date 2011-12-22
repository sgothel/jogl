/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package jogamp.opengl;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GL;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

/** Platform-independent class exposing pbuffer functionality to
    applications. This class is not exposed in the public API as it
    would probably add no value; however it implements the GLDrawable
    interface so can be interacted with via its display() method. */

public class GLPbufferImpl implements GLPbuffer {
  private GLDrawableImpl pbufferDrawable;
  private GLContextImpl context;
  private GLDrawableHelper drawableHelper = new GLDrawableHelper();
  private int floatMode;
  private int additionalCtxCreationFlags = 0;

  public GLPbufferImpl(GLDrawableImpl pbufferDrawable,
                       GLContext parentContext) {
    GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable)
         pbufferDrawable.getNativeSurface().getGraphicsConfiguration().getChosenCapabilities();
    if(caps.isOnscreen()) {
        if(caps.isPBuffer()) {
            throw new IllegalArgumentException("Error: Given drawable is Onscreen and Pbuffer: "+pbufferDrawable);
        }
        throw new IllegalArgumentException("Error: Given drawable is Onscreen: "+pbufferDrawable);
    } else {
        if(!caps.isPBuffer()) {
            throw new IllegalArgumentException("Error: Given drawable is not Pbuffer: "+pbufferDrawable);
        }
    }
    this.pbufferDrawable = pbufferDrawable;
    context = (GLContextImpl) pbufferDrawable.createContext(parentContext);
    context.setSynchronized(true);
  }

  public GLContext createContext(GLContext shareWith) {
    return pbufferDrawable.createContext(shareWith);
  }

  public void setRealized(boolean realized) {
  }

  public boolean isRealized() {
    return true;
  }

  class DisposeAction implements Runnable {
    public void run() {
        // Lock: Covered by DestroyAction ..
        drawableHelper.dispose(GLPbufferImpl.this);
    }
  }
  DisposeAction disposeAction = new DisposeAction();

  public void destroy() {
    if(pbufferDrawable.isRealized()) {
        final AbstractGraphicsDevice adevice = pbufferDrawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
        
        if (null != context && context.isCreated()) {
            try {
                drawableHelper.invokeGL(pbufferDrawable, context, disposeAction, null);
            } catch (GLException gle) {
                gle.printStackTrace();
            }
            context.destroy();
            // drawableHelper.reset();
        }
        pbufferDrawable.destroy();
        
        if(null != adevice) {
            adevice.close();
        }
    }
  }

  public void setSize(int width, int height) {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  public NativeSurface getNativeSurface() {
      return pbufferDrawable.getNativeSurface();
  }

  public long getHandle() {
    return pbufferDrawable.getHandle();
  }

  public GLDrawableFactory getFactory() {
      return pbufferDrawable.getFactory();
  }

  public int getWidth() {
    return pbufferDrawable.getWidth();
  }

  public int getHeight() {
    return pbufferDrawable.getHeight();
  }

  public void display() {
    invokeGL(displayAction);
  }

  public void repaint() {
    display();
  }

  public void addGLEventListener(GLEventListener listener) {
    drawableHelper.addGLEventListener(listener);
  }

  public void addGLEventListener(int index, GLEventListener listener) {
    drawableHelper.addGLEventListener(index, listener);
  }

  public void removeGLEventListener(GLEventListener listener) {
    drawableHelper.removeGLEventListener(listener);
  }

  public void setAnimator(GLAnimatorControl animatorControl) {
    drawableHelper.setAnimator(animatorControl);
  }

  public GLAnimatorControl getAnimator() {
    return drawableHelper.getAnimator();
  }

  public void invoke(boolean wait, GLRunnable glRunnable) {
    drawableHelper.invoke(this, wait, glRunnable);
  }

  public void setContext(GLContext ctx) {
    context=(GLContextImpl)ctx;
    if(null != context) {
        context.setContextCreationFlags(additionalCtxCreationFlags);
    }    
  }

  public GLContext getContext() {
    return context;
  }

  public GLDrawable getDrawable() {
    return pbufferDrawable;
  }

  public GL getGL() {
    return getContext().getGL();
  }

  public GL setGL(GL gl) {
    return getContext().setGL(gl);
  }

  public void setAutoSwapBufferMode(boolean onOrOff) {
    drawableHelper.setAutoSwapBufferMode(onOrOff);
  }

  public boolean getAutoSwapBufferMode() {
    return drawableHelper.getAutoSwapBufferMode();
  }

  public void swapBuffers() {
    invokeGL(swapBuffersAction);
  }

  public void setContextCreationFlags(int flags) {
    additionalCtxCreationFlags = flags;
    if(null != context) {
        context.setContextCreationFlags(additionalCtxCreationFlags);
    }        
  }
      
  public int getContextCreationFlags() {
    return additionalCtxCreationFlags;                
  }
            
  public void bindTexture() {
    // Doesn't make much sense to try to do this on the event dispatch
    // thread given that it has to be called while the context is current
    context.bindPbufferToTexture();
  }

  public void releaseTexture() {
    // Doesn't make much sense to try to do this on the event dispatch
    // thread given that it has to be called while the context is current
    context.releasePbufferFromTexture();
  }

  public GLCapabilitiesImmutable getChosenGLCapabilities() {
    if (pbufferDrawable == null)
      return null;

    return pbufferDrawable.getChosenGLCapabilities();
  }

  public GLCapabilitiesImmutable getRequestedGLCapabilities() {
    if (pbufferDrawable == null)
      return null;

    return pbufferDrawable.getRequestedGLCapabilities();
  }

  public GLProfile getGLProfile() {
    if (pbufferDrawable == null)
      return null;

    return pbufferDrawable.getGLProfile();
  }

  private RecursiveLock recurLock = LockFactory.createRecursiveLock();

  public int lockSurface() throws GLException {
    recurLock.lock();
    return NativeSurface.LOCK_SUCCESS;
  }

  public void unlockSurface() {
    recurLock.unlock();
  }

  public boolean isSurfaceLocked() {
    return recurLock.isLocked();
  }

  public int getFloatingPointMode() {
    if (floatMode == 0) {
      throw new GLException("Pbuffer not initialized, or floating-point support not requested");
    }
    return floatMode;
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void invokeGL(Runnable invokeGLAction) {
    drawableHelper.invokeGL(pbufferDrawable, context, invokeGLAction, initAction);
  }


  class InitAction implements Runnable {
    public void run() {
      floatMode = context.getFloatingPointMode();
      drawableHelper.init(GLPbufferImpl.this);
    }
  }
  private InitAction initAction = new InitAction();
  
  class DisplayAction implements Runnable {
    public void run() {
      drawableHelper.display(GLPbufferImpl.this);
    }
  }
  private DisplayAction displayAction = new DisplayAction();

  class SwapBuffersAction implements Runnable {
    public void run() {
      pbufferDrawable.swapBuffers();
    }
  }
  private SwapBuffersAction swapBuffersAction = new SwapBuffersAction();
}
