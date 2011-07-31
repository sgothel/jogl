/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.opengl.windows.wgl;

import java.util.Comparator;

import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.PIXELFORMATDESCRIPTOR;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

public class WGLGLCapabilities extends GLCapabilities {
  final PIXELFORMATDESCRIPTOR pfd;
  final int pfdID;
  int arb_pixelformat; // -1 PFD, 0 NOP, 1 ARB

  /** Comparing pfd id only */
  public static class PfdIDComparator implements Comparator {

      public int compare(Object o1, Object o2) {
        if ( ! ( o1 instanceof WGLGLCapabilities ) ) {
            Class c = (null != o1) ? o1.getClass() : null ;
            throw new ClassCastException("arg1 not a WGLGLCapabilities object: " + c);
        }
        if ( ! ( o2 instanceof WGLGLCapabilities ) ) {
            Class c = (null != o2) ? o2.getClass() : null ;
            throw new ClassCastException("arg2 not a WGLGLCapabilities object: " + c);
        }

        final WGLGLCapabilities caps1 = (WGLGLCapabilities) o1;
        final long id1 = caps1.getPFDID();

        final WGLGLCapabilities caps2 = (WGLGLCapabilities) o2;
        final long id2 = caps2.getPFDID();

        if(id1 > id2) {
            return 1;
        } else if(id1 < id2) {
            return -1;
        }
        return 0;
      }
  }

  public WGLGLCapabilities(PIXELFORMATDESCRIPTOR pfd, int pfdID, GLProfile glp) {
      super(glp);
      this.pfd = pfd;
      this.pfdID = pfdID;
      this.arb_pixelformat = 0;
  }

  public boolean setValuesByGDI() {
      arb_pixelformat = -1;

      setRedBits(pfd.getCRedBits());
      setGreenBits(pfd.getCGreenBits());
      setBlueBits(pfd.getCBlueBits());
      setAlphaBits(pfd.getCAlphaBits());
      setAccumRedBits(pfd.getCAccumRedBits());
      setAccumGreenBits(pfd.getCAccumGreenBits());
      setAccumBlueBits(pfd.getCAccumBlueBits());
      setAccumAlphaBits(pfd.getCAccumAlphaBits());
      setDepthBits(pfd.getCDepthBits());
      setStencilBits(pfd.getCStencilBits());
      setDoubleBuffered((pfd.getDwFlags() & GDI.PFD_DOUBLEBUFFER) != 0);
      setStereo((pfd.getDwFlags() & GDI.PFD_STEREO) != 0);
      setHardwareAccelerated((pfd.getDwFlags() & GDI.PFD_GENERIC_FORMAT) == 0
                          || (pfd.getDwFlags() & GDI.PFD_GENERIC_ACCELERATED) != 0);
      // n/a with non ARB/GDI method:
      //       multisample
      //       opaque
      //       pbuffer

      return true;
  }

  public boolean setValuesByARB(final int[] iattribs, final int niattribs, final int[] iresults) {
      arb_pixelformat = 1;

      for (int i = 0; i < niattribs; i++) {
          int attr = iattribs[i];
          switch (attr) {
              case WGLExt.WGL_DRAW_TO_WINDOW_ARB:
              case WGLExt.WGL_DRAW_TO_BITMAP_ARB:
              case WGLExt.WGL_DRAW_TO_PBUFFER_ARB:
                  break;

              case WGLExt.WGL_ACCELERATION_ARB:
                  setHardwareAccelerated(iresults[i] == WGLExt.WGL_FULL_ACCELERATION_ARB);
                  break;

              case WGLExt.WGL_SUPPORT_OPENGL_ARB:
                  if (iresults[i] != GL.GL_TRUE) {
                      return false;
                  }
                  break;

              case WGLExt.WGL_DEPTH_BITS_ARB:
                  setDepthBits(iresults[i]);
                  break;

              case WGLExt.WGL_STENCIL_BITS_ARB:
                  setStencilBits(iresults[i]);
                  break;

              case WGLExt.WGL_DOUBLE_BUFFER_ARB:
                  setDoubleBuffered(iresults[i] == GL.GL_TRUE);
                  break;

              case WGLExt.WGL_STEREO_ARB:
                  setStereo(iresults[i] == GL.GL_TRUE);
                  break;

              case WGLExt.WGL_PIXEL_TYPE_ARB:
                  if(iresults[i] == WGLExt.WGL_TYPE_COLORINDEX_ARB) {
                      return false; // color index not supported
                  }

                  if (iresults[i] == WGLExt.WGL_TYPE_RGBA_FLOAT_ARB) {
                      setPbufferFloatingPointBuffers(true);
                  }
                  
                  // normal RGBA FB: WGLExt.WGL_TYPE_RGBA_ARB
                  // ignore unknown results here
                  break;

              case WGLExt.WGL_FLOAT_COMPONENTS_NV:
                  if (iresults[i] != 0) {
                      setPbufferFloatingPointBuffers(true);
                  }
                  break;

              case WGLExt.WGL_RED_BITS_ARB:
                  setRedBits(iresults[i]);
                  break;

              case WGLExt.WGL_GREEN_BITS_ARB:
                  setGreenBits(iresults[i]);
                  break;

              case WGLExt.WGL_BLUE_BITS_ARB:
                  setBlueBits(iresults[i]);
                  break;

              case WGLExt.WGL_ALPHA_BITS_ARB:
                  setAlphaBits(iresults[i]);
                  break;

              case WGLExt.WGL_ACCUM_RED_BITS_ARB:
                  setAccumRedBits(iresults[i]);
                  break;

              case WGLExt.WGL_ACCUM_GREEN_BITS_ARB:
                  setAccumGreenBits(iresults[i]);
                  break;

              case WGLExt.WGL_ACCUM_BLUE_BITS_ARB:
                  setAccumBlueBits(iresults[i]);
                  break;

              case WGLExt.WGL_ACCUM_ALPHA_BITS_ARB:
                  setAccumAlphaBits(iresults[i]);
                  break;

              case WGLExt.WGL_SAMPLE_BUFFERS_ARB:
                  setSampleBuffers(iresults[i] != 0);
                  break;

              case WGLExt.WGL_SAMPLES_ARB:
                  setNumSamples(iresults[i]);
                  break;

              default:
                  throw new GLException("Unknown pixel format attribute " + iattribs[i]);
          }
      }
      return true;
  }

  public Object cloneMutable() {
    return clone();
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (RuntimeException e) {
      throw new GLException(e);
    }
  }

  final public PIXELFORMATDESCRIPTOR getPFD() { return pfd; }
  final public int getPFDID() { return pfdID; }
  
  final public boolean isSetByARB() { return 0 < arb_pixelformat; }
  final public boolean isSetByGDI() { return 0 > arb_pixelformat; }
  final public boolean isSet()      { return 0 != arb_pixelformat; }
  
  public StringBuffer toString(StringBuffer sink) {
    if(null == sink) {
        sink = new StringBuffer();
    }
    sink.append(pfdID).append(" ");
    switch (arb_pixelformat) {
        case -1: 
            sink.append("gdi");
            break;
        case  0:
            sink.append("nop");
            break;
        case  1:
            sink.append("arb");
            break;
        default:
            throw new InternalError("invalid arb_pixelformat: " + arb_pixelformat);
    }
    sink.append(": ");
    return super.toString(sink);
  }
}