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

/** <P> Provides a virtual machine- and operating system-independent
    mechanism for creating {@link net.java.games.jogl.GLCanvas} and {@link
    net.java.games.jogl.GLJPanel} objects. </P>

    <P> The {@link net.java.games.jogl.GLCapabilities} objects passed in to the
    various factory methods are used as a hint for the properties of
    the returned drawable. The default capabilities selection
    algorithm (equivalent to passing in a null {@link
    GLCapabilitiesChooser}) is described in {@link
    DefaultGLCapabilitiesChooser}. Sophisticated applications needing
    to change the selection algorithm may pass in their own {@link
    GLCapabilitiesChooser} which can select from the available pixel
    formats. </P>

    <P> Because of the multithreaded nature of the Java platform's
    window system toolkit, it is typically not possible to immediately
    reject a given {@link GLCapabilities} as being unsupportable by
    either returning <code>null</code> from the creation routines or
    raising a {@link GLException}. The semantics of the rejection
    process are (unfortunately) left unspecified for now. The current
    implementation will cause a {@link GLException} to be raised
    during the first repaint of the {@link GLCanvas} or {@link
    GLJPanel} if the capabilities can not be met. </P>
*/

public class GLDrawableFactory {
  private static GLDrawableFactory factory = new GLDrawableFactory();

  private GLDrawableFactory() {}

  /** Returns the sole GLDrawableFactory instance. */
  public static GLDrawableFactory getFactory() {
    return factory;
  }

  /** Creates a {@link GLCanvas} with the specified capabilities using
      the default capabilities selection algorithm. */
  public GLCanvas createGLCanvas(GLCapabilities capabilities) {
    return createGLCanvas(capabilities, null, null);
  }

  /** Creates a {@link GLCanvas} with the specified capabilities using
      the default capabilities selection algorithm. The canvas will
      share textures and display lists with the specified {@link
      GLDrawable}; the drawable must either be null or have been
      fabricated from this factory or by classes in this package. A
      null drawable indicates no sharing. */
  public GLCanvas createGLCanvas(GLCapabilities capabilities, GLDrawable shareWith) {
    return createGLCanvas(capabilities, null, shareWith);
  }

  /** Creates a {@link GLCanvas} with the specified capabilities using
      the supplied capabilities selection algorithm. A null chooser is
      equivalent to using the {@link DefaultGLCapabilitiesChooser}. */
  public GLCanvas createGLCanvas(GLCapabilities capabilities, GLCapabilitiesChooser chooser) {
    return createGLCanvas(capabilities, chooser, null);
  }

  /** Creates a {@link GLCanvas} with the specified capabilities using
      the supplied capabilities selection algorithm. A null chooser is
      equivalent to using the {@link DefaultGLCapabilitiesChooser}.
      The canvas will share textures and display lists with the
      specified {@link GLDrawable}; the drawable must either be null
      or have been fabricated from this factory or by classes in this
      package. A null drawable indicates no sharing. */
  public GLCanvas createGLCanvas(GLCapabilities capabilities,
                                 GLCapabilitiesChooser chooser,
                                 GLDrawable shareWith) {
    // FIXME: do we need to select a GraphicsConfiguration here as in
    // GL4Java? If so, this class will have to be made abstract and
    // we'll have to provide hooks into this package to get at the
    // GLCanvas and GLJPanel constructors.
    if (chooser == null) {
      chooser = new DefaultGLCapabilitiesChooser();
    }
    return new GLCanvas(capabilities, chooser, shareWith);
  }

  /** Creates a {@link GLJPanel} with the specified capabilities using
      the default capabilities selection algorithm. */
  public GLJPanel createGLJPanel(GLCapabilities capabilities) {
    return createGLJPanel(capabilities, null, null);
  }

  /** Creates a {@link GLJPanel} with the specified capabilities using
      the default capabilities selection algorithm. The panel will
      share textures and display lists with the specified {@link
      GLDrawable}; the drawable must either be null or have been
      fabricated from this factory or by classes in this package. A
      null drawable indicates no sharing. */
  public GLJPanel createGLJPanel(GLCapabilities capabilities, GLDrawable shareWith) {
    return createGLJPanel(capabilities, null, shareWith);
  }

  /** Creates a {@link GLJPanel} with the specified capabilities using
      the supplied capabilities selection algorithm. A null chooser is
      equivalent to using the {@link DefaultGLCapabilitiesChooser}. */
  public GLJPanel createGLJPanel(GLCapabilities capabilities, GLCapabilitiesChooser chooser) {
    return createGLJPanel(capabilities, chooser, null);
  }

  /** Creates a {@link GLJPanel} with the specified capabilities using
      the supplied capabilities selection algorithm. A null chooser is
      equivalent to using the {@link DefaultGLCapabilitiesChooser}.
      The panel will share textures and display lists with the
      specified {@link GLDrawable}; the drawable must either be null
      or have been fabricated from this factory or by classes in this
      package. A null drawable indicates no sharing. */
  public GLJPanel createGLJPanel(GLCapabilities capabilities,
                                 GLCapabilitiesChooser chooser,
                                 GLDrawable shareWith) {
    if (chooser == null) {
      chooser = new DefaultGLCapabilitiesChooser();
    }
    return new GLJPanel(capabilities, chooser, shareWith);
  }
}
