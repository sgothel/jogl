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

package com.sun.opengl.impl.x11.glx;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.opengl.impl.x11.*;

public class X11PbufferGLXContext extends X11GLXContext {
  private X11PbufferGLXDrawable drawable;

  public X11PbufferGLXContext(X11PbufferGLXDrawable drawable,
                             GLContext shareWith) {
    super(drawable, shareWith);
    this.drawable = drawable;
  }

  public void bindPbufferToTexture() {
    // FIXME: figure out how to implement this
    throw new GLException("Not yet implemented");
  }

  public void releasePbufferFromTexture() {
    // FIXME: figure out how to implement this
    throw new GLException("Not yet implemented");
  }


  public int getFloatingPointMode() {
    return drawable.getFloatingPointMode();
  }

  protected void create() {
    if (DEBUG) {
      System.err.println("Creating context for pbuffer " + drawable.getWidth() +
                         " x " + drawable.getHeight());
    }

    // Create a gl context for the p-buffer.
    X11GLXContext other = (X11GLXContext) GLContextShareSet.getShareContext(this);
    long share = 0;
    if (other != null) {
      share = other.getContext();
      if (share == 0) {
        throw new GLException("GLContextShareSet returned an invalid OpenGL context");
      }
    }
    X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)
        getGLDrawable().getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();

    context = GLX.glXCreateNewContext(drawable.getNativeWindow().getDisplayHandle(), 
                                      config.getFBConfig(), GLX.GLX_RGBA_TYPE, share, true);
    if (context == 0) {
      throw new GLException("pbuffer creation error: glXCreateNewContext() failed");
    }
    GLContextShareSet.contextCreated(this);

    if (DEBUG) {
      System.err.println("Created context for pbuffer " + drawable.getWidth() +
                         " x " + drawable.getHeight());
    }
  }
}
