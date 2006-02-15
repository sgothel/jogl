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

import java.security.*;
import com.sun.opengl.impl.*;

/** <P> Provides a virtual machine- and operating system-independent
    mechanism for creating {@link GLDrawable}s. </P>

    <P> The {@link javax.media.opengl.GLCapabilities} objects passed
    in to the various factory methods are used as a hint for the
    properties of the returned drawable. The default capabilities
    selection algorithm (equivalent to passing in a null {@link
    GLCapabilitiesChooser}) is described in {@link
    DefaultGLCapabilitiesChooser}. Sophisticated applications needing
    to change the selection algorithm may pass in their own {@link
    GLCapabilitiesChooser} which can select from the available pixel
    formats. The GLCapabilitiesChooser mechanism may not be supported
    by all implementations or on all platforms, in which case any
    passed GLCapabilitiesChooser will be ignored. </P>

    <P> Because of the multithreaded nature of the Java platform's
    window system toolkit, it is typically not possible to immediately
    reject a given {@link GLCapabilities} as being unsupportable by
    either returning <code>null</code> from the creation routines or
    raising a {@link GLException}. The semantics of the rejection
    process are (unfortunately) left unspecified for now. The current
    implementation will cause a {@link GLException} to be raised
    during the first repaint of the {@link GLCanvas} or {@link
    GLJPanel} if the capabilities can not be met. Pbuffers are always
    created immediately and their creation will fail with a {@link
    GLException} if errors occur. </P>

    <P> The concrete GLDrawableFactory subclass instantiated by {@link
    #getFactory getFactory} can be changed by setting the system
    property <code>opengl.factory.class.name</code> to the
    fully-qualified name of the desired class. </P>
*/

public abstract class GLDrawableFactory {
  private static GLDrawableFactory factory;

  protected GLDrawableFactory() {}

  /** Returns the sole GLDrawableFactory instance. */
  public static GLDrawableFactory getFactory() {
    if (factory == null) {
      try {
        String factoryClassName =
          (String) AccessController.doPrivileged(new PrivilegedAction() {
              public Object run() {
                return System.getProperty("opengl.factory.class.name");
              }
            });
        String osName = System.getProperty("os.name");
        String osNameLowerCase = osName.toLowerCase();
        Class factoryClass = null;

        // Because there are some complications with generating all
        // platforms' Java glue code on all platforms (among them that we
        // would have to include jawt.h and jawt_md.h in the jogl
        // sources, which we currently don't have to do) we break the only
        // static dependencies with platform-specific code here using reflection.

        if (factoryClassName != null) {
          factoryClass = Class.forName(factoryClassName);
        } else if (osNameLowerCase.startsWith("wind")) {
          factoryClass = Class.forName("com.sun.opengl.impl.windows.WindowsGLDrawableFactory");
        } else if (osNameLowerCase.startsWith("mac os x")) {
          factoryClass = Class.forName("com.sun.opengl.impl.macosx.MacOSXGLDrawableFactory");
        } else {
          // Assume Linux, Solaris, etc. Should probably test for these explicitly.
          factoryClass = Class.forName("com.sun.opengl.impl.x11.X11GLDrawableFactory");
        }

        if (factoryClass == null) {
          throw new GLException("OS " + osName + " not yet supported");
        }

        factory = (GLDrawableFactory) factoryClass.newInstance();
      } catch (Exception e) {
        throw new GLException(e);
      }
    }

    return factory;
  }

  /**
   * <P> Selects a graphics configuration on the specified graphics
   * device compatible with the supplied GLCapabilities. This method
   * is intended to be used by applications which do not use the
   * supplied GLCanvas class but instead wrap their own Canvas or
   * other window toolkit-specific object with a GLDrawable. Some
   * platforms (specifically X11) require the graphics configuration
   * to be specified when the window toolkit object is created. This
   * method may return null on platforms on which the OpenGL pixel
   * format selection process is performed later. </P>
   *
   * <P> The concrete data type of the passed graphics device and
   * returned graphics configuration must be specified in the
   * documentation binding this particular API to the underlying
   * window toolkit. The Reference Implementation accepts {@link
   * AWTGraphicsDevice AWTGraphicsDevice} objects and returns {@link
   * AWTGraphicsConfiguration AWTGraphicsConfiguration} objects. </P>
   *
   * @see java.awt.Canvas#Canvas(java.awt.GraphicsConfiguration)
   *
   * @throws IllegalArgumentException if the data type of the passed
   *         AbstractGraphicsDevice is not supported by this
   *         GLDrawableFactory.
   * @throws GLException if any window system-specific errors caused
   *         the selection of the graphics configuration to fail.
   */
  public abstract AbstractGraphicsConfiguration
    chooseGraphicsConfiguration(GLCapabilities capabilities,
                                GLCapabilitiesChooser chooser,
                                AbstractGraphicsDevice device)
    throws IllegalArgumentException, GLException;

