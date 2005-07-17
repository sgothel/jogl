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

package net.java.games.jogl.impl.windows;

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class WindowsDummyGLDrawable extends WindowsGLDrawable {
  private long hwnd;

  public WindowsDummyGLDrawable() {
    super(null, new GLCapabilities(), null);
    // All entries to CreateDummyWindow must synchronize on one object
    // to avoid accidentally registering the dummy window class twice
    synchronized (WindowsDummyGLDrawable.class) {
      hwnd = WGL.CreateDummyWindow(0, 0, 1, 1);
    }
    hdc = WGL.GetDC(hwnd);
    // Choose a (hopefully hardware-accelerated) OpenGL pixel format for this device context
    GLCapabilities caps = new GLCapabilities();
    caps.setDepthBits(16);
    PIXELFORMATDESCRIPTOR pfd = glCapabilities2PFD(caps, true);
    int pixelFormat = WGL.ChoosePixelFormat(hdc, pfd);
    if ((pixelFormat == 0) ||
        (!WGL.SetPixelFormat(hdc, pixelFormat, pfd))) {
      destroy();
    }
  }

  public void setSize(int width, int height) {
  }

  public int getWidth() {
    return 1;
  }

  public int getHeight() {
    return 1;
  }

  public GLContext createContext(GLContext shareWith) {
    if (hdc == 0) {
      // Construction failed
      return null;
    }
    return new WindowsGLContext(this, shareWith);
  }

  public void destroy() {
    if (hdc != 0) {
      WGL.ReleaseDC(hwnd, hdc);
      hdc = 0;
    }
    if (hwnd != 0) {
      WGL.ShowWindow(hwnd, WGL.SW_HIDE);
      WGL.DestroyWindow(hwnd);
      hwnd = 0;
    }
  }
}
