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

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.SurfaceChangeable;
import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
// import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.windows.GDI;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.windows.wgl.WindowsWGLDrawableFactory.SharedResource;

import javax.media.opengl.GLCapabilitiesImmutable;

public class WindowsPbufferWGLDrawable extends WindowsWGLDrawable {
  private WGLExt cachedWGLExt; // cached WGLExt instance from parent GLCanvas,
                               // needed to destroy pbuffer
  private long buffer; // pbuffer handle

  private int floatMode;
  
  protected WindowsPbufferWGLDrawable(GLDrawableFactory factory, NativeSurface target) {
    super(factory, target, false);
  }

  protected void destroyImpl() {
      setRealized(false);
  }
  
  protected void setRealizedImpl() {
    if(realized) {
        createPbuffer();
    } else {
        destroyPbuffer();
    }
  }

  public GLContext createContext(GLContext shareWith) {
    return new WindowsPbufferWGLContext(this, shareWith);
  }

  protected void destroyPbuffer() {
    NativeSurface ns = getNativeSurface();
    if(0!=buffer) {
        WGLExt wglExt = cachedWGLExt;
        if (ns.getSurfaceHandle() != 0) {
          // Must release DC and pbuffer
          // NOTE that since the context is not current, glGetError() can
          // not be called here, so we skip the use of any composable
          // pipelines (see WindowsOnscreenWGLContext.makeCurrentImpl)
          if (wglExt.wglReleasePbufferDCARB(buffer, ns.getSurfaceHandle()) == 0) {
            throw new GLException("Error releasing pbuffer device context: error code " + GDI.GetLastError());
          }
          ((SurfaceChangeable)ns).setSurfaceHandle(0);
        }
        if (!wglExt.wglDestroyPbufferARB(buffer)) {
            throw new GLException("Error destroying pbuffer: error code " + GDI.GetLastError());
        }
        buffer = 0;
    }
  }

  public long getPbufferHandle() {
    // The actual to-be-used handle for makeCurrent etc,
    // is the derived DC, set in the NativeSurface surfaceHandle
    // returned by getHandle().
    return buffer;
  }

  public int getFloatingPointMode() {
    return floatMode;
  }

  private void createPbuffer() {
    WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration) getNativeSurface().getGraphicsConfiguration();
    SharedResource sharedResource = ((WindowsWGLDrawableFactory)factory).getOrCreateSharedResource(config.getScreen().getDevice());
    NativeSurface sharedSurface = sharedResource.getDrawable().getNativeSurface();
    if (NativeSurface.LOCK_SURFACE_NOT_READY >= sharedSurface.lockSurface()) {
      throw new NativeWindowException("Could not lock (sharedSurface): "+this);
    }
    try {
        long sharedHdc = sharedSurface.getSurfaceHandle();
        WGLExt wglExt = sharedResource.getContext().getWGLExt();
        
        if (DEBUG) {
            System.out.println("Pbuffer config: " + config);
        }
    
        int[]   iattributes = new int  [2*WindowsWGLGraphicsConfiguration.MAX_ATTRIBS];
        float[] fattributes = new float[1];
        int[]   floatModeTmp = new int[1];
        int     niattribs   = 0;
        int     width, height;
    
        GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable)config.getChosenCapabilities();
        GLProfile glProfile = chosenCaps.getGLProfile();
    
        if (DEBUG) {
          System.out.println("Pbuffer parentHdc = " + toHexString(sharedHdc));
          System.out.println("Pbuffer chosenCaps: " + chosenCaps);
        }
    
        if(!WindowsWGLGraphicsConfiguration.GLCapabilities2AttribList(chosenCaps,
                                        iattributes, sharedResource, -1, floatModeTmp)){
          throw new GLException("Pbuffer-related extensions not supported");
        }
    
        floatMode = floatModeTmp[0];
        boolean rtt      = chosenCaps.getPbufferRenderToTexture();
        boolean rect     = chosenCaps.getPbufferRenderToTextureRectangle();
        boolean useFloat = chosenCaps.getPbufferFloatingPointBuffers();
        // boolean ati      = false;
    
        /**
        if (useFloat) {
          ati = (floatMode == GLPbuffer.ATI_FLOAT);
        } */
    
