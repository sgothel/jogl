/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package javax.media.opengl;

/** Provides offscreen rendering support via pbuffers. The principal
    addition of this interface is a {@link #destroy} method to
    deallocate the pbuffer and its associated resources. It also
    contains experimental methods for accessing the pbuffer's contents
    as a texture map and enabling rendering to floating-point frame
    buffers. These methods are not guaranteed to be supported on all
    platforms and may be deprecated in a future release. */

public interface GLPbuffer extends GLAutoDrawable {
  /** Indicates the GL_APPLE_float_pixels extension is being used for this pbuffer. */
  public static final int APPLE_FLOAT = 1;

  /** Indicates the GL_ATI_texture_float extension is being used for this pbuffer. */
  public static final int ATI_FLOAT = 2;

  /** Indicates the GL_NV_float_buffer extension is being used for this pbuffer. */
  public static final int NV_FLOAT = 3;

  /** Binds this pbuffer to its internal texture target. Only valid to
      call if offscreen render-to-texture has been specified in the
      GLCapabilities for this GLPbuffer. If the
      render-to-texture-rectangle capability has also been specified,
      this will use e.g. wglBindTexImageARB as its implementation and
      cause the texture to be bound to e.g. the
      GL_TEXTURE_RECTANGLE_NV state; otherwise, during the display()
      phase the pixels will have been copied into an internal texture
      target and this will cause that to be bound to the GL_TEXTURE_2D
      state. */
  public void bindTexture();

  /** Unbinds the pbuffer from its internal texture target. */
  public void releaseTexture();

  /** Destroys the native resources associated with this pbuffer. It
      is not valid to call display() or any other routines on this
      pbuffer after it has been destroyed. Before destroying the
      pbuffer, the application must destroy any additional OpenGL
      contexts which have been created for the pbuffer via {@link
      #createContext}. */
  public void destroy();

  /** Indicates which vendor's extension is being used to support
      floating point channels in this pbuffer if that capability was
      requested in the GLCapabilities during pbuffer creation. Returns
      one of NV_FLOAT, ATI_FLOAT or APPLE_FLOAT, or throws GLException
      if floating-point channels were not requested for this pbuffer.
      This function may only be called once the init method for this
      pbuffer's GLEventListener has been called. */
  public int getFloatingPointMode();
}
