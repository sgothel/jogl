/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package jogamp.opengl.macosx.cgl.awt;

import java.awt.Graphics;

import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import jogamp.opengl.GLContextImpl;
import jogamp.opengl.awt.Java2D;
import jogamp.opengl.awt.Java2DGLContext;
import jogamp.opengl.macosx.cgl.MacOSXCGLContext;
import jogamp.opengl.macosx.cgl.MacOSXCGLDrawable.GLBackendType;


/** MacOSXCGLContext implementation supporting the Java2D/JOGL bridge
 * on Mac OS X. The external GLDrawable mechanism does not work on Mac
 * OS X due to how drawables and contexts are operated upon on this
 * platform, so it is necessary to supply an alternative means to
 * create, make current, and destroy contexts on the Java2D "drawable"
 * on the Mac platform.
 */

public class MacOSXJava2DCGLContext extends MacOSXCGLContext implements Java2DGLContext {
  private Graphics graphics;

  // FIXME: ignoring context sharing for the time being; will need to
  // rethink this in particular if using FBOs to implement the
  // Java2D/OpenGL pipeline on Mac OS X

  MacOSXJava2DCGLContext(GLContext shareWith) {
    super(null, shareWith);
  }

  public void setGraphics(Graphics g) {
    this.graphics = g;
  }

  protected void makeCurrentImpl() throws GLException {
    if (!Java2D.makeOGLContextCurrentOnSurface(graphics, contextHandle)) {
      throw new GLException("Error making context current");
    }            
  }

  protected boolean createImpl(GLContextImpl shareWith) {
    long share = createImplPreset(shareWith);
    
    long ctx = Java2D.createOGLContextOnSurface(graphics, share);
    if (ctx == 0) {
      if(DEBUG) { 
          System.err.println("Error creating current: "+this);
      }
      return false;
    }
    if (!Java2D.makeOGLContextCurrentOnSurface(graphics, contextHandle)) {
      Java2D.destroyOGLContext(ctx);
      if(DEBUG) { 
          System.err.println("Error making created context current: "+this);
      }
      return false;
    }
    setGLFunctionAvailability(true, 0, 0, CTX_PROFILE_COMPAT|CTX_OPTION_ANY); // use GL_VERSION
    contextHandle = ctx;
    return true;
  }

  protected void releaseImpl() throws GLException {
    // FIXME: would need another primitive in the Java2D class in
    // order to implement this; hopefully should not matter for
    // correctness
  }

  protected void destroyImpl() throws GLException {
      Java2D.destroyOGLContext(contextHandle);
  }

  public void setOpenGLMode(GLBackendType mode) {
    if (mode != GLBackendType.CGL) {
      throw new GLException("OpenGL mode switching not supported for Java2D GLContexts");
    }
    super.setOpenGLMode(mode);
  }
}
