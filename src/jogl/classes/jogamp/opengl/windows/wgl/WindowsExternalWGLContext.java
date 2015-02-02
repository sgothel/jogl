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

package jogamp.opengl.windows.wgl;

import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.windows.GDI;
import jogamp.opengl.GLContextShareSet;

public class WindowsExternalWGLContext extends WindowsWGLContext {

  private WindowsExternalWGLContext(final Drawable drawable, final long ctx, final WindowsWGLGraphicsConfiguration cfg) {
    super(drawable, null);
    this.contextHandle = ctx;
    if (DEBUG) {
      System.err.println(getThreadName() + ": Created external OpenGL context " + toHexString(ctx) + " for " + this);
    }
    GLContextShareSet.contextCreated(this);
    if( !setGLFunctionAvailability(false, 0, 0, CTX_PROFILE_COMPAT, false /* strictMatch */, false /* withinGLVersionsMapping */) ) { // use GL_VERSION
        throw new InternalError("setGLFunctionAvailability !strictMatch failed");
    }
    getGLStateTracker().setEnabled(false); // external context usage can't track state in Java
  }

  protected static WindowsExternalWGLContext create(final GLDrawableFactory factory, final GLProfile glp) {
    if(DEBUG) {
        System.err.println("WindowsExternalWGLContext 0: werr: " + GDI.GetLastError());
    }

    final long ctx = WGL.wglGetCurrentContext();
    if (0 == ctx) {
      throw new GLException("Error: attempted to make an external GLContext without a context current, werr " + GDI.GetLastError());
    }

    final long hdc = WGL.wglGetCurrentDC();
    if (0 == hdc) {
      throw new GLException("Error: attempted to make an external GLDrawable without a drawable current, werr " + GDI.GetLastError());
    }
    final AbstractGraphicsScreen aScreen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_WINDOWS);
    WindowsWGLGraphicsConfiguration cfg;
    final int pfdID = WGLUtil.GetPixelFormat(hdc);
    if (0 == pfdID) {
        // This could have happened if the HDC was released right after the GL ctx made current (SWT),
        // WinXP-32bit will not be able to use this HDC afterwards.
        // Workaround: Use a fake default configuration
        final int werr = GDI.GetLastError();
        cfg = WindowsWGLGraphicsConfigurationFactory.createDefaultGraphicsConfiguration(new GLCapabilities(GLProfile.getDefault()), aScreen);
        cfg.markExternal();
        if(DEBUG) {
            System.err.println("WindowsExternalWGLContext invalid hdc/pfd werr "+werr+", using default cfg: " + cfg);
        }
    } else {
        cfg = WindowsWGLGraphicsConfiguration.createFromExternal(factory, hdc, pfdID, glp, aScreen, true);
        if(DEBUG) {
            System.err.println("WindowsExternalWGLContext valid hdc/pfd, retrieved cfg: " + cfg);
        }
    }
    return new WindowsExternalWGLContext(new Drawable(factory, new WrappedSurface(cfg, hdc, 64, 64, true)), ctx, cfg);
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
  static class Drawable extends WindowsWGLDrawable {
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