  /**
   * Returns a GLDrawable that wraps a platform-specific window system
   * object, such as an AWT or LCDUI Canvas. On platforms which
   * support it, selects a pixel format compatible with the supplied
   * GLCapabilities, or if the passed GLCapabilities object is null,
   * uses a default set of capabilities. On these platforms, uses
   * either the supplied GLCapabilitiesChooser object, or if the
   * passed GLCapabilitiesChooser object is null, uses a
   * DefaultGLCapabilitiesChooser instance.
   *
   * @throws IllegalArgumentException if the passed target is either
   *         null or its data type is not supported by this GLDrawableFactory.
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLDrawable to fail.
   */
  public abstract GLDrawable getGLDrawable(Object target,
                                           GLCapabilities capabilities,
                                           GLCapabilitiesChooser chooser)
    throws IllegalArgumentException, GLException;
  
  //----------------------------------------------------------------------
  // Methods to create high-level objects

  /**
   * Returns true if it is possible to create a GLPbuffer. Some older
   * graphics cards do not have this capability.
   */
  public abstract boolean canCreateGLPbuffer();

  /**
   * Creates a GLPbuffer with the given capabilites and dimensions. <P>
   *
   * See the note in the overview documentation on
   * <a href="../../../overview-summary.html#SHARING">context sharing</a>.
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLPbuffer to fail.
   */
  public abstract GLPbuffer createGLPbuffer(GLCapabilities capabilities,
                                            GLCapabilitiesChooser chooser,
                                            int initialWidth,
                                            int initialHeight,
                                            GLContext shareWith)
    throws GLException;

  //----------------------------------------------------------------------
  // Methods for interacting with third-party OpenGL libraries

  /**
   * <P> Creates a GLContext object representing an existing OpenGL
   * context in an external (third-party) OpenGL-based library. This
   * GLContext object may be used to draw into this preexisting
   * context using its {@link GL} and {@link
   * javax.media.opengl.glu.GLU} objects. New contexts created through
   * {@link GLDrawable}s may share textures and display lists with
   * this external context. </P>
   *
   * <P> The underlying OpenGL context must be current on the current
   * thread at the time this method is called. The user is responsible
   * for the maintenance of the underlying OpenGL context; calls to
   * <code>makeCurrent</code> and <code>release</code> on the returned
   * GLContext object have no effect. If the underlying OpenGL context
   * is destroyed, the <code>destroy</code> method should be called on
   * the <code>GLContext</code>. A new <code>GLContext</code> object
   * should be created for each newly-created underlying OpenGL
   * context.
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the external GLContext to fail.
   */
  public abstract GLContext createExternalGLContext()
    throws GLException;

  /**
   * Returns true if it is possible to create an external GLDrawable
   * object via {@link #createExternalGLDrawable}.
   */
  public abstract boolean canCreateExternalGLDrawable();

  /**
   * <P> Creates a {@link GLDrawable} object representing an existing
   * OpenGL drawable in an external (third-party) OpenGL-based
   * library. This GLDrawable object may be used to create new,
   * fully-functional {@link GLContext}s on the OpenGL drawable. This
   * is useful when interoperating with a third-party OpenGL-based
   * library and it is essential to not perturb the state of the
   * library's existing context, even to the point of not sharing
   * textures or display lists with that context. </P>
   *
   * <P> An underlying OpenGL context must be current on the desired
   * drawable and the current thread at the time this method is
   * called. The user is responsible for the maintenance of the
   * underlying drawable. If one or more contexts are created on the
   * drawable using {@link GLDrawable#createContext}, and the drawable
   * is deleted by the third-party library, the user is responsible
   * for calling {@link GLContext#destroy} on these contexts. </P>
   *
   * <P> Calls to <code>setSize</code>, <code>getWidth</code> and
   * <code>getHeight</code> are illegal on the returned GLDrawable. If
   * these operations are required by the user, they must be performed
   * by the third-party library. </P>
   *
   * <P> It is legal to create both an external GLContext and
   * GLDrawable representing the same third-party OpenGL entities.
   * This can be used, for example, to query current state information
   * using the external GLContext and then create and set up new
   * GLContexts using the external GLDrawable. </P>
   *
   * <P> This functionality may not be available on all platforms and
   * {@link #canCreateExternalGLDrawable} should be called first to
   * see if it is present. For example, on X11 platforms, this API
   * requires the presence of GLX 1.3 or later.
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the external GLDrawable to fail.
   */
  public abstract GLDrawable createExternalGLDrawable()
    throws GLException;
}
