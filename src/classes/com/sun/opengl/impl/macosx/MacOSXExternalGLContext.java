/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl.macosx;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class MacOSXExternalGLContext extends MacOSXGLContext {
  private boolean firstMakeCurrent = true;
  private boolean created = true;
  private GLContext lastContext;

  public MacOSXExternalGLContext() {
    super(null, null);

    // FIXME: we don't have a "current context" primitive implemented
    // yet on OS X. In the current implementation this would need to
    // return an NSOpenGLContext*, but "external" toolkits are not
    // guaranteed to be using the Cocoa OpenGL API. Additionally, if
    // we switched this implementation to use the low-level CGL APIs,
    // we would lose the ability to share textures and display lists
    // between contexts since you need an NSOpenGLContext, not a
    // CGLContextObj, in order to share textures and display lists
    // between two NSOpenGLContexts.
    //
    // The ramifications here are that it is not currently possible to
    // share textures and display lists between an OpenGL context
    // created by JOGL and one created by a third-party library on OS
    // X.

    // context = CGL.CGLGetCurrentContext();

    GLContextShareSet.contextCreated(this);
    resetGLFunctionAvailability();
  }

  protected boolean create() {
    return true;
  }

  public int makeCurrent() throws GLException {
    // Save last context if necessary to allow external GLContexts to
    // talk to other GLContexts created by this library
    GLContext cur = getCurrent();
    if (cur != null && cur != this) {
      lastContext = cur;
      setCurrent(null);
    }
    return super.makeCurrent();
  }  

  public void release() throws GLException {
    super.release();
    setCurrent(lastContext);
    lastContext = null;
  }

  protected int makeCurrentImpl() throws GLException {
    if (firstMakeCurrent) {
      firstMakeCurrent = false;
      return CONTEXT_CURRENT_NEW;
    }
    return CONTEXT_CURRENT;
  }

  protected void releaseImpl() throws GLException {
  }

  protected void destroyImpl() throws GLException {
    created = false;
    GLContextShareSet.contextDestroyed(this);
  }

  public boolean isCreated() {
    return created;
  }

  public void setOpenGLMode(int mode) {
    if (mode != MacOSXGLDrawable.NSOPENGL_MODE)
      throw new GLException("OpenGL mode switching not supported for external GLContexts");
  }
    
  public int  getOpenGLMode() {
    return MacOSXGLDrawable.NSOPENGL_MODE;
  }
}
