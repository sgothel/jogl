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

import java.nio.IntBuffer;

import jogamp.nativewindow.windows.GDI;
import jogamp.nativewindow.windows.PIXELFORMATDESCRIPTOR;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

public class WGLGLCapabilities extends GLCapabilities {
  final private PIXELFORMATDESCRIPTOR pfd;
  final private int pfdID;
  private int arb_pixelformat; // -1 PFD, 0 NOP, 1 ARB

  public WGLGLCapabilities(final PIXELFORMATDESCRIPTOR pfd, final int pfdID, final GLProfile glp) {
      super(glp);
      this.pfd = pfd;
      this.pfdID = pfdID;
      this.arb_pixelformat = 0;
  }

  public boolean setValuesByGDI() {
      arb_pixelformat = -1;

      // ALPHA shall be set at last - due to it's auto setting by !opaque / samples
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
      final int dwFlags = pfd.getDwFlags();
      setDoubleBuffered((dwFlags & GDI.PFD_DOUBLEBUFFER) != 0);
      setStereo((dwFlags & GDI.PFD_STEREO) != 0);
      setHardwareAccelerated((dwFlags & GDI.PFD_GENERIC_FORMAT) == 0
                          || (dwFlags & GDI.PFD_GENERIC_ACCELERATED) != 0);
      // n/a with non ARB/GDI method:
      //       multisample
      //       opaque
      //       pbuffer

      return true;
  }

  public static final String PFD2String(final PIXELFORMATDESCRIPTOR pfd, final int pfdID) {
      final int dwFlags = pfd.getDwFlags();
      final StringBuilder sb = new StringBuilder();
      boolean sep = false;

      if( 0 != (GDI.PFD_DRAW_TO_WINDOW & dwFlags ) ) {
          sep = true;
          sb.append("window");
      }
      if( 0 != (GDI.PFD_DRAW_TO_BITMAP & dwFlags ) ) {
          if(sep) { sb.append(CSEP); } sep=true;
          sb.append("bitmap");
      }
      if( 0 != (GDI.PFD_SUPPORT_OPENGL & dwFlags ) ) {
          if(sep) { sb.append(CSEP); } sep=true;
          sb.append("opengl");
      }
      if( 0 != (GDI.PFD_DOUBLEBUFFER & dwFlags ) ) {
          if(sep) { sb.append(CSEP); } sep=true;
          sb.append("dblbuf");
      }
      if( 0 != (GDI.PFD_STEREO & dwFlags ) ) {
          if(sep) { sb.append(CSEP); } sep=true;
          sb.append("stereo");
      }
      if( 0 == (GDI.PFD_GENERIC_FORMAT & dwFlags ) || 0 == (GDI.PFD_GENERIC_ACCELERATED & dwFlags ) ) {
          if(sep) { sb.append(CSEP); } sep=true;
          sb.append("hw-accel");
      }
      return "PFD[id = "+pfdID+" (0x"+Integer.toHexString(pfdID)+
              "), colorBits "+pfd.getCColorBits()+", rgba "+pfd.getCRedBits()+ESEP+pfd.getCGreenBits()+ESEP+pfd.getCBlueBits()+ESEP+pfd.getCAlphaBits()+
              ", accum-rgba "+pfd.getCAccumRedBits()+ESEP+pfd.getCAccumGreenBits()+ESEP+pfd.getCAccumBlueBits()+ESEP+pfd.getCAccumAlphaBits()+
              ", dp/st/ms: "+pfd.getCDepthBits()+ESEP+pfd.getCStencilBits()+ESEP+"0"+
              ", flags: "+sb.toString();
  }

