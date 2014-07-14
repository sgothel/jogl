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

package jogamp.opengl.x11.glx;

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLDrawableFactory;

import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;

public abstract class X11GLXDrawable extends GLDrawableImpl {
  protected X11GLXDrawable(final GLDrawableFactory factory, final NativeSurface comp, final boolean realized) {
    super(factory, comp, realized);
  }

  @Override
  public GLDynamicLookupHelper getGLDynamicLookupHelper() {
    return getFactoryImpl().getGLDynamicLookupHelper(0);
  }

  @Override
  protected void setRealizedImpl() {
    if(realized) {
        final X11GLXGraphicsConfiguration config = (X11GLXGraphicsConfiguration)getNativeSurface().getGraphicsConfiguration();
        config.updateGraphicsConfiguration();

        if (DEBUG) {
          System.err.println(getThreadName()+": X11GLXDrawable.setRealized(true): "+config);
        }
    }
  }

  @Override
  protected final void swapBuffersImpl(final boolean doubleBuffered) {
    if(doubleBuffered) {
        GLX.glXSwapBuffers(getNativeSurface().getDisplayHandle(), getHandle());
    }
  }

  //---------------------------------------------------------------------------
  // Internals only below this point
  //
}
