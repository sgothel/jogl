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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.jogl;

/** Offscreen rendering support via pbuffers. This class adds very
    little functionality over the GLDrawable class; the only methods
    are those which allow access to the pbuffer's contents as a
    texture map. These methods are currently highly experimental and
    may be removed in a future release. */

public interface GLPbuffer extends GLDrawable {
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

  /** Queries initialization status of this pBuffer. */
  public boolean isInitialized();

  /** Destroys the native resources associated with this pbuffer. It
      is not valid to call display() or any other routines on this
      pbuffer after it has been destroyed. */
  public void destroy();
}
