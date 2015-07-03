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
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.windows.GDI;


public class WindowsExternalWGLDrawable extends WindowsWGLDrawable {

  private WindowsExternalWGLDrawable(final GLDrawableFactory factory, final NativeSurface component) {
    super(factory, component, true);
  }

  protected static WindowsExternalWGLDrawable create(final GLDrawableFactory factory, final GLProfile glp) {
    final long hdc = WGL.wglGetCurrentDC();
    if (0==hdc) {
      throw new GLException("Error: attempted to make an external GLDrawable without a drawable current, werr " + GDI.GetLastError());
    }
    final int pfdID = WGLUtil.GetPixelFormat(hdc);
    if (pfdID == 0) {
      throw new GLException("Error: attempted to make an external GLContext without a valid pixelformat, werr " + GDI.GetLastError());
    }

    final AbstractGraphicsScreen aScreen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_WINDOWS);
    final WindowsWGLGraphicsConfiguration cfg = WindowsWGLGraphicsConfiguration.createFromExternal(factory, hdc, pfdID, glp, aScreen, true);
    return new WindowsExternalWGLDrawable(factory, new WrappedSurface(cfg, hdc, 64, 64, true));
  }


  @Override
  public GLContext createContext(final GLContext shareWith) {
    return new WindowsWGLContext(this, shareWith);
  }

  public void setSize(final int newWidth, final int newHeight) {
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
}
