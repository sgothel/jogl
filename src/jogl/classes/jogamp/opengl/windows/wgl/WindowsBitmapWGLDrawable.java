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

import com.jogamp.common.nio.PointerBuffer;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.SurfaceChangeable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;

import jogamp.nativewindow.windows.BITMAPINFO;
import jogamp.nativewindow.windows.BITMAPINFOHEADER;
import jogamp.nativewindow.windows.GDI;
import javax.media.opengl.GLCapabilitiesImmutable;

public class WindowsBitmapWGLDrawable extends WindowsWGLDrawable {
  private long origbitmap;
  private long hbitmap;

  protected WindowsBitmapWGLDrawable(GLDrawableFactory factory, NativeSurface target) {
    super(factory, target, false);
  }

  protected void destroyImpl() {
      setRealized(false);
  }
  
  protected void setRealizedImpl() {
    if(realized) {
        createBitmap();
    } else {
        destroyBitmap();
    }
  }

  public GLContext createContext(GLContext shareWith) {
    return new WindowsBitmapWGLContext(this, shareWith);
  }

  private void createBitmap() {
    int werr;
    NativeSurface ns = getNativeSurface();
    if(DEBUG) {
        System.err.println("WindowsBitmapWGLDrawable (1): "+ns);
    }
    WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)ns.getGraphicsConfiguration();
    GLCapabilitiesImmutable capabilities = (GLCapabilitiesImmutable)config.getRequestedCapabilities();
    int width = getWidth();
    int height = getHeight();

    //
    // 1. Create DIB Section
    //
    BITMAPINFO info = BITMAPINFO.create();
    BITMAPINFOHEADER header = info.getBmiHeader();
    int bitsPerPixel = (capabilities.getRedBits() +
                        capabilities.getGreenBits() +
                        capabilities.getBlueBits() +
                        capabilities.getAlphaBits());
    header.setBiSize(BITMAPINFOHEADER.size());
    header.setBiWidth(width);
    // NOTE: negating the height causes the DIB to be in top-down row
    // order rather than bottom-up; ends up being correct during pixel
    // readback
    header.setBiHeight(-1 * height);
    header.setBiPlanes((short) 1);
    header.setBiBitCount((short) bitsPerPixel);
    header.setBiXPelsPerMeter(0);
    header.setBiYPelsPerMeter(0);
    header.setBiClrUsed(0);
    header.setBiClrImportant(0);
    header.setBiCompression(GDI.BI_RGB);
    int byteNum = width * height * ( bitsPerPixel >> 3 ) ;
    header.setBiSizeImage(byteNum);

    PointerBuffer pb = PointerBuffer.allocateDirect(1);
    hbitmap = GDI.CreateDIBSection(0, info, GDI.DIB_RGB_COLORS, pb, 0, 0);
    werr = GDI.GetLastError();
    if(DEBUG) {
        long p = ( pb.capacity() > 0 ) ? pb.get(0) : 0;
        System.err.println("WindowsBitmapWGLDrawable: pb sz/ptr "+pb.capacity() + ", "+toHexString(p));
        System.err.println("WindowsBitmapWGLDrawable: " + width+"x"+height +
                            ", bpp " + bitsPerPixel +
                            ", bytes " + byteNum +
                            ", header sz " + BITMAPINFOHEADER.size() +
                            ", DIB ptr num " + pb.capacity()+
                            ", "+capabilities+
                            ", werr "+werr);
    }
    if (hbitmap == 0) {
      throw new GLException("Error creating offscreen bitmap of " + ns + ", werr " + werr);
    }

    //
    // 2. Create memory DC (device context) , and associate it with the DIB.
    //
    long hdc = GDI.CreateCompatibleDC(0);
    werr = GDI.GetLastError();
    if (hdc == 0) {
        GDI.DeleteObject(hbitmap);
        hbitmap = 0;
      throw new GLException("Error creating device context for offscreen OpenGL context, werr "+werr);
    }
    ((SurfaceChangeable)ns).setSurfaceHandle(hdc);
    if(DEBUG) {
        System.err.println("WindowsBitmapWGLDrawable (2): "+ns);
    }

    if ((origbitmap = GDI.SelectObject(hdc, hbitmap)) == 0) {
      GDI.DeleteObject(hbitmap);
      hbitmap = 0;
      GDI.DeleteDC(hdc);
      hdc = 0;
      throw new GLException("Error selecting bitmap into new device context");
    }
    
    config.updateGraphicsConfiguration(getFactory(), ns, null);
  }
  
  protected void destroyBitmap() {
    NativeSurface ns = getNativeSurface();
    if (ns.getSurfaceHandle() != 0) {
      // Must destroy bitmap and device context
      GDI.SelectObject(ns.getSurfaceHandle(), origbitmap);
      GDI.DeleteObject(hbitmap);
      GDI.DeleteDC(ns.getSurfaceHandle());
      origbitmap = 0;
      hbitmap = 0;
      ((SurfaceChangeable)ns).setSurfaceHandle(0);
    }
  }
}
