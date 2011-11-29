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

import javax.media.opengl.*;

import jogamp.opengl.GLContextImpl;

public class WindowsPbufferWGLContext extends WindowsWGLContext {
  // State for render-to-texture and render-to-texture-rectangle support
  private boolean rtt;       // render-to-texture?
  private boolean hasRTT;    // render-to-texture extension available?
  private boolean rect;      // render-to-texture-rectangle?
  private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  private int texture;       // actual texture object

  protected WindowsPbufferWGLContext(WindowsPbufferWGLDrawable drawable,
                                 GLContext shareWith) {
    super(drawable, shareWith);
  }

  public void bindPbufferToTexture() {
    if (!rtt) {
      throw new GLException("Shouldn't try to bind a pbuffer to a texture if render-to-texture hasn't been " +
                            "specified in its GLCapabilities");
    }
    GL gl = getGL();
    WGLExt wglExt = getWGLExt();
    gl.glBindTexture(textureTarget, texture);
    if (rtt && hasRTT) {
      if (!wglExt.wglBindTexImageARB(((WindowsPbufferWGLDrawable)drawable).getPbufferHandle(), WGLExt.WGL_FRONT_LEFT_ARB)) {
        throw new GLException("Binding of pbuffer to texture failed: " + wglGetLastError());
      }
    }
    // FIXME: comment is wrong now
    // Note that if the render-to-texture extension is not supported,
    // we perform a glCopyTexImage2D in swapBuffers().
  }

  public void releasePbufferFromTexture() {
    if (!rtt) {
      throw new GLException("Shouldn't try to bind a pbuffer to a texture if render-to-texture hasn't been " +
                            "specified in its GLCapabilities");
    }
    if (rtt && hasRTT) {
      WGLExt wglExt = getWGLExt();
      if (!wglExt.wglReleaseTexImageARB(((WindowsPbufferWGLDrawable)drawable).getPbufferHandle(), WGLExt.WGL_FRONT_LEFT_ARB)) {
        throw new GLException("Releasing of pbuffer from texture failed: " + wglGetLastError());
      }
    }
  }

  protected boolean createImpl(GLContextImpl shareWith) {
    boolean res = super.createImpl(shareWith);
    if(res) {
      GLCapabilitiesImmutable capabilities = drawable.getChosenGLCapabilities();

      // Initialize render-to-texture support if requested
      GL gl = getGL();
      rtt  = capabilities.getPbufferRenderToTexture();
      rect = gl.isGL2GL3() && capabilities.getPbufferRenderToTextureRectangle();

      if (rtt) {
        if (DEBUG) {
          System.err.println("Initializing render-to-texture support");
        }

        if (!gl.isExtensionAvailable("WGL_ARB_render_texture")) {
          System.err.println("WindowsPbufferWGLContext: WARNING: WGL_ARB_render_texture extension not " +
                             "supported; implementing render_to_texture support using slow texture readback");
        } else {
          hasRTT = true;

          if (rect && !gl.isExtensionAvailable("GL_NV_texture_rectangle")) {
            System.err.println("WindowsPbufferWGLContext: WARNING: GL_NV_texture_rectangle extension not " +
                               "supported; skipping requested render_to_texture_rectangle support for pbuffer");
            rect = false;
          }
          if (rect) {
            if (DEBUG) {
              System.err.println("  Using render-to-texture-rectangle");
            }
            textureTarget = GL2.GL_TEXTURE_RECTANGLE_ARB;
          } else {
            if (DEBUG) {
              System.err.println("  Using vanilla render-to-texture");
            }
            textureTarget = GL.GL_TEXTURE_2D;
          }
          int[] tmp = new int[1];
          gl.glGenTextures(1, tmp, 0);
          texture = tmp[0];
          gl.glBindTexture(textureTarget, texture);
          gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
          gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
          gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
          gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
          gl.glCopyTexImage2D(textureTarget, 0, GL.GL_RGB, 0, 0, drawable.getWidth(), drawable.getHeight(), 0);
        }
      }
    }
    return res;
  }

  public int getFloatingPointMode() {
    return ((WindowsPbufferWGLDrawable)drawable).getFloatingPointMode();
  }

  private static String wglGetLastError() {
    return WindowsWGLDrawableFactory.wglGetLastError();
  }
}
