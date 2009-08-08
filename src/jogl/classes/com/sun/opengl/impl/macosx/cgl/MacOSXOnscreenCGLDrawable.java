/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl.macosx.cgl;

import java.lang.ref.WeakReference;
import java.security.*;
import java.util.*;

import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class MacOSXOnscreenCGLDrawable extends MacOSXCGLDrawable {
  private List/*<WeakReference<GLContext>>*/ createdContexts =
    new ArrayList();

  protected MacOSXOnscreenCGLDrawable(GLDrawableFactory factory, NativeWindow component) {
    super(factory, component, false);
  }

  public GLContext createContext(GLContext shareWith) {
    MacOSXOnscreenCGLContext context =
      new MacOSXOnscreenCGLContext(this, shareWith);
    // NOTE: we need to keep track of the created contexts in order to
    // implement swapBuffers() because of how Mac OS X implements its
    // OpenGL window interface
    synchronized (this) {
      List newContexts = new ArrayList();
      newContexts.addAll(createdContexts);
      newContexts.add(new WeakReference(context));
      createdContexts = newContexts;
    }
    return context;
  }

  public int getWidth() {
    return component.getWidth();
  }

  public int getHeight() {
    return component.getHeight();
  }

  protected void swapBuffersImpl() {
    for (Iterator iter = createdContexts.iterator(); iter.hasNext(); ) {
      WeakReference ref = (WeakReference) iter.next();
      MacOSXOnscreenCGLContext ctx = (MacOSXOnscreenCGLContext) ref.get();
      // FIXME: clear out unreachable contexts
      if (ctx != null) {
        ctx.swapBuffers();
      }
    }
  }
  
  public void setOpenGLMode(int mode) {
    if (mode != NSOPENGL_MODE)
      throw new GLException("OpenGL mode switching not supported for on-screen GLDrawables");
  }

  public int  getOpenGLMode() {
    return NSOPENGL_MODE;
  }
}
