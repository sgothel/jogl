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

package jogamp.opengl.macosx.cgl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;

import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;

public abstract class MacOSXCGLDrawable extends GLDrawableImpl {
  // The Java2D/OpenGL pipeline on OS X uses low-level CGLContextObjs
  // to represent the contexts for e.g. the Java2D back buffer. When
  // the Java2D/JOGL bridge is active, this means that if we want to
  // be able to share textures and display lists with the Java2D
  // contexts, we need to use the CGL APIs rather than the NSOpenGL
  // APIs on the JOGL side. For example, if we create a pbuffer using
  // the NSOpenGL APIs and want to share textures and display lists
  // between it and the Java2D back buffer, there is no way to do so,
  // because the Java2D context is actually a CGLContextObj and the
  // NSOpenGLContext's initWithFormat:shareContext: only accepts an
  // NSOpenGLContext as its second argument. Of course there is no way
  // to wrap an NSOpenGLContext around an arbitrary CGLContextObj.
  //
  // The situation we care most about is allowing a GLPbuffer to share
  // textures, etc. with a GLJPanel when the Java2D/JOGL bridge is
  // active; several of the demos rely on this functionality. We aim
  // to get there by allowing a GLPBuffer to switch its implementation
  // between using an NSOpenGLPixelBuffer and a CGLPBufferObj. In
  // order to track whether this has been done we need to have the
  // notion of a "mode" of both the MacOSXCGLDrawable and the
  // MacOSXGLContext. Initially the mode is "unspecified", meaning it
  // leans toward the default (NSOpenGL). If sharing is requested
  // between either a GLJPanel and a GLPbuffer or a GLCanvas and a
  // GLPbuffer, the GLPbuffer will be switched into the appropriate
  // mode: CGL mode for a GLJPanel and NSOpenGL mode for a GLCanvas.
  // To avoid thrashing we support exactly one such switch during the
  // lifetime of a given GLPbuffer. This is not a fully general
  // solution (for example, you can't share textures among a
  // GLPbuffer, a GLJPanel and a GLCanvas simultaneously) but should
  // be enough to get things off the ground.  
  public enum GLBackendType {
    NSOPENGL(0), CGL(1); 
    
    public final int id;

    GLBackendType(int id){
        this.id = id;
    }
  }
  private List<WeakReference<MacOSXCGLContext>> createdContexts = new ArrayList<WeakReference<MacOSXCGLContext>>();
  
  private boolean haveSetOpenGLMode = false;
  private GLBackendType openGLMode = GLBackendType.NSOPENGL;
  
  public MacOSXCGLDrawable(GLDrawableFactory factory, NativeSurface comp, boolean realized) {
    super(factory, comp, realized);
    initOpenGLImpl(getOpenGLMode());
  }
  
  protected void setRealizedImpl() {
  }

  protected long getNSViewHandle() {
      return GLBackendType.NSOPENGL == openGLMode ? getHandle() : null;
  }
  
  protected void registerContext(MacOSXCGLContext ctx) {
    // NOTE: we need to keep track of the created contexts in order to
    // implement swapBuffers() because of how Mac OS X implements its
    // OpenGL window interface
    synchronized (createdContexts) {
      createdContexts.add(new WeakReference<MacOSXCGLContext>(ctx));
    }
  }
  protected final void swapBuffersImpl() {
    // single-buffer is already filtered out @ GLDrawableImpl#swapBuffers()
    synchronized (createdContexts) {
        for (Iterator<WeakReference<MacOSXCGLContext>> iter = createdContexts.iterator(); iter.hasNext(); ) {
          WeakReference<MacOSXCGLContext> ref = iter.next();
          MacOSXCGLContext ctx = ref.get();
          if (ctx != null) {
            ctx.swapBuffers();
          } else {
            iter.remove();
          }
        }
    }
  }  
    
  public GLDynamicLookupHelper getGLDynamicLookupHelper() {
    return getFactoryImpl().getGLDynamicLookupHelper(0);
  }

  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }

  // Support for "mode switching" as described in MacOSXCGLDrawable
  public void setOpenGLMode(GLBackendType mode) {
      if (mode == openGLMode) {
        return;
      }
      if (haveSetOpenGLMode) {
        throw new GLException("Can't switch between using NSOpenGLPixelBuffer and CGLPBufferObj more than once");
      }
    
      destroyImpl();
      if (DEBUG) {
        System.err.println("Switching context mode " + openGLMode + " -> " + mode);
      }
      initOpenGLImpl(mode);
      openGLMode = mode;
      haveSetOpenGLMode = true;      
  }
  public final GLBackendType getOpenGLMode() { return openGLMode; }

  protected void initOpenGLImpl(GLBackendType backend) { /* nop */ }
  
}
