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

package jogamp.opengl.macosx.cgl;

import java.lang.ref.WeakReference;

import com.jogamp.nativewindow.DefaultGraphicsConfiguration;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.MutableSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;

import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.opengl.util.GLBuffers;

public class MacOSXPbufferCGLDrawable extends MacOSXCGLDrawable {
  // Abstract interface for implementation of this drawable (either
  // NSOpenGL-based or CGL-based)
  interface GLBackendImpl {
    public long create(int renderTarget, int internalFormat, int width, int height);
    public void destroy(long pbuffer);
  }

  // Implementation object (either NSOpenGL-based or CGL-based)
  protected GLBackendImpl impl;

  // State for render-to-texture and render-to-texture-rectangle support
  // private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  // private int texture;       // actual texture object

  protected int pBufferTexTarget, pBufferTexWidth, pBufferTexHeight;

  public MacOSXPbufferCGLDrawable(final GLDrawableFactory factory, final NativeSurface target) {
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
    return new MacOSXCGLContext(this, shareWith);
  }

  protected int getTextureTarget() { return pBufferTexTarget;  }
  protected int getTextureWidth() { return pBufferTexWidth; }
  protected int getTextureHeight() { return pBufferTexHeight; }

  protected void destroyPbuffer() {
    final MutableSurface ms = (MutableSurface) getNativeSurface();
    final long pBuffer = ms.getSurfaceHandle();
    if (0 != pBuffer) {
      synchronized (createdContexts) {
        for(int i=0; i<createdContexts.size(); ) {
          final WeakReference<MacOSXCGLContext> ref = createdContexts.get(i);
          final MacOSXCGLContext ctx = ref.get();
          if (ctx != null) {
            ctx.detachPBuffer();
            i++;
          } else {
            createdContexts.remove(i);
          }
        }
      }
      impl.destroy(pBuffer);
      ms.setSurfaceHandle(0);
    }
  }

  private void createPbuffer() {
    final MutableSurface ms = (MutableSurface) getNativeSurface();
    final DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) ms.getGraphicsConfiguration();
    final MacOSXCGLDrawableFactory.SharedResource sr = ((MacOSXCGLDrawableFactory)factory).getOrCreateSharedResourceImpl(config.getScreen().getDevice());

    if (DEBUG) {
        System.out.println(getThreadName()+": Pbuffer config: " + config);
        if(null != sr) {
            System.out.println("Pbuffer NPOT Texure  avail: "+sr.isNPOTTextureAvailable());
            System.out.println("Pbuffer RECT Texture avail: "+sr.isRECTTextureAvailable());
        } else {
            System.out.println("Pbuffer no sr, no RECT/NPOT Texture avail");
        }
    }

    pBufferTexTarget = GL.GL_TEXTURE_2D;
    if ( null!=sr && sr.isNPOTTextureAvailable() ) {
      pBufferTexWidth = getSurfaceWidth();
      pBufferTexHeight = getSurfaceHeight();
    } else {
      pBufferTexWidth = GLBuffers.getNextPowerOf2(getSurfaceWidth());
      pBufferTexHeight = GLBuffers.getNextPowerOf2(getSurfaceHeight());
    }

    final int internalFormat = GL.GL_RGBA;
    final long pBuffer = impl.create(pBufferTexTarget, internalFormat, getSurfaceWidth(), getSurfaceHeight());
    if(DEBUG) {
        System.err.println("MacOSXPbufferCGLDrawable tex: target "+toHexString(pBufferTexTarget)+
                            ", pbufferSize "+getSurfaceWidth()+"x"+getSurfaceHeight()+
                            ", texSize "+pBufferTexWidth+"x"+pBufferTexHeight+
                            ", internal-fmt "+toHexString(internalFormat));
        System.err.println("MacOSXPbufferCGLDrawable pBuffer: "+toHexString(pBuffer));
        // Thread.dumpStack();
    }
    if (pBuffer == 0) {
      throw new GLException("pbuffer creation error: CGL.createPBuffer() failed");
    }

    ms.setSurfaceHandle(pBuffer);
  }

  @Override
  public void setOpenGLMode(final GLBackendType mode) {
    super.setOpenGLMode(mode);
    createPbuffer(); // recreate
  }

  @Override
  protected void initOpenGLImpl(final GLBackendType backend) {
    switch (backend) {
      case NSOPENGL:
        impl = new NSOpenGLImpl();
        break;
      case CGL:
        impl = new CGLImpl();
        break;
      default:
        throw new InternalError("Illegal implementation mode " + backend);
    }
  }

  // NSOpenGLPixelBuffer implementation
  static class NSOpenGLImpl implements GLBackendImpl {
    @Override
    public long create(final int renderTarget, final int internalFormat, final int width, final int height) {
      return CGL.createPBuffer(renderTarget, internalFormat, width, height);
    }

    @Override
    public void destroy(final long pbuffer) {
      CGL.destroyPBuffer(pbuffer);
    }
  }

  // CGL implementation
  static class CGLImpl implements GLBackendImpl {
    @Override
    public long create(final int renderTarget, final int internalFormat, final int width, final int height) {
      final PointerBuffer pbuffer = PointerBuffer.allocateDirect(1);
      final int res = CGL.CGLCreatePBuffer(width, height, renderTarget, internalFormat, 0, pbuffer);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error creating CGL-based pbuffer: error code " + res);
      }
      return pbuffer.get(0);
    }

    @Override
    public void destroy(final long pbuffer) {
      final int res = CGL.CGLDestroyPBuffer(pbuffer);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error destroying CGL-based pbuffer: error code " + res);
      }
    }
  }

}
