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

import javax.media.nativewindow.*;

import java.lang.reflect.*;
import java.security.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.NWReflection;

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
    Abstract Window Toolkit, it is typically not possible to immediately
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
  /** Creates a new GLDrawableFactory instance. End users do not need
      to call this method. */
  protected GLDrawableFactory() {
  }

  private static final GLDrawableFactory eglFactory;
  private static final GLDrawableFactory nativeOSFactory;
  private static final String nativeOSType;
  static final String macosxFactoryClassNameCGL = "com.sun.opengl.impl.macosx.cgl.MacOSXCGLDrawableFactory";
  static final String macosxFactoryClassNameAWTCGL = "com.sun.opengl.impl.macosx.cgl.awt.MacOSXAWTCGLDrawableFactory";

  /**
   * Instantiate singleton factories if available, EGLES1, EGLES2 and the OS native ones.
   */
  static {
    GLDrawableFactory tmp = null;
    try {
        tmp = (GLDrawableFactory) NWReflection.createInstance("com.sun.opengl.impl.egl.EGLDrawableFactory");
    } catch (Throwable t) {
        if (GLProfile.DEBUG) {
            System.err.println("GLDrawableFactory.static - EGLDrawableFactory - not available");
            t.printStackTrace();
        }
    }
    eglFactory = tmp;

    nativeOSType = NativeWindowFactory.getNativeWindowType(true);

    String factoryClassName = null;
    tmp = null;
    try {
        factoryClassName = Debug.getProperty("jogl.gldrawablefactory.class.name", true, AccessController.getContext());
        if (null == factoryClassName) {
            if ( nativeOSType.equals(NativeWindowFactory.TYPE_X11) ) {
              factoryClassName = "com.sun.opengl.impl.x11.glx.X11GLXDrawableFactory";
            } else if ( nativeOSType.equals(NativeWindowFactory.TYPE_WINDOWS) ) {
              factoryClassName = "com.sun.opengl.impl.windows.wgl.WindowsWGLDrawableFactory";
            } else if ( nativeOSType.equals(NativeWindowFactory.TYPE_MACOSX) ) {
                if(NWReflection.isClassAvailable(macosxFactoryClassNameAWTCGL)) {
                    factoryClassName = macosxFactoryClassNameAWTCGL;
                } else {
                    factoryClassName = macosxFactoryClassNameCGL;
                }
            } else {
              // may use egl*Factory ..
              if (GLProfile.DEBUG) {
                  System.err.println("GLDrawableFactory.static - No native OS Factory for: "+nativeOSType+"; May use EGLDrawableFactory, if available." );
              }
            }
        }
        if (null != factoryClassName) {
          if (GLProfile.DEBUG) {
              System.err.println("GLDrawableFactory.static - Native OS Factory for: "+nativeOSType+": "+factoryClassName);
          }
          tmp = (GLDrawableFactory) NWReflection.createInstance(factoryClassName);
        }
    } catch (Throwable t) {
        if (GLProfile.DEBUG) {
            System.err.println("GLDrawableFactory.static - Native Platform: "+nativeOSType+" - not available: "+factoryClassName);
            t.printStackTrace();
        }
    }
    nativeOSFactory = tmp;
  }

  /** 
   * Returns the sole GLDrawableFactory instance. 
   * 
   * @param glProfile GLProfile to determine the factory type, ie EGLDrawableFactory, 
   *                or one of the native GLDrawableFactory's, ie X11/GLX, Windows/WGL or MacOSX/CGL.
   */
  public static GLDrawableFactory getFactory(GLProfile glProfile) throws GLException {
    return getFactoryImpl(glProfile.getImplName());
  }

  protected static GLDrawableFactory getFactoryImpl(String glProfileImplName) throws GLException {
    if ( GLProfile.usesNativeGLES(glProfileImplName) ) {
        if(null==eglFactory) throw new GLException("EGLDrawableFactory unavailable: "+glProfileImplName);
        return eglFactory;
    }
    if(null!=nativeOSFactory) {
        return nativeOSFactory;
    }
    if(null!=eglFactory) {
        return eglFactory;
    }
    throw new GLException("No native platform GLDrawableFactory, nor EGLDrawableFactory available: "+glProfileImplName);
  }

  /** Shuts down this GLDrawableFactory, releasing resources
      associated with it. Before calling this method you should first
      destroy any GLContexts and GLDrawables that have been created
      and are still in use. No further OpenGL calls may be made after
      shutting down the GLDrawableFactory. */
  public void shutdown() {
  }

  //----------------------------------------------------------------------
  // Methods to create high-level objects

  /**
   * Returns a GLDrawable according to it's chosen Capabilities,<br>
   * which determines pixel format, on- and offscreen incl. PBuffer type.
   * <p>
   * The native platform's chosen Capabilties are referenced within the target
   * NativeWindow's AbstractGraphicsConfiguration.<p>
   *
   * In case {@link javax.media.nativewindow.Capabilties#isOnscreen()} is true,<br>
   * an onscreen GLDrawable will be realized.
   * <p>
   * In case {@link javax.media.nativewindow.Capabilties#isOnscreen()} is false,<br>
   * either a Pbuffer drawable is created if {@link javax.media.opengl.GLCapabilities#isPBuffer()} is true,<br>
   * or a simple offscreen drawable is creates. The latter is unlikely to be hardware accelerated.<br>
   * <p>
   *
   * @throws IllegalArgumentException if the passed target is null
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLDrawable to fail.
   *
   * @see javax.media.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
   */
  public abstract GLDrawable createGLDrawable(NativeWindow target)
    throws IllegalArgumentException, GLException;

  /**
   * Creates a Offscreen GLDrawable with the given capabilites and dimensions. <P>
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the Offscreen to fail.
   */
  public abstract GLDrawable createOffscreenDrawable(GLCapabilities capabilities,
                                                     GLCapabilitiesChooser chooser,
                                                     int width, int height)
    throws GLException;

  /**
   * Returns true if it is possible to create a GLPbuffer. Some older
   * graphics cards do not have this capability.
   */
  public abstract boolean canCreateGLPbuffer();

  /**
   * Creates a Pbuffer GLDrawable with the given capabilites and dimensions. <P>
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLPbuffer to fail.
   */
  public abstract GLDrawable createGLPbufferDrawable(GLCapabilities capabilities,
                                                     GLCapabilitiesChooser chooser,
                                                     int initialWidth,
                                                     int initialHeight)
    throws GLException;

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
