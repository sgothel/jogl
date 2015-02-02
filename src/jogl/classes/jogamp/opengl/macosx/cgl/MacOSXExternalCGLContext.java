/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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

import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;

import jogamp.nativewindow.WrappedSurface;
import jogamp.opengl.GLContextShareSet;
import jogamp.opengl.macosx.cgl.MacOSXCGLDrawable.GLBackendType;


public class MacOSXExternalCGLContext extends MacOSXCGLContext {

  private MacOSXExternalCGLContext(final Drawable drawable, final boolean isNSContext, final long handle) {
    super(drawable, null);
    setOpenGLMode(isNSContext ? GLBackendType.NSOPENGL : GLBackendType.CGL );
    this.contextHandle = handle;
    GLContextShareSet.contextCreated(this);
    if( !setGLFunctionAvailability(false, 0, 0, CTX_PROFILE_COMPAT, false /* strictMatch */, false /* withinGLVersionsMapping */) ) { // use GL_VERSION
        throw new InternalError("setGLFunctionAvailability !strictMatch failed");
    }
    getGLStateTracker().setEnabled(false); // external context usage can't track state in Java
  }

  protected static MacOSXExternalCGLContext create(final GLDrawableFactory factory) {
    long pixelFormat = 0;
    long currentDrawable = 0;
    long contextHandle = CGL.getCurrentContext(); // Check: MacOSX 10.3 ..
    final boolean isNSContext = 0 != contextHandle;
    if( isNSContext ) {
        final long ctx = CGL.getCGLContext(contextHandle);
        if (ctx == 0) {
          throw new GLException("Error: NULL Context (CGL) of Context (NS) 0x" +Long.toHexString(contextHandle));
        }
        pixelFormat = CGL.CGLGetPixelFormat(ctx);
        currentDrawable = CGL.getNSView(contextHandle);
        if(DEBUG) {
            System.err.println("MacOSXExternalCGLContext Create Context (NS) 0x"+Long.toHexString(contextHandle)+
                               ", Context (CGL) 0x"+Long.toHexString(ctx)+
                               ", pixelFormat 0x"+Long.toHexString(pixelFormat)+
                               ", drawable 0x"+Long.toHexString(currentDrawable));
        }
    } else {
        contextHandle = CGL.CGLGetCurrentContext();
        if (contextHandle == 0) {
          throw new GLException("Error: current Context (CGL) null, no Context (NS)");
        }
        pixelFormat = CGL.CGLGetPixelFormat(contextHandle);
        if(DEBUG) {
            System.err.println("MacOSXExternalCGLContext Create Context (CGL) 0x"+Long.toHexString(contextHandle)+
                               ", pixelFormat 0x"+Long.toHexString(pixelFormat));
        }
    }

    if (0 == pixelFormat) {
      throw new GLException("Error: current pixelformat of current Context 0x"+Long.toHexString(contextHandle)+" is null");
    }
    final GLCapabilitiesImmutable caps = MacOSXCGLGraphicsConfiguration.CGLPixelFormat2GLCapabilities(pixelFormat);
    if(DEBUG) {
        System.err.println("MacOSXExternalCGLContext Create "+caps);
    }

    final AbstractGraphicsScreen aScreen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_MACOSX);
    final MacOSXCGLGraphicsConfiguration cfg = new MacOSXCGLGraphicsConfiguration(aScreen, caps, caps);

    if(0 == currentDrawable) {
        // set a fake marker stating a valid drawable
        currentDrawable = 1;
    }
    final WrappedSurface ns = new WrappedSurface(cfg, currentDrawable, 64, 64, true);
    return new MacOSXExternalCGLContext(new Drawable(factory, ns), isNSContext, contextHandle);
  }

  @Override
  protected boolean createImpl(final long shareWithHandle) throws GLException {
      return true;
  }

  @Override
  protected void makeCurrentImpl() throws GLException {
  }

  @Override
  protected void releaseImpl() throws GLException {
  }

  @Override
  protected void destroyImpl() throws GLException {
  }

  // Need to provide the display connection to extension querying APIs
  static class Drawable extends MacOSXCGLDrawable {
    Drawable(final GLDrawableFactory factory, final NativeSurface comp) {
      super(factory, comp, true);
    }

    @Override
    public GLContext createContext(final GLContext shareWith) {
      throw new GLException("Should not call this");
    }

    @Override
    public int getSurfaceWidth() {
      throw new GLException("Should not call this");
    }

    @Override
    public int getSurfaceHeight() {
      throw new GLException("Should not call this");
    }

    public void setSize(final int width, final int height) {
      throw new GLException("Should not call this");
    }
  }
}
