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

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.MutableSurface;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.windows.BITMAPINFO;
import jogamp.nativewindow.windows.BITMAPINFOHEADER;
import jogamp.nativewindow.windows.GDI;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.nio.PointerBuffer;

public class WindowsBitmapWGLDrawable extends WindowsWGLDrawable {
  private long origbitmap;
  private long hbitmap;

  private WindowsBitmapWGLDrawable(final GLDrawableFactory factory, final NativeSurface comp) {
    super(factory, comp, false);
  }

  protected static WindowsBitmapWGLDrawable create(final GLDrawableFactory factory, final NativeSurface comp) {
    final WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)comp.getGraphicsConfiguration();
    final AbstractGraphicsDevice aDevice = config.getScreen().getDevice();
    if( !GLProfile.isAvailable(aDevice, GLProfile.GL2) ) {
        throw new GLException("GLProfile GL2 n/a on "+aDevice+" but required for Windows BITMAP");
    }
    final GLProfile glp = GLProfile.get(GLProfile.GL2);
    final GLCapabilitiesImmutable capsChosen0 = (GLCapabilitiesImmutable)config.getChosenCapabilities();
    // RGB555 and also alpha channel is experienced to fail on some Windows machines
    final GLCapabilitiesImmutable capsChosen1 = GLGraphicsConfigurationUtil.clipRGBAGLCapabilities(capsChosen0, false /* allowRGB555 */, false /* allowAlpha */);
    final GLCapabilitiesImmutable capsChosen2 = GLGraphicsConfigurationUtil.fixGLProfile(capsChosen1, glp);
    if( capsChosen0 != capsChosen2 ) {
        config.setChosenCapabilities(capsChosen2);
        if(DEBUG) {
            System.err.println("WindowsBitmapWGLDrawable: "+capsChosen0+" -> "+capsChosen2);
        }
    }
    return new WindowsBitmapWGLDrawable(factory, comp);
  }

  @Override
  protected void setRealizedImpl() {
    if(realized) {
        createBitmap();
    } else {
        destroyBitmap();
    }
  }

  @Override
  public GLContext createContext(final GLContext shareWith) {
    return new WindowsWGLContext(this, shareWith);
  }

  @Override
  public boolean isGLOriented() {
      return false;
  }

  private void createBitmap() {
    int werr;
    final NativeSurface ns = getNativeSurface();
    if(DEBUG) {
        System.err.println(getThreadName()+": WindowsBitmapWGLDrawable (1): "+ns);
    }
    final WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)ns.getGraphicsConfiguration();
    final GLCapabilitiesImmutable capsChosen = (GLCapabilitiesImmutable)config.getChosenCapabilities();
    final int width = getSurfaceWidth();
    final int height = getSurfaceHeight();

    //
    // 1. Create DIB Section
    //
    final BITMAPINFO info = BITMAPINFO.create();
    final BITMAPINFOHEADER header = info.getBmiHeader();
    final int bitsPerPixelIn = capsChosen.getRedBits() +
                               capsChosen.getGreenBits() +
                               capsChosen.getBlueBits();
    final int bitsPerPixel;
    // Note: For BITMAP 32 bpp, the high-byte is _not_ used and hence maximum color is RGB888!
    // Note: For BITAMP a biBitCount value other than 24 (RGB888) usually does not work!
    bitsPerPixel = 24; // RGB888 only!
    header.setBiSize(BITMAPINFOHEADER.size());
    header.setBiWidth(width);
    // NOTE: Positive height causes the DIB's origin at bottom-left (OpenGL),
    // a negative height causes the DIB's origin at top-left (Java AWT, Windows, ..).
    // We use !OpenGL origin to remove the need for vertical flip, see 'isGLOriented()' above.
    header.setBiHeight(-1 * height);
    header.setBiPlanes((short) 1);
    header.setBiBitCount((short) bitsPerPixel);
    header.setBiXPelsPerMeter(0);
    header.setBiYPelsPerMeter(0);
    header.setBiClrUsed(0);
    header.setBiClrImportant(0);
    header.setBiCompression(GDI.BI_RGB);
    final int byteNum = width * height * ( bitsPerPixel >> 3 ) ;
    header.setBiSizeImage(byteNum);

    final PointerBuffer pb = PointerBuffer.allocateDirect(1);
    hbitmap = GDI.CreateDIBSection(0, info, GDI.DIB_RGB_COLORS, pb, 0, 0);
    werr = GDI.GetLastError();
    if(DEBUG) {
        final long p = ( pb.capacity() > 0 ) ? pb.get(0) : 0;
        System.err.println("WindowsBitmapWGLDrawable: pb sz/ptr "+pb.capacity() + ", "+toHexString(p));
        System.err.println("WindowsBitmapWGLDrawable: " + width+"x"+height +
                            ", bpp " + bitsPerPixelIn + " -> " + bitsPerPixel +
                            ", bytes " + byteNum +
                            ", header sz " + BITMAPINFOHEADER.size() +
                            ", DIB ptr num " + pb.capacity()+
                            ", "+capsChosen+
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
    ((MutableSurface)ns).setSurfaceHandle(hdc);
    if(DEBUG) {
        System.err.println(getThreadName()+": WindowsBitmapWGLDrawable (2): "+ns);
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
    final NativeSurface ns = getNativeSurface();
    if (ns.getSurfaceHandle() != 0) {
      // Must destroy bitmap and device context
      GDI.SelectObject(ns.getSurfaceHandle(), origbitmap);
      GDI.DeleteObject(hbitmap);
      GDI.DeleteDC(ns.getSurfaceHandle());
      origbitmap = 0;
      hbitmap = 0;
      ((MutableSurface)ns).setSurfaceHandle(0);
    }
  }
}
