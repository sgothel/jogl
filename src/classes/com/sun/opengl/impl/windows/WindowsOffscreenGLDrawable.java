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

package com.sun.opengl.impl.windows;

import javax.media.opengl.*;
import com.sun.opengl.impl.*;

public class WindowsOffscreenGLDrawable extends WindowsGLDrawable {
  private long origbitmap;
  private long hbitmap;
  // Width and height of the underlying bitmap
  private int  width;
  private int  height;

  public WindowsOffscreenGLDrawable(GLCapabilities capabilities,
                                    GLCapabilitiesChooser chooser) {
    super(capabilities, chooser);
  }

  public GLContext createContext(GLContext shareWith) {
    return new WindowsOffscreenGLContext(this, shareWith);
  }

  public void setSize(int newWidth, int newHeight) {
    width = newWidth;
    height = newHeight;
    if (hdc != 0) {
      destroy();
    }
    create();
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }
  
  private void create() {
    BITMAPINFO info = BITMAPINFO.create();
    BITMAPINFOHEADER header = info.bmiHeader();
    int bitsPerPixel = (capabilities.getRedBits() +
                        capabilities.getGreenBits() +
                        capabilities.getBlueBits());
    header.biSize(header.size());
    header.biWidth(width);
    // NOTE: negating the height causes the DIB to be in top-down row
    // order rather than bottom-up; ends up being correct during pixel
    // readback
    header.biHeight(-1 * height);
    header.biPlanes((short) 1);
    header.biBitCount((short) bitsPerPixel);
    header.biXPelsPerMeter(0);
    header.biYPelsPerMeter(0);
    header.biClrUsed(0);
    header.biClrImportant(0);
    header.biCompression(WGL.BI_RGB);
    header.biSizeImage(width * height * bitsPerPixel / 8);

    hdc = WGL.CreateCompatibleDC(0);
    if (hdc == 0) {
      System.out.println("LastError: " + WGL.GetLastError());
      throw new GLException("Error creating device context for offscreen OpenGL context");
    }
    hbitmap = WGL.CreateDIBSection(hdc, info, WGL.DIB_RGB_COLORS, 0, 0, 0);
    if (hbitmap == 0) {
      WGL.DeleteDC(hdc);
      hdc = 0;
      throw new GLException("Error creating offscreen bitmap of width " + width +
                            ", height " + height);
    }
    if ((origbitmap = WGL.SelectObject(hdc, hbitmap)) == 0) {
      WGL.DeleteObject(hbitmap);
      hbitmap = 0;
      WGL.DeleteDC(hdc);
      hdc = 0;
      throw new GLException("Error selecting bitmap into new device context");
    }
    
    choosePixelFormat(false);
  }
  
  public void destroy() {
    if (hdc != 0) {
      // Must destroy bitmap and device context
      WGL.SelectObject(hdc, origbitmap);
      WGL.DeleteObject(hbitmap);
      WGL.DeleteDC(hdc);
      origbitmap = 0;
      hbitmap = 0;
      hdc = 0;
      setChosenGLCapabilities(null);
    }
  }
}
