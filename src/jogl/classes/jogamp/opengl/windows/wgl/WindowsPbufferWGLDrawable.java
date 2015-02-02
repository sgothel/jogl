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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.MutableSurface;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.common.nio.Buffers;

import jogamp.nativewindow.windows.GDI;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.windows.wgl.WindowsWGLDrawableFactory.SharedResource;

public class WindowsPbufferWGLDrawable extends WindowsWGLDrawable {
  private WGLExt cachedWGLExt; // cached WGLExt instance from parent GLCanvas,
                               // needed to destroy pbuffer
  private long buffer; // pbuffer handle

  protected WindowsPbufferWGLDrawable(final GLDrawableFactory factory, final NativeSurface target) {
    super(factory, target, false);
  }

  @Override
  protected void setRealizedImpl() {
    if(realized) {
        createPbuffer();
    } else {
        destroyPbuffer();
    }
  }

  @Override
  public GLContext createContext(final GLContext shareWith) {
    return new WindowsWGLContext(this, shareWith);
  }

  protected void destroyPbuffer() {
    final NativeSurface ns = getNativeSurface();
    if(0!=buffer) {
        final WGLExt wglExt = cachedWGLExt;
        if (ns.getSurfaceHandle() != 0) {
          // Must release DC and pbuffer
          // NOTE that since the context is not current, glGetError() can
          // not be called here, so we skip the use of any composable
          // pipelines (see WindowsOnscreenWGLContext.makeCurrentImpl)
          if (wglExt.wglReleasePbufferDCARB(buffer, ns.getSurfaceHandle()) == 0) {
            throw new GLException("Error releasing pbuffer device context: error code " + GDI.GetLastError());
          }
          ((MutableSurface)ns).setSurfaceHandle(0);
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

  private void createPbuffer() {
    final WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration) getNativeSurface().getGraphicsConfiguration();
    final SharedResource sharedResource = ((WindowsWGLDrawableFactory)factory).getOrCreateSharedResourceImpl(config.getScreen().getDevice());
    final NativeSurface sharedSurface = sharedResource.getDrawable().getNativeSurface();
    if (NativeSurface.LOCK_SURFACE_NOT_READY >= sharedSurface.lockSurface()) {
      throw new NativeWindowException("Could not lock (sharedSurface): "+this);
    }
    try {
        final long sharedHdc = sharedSurface.getSurfaceHandle();
        final WGLExt wglExt = ((WindowsWGLContext)sharedResource.getContext()).getWGLExt();

        if (DEBUG) {
            System.err.println(getThreadName()+": Pbuffer config: " + config);
        }

        final int winattrPbuffer = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(false /* onscreen */, false /* fbo */, true /* pbuffer */, false /* bitmap */);

        final IntBuffer iattributes = Buffers.newDirectIntBuffer(2*WindowsWGLGraphicsConfiguration.MAX_ATTRIBS);
        final FloatBuffer fattributes = Buffers.newDirectFloatBuffer(1);
        final int[]   floatModeTmp = new int[1];
        int     niattribs   = 0;

        final GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable)config.getChosenCapabilities();
        final GLProfile glProfile = chosenCaps.getGLProfile();
        final AbstractGraphicsDevice device = config.getScreen().getDevice();

        if (DEBUG) {
          System.err.println(getThreadName()+": Pbuffer parentHdc = " + toHexString(sharedHdc));
          System.err.println(getThreadName()+": Pbuffer chosenCaps: " + chosenCaps);
        }

        if( !WindowsWGLGraphicsConfiguration.GLCapabilities2AttribList( sharedResource, chosenCaps,
                                                                        iattributes, -1, floatModeTmp) ) {
          throw new GLException("Pbuffer-related extensions not supported");
        }

        final IntBuffer pformats = Buffers.newDirectIntBuffer(WindowsWGLGraphicsConfiguration.MAX_PFORMATS);
        final IntBuffer nformatsTmp = Buffers.newDirectIntBuffer(1);
        if (!wglExt.wglChoosePixelFormatARB(sharedHdc,
                                            iattributes, fattributes, WindowsWGLGraphicsConfiguration.MAX_PFORMATS,
                                            pformats, nformatsTmp)) {
          throw new GLException("pbuffer creation error: wglChoosePixelFormat() failed");
        }
        final int nformats = Math.min(nformatsTmp.get(0), WindowsWGLGraphicsConfiguration.MAX_PFORMATS);
        if (nformats <= 0) {
          throw new GLException("pbuffer creation error: Couldn't find a suitable pixel format");
        }

        if (DEBUG) {
          System.err.println("" + nformats + " suitable pixel formats found");
          for (int i = 0; i < nformats; i++) {
            final WGLGLCapabilities dbgCaps = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilitiesNoCheck(sharedResource, device, glProfile,
                                          sharedHdc, pformats.get(i), winattrPbuffer);
            System.err.println("pixel format " + pformats.get(i) + " (index " + i + "): " + dbgCaps);
          }
        }

        int pfdid = 0;
        long tmpBuffer = 0;
        {
            int whichFormat;
            // Loop is a workaround for bugs in NVidia's recent drivers
            for (whichFormat = 0; whichFormat < nformats; whichFormat++) {
              final int format = pformats.get(whichFormat);

              // Create the p-buffer.
              niattribs = 0;

              iattributes.put(niattribs++, 0);

              tmpBuffer = wglExt.wglCreatePbufferARB(sharedHdc, format, getSurfaceWidth(), getSurfaceHeight(), iattributes);
              if (tmpBuffer != 0) {
                // Done
                break;
              }
            }

            if (0 == tmpBuffer) {
              throw new GLException("pbuffer creation error: wglCreatePbuffer() failed: tried " + nformats +
                                    " pixel formats, last error was: " + wglGetLastError());
            }
            pfdid = pformats.get(whichFormat);
        }

        // Get the device context.
        final long tmpHdc = wglExt.wglGetPbufferDCARB(tmpBuffer);
        if (tmpHdc == 0) {
          throw new GLException("pbuffer creation error: wglGetPbufferDC() failed");
        }

        final NativeSurface ns = getNativeSurface();
        // Set up instance variables
        buffer = tmpBuffer;
        ((MutableSurface)ns).setSurfaceHandle(tmpHdc);
        cachedWGLExt = wglExt;

        // Re-query chosen pixel format
        {
          final WGLGLCapabilities newCaps = WindowsWGLGraphicsConfiguration.wglARBPFID2GLCapabilities(sharedResource, device, glProfile,
                                          sharedHdc, pfdid, winattrPbuffer);
          if(null == newCaps) {
            throw new GLException("pbuffer creation error: unable to re-query chosen PFD ID: " + pfdid + ", hdc " + GLDrawableImpl.toHexString(tmpHdc));
          }
          if(newCaps.isOnscreen() || !newCaps.isPBuffer()) {
            throw new GLException("Error: Selected Onscreen Caps for PBuffer: "+newCaps);
          }
          config.setCapsPFD(newCaps);
        }
    } finally {
        sharedSurface.unlockSurface();
    }
  }

  private static String wglGetLastError() {
    return WindowsWGLDrawableFactory.wglGetLastError();
  }
}
