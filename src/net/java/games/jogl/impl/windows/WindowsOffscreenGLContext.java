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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

import java.awt.image.BufferedImage;
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class WindowsOffscreenGLContext extends WindowsGLContext {
  private long origbitmap;
  private long hbitmap;
  // Width and height of the underlying bitmap
  private int  width;
  private int  height;

  public WindowsOffscreenGLContext(GLCapabilities capabilities,
                                   GLCapabilitiesChooser chooser,
                                   GLContext shareWith) {
    super(null, capabilities, chooser, shareWith);
  }

  protected GL createGL()
  {
    return new WindowsGLImpl(this);
  }

  protected boolean isOffscreen() {
    return true;
  }

  public int getOffscreenContextBufferedImageType() {
    if (capabilities.getAlphaBits() > 0) {
      return BufferedImage.TYPE_INT_ARGB;
    } else {
      return BufferedImage.TYPE_INT_RGB;
    }
  }

  public int getOffscreenContextWidth() {
      return width;
  }

  public int getOffscreenContextHeight() {
      return height;
  }

  public int getOffscreenContextPixelDataType() {
      return GL.GL_UNSIGNED_BYTE;
  }
  
  public int getOffscreenContextReadBuffer() {
    // On Windows these contexts are always single-buffered
    return GL.GL_FRONT;
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    // We can take care of this in the DIB creation (see below)
    return false;
  }

  public boolean canCreatePbufferContext() {
    // For now say no
    return false;
  }

  public synchronized GLContext createPbufferContext(GLCapabilities capabilities,
                                                     int initialWidth,
                                                     int initialHeight) {
    throw new GLException("Not supported");
  }

  public void bindPbufferToTexture() {
    throw new GLException("Should not call this");
  }

  public void releasePbufferFromTexture() {
    throw new GLException("Should not call this");
  }

  protected synchronized boolean makeCurrent(Runnable initAction) throws GLException {
    if (pendingOffscreenResize) {
      if (pendingOffscreenWidth != width || pendingOffscreenHeight != height) {
        if (hglrc != 0) {
          destroyImpl();
        }
        width  = pendingOffscreenWidth;
        height = pendingOffscreenHeight;
        pendingOffscreenResize = false;
      }
    }
    return super.makeCurrent(initAction);
  }

  protected void destroyImpl() {
    if (hglrc != 0) {
      super.destroyImpl();
      // Must destroy OpenGL context, bitmap and device context
      WGL.SelectObject(hdc, origbitmap);
      WGL.DeleteObject(hbitmap);
      WGL.DeleteDC(hdc);
      origbitmap = 0;
      hbitmap = 0;
      hdc = 0;
    }
  }

  public synchronized void swapBuffers() throws GLException {
  }

  protected void create() {
    BITMAPINFO info = new BITMAPINFO();
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
      throw new GLException("Error creating offscreen bitmap");
    }
    if ((origbitmap = WGL.SelectObject(hdc, hbitmap)) == 0) {
      throw new GLException("Error selecting bitmap into new device context");
    }
    
    choosePixelFormatAndCreateContext(false);
  }
}
