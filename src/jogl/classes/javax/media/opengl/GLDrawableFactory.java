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

package javax.media.opengl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.JogampRuntimeException;
import jogamp.common.Debug;

import com.jogamp.common.util.ReflectionUtil;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GLProfile.ShutdownType;

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
    during the first repaint of the {@link javax.media.opengl.awt.GLCanvas} or {@link
    javax.media.opengl.awt.GLJPanel} if the capabilities can not be met.<br>
    {@link javax.media.opengl.GLPbuffer} are always
    created immediately and their creation will fail with a 
    {@link javax.media.opengl.GLException} if errors occur. </P>

    <P> The concrete GLDrawableFactory subclass instantiated by {@link
    #getFactory getFactory} can be changed by setting the system
    property <code>opengl.factory.class.name</code> to the
    fully-qualified name of the desired class. </P>
*/
public abstract class GLDrawableFactory {

  static final String macosxFactoryClassNameCGL = "jogamp.opengl.macosx.cgl.MacOSXCGLDrawableFactory";
  static final String macosxFactoryClassNameAWTCGL = "jogamp.opengl.macosx.cgl.awt.MacOSXAWTCGLDrawableFactory";
  
  private static volatile boolean isInit = false;
  private static GLDrawableFactory eglFactory;
  private static GLDrawableFactory nativeOSFactory;

  protected static ArrayList<GLDrawableFactory> glDrawableFactories = new ArrayList<GLDrawableFactory>();

  // Shutdown hook mechanism for the factory
  private static boolean factoryShutdownHookRegistered = false;
  private static Thread factoryShutdownHook = null;

  /**
   * Instantiate singleton factories if available, EGLES1, EGLES2 and the OS native ones.
   */
  public static final void initSingleton() { 
      if (!isInit) { // volatile: ok
          synchronized (GLDrawableFactory.class) {
              if (!isInit) {
                  isInit=true;
                  initSingletonImpl();
              }
          }
      }
  }  
  private static final void initSingletonImpl() {
    registerFactoryShutdownHook();
    
    final String nativeOSType = NativeWindowFactory.getNativeWindowType(true);
    GLDrawableFactory tmp = null;
    String factoryClassName = Debug.getProperty("jogl.gldrawablefactory.class.name", true, AccessController.getContext());
    ClassLoader cl = GLDrawableFactory.class.getClassLoader();
    if (null == factoryClassName) {
        if ( nativeOSType.equals(NativeWindowFactory.TYPE_X11) ) {
          factoryClassName = "jogamp.opengl.x11.glx.X11GLXDrawableFactory";
        } else if ( nativeOSType.equals(NativeWindowFactory.TYPE_WINDOWS) ) {
          factoryClassName = "jogamp.opengl.windows.wgl.WindowsWGLDrawableFactory";
        } else if ( nativeOSType.equals(NativeWindowFactory.TYPE_MACOSX) ) {
            if(ReflectionUtil.isClassAvailable(macosxFactoryClassNameAWTCGL, cl)) {
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
      try {
          tmp = (GLDrawableFactory) ReflectionUtil.createInstance(factoryClassName, cl);
      } catch (JogampRuntimeException jre) { 
          if (GLProfile.DEBUG) {
              System.err.println("Info: GLDrawableFactory.static - Native Platform: "+nativeOSType+" - not available: "+factoryClassName);
              jre.printStackTrace();
          }
      }
    }
    nativeOSFactory = tmp;

    tmp = null;
    try {
        tmp = (GLDrawableFactory) ReflectionUtil.createInstance("jogamp.opengl.egl.EGLDrawableFactory", cl);
    } catch (JogampRuntimeException jre) {
        if (GLProfile.DEBUG) {
            System.err.println("Info: GLDrawableFactory.static - EGLDrawableFactory - not available");
            jre.printStackTrace();
        }
    }
    eglFactory = tmp;
  }

  protected static void shutdown(ShutdownType shutdownType) {
    if (isInit) { // volatile: ok
      synchronized (GLDrawableFactory.class) {
          if (isInit) {
              isInit=false;
              unregisterFactoryShutdownHook();
              shutdownImpl(shutdownType);
          }
      }
    }
  }
  private static void shutdownImpl(ShutdownType shutdownType) {
    synchronized(glDrawableFactories) {
        for(int i=0; i<glDrawableFactories.size(); i++) {
            glDrawableFactories.get(i).destroy(shutdownType);
        }
        glDrawableFactories.clear();
        
        // both were members of glDrawableFactories and are shutdown already 
        nativeOSFactory = null;
        eglFactory = null;
    }
  }
  
  private static synchronized void registerFactoryShutdownHook() {
    if (factoryShutdownHookRegistered) {
        return;
    }
    factoryShutdownHook = new Thread(new Runnable() {
        public void run() {
            GLDrawableFactory.shutdownImpl(GLProfile.ShutdownType.COMPLETE);
        }
    });
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run() {
            Runtime.getRuntime().addShutdownHook(factoryShutdownHook);
            return null;
        }
    });
    factoryShutdownHookRegistered = true;
  }

  private static synchronized void unregisterFactoryShutdownHook() {
    if (!factoryShutdownHookRegistered) {
        return;
    }
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run() {
            Runtime.getRuntime().removeShutdownHook(factoryShutdownHook);
            return null;
        }
    });
    factoryShutdownHookRegistered = false;
  }


  protected GLDrawableFactory() {
    synchronized(glDrawableFactories) {
        glDrawableFactories.add(this);
    }
  }

  protected void enterThreadCriticalZone() {};
  protected void leaveThreadCriticalZone() {};

  protected abstract void destroy(ShutdownType shutdownType);

  /**
   * Retrieve the default <code>device</code> {@link AbstractGraphicsDevice#getConnection() connection},
   * {@link AbstractGraphicsDevice#getUnitID() unit ID} and {@link AbstractGraphicsDevice#getUniqueID() unique ID name}. for this factory<br>
   * The implementation must return a non <code>null</code> default device, which must not be opened, ie. it's native handle is <code>null</code>.
   * @return the default shared device for this factory, eg. :0.0 on X11 desktop.
   */
  public abstract AbstractGraphicsDevice getDefaultDevice();

  /**
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @return true if the device is compatible with this factory, ie. if it can be used for creation. Otherwise false.
   */
  public abstract boolean getIsDeviceCompatible(AbstractGraphicsDevice device);

  protected final AbstractGraphicsDevice validateDevice(AbstractGraphicsDevice device) {
      if(null==device) {
          device = getDefaultDevice();
          if(null==device) {
              throw new InternalError("no default device");
          }
          if (GLProfile.DEBUG) {
              System.err.println("Info: GLDrawableFactory.validateDevice: using default device : "+device);
          }
      } else if( !getIsDeviceCompatible(device) ) {
          if (GLProfile.DEBUG) {
              System.err.println("Info: GLDrawableFactory.validateDevice: device not compatible : "+device);
          }
          return null;
      }
      return device;
  }

  /**
   * Validate and start the shared resource runner thread if necessary and 
   * if the implementation uses it.
   * 
   * @return the shared resource runner thread, if implementation uses it.
   */
  protected abstract Thread getSharedResourceThread();
  
  /**
   * Create the shared resource used internally as a reference for capabilities etc.
   * <p>
   * Returns true if a shared resource could be created 
   * for the <code>device</code> {@link AbstractGraphicsDevice#getConnection()}.<br>
   * This does not imply a shared resource is mapped (ie. made persistent), but is available in general<br>.
   * </p>
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @return true if a shared resource could been created, otherwise false. 
   */
  protected abstract boolean createSharedResource(AbstractGraphicsDevice device);
  
  /**
   * Returns the sole GLDrawableFactory instance for the desktop (X11, WGL, ..) if exist or null
   */
  public static GLDrawableFactory getDesktopFactory() {
    GLProfile.initSingleton();    
    return nativeOSFactory;
  }

  /**
   * Returns the sole GLDrawableFactory instance for EGL if exist or null
   */
  public static GLDrawableFactory getEGLFactory() {
    GLProfile.initSingleton();    
    return eglFactory;
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
        if(null==eglFactory) {
            throw new GLException("No EGLDrawableFactory available for profile: "+glProfileImplName);
        }
        return eglFactory;
    }
    if(null==nativeOSFactory) {
        throw new GLException("No native platform GLDrawableFactory available for profile: "+glProfileImplName);
    }
    return nativeOSFactory;
  }

  protected static GLDrawableFactory getFactoryImpl(AbstractGraphicsDevice device) throws GLException {
    if(null != nativeOSFactory && nativeOSFactory.getIsDeviceCompatible(device)) {
        return nativeOSFactory;
    }
    if(null != eglFactory && eglFactory.getIsDeviceCompatible(device)) {
        return eglFactory;
    }
    throw new GLException("No native platform GLDrawableFactory, nor EGLDrawableFactory available: "+device);
  }

  /**
   * Returns an array of available GLCapabilities for the device.<br>
   * The list is sorted by the native ID, ascending.<br>
   * The chosen GLProfile statement in the result may not refer to the maximum available profile
   * due to implementation constraints, ie using the shared resource.
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @return A list of {@link javax.media.opengl.GLCapabilitiesImmutable}'s, maybe empty if none is available.
   */
  public final List<GLCapabilitiesImmutable> getAvailableCapabilities(AbstractGraphicsDevice device) {
      device = validateDevice(device);
      if(null!=device) {
        return getAvailableCapabilitiesImpl(device);
      }
      return null;
  }
  protected abstract List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(AbstractGraphicsDevice device);

  //----------------------------------------------------------------------
  // Methods to create high-level objects

  /**
   * Returns a GLDrawable according to it's chosen Capabilities,<br>
   * which determines pixel format, on- and offscreen incl. PBuffer type.
   * <p>
   * The native platform's chosen Capabilties are referenced within the target
   * NativeSurface's AbstractGraphicsConfiguration.<p>
   *
   * In case target's {@link javax.media.nativewindow.Capabilities#isOnscreen()} is true,<br>
   * an onscreen GLDrawable will be realized.
   * <p>
   * In case target's {@link javax.media.nativewindow.Capabilities#isOnscreen()} is false,<br>
   * either a Pbuffer drawable is created if target's {@link javax.media.opengl.GLCapabilities#isPBuffer()} is true,<br>
   * or a simple pixmap/bitmap drawable is created. The latter is unlikely to be hardware accelerated.<br>
   * <p>
   *
   * @throws IllegalArgumentException if the passed target is null
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLDrawable to fail.
   *
   * @see javax.media.nativewindow.GraphicsConfigurationFactory#chooseGraphicsConfiguration(Capabilities, CapabilitiesChooser, AbstractGraphicsScreen)
   */
  public abstract GLDrawable createGLDrawable(NativeSurface target)
    throws IllegalArgumentException, GLException;

  /**
   * Creates a Offscreen GLDrawable incl it's offscreen {@link javax.media.nativewindow.NativeSurface} with the given capabilites and dimensions.
   * <p>
   * A Pbuffer drawable/surface is created if both {@link javax.media.opengl.GLCapabilities#isPBuffer() caps.isPBuffer()}
   * and {@link #canCreateGLPbuffer(javax.media.nativewindow.AbstractGraphicsDevice) canCreateGLPbuffer(device)} is true.<br>
   * Otherwise a simple pixmap/bitmap drawable/surface is created, which is unlikely to be hardware accelerated.<br>
   * </p>
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared device to be used, may be <code>null</code> for the platform's default device.
   * @param caps the requested GLCapabilties
   * @param chooser the custom chooser, may be null for default
   * @param width the requested offscreen width
   * @param height the requested offscreen height
   *
   * @return the created offscreen GLDrawable
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the Offscreen to fail.
   */
  public abstract GLDrawable createOffscreenDrawable(AbstractGraphicsDevice device,
                                                     GLCapabilitiesImmutable capabilities,
                                                     GLCapabilitiesChooser chooser,
                                                     int width, int height)
    throws GLException;

  /**
   * Creates an offscreen NativeSurface.<br>
   * A Pbuffer surface is created if both {@link javax.media.opengl.GLCapabilities#isPBuffer() caps.isPBuffer()}
   * and {@link #canCreateGLPbuffer(javax.media.nativewindow.AbstractGraphicsDevice) canCreateGLPbuffer(device)} is true.<br>
   * Otherwise a simple pixmap/bitmap surface is created. The latter is unlikely to be hardware accelerated.<br>
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @param caps the requested GLCapabilties
   * @param chooser the custom chooser, may be null for default
   * @param width the requested offscreen width
   * @param height the requested offscreen height
   * @return the created offscreen native surface
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLDrawable to fail.
   */
  public abstract NativeSurface createOffscreenSurface(AbstractGraphicsDevice device,
                                                       GLCapabilitiesImmutable caps,
                                                       GLCapabilitiesChooser chooser,
                                                       int width, int height);

  /**
   * Highly experimental API entry, allowing developer of new windowing system bindings 
   * to leverage the native window handle to produce a NativeSurface implementation (ProxySurface), having the required GLCapabilities.<br>
   * Such surface can be used to instantiate a GLDrawable and hence test your new binding w/o the 
   * costs of providing a full set of abstraction like the AWT GLCanvas or even the native NEWT bindings.
   * 
   * @param device the platform's target device, shall not be <code>null</code>
   * @param windowHandle the native window handle
   * @param caps the requested GLCapabilties
   * @param chooser the custom chooser, may be null for default
   * @return The proxy surface wrapping the windowHandle on the device
   */
  public abstract ProxySurface createProxySurface(AbstractGraphicsDevice device, 
                                                  long windowHandle, 
                                                  GLCapabilitiesImmutable caps, 
                                                  GLCapabilitiesChooser chooser);

  /**
   * Returns true if it is possible to create a GLPbuffer. Some older
   * graphics cards do not have this capability.
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   */
  public abstract boolean canCreateGLPbuffer(AbstractGraphicsDevice device);

  /**
   * Creates a GLPbuffer with the given capabilites and dimensions. <P>
   *
   * See the note in the overview documentation on
   * <a href="../../../overview-summary.html#SHARING">context sharing</a>.
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @param capabilities the requested capabilities
   * @param chooser the custom chooser, may be null for default
   * @param initialWidth initial width of pbuffer
   * @param initialHeight initial height of pbuffer
   * @param shareWith a shared GLContext this GLPbuffer shall use
   *
   * @return the new {@link GLPbuffer} specific {@link GLAutoDrawable}
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLPbuffer to fail.
   */
  public abstract GLPbuffer createGLPbuffer(AbstractGraphicsDevice device,
                                            GLCapabilitiesImmutable capabilities,
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
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   */
  public abstract boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device);

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
