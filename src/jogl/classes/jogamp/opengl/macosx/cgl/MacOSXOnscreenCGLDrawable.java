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
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;

public class MacOSXOnscreenCGLDrawable extends MacOSXCGLDrawable {
  private List<WeakReference<MacOSXCGLContext>> createdContexts = new ArrayList<WeakReference<MacOSXCGLContext>>();

  protected MacOSXOnscreenCGLDrawable(GLDrawableFactory factory, NativeSurface component) {
    super(factory, component, false);
  }

  public GLContext createContext(GLContext shareWith) {
    MacOSXOnscreenCGLContext ctx= new MacOSXOnscreenCGLContext(this, shareWith);
    // NOTE: we need to keep track of the created contexts in order to
    // implement swapBuffers() because of how Mac OS X implements its
    // OpenGL window interface
    synchronized (createdContexts) {
      createdContexts.add(new WeakReference<MacOSXCGLContext>(ctx));
    }
    return ctx;
  }

  protected void swapBuffersImpl() {
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
}
