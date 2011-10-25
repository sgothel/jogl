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
 */

package jogamp.opengl.macosx.cgl;

import javax.media.nativewindow.DefaultGraphicsConfiguration;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPbuffer;


public class MacOSXPbufferCGLContext extends MacOSXCGLContext {

  // State for render-to-texture and render-to-texture-rectangle support
  private int textureTarget; // e.g. GL_TEXTURE_2D, GL_TEXTURE_RECTANGLE_NV
  private int texture;       // actual texture object

  public MacOSXPbufferCGLContext(MacOSXPbufferCGLDrawable drawable,
                                GLContext shareWith) {
    super(drawable, shareWith);
  }

  public void bindPbufferToTexture() {
    GL gl = getGL();
    gl.glBindTexture(textureTarget, texture);
    // FIXME: not clear whether this is really necessary, but since
    // the API docs seem to imply it is and since it doesn't seem to
    // impact performance, leaving it in
    CGL.setContextTextureImageToPBuffer(contextHandle, drawable.getHandle(), GL.GL_FRONT);
  }

  public void releasePbufferFromTexture() {
  }

  protected void makeCurrentImpl(boolean newCreated) throws GLException {
    super.makeCurrentImpl(newCreated);
    
    if (newCreated) {
      // Initialize render-to-texture support if requested
      DefaultGraphicsConfiguration config = (DefaultGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration().getNativeGraphicsConfiguration();
      GLCapabilitiesImmutable capabilities = (GLCapabilitiesImmutable)config.getChosenCapabilities();
      GL gl = getGL();
      boolean rect = gl.isGL2GL3() && capabilities.getPbufferRenderToTextureRectangle();
      if (rect) {
        if (!gl.isExtensionAvailable("GL_EXT_texture_rectangle")) {
          System.err.println("MacOSXPbufferCGLContext: WARNING: GL_EXT_texture_rectangle extension not " +
                             "supported; skipping requested render_to_texture_rectangle support for pbuffer");
          rect = false;
        }
      }
      textureTarget = (rect ? GL2.GL_TEXTURE_RECTANGLE : GL.GL_TEXTURE_2D);
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

  public int getFloatingPointMode() {
    return GLPbuffer.APPLE_FLOAT;
  }
}