  public boolean setValuesByARB(final IntBuffer iattribs, final int niattribs, final IntBuffer iresults) {
      arb_pixelformat = 1;

      int alphaBits = 0;
      for (int i = 0; i < niattribs; i++) {
          final int attr = iattribs.get(i);
          final int res = iresults.get(i);
          switch (attr) {
              case WGLExt.WGL_DRAW_TO_WINDOW_ARB:
              case WGLExt.WGL_DRAW_TO_BITMAP_ARB:
              case WGLExt.WGL_DRAW_TO_PBUFFER_ARB:
                  break;

              case WGLExt.WGL_ACCELERATION_ARB:
                  setHardwareAccelerated(res == WGLExt.WGL_FULL_ACCELERATION_ARB);
                  break;

              case WGLExt.WGL_SUPPORT_OPENGL_ARB:
                  if (res != GL.GL_TRUE) {
                      return false;
                  }
                  break;

              case WGLExt.WGL_DEPTH_BITS_ARB:
                  setDepthBits(res);
                  break;

              case WGLExt.WGL_STENCIL_BITS_ARB:
                  setStencilBits(res);
                  break;

              case WGLExt.WGL_DOUBLE_BUFFER_ARB:
                  setDoubleBuffered(res == GL.GL_TRUE);
                  break;

              case WGLExt.WGL_STEREO_ARB:
                  setStereo(res == GL.GL_TRUE);
                  break;

              case WGLExt.WGL_PIXEL_TYPE_ARB:
                  if(res == WGLExt.WGL_TYPE_COLORINDEX_ARB) {
                      return false; // color index not supported
                  }

                  if (res == WGLExt.WGL_TYPE_RGBA_FLOAT_ARB) {
                      return false; // not supported
                  }

                  // normal RGBA FB: WGLExt.WGL_TYPE_RGBA_ARB
                  // ignore unknown results here
                  break;

              case WGLExt.WGL_RED_BITS_ARB:
                  setRedBits(res);
                  break;

              case WGLExt.WGL_GREEN_BITS_ARB:
                  setGreenBits(res);
                  break;

              case WGLExt.WGL_BLUE_BITS_ARB:
                  setBlueBits(res);
                  break;

              case WGLExt.WGL_ALPHA_BITS_ARB:
                  // ALPHA shall be set at last - due to it's auto setting by !opaque / samples
                  alphaBits = res;
                  break;

              case WGLExt.WGL_ACCUM_RED_BITS_ARB:
                  setAccumRedBits(res);
                  break;

              case WGLExt.WGL_ACCUM_GREEN_BITS_ARB:
                  setAccumGreenBits(res);
                  break;

              case WGLExt.WGL_ACCUM_BLUE_BITS_ARB:
                  setAccumBlueBits(res);
                  break;

              case WGLExt.WGL_ACCUM_ALPHA_BITS_ARB:
                  setAccumAlphaBits(res);
                  break;

              case WGLExt.WGL_SAMPLE_BUFFERS_ARB:
                  setSampleBuffers(res != 0);
                  break;

              case WGLExt.WGL_SAMPLES_ARB:
                  setNumSamples(res);
                  break;

              default:
                  throw new GLException("Unknown pixel format attribute " + attr);
          }
      }
      setAlphaBits(alphaBits);
      return true;
  }

  @Override
  public Object cloneMutable() {
    return clone();
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (final RuntimeException e) {
      throw new GLException(e);
    }
  }

  final public PIXELFORMATDESCRIPTOR getPFD() { return pfd; }
  final public int getPFDID() { return pfdID; }

  final public boolean isSetByARB() { return 0 < arb_pixelformat; }
  final public boolean isSetByGDI() { return 0 > arb_pixelformat; }
  final public boolean isSet()      { return 0 != arb_pixelformat; }

  @Override
  final public int getVisualID(final VIDType type) throws NativeWindowException {
      switch(type) {
          case INTRINSIC:
          case NATIVE:
          case WIN32_PFD:
              return getPFDID();
          default:
              throw new NativeWindowException("Invalid type <"+type+">");
      }
  }

  @Override
  public StringBuilder toString(StringBuilder sink) {
    if(null == sink) {
        sink = new StringBuilder();
    }
    sink.append("wgl vid ").append(pfdID).append(" ");
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