        int[] pformats = new int[WindowsWGLGraphicsConfiguration.MAX_PFORMATS];
        int   nformats;
        int[] nformatsTmp = new int[1];
        if (!wglExt.wglChoosePixelFormatARB(sharedHdc,
                                            iattributes, 0,
                                            fattributes, 0,
                                            WindowsWGLGraphicsConfiguration.MAX_PFORMATS,
                                            pformats, 0,
                                            nformatsTmp, 0)) {
          throw new GLException("pbuffer creation error: wglChoosePixelFormat() failed");
        }
        nformats = nformatsTmp[0];
        if (nformats <= 0) {
          throw new GLException("pbuffer creation error: Couldn't find a suitable pixel format");
        }
    
        if (DEBUG) {
          System.err.println("" + nformats + " suitable pixel formats found");
          for (int i = 0; i < nformats; i++) {
            WGLGLCapabilities dbgCaps = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilities(sharedResource, sharedHdc, pformats[i], glProfile, false, true);
            System.err.println("pixel format " + pformats[i] + " (index " + i + "): " + dbgCaps);
          }
        }
    
        int pfdid = 0;
        long tmpBuffer = 0;
        {
            int whichFormat;
            // Loop is a workaround for bugs in NVidia's recent drivers
            for (whichFormat = 0; whichFormat < nformats; whichFormat++) {
              int format = pformats[whichFormat];
    
              // Create the p-buffer.
              niattribs = 0;
    
              if (rtt) {
                iattributes[niattribs++]   = WGLExt.WGL_TEXTURE_FORMAT_ARB;
                if (useFloat) {
                  iattributes[niattribs++] = WGLExt.WGL_TEXTURE_FLOAT_RGB_NV;
                } else {
                  iattributes[niattribs++] = WGLExt.WGL_TEXTURE_RGBA_ARB;
                }
    
                iattributes[niattribs++] = WGLExt.WGL_TEXTURE_TARGET_ARB;
                iattributes[niattribs++] = rect ? WGLExt.WGL_TEXTURE_RECTANGLE_NV : WGLExt.WGL_TEXTURE_2D_ARB;
    
                iattributes[niattribs++] = WGLExt.WGL_MIPMAP_TEXTURE_ARB;
                iattributes[niattribs++] = GL.GL_FALSE;
    
                iattributes[niattribs++] = WGLExt.WGL_PBUFFER_LARGEST_ARB;
                iattributes[niattribs++] = GL.GL_FALSE;
              }
    
              iattributes[niattribs++] = 0;
    
              tmpBuffer = wglExt.wglCreatePbufferARB(sharedHdc, format, getWidth(), getHeight(), iattributes, 0);
              if (tmpBuffer != 0) {
                // Done
                break;
              }
            }
    
            if (0 == tmpBuffer) {
              throw new GLException("pbuffer creation error: wglCreatePbuffer() failed: tried " + nformats +
                                    " pixel formats, last error was: " + wglGetLastError());
            }
            pfdid = pformats[whichFormat];
        }
    
        // Get the device context.
        long tmpHdc = wglExt.wglGetPbufferDCARB(tmpBuffer);
        if (tmpHdc == 0) {
          throw new GLException("pbuffer creation error: wglGetPbufferDC() failed");
        }
    
        NativeSurface ns = getNativeSurface();
        // Set up instance variables
        buffer = tmpBuffer;
        ((SurfaceChangeable)ns).setSurfaceHandle(tmpHdc);
        cachedWGLExt = wglExt;   
    
        // Re-query chosen pixel format
        {
          WGLGLCapabilities newCaps = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilities(sharedResource, sharedHdc, pfdid, glProfile, false, true);
          if(null == newCaps) {
            throw new GLException("pbuffer creation error: unable to re-query chosen PFD ID: " + pfdid + ", hdc " + GLDrawableImpl.toHexString(tmpHdc));
          }
          if(newCaps.isOnscreen() || !newCaps.isPBuffer()) {
            throw new GLException("Error: Selected Onscreen Caps for PBuffer: "+newCaps);
          }
          config.setCapsPFD(newCaps);
        }
    
        // Determine the actual width and height we were able to create.
        int[] tmp = new int[1];
        wglExt.wglQueryPbufferARB( buffer, WGLExt.WGL_PBUFFER_WIDTH_ARB,  tmp, 0 );
        width = tmp[0];
        wglExt.wglQueryPbufferARB( buffer, WGLExt.WGL_PBUFFER_HEIGHT_ARB, tmp, 0 );
        height = tmp[0];
        ((SurfaceChangeable)ns).surfaceSizeChanged(width, height);
    } finally {
        sharedSurface.unlockSurface();
    }
  }

  private static String wglGetLastError() {
    return WindowsWGLDrawableFactory.wglGetLastError();
  }
}
