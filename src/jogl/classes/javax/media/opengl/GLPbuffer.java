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
    platforms and may be deprecated in a future release.

    @deprecated Use {@link GLOffscreenAutoDrawable} w/ {@link GLCapabilities#setFBO(boolean)}
                via {@link GLDrawableFactory#createOffscreenAutoDrawable(javax.media.nativewindow.AbstractGraphicsDevice, GLCapabilitiesImmutable, GLCapabilitiesChooser, int, int, GLContext) GLDrawableFactory.createOffscreenAutoDrawable(..)}.
  */
public interface GLPbuffer extends GLAutoDrawable {
  /** Destroys the native resources associated with this pbuffer. It
      is not valid to call display() or any other routines on this
      pbuffer after it has been destroyed. Before destroying the
      pbuffer, the application must destroy any additional OpenGL
      contexts which have been created for the pbuffer via {@link
      #createContext}. */
  @Override
  public void destroy();
}
