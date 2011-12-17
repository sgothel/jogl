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

import javax.media.nativewindow.DefaultGraphicsConfiguration;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.SurfaceChangeable;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

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

  // Note that we can not store this in the NativeSurface because the
  // semantic is that contains an NSView
  protected long pBuffer;
  protected int pBufferTexTarget, pBufferTexWidth, pBufferTexHeight;

  public MacOSXPbufferCGLDrawable(GLDrawableFactory factory, NativeSurface target) {
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
    final MacOSXPbufferCGLContext ctx = new MacOSXPbufferCGLContext(this, shareWith);
    registerContext(ctx);
    return ctx;
  }

  @Override
  protected long getNSViewHandle() {
      // pbuffer handle is NSOpenGLPixelBuffer
      return 0;
  }
  
  @Override
  public long getHandle() {
    return pBuffer;
  }
  
  protected int getTextureTarget() { return pBufferTexTarget;  }
  protected int getTextureWidth() { return pBufferTexWidth; }
  protected int getTextureHeight() { return pBufferTexHeight; }
    
  protected void destroyPbuffer() {
    if (this.pBuffer != 0) {
      NativeSurface ns = getNativeSurface();
      impl.destroy(pBuffer);
      this.pBuffer = 0;
      ((SurfaceChangeable)ns).setSurfaceHandle(0);
    }
  }

  private void createPbuffer() {
    final NativeSurface ns = getNativeSurface();
    final DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) ns.getGraphicsConfiguration();
    final GLCapabilitiesImmutable capabilities = (GLCapabilitiesImmutable)config.getChosenCapabilities();
    final GLProfile glProfile = capabilities.getGLProfile();
    MacOSXCGLDrawableFactory.SharedResource sr = ((MacOSXCGLDrawableFactory)factory).getOrCreateOSXSharedResource(config.getScreen().getDevice());
    
    if (DEBUG) {
        System.out.println("Pbuffer config: " + config);
    }

    if ( capabilities.getPbufferRenderToTextureRectangle() && null!=sr && sr.isRECTTextureAvailable() ) {
      pBufferTexTarget = GL2.GL_TEXTURE_RECTANGLE;
    } else {
      pBufferTexTarget = GL.GL_TEXTURE_2D;
    }
    if ( GL2.GL_TEXTURE_RECTANGLE == pBufferTexTarget || ( null!=sr && sr.isNPOTTextureAvailable() ) ) { 
      pBufferTexWidth = getWidth();
      pBufferTexHeight = getHeight();
    } else {
      pBufferTexWidth = GLBuffers.getNextPowerOf2(getWidth());
      pBufferTexHeight = GLBuffers.getNextPowerOf2(getHeight());
    }

    int internalFormat = GL.GL_RGBA;
    if (capabilities.getPbufferFloatingPointBuffers()) {
      if(!glProfile.isGL2GL3() || null==sr || sr.isAppletFloatPixelsAvailable()) {
          throw new GLException("Floating-point support (GL_APPLE_float_pixels) not available");
      }
      switch (capabilities.getRedBits()) {
        case 16: internalFormat = GL2.GL_RGBA_FLOAT16_APPLE; break;
        case 32: internalFormat = GL2.GL_RGBA_FLOAT32_APPLE; break;
        default: throw new GLException("Invalid floating-point bit depth (only 16 and 32 supported)");
      }
    }
    
    pBuffer = impl.create(pBufferTexTarget, internalFormat, getWidth(), getHeight());
    if(DEBUG) {
        System.err.println("MacOSXPbufferCGLDrawable tex: target "+toHexString(pBufferTexTarget)+
                            ", pbufferSize "+getWidth()+"x"+getHeight()+
                            ", texSize "+pBufferTexWidth+"x"+pBufferTexHeight+
                            ", internal-fmt "+toHexString(internalFormat));
        System.err.println("MacOSXPbufferCGLDrawable pBuffer: "+toHexString(pBuffer));
        // Thread.dumpStack();
    }
    if (pBuffer == 0) {
      throw new GLException("pbuffer creation error: CGL.createPBuffer() failed");
    }

    ((SurfaceChangeable)ns).setSurfaceHandle(pBuffer);
  }

  public void setOpenGLMode(GLBackendType mode) {
    super.setOpenGLMode(mode);
    createPbuffer(); // recreate
  }

  protected void initOpenGLImpl(GLBackendType backend) {
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
  class NSOpenGLImpl implements GLBackendImpl {
    public long create(int renderTarget, int internalFormat, int width, int height) {
      return CGL.createPBuffer(renderTarget, internalFormat, width, height);
    }

    public void destroy(long pbuffer) {
      CGL.destroyPBuffer(pbuffer);
    }
  }

  // CGL implementation
  class CGLImpl implements GLBackendImpl {
    public long create(int renderTarget, int internalFormat, int width, int height) {
      PointerBuffer pbuffer = PointerBuffer.allocateDirect(1);
      int res = CGL.CGLCreatePBuffer(width, height, renderTarget, internalFormat, 0, pbuffer);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error creating CGL-based pbuffer: error code " + res);
      }
      return pbuffer.get(0);
    }

    public void destroy(long pbuffer) {
      int res = CGL.CGLDestroyPBuffer(pbuffer);
      if (res != CGL.kCGLNoError) {
        throw new GLException("Error destroying CGL-based pbuffer: error code " + res);
      }
    }
  }  
  
}
