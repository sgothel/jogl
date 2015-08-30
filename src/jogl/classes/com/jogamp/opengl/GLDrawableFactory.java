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

package com.jogamp.opengl;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.GLRendererQuirks;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

import jogamp.opengl.Debug;

/** <p> Provides a virtual machine- and operating system-independent
    mechanism for creating {@link GLDrawable}s.
    </p>
    <p> The {@link com.jogamp.opengl.GLCapabilities} objects passed
    in to the various factory methods are used as a hint for the
    properties of the returned drawable. The default capabilities
    selection algorithm (equivalent to passing in a null {@link
    GLCapabilitiesChooser}) is described in {@link
    DefaultGLCapabilitiesChooser}. Sophisticated applications needing
    to change the selection algorithm may pass in their own {@link
    GLCapabilitiesChooser} which can select from the available pixel
    formats. The GLCapabilitiesChooser mechanism may not be supported
    by all implementations or on all platforms, in which case any
    passed GLCapabilitiesChooser will be ignored.
    </p>

    <p> Because of the multithreaded nature of the Java platform's
    Abstract Window Toolkit, it is typically not possible to immediately
    reject a given {@link GLCapabilities} as being unsupportable by
    either returning <code>null</code> from the creation routines or
    raising a {@link GLException}. The semantics of the rejection
    process are (unfortunately) left unspecified for now. The current
    implementation will cause a {@link GLException} to be raised
    during the first repaint of the {@link com.jogamp.opengl.awt.GLCanvas} or {@link
    com.jogamp.opengl.awt.GLJPanel} if the capabilities can not be met.<br>
    {@link GLOffscreenAutoDrawable} are created lazily,
    see {@link #createOffscreenAutoDrawable(AbstractGraphicsDevice, GLCapabilitiesImmutable, GLCapabilitiesChooser, int, int) createOffscreenAutoDrawable(..)}.
    </p>

    <p> The concrete GLDrawableFactory subclass instantiated by {@link
    #getFactory getFactory} can be changed by setting the system
    property <code>opengl.factory.class.name</code> to the
    fully-qualified name of the desired class.
    </p>
*/
public abstract class GLDrawableFactory {

  protected static final boolean DEBUG = Debug.debug("GLDrawable");

  private static volatile boolean isInit = false;
  private static GLDrawableFactory eglFactory;
  private static GLDrawableFactory nativeOSFactory;

  private static ArrayList<GLDrawableFactory> glDrawableFactories = new ArrayList<GLDrawableFactory>();

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
    NativeWindowFactory.initSingleton();
    NativeWindowFactory.addCustomShutdownHook(false /* head */, new Runnable() {
       @Override
       public void run() {
           shutdown0();
       }
    });

    final String nwt = NativeWindowFactory.getNativeWindowType(true);
    GLDrawableFactory tmp = null;
    String factoryClassName = PropertyAccess.getProperty("jogl.gldrawablefactory.class.name", true);
    final ClassLoader cl = GLDrawableFactory.class.getClassLoader();
    if (null == factoryClassName) {
        if ( nwt == NativeWindowFactory.TYPE_X11 ) {
          factoryClassName = "jogamp.opengl.x11.glx.X11GLXDrawableFactory";
        } else if ( nwt == NativeWindowFactory.TYPE_WINDOWS ) {
          factoryClassName = "jogamp.opengl.windows.wgl.WindowsWGLDrawableFactory";
        } else if ( nwt == NativeWindowFactory.TYPE_MACOSX ) {
          factoryClassName = "jogamp.opengl.macosx.cgl.MacOSXCGLDrawableFactory";
        } else {
          // may use egl*Factory ..
          if (DEBUG || GLProfile.DEBUG) {
              System.err.println("GLDrawableFactory.static - No native Windowing Factory for: "+nwt+"; May use EGLDrawableFactory, if available." );
          }
        }
    }
    if ( !GLProfile.disableOpenGLDesktop ) {
        if ( null != factoryClassName ) {
            if (DEBUG || GLProfile.DEBUG) {
                System.err.println("GLDrawableFactory.static - Native OS Factory for: "+nwt+": "+factoryClassName);
            }
            try {
                tmp = (GLDrawableFactory) ReflectionUtil.createInstance(factoryClassName, cl);
            } catch (final Exception jre) {
                if (DEBUG || GLProfile.DEBUG) {
                    System.err.println("Info: GLDrawableFactory.static - Native Platform: "+nwt+" - not available: "+factoryClassName);
                    jre.printStackTrace();
                }
            }
            if(null != tmp && tmp.isComplete()) {
                nativeOSFactory = tmp;
            }
            tmp = null;
        } else if( DEBUG || GLProfile.DEBUG ) {
            System.err.println("Info: GLDrawableFactory.static - Desktop GLDrawableFactory unspecified!");
        }
    } else if( DEBUG || GLProfile.DEBUG ) {
        System.err.println("Info: GLDrawableFactory.static - Desktop GLDrawableFactory - disabled!");
    }

    if(!GLProfile.disableOpenGLES) {
        try {
            tmp = (GLDrawableFactory) ReflectionUtil.createInstance("jogamp.opengl.egl.EGLDrawableFactory", cl);
        } catch (final Exception jre) {
            if (DEBUG || GLProfile.DEBUG) {
                System.err.println("Info: GLDrawableFactory.static - EGLDrawableFactory - not available");
                jre.printStackTrace();
            }
        }
        if(null != tmp && tmp.isComplete()) {
            eglFactory = tmp;
        }
    } else if( DEBUG || GLProfile.DEBUG ) {
        System.err.println("Info: GLDrawableFactory.static - EGLDrawableFactory - disabled!");
    }
  }

  protected static void shutdown() {
    if (isInit) { // volatile: ok
      synchronized (GLDrawableFactory.class) {
          if (isInit) {
              isInit=false;
              shutdown0();
          }
      }
    }
  }

  private static void shutdown0() {
    // Following code will _always_ remain in shutdown hook
    // due to special semantics of native utils, i.e. X11Utils.
    // The latter requires shutdown at JVM-Shutdown only.
    synchronized(glDrawableFactories) {
        final int gldfCount = glDrawableFactories.size();
        if( DEBUG ) {
            System.err.println("GLDrawableFactory.shutdownAll "+gldfCount+" instances, on thread "+getThreadName());
        }
        for(int i=0; i<gldfCount; i++) {
            final GLDrawableFactory gldf = glDrawableFactories.get(i);
            if( DEBUG ) {
                System.err.println("GLDrawableFactory.shutdownAll["+(i+1)+"/"+gldfCount+"]:  "+gldf.getClass().getName());
            }
            try {
                gldf.resetAllDisplayGammaNoSync();
                gldf.shutdownImpl();
            } catch (final Throwable t) {
                System.err.println("GLDrawableFactory.shutdownImpl: Caught "+t.getClass().getName()+" during factory shutdown #"+(i+1)+"/"+gldfCount+" "+gldf.getClass().getName());
                if( DEBUG ) {
                    t.printStackTrace();
                }
            }
        }
        glDrawableFactories.clear();

        // both were members of glDrawableFactories and are shutdown already
        nativeOSFactory = null;
        eglFactory = null;
    }
    GLContext.shutdown();
    if( DEBUG ) {
        System.err.println("GLDrawableFactory.shutdownAll.X on thread "+getThreadName());
    }
  }

  protected GLDrawableFactory() {
    synchronized(glDrawableFactories) {
        glDrawableFactories.add(this);
    }
  }

  protected static String getThreadName() { return Thread.currentThread().getName(); }

  /** Returns true if this factory is complete, i.e. ready to be used. Otherwise return false. */
  protected abstract boolean isComplete();

  protected void enterThreadCriticalZone() {};
  protected void leaveThreadCriticalZone() {};

  protected abstract void shutdownImpl();

  /**
   * Sets the gamma, brightness, and contrast of the display associated with the given <code>surface</code>.
   * <p>
   * This functionality is not available on all platforms and
   * graphics hardware. Returns true if the settings were successfully
   * changed, false if not. This method may return false for some
   * values of the incoming arguments even on hardware which does
   * support the underlying functionality. </p>
   * <p>
   * If this method returns true, the display settings will
   * automatically be reset to their original values upon JVM exit
   * (assuming the JVM does not crash); if the user wishes to change
   * the display settings back to normal ahead of time,
   * use {@link #resetDisplayGamma(NativeSurface)} or {@link #resetAllDisplayGamma()}.
   * </p>
   * <p>
   * It is recommended to call {@link #resetDisplayGamma(NativeSurface)} or {@link #resetAllDisplayGamma()}
   * before calling e.g. <code>System.exit()</code> from the application rather than
   * rely on the shutdown hook functionality due to inevitable race
   * conditions and unspecified behavior during JVM teardown.
   * </p>
   * <p>
   * This method may be called multiple times during the application's
   * execution, but calling {@link #resetDisplayGamma(NativeSurface)}
   * will only reset the settings to the values
   * before the first call to this method. </p>
   *
   * @param surface denominates the display device
   * @param gamma The gamma value, typically > 1.0 (default values vary, but typically roughly 1.0)
   * @param brightness The brightness value between -1.0 and 1.0, inclusive (default values vary, but typically 0)
   * @param contrast The contrast, greater than 0.0 (default values vary, but typically 1)
   *
   * @return true if gamma settings were successfully changed, false if not
   * @throws IllegalArgumentException if any of the parameters were out-of-bounds
   * @see #resetDisplayGamma(NativeSurface)
   * @see #resetAllDisplayGamma()
   */
  public abstract boolean setDisplayGamma(final NativeSurface surface, final float gamma, final float brightness, final float contrast) throws IllegalArgumentException;

  /**
   * Resets the gamma, brightness and contrast values of the display associated with the given <code>surface</code>
   * to its original values before {@link #setDisplayGamma(NativeSurface, float, float, float) setDisplayGamma}
   * was called the first time.
   * <p>
   * While it is not explicitly required that this method be called before
   * exiting manually, calling it is recommended because of the inevitable
   * unspecified behavior during JVM teardown.
   * </p>
   */
  public abstract void resetDisplayGamma(final NativeSurface surface);

  /**
   * Resets the gamma, brightness and contrast values of all modified
   * displays to their original values before {@link #setDisplayGamma(NativeSurface, float, float, float) setDisplayGamma}
   * was called the first time.
   * <p>
   * While it is not explicitly required that this method be called before
   * exiting manually, calling it is recommended because of the inevitable
   * unspecified behavior during JVM teardown.
   * </p>
   */
  public abstract void resetAllDisplayGamma();

  protected abstract void resetAllDisplayGammaNoSync();

  /**
   * Retrieve the default <code>device</code> {@link AbstractGraphicsDevice#getConnection() connection},
   * {@link AbstractGraphicsDevice#getUnitID() unit ID} and {@link AbstractGraphicsDevice#getUniqueID() unique ID name}. for this factory<br>
   * The implementation must return a non <code>null</code> default device, which must not be opened, ie. it's native handle is <code>null</code>.
   * <p>
   * This method shall return the default device if available
   * even if the GLDrawableFactory is not functional and hence not compatible.
   * The latter situation may happen because no native OpenGL implementation is available for the specific implementation.
   * </p>
   * @return the default shared device for this factory, eg. :0.0 on X11 desktop.
   * @see #getIsDeviceCompatible(AbstractGraphicsDevice)
   */
  public abstract AbstractGraphicsDevice getDefaultDevice();

  /**
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @return true if the device is compatible with this factory, ie. if it can be used for GLDrawable creation. Otherwise false.
   *         This implies validation whether the implementation is functional.
   *
   * @see #getDefaultDevice()
   */
  public abstract boolean getIsDeviceCompatible(AbstractGraphicsDevice device);

  protected final AbstractGraphicsDevice validateDevice(AbstractGraphicsDevice device) {
      if(null==device) {
          device = getDefaultDevice();
          if(null==device) {
              throw new InternalError("no default device available");
          }
          if (GLProfile.DEBUG) {
              System.err.println("Info: "+getClass().getSimpleName()+".validateDevice: using default device : "+device);
          }
      }

      // Always validate the device,
      // since even the default device may not be used by this factory.
      if( !getIsDeviceCompatible(device) ) {
          if (GLProfile.DEBUG) {
              System.err.println("Info: "+getClass().getSimpleName()+".validateDevice: device not compatible : "+device);
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
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @return true if a shared resource could been created, otherwise false.
   */
  protected final boolean createSharedResource(final AbstractGraphicsDevice device) {
      return createSharedResourceImpl(device);
  }
  protected abstract boolean createSharedResourceImpl(AbstractGraphicsDevice device);

  /**
   * Returns true if the <code>quirk</code> exist in the shared resource's context {@link GLRendererQuirks}.
   * <p>
   * Convenience method for:
   * <pre>
      final GLRendererQuirks glrq = factory.getRendererQuirks(device);
      return null != glrq ? glrq.exist(quirk) : false;
   * </pre>
   * </p>
   *
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @param glp {@link GLProfile} to identify the device's {@link GLRendererQuirks}, maybe {@code null}
   * @param quirk the quirk to be tested, e.g. {@link GLRendererQuirks#NoDoubleBufferedPBuffer}.
   * @throws IllegalArgumentException if the quirk is out of range
   * @see #getRendererQuirks(AbstractGraphicsDevice, GLProfile)
   * @see GLRendererQuirks
   */
  public final boolean hasRendererQuirk(final AbstractGraphicsDevice device, final GLProfile glp, final int quirk) {
      final GLRendererQuirks glrq = getRendererQuirks(device, glp);
      return null != glrq ? glrq.exist(quirk) : false;
  }

  /**
   * Returns the shared resource's context {@link GLRendererQuirks}.
   * <p>
   * Implementation calls {@link GLContext#getRendererQuirks()} on the shared resource context.
   * </p>
   * <p>
   * In case no shared device exist yet or the implementation doesn't support tracking quirks,
   * the result is always <code>null</code>.
   * </p>
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @param glp {@link GLProfile} to identify the device's {@link GLRendererQuirks}, maybe {@code null}
   * @see GLContext#getRendererQuirks()
   * @see GLRendererQuirks
   */
  public abstract GLRendererQuirks getRendererQuirks(AbstractGraphicsDevice device, final GLProfile glp);

  /**
   * Method returns {@code true} if underlying implementation may support native desktop OpenGL,
   * otherwise {@code false}.
   */
  public abstract boolean hasOpenGLDesktopSupport();

  /**
   * Method returns {@code true} if underlying implementation may support native embedded OpenGL ES,
   * otherwise {@code false}.
   */
  public abstract boolean hasOpenGLESSupport();

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
  public static GLDrawableFactory getFactory(final GLProfile glProfile) throws GLException {
    return getFactoryImpl(glProfile.getImplName());
  }

  protected static GLDrawableFactory getFactoryImpl(final String glProfileImplName) throws GLException {
    if ( GLProfile.usesNativeGLES(glProfileImplName) ) {
        if(null!=eglFactory) {
            return eglFactory;
        }
    } else if(null!=nativeOSFactory) {
        return nativeOSFactory;
    }
    throw new GLException("No GLDrawableFactory available for profile: "+glProfileImplName);
  }

  protected static GLDrawableFactory getFactoryImpl(final AbstractGraphicsDevice device) throws GLException {
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
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @return A list of {@link com.jogamp.opengl.GLCapabilitiesImmutable}'s, maybe empty if none is available.
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
   * Returns an {@link GLDrawable#isRealized() unrealized} GLDrawable according to it's chosen {@link GLCapabilitiesImmutable},<br>
   * which determines pixel format, on- and offscreen incl. PBuffer type.
   * <p>
   * The chosen {@link GLCapabilitiesImmutable} are referenced within the target
   * {@link NativeSurface}'s {@link AbstractGraphicsConfiguration}.<p>
   * </p>
   * <p>
   * An onscreen GLDrawable is created if {@link CapabilitiesImmutable#isOnscreen() caps.isOnscreen()} is true.
   * </p>
   * <p>
   * A FBO drawable is created if both {@link GLCapabilitiesImmutable#isFBO() caps.isFBO()}
   * and {@link GLContext#isFBOAvailable(AbstractGraphicsDevice, GLProfile) canCreateFBO(device, caps.getGLProfile())} is true.
   * </p>
   * <p>
   * A Pbuffer drawable is created if both {@link GLCapabilitiesImmutable#isPBuffer() caps.isPBuffer()}
   * and {@link #canCreateGLPbuffer(AbstractGraphicsDevice, GLProfile) canCreateGLPbuffer(device)} is true.
   * </p>
   * <p>
   * If not onscreen and neither FBO nor Pbuffer is available,
   * a simple pixmap/bitmap drawable/surface is created, which is unlikely to be hardware accelerated.
   * </p>
   *
   * @throws IllegalArgumentException if the passed target is null
   * @throws GLException if any window system-specific errors caused
   *         the creation of the GLDrawable to fail.
   *
   * @see #canCreateGLPbuffer(AbstractGraphicsDevice, GLProfile)
   * @see GLContext#isFBOAvailable(AbstractGraphicsDevice, GLProfile)
   * @see com.jogamp.opengl.GLCapabilities#isOnscreen()
   * @see com.jogamp.opengl.GLCapabilities#isFBO()
   * @see com.jogamp.opengl.GLCapabilities#isPBuffer()
   * @see GraphicsConfigurationFactory#chooseGraphicsConfiguration(CapabilitiesImmutable, CapabilitiesImmutable, CapabilitiesChooser, AbstractGraphicsScreen, int)
   */
  public abstract GLDrawable createGLDrawable(NativeSurface target)
    throws IllegalArgumentException, GLException;

  /**
   * Creates a {@link GLDrawable#isRealized() realized} {@link GLOffscreenAutoDrawable}
   * incl it's offscreen {@link NativeSurface} with the given capabilites and dimensions.
   * <p>
   * The {@link GLOffscreenAutoDrawable}'s {@link GLDrawable} is {@link GLDrawable#isRealized() realized}
   * <i>without</i> an assigned {@link GLContext}, hence not initialized completely.<br>
   *
   * The {@link GLContext} can be assigned later manually via {@link GLAutoDrawable#setContext(GLContext, boolean) setContext(ctx)}
   * <i>or</i> it will be created <i>lazily</i> at the 1st {@link GLAutoDrawable#display() display()} method call.<br>
   *
   * <i>Lazy</i> {@link GLContext} creation will take a shared {@link GLContext} into account
   * which has been set {@link GLOffscreenAutoDrawable#setSharedContext(GLContext) directly}
   * or {@link GLOffscreenAutoDrawable#setSharedAutoDrawable(GLAutoDrawable) via another GLAutoDrawable}.
   * </p>
   * <p>
   * In case the passed {@link GLCapabilitiesImmutable} contains default values, i.e.
   * {@link GLCapabilitiesImmutable#isOnscreen() caps.isOnscreen()} <code> == true</code>,
   * it is auto-configured. Auto configuration will set {@link GLCapabilitiesImmutable caps} to offscreen
   * and FBO <i>or</i> Pbuffer, whichever is available in that order.
   * </p>
   * <p>
   * A FBO based auto drawable, {@link GLOffscreenAutoDrawable.FBO}, is created if both {@link GLCapabilitiesImmutable#isFBO() caps.isFBO()}
   * and {@link GLContext#isFBOAvailable(AbstractGraphicsDevice, GLProfile) canCreateFBO(device, caps.getGLProfile())} is true.
   * </p>
   * <p>
   * A Pbuffer based auto drawable is created if both {@link GLCapabilitiesImmutable#isPBuffer() caps.isPBuffer()}
   * and {@link #canCreateGLPbuffer(AbstractGraphicsDevice, GLProfile) canCreateGLPbuffer(device)} is true.
   * </p>
   * <p>
   * If neither FBO nor Pbuffer is available,
   * a simple pixmap/bitmap auto drawable is created, which is unlikely to be hardware accelerated.
   * </p>
   * <p>
   * The resulting {@link GLOffscreenAutoDrawable} has it's own independent device instance using <code>device</code> details.
   * </p>
   *
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared device to be used, may be <code>null</code> for the platform's default device.
   * @param caps the requested GLCapabilties
   * @param chooser the custom chooser, may be null for default
   * @param width the requested offscreen width
   * @param height the requested offscreen height
   * @return the created and realized offscreen {@link GLOffscreenAutoDrawable} instance
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the Offscreen to fail.
   *
   * @see #createOffscreenDrawable(AbstractGraphicsDevice, GLCapabilitiesImmutable, GLCapabilitiesChooser, int, int)
   */
  public abstract GLOffscreenAutoDrawable createOffscreenAutoDrawable(AbstractGraphicsDevice device,
                                                                      GLCapabilitiesImmutable caps,
                                                                      GLCapabilitiesChooser chooser,
                                                                      int width, int height) throws GLException;

  /**
   * Creates a {@link GLDrawable#isRealized() realized} <i>dummy</i> {@link GLAutoDrawable}
   * incl it's <i>dummy, invisible</i> {@link NativeSurface}
   * as created with {@link #createDummyDrawable(AbstractGraphicsDevice, boolean, GLCapabilitiesImmutable, GLCapabilitiesChooser)}.
   * <p>
   * The <i>dummy</i> {@link GLAutoDrawable}'s {@link GLDrawable} is {@link GLDrawable#isRealized() realized}
   * <i>without</i> an assigned {@link GLContext}, hence not initialized completely.<br>
   * The {@link GLContext} can be assigned later manually via {@link GLAutoDrawable#setContext(GLContext, boolean) setContext(ctx)}
   * <i>or</i> it will be created <i>lazily</i> at the 1st {@link GLAutoDrawable#display() display()} method call.<br>
   * <i>Lazy</i> {@link GLContext} creation will take a shared {@link GLContext} into account
   * which has been set {@link GLOffscreenAutoDrawable#setSharedContext(GLContext) directly}
   * or {@link GLOffscreenAutoDrawable#setSharedAutoDrawable(GLAutoDrawable) via another GLAutoDrawable}.
   * </p>
   *
   * @param deviceReq which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared device to be used, may be <code>null</code> for the platform's default device.
   * @param createNewDevice if <code>true</code> a new independent device instance is created from the <code>deviceReq</code>, otherwise <code>deviceReq</code> is used as-is and must be valid!
   * @param capsRequested the desired {@link GLCapabilitiesImmutable}, incl. it's {@link GLProfile}.
   *                      For shared context, same {@link GLCapabilitiesImmutable#getVisualID(com.jogamp.nativewindow.VisualIDHolder.VIDType)}
   *                      across shared drawables will yield best compatibility.
   * @param chooser the custom chooser, may be null for default
   * @return the created and realized <i>dummy</i> {@link GLAutoDrawable} instance
   *
   * @see #createDummyDrawable(AbstractGraphicsDevice, boolean, GLCapabilitiesImmutable, GLCapabilitiesChooser)
   */
  public abstract GLAutoDrawable createDummyAutoDrawable(AbstractGraphicsDevice deviceReq, boolean createNewDevice, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser);

  /**
   * Creates an {@link GLDrawable#isRealized() unrealized} offscreen {@link GLDrawable}
   * incl it's offscreen {@link NativeSurface} with the given capabilites and dimensions.
   * <p>
   * In case the passed {@link GLCapabilitiesImmutable} contains default values, i.e.
   * {@link GLCapabilitiesImmutable#isOnscreen() caps.isOnscreen()} <code> == true</code>,
   * it is auto-configured. The latter will set offscreen and also FBO <i>or</i> Pbuffer, whichever is available in that order.
   * </p>
   * <p>
   * A resizeable FBO drawable, {@link GLFBODrawable.Resizeable}, is created if both {@link GLCapabilitiesImmutable#isFBO() caps.isFBO()}
   * and {@link GLContext#isFBOAvailable(AbstractGraphicsDevice, GLProfile) canCreateFBO(device, caps.getGLProfile())} is true.
   * </p>
   * <p>
   * A Pbuffer drawable is created if both {@link GLCapabilitiesImmutable#isPBuffer() caps.isPBuffer()}
   * and {@link #canCreateGLPbuffer(AbstractGraphicsDevice, GLProfile) canCreateGLPbuffer(device)} is true.
   * </p>
   * <p>
   * If neither FBO nor Pbuffer is available,
   * a simple pixmap/bitmap drawable is created, which is unlikely to be hardware accelerated.
   * </p>
   * <p>
   * The resulting {@link GLDrawable} has it's own independent device instance using <code>device</code> details.
   * </p>
   *
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared device to be used, may be <code>null</code> for the platform's default device.
   * @param caps the requested GLCapabilties
   * @param chooser the custom chooser, may be null for default
   * @param width the requested offscreen width
   * @param height the requested offscreen height
   *
   * @return the created unrealized offscreen {@link GLDrawable}
   *
   * @throws GLException if any window system-specific errors caused
   *         the creation of the Offscreen to fail.
   *
   * @see #createOffscreenAutoDrawable(AbstractGraphicsDevice, GLCapabilitiesImmutable, GLCapabilitiesChooser, int, int, GLContext)
   */
  public abstract GLDrawable createOffscreenDrawable(AbstractGraphicsDevice device,
                                                     GLCapabilitiesImmutable caps,
                                                     GLCapabilitiesChooser chooser,
                                                     int width, int height) throws GLException;

  /**
   * Creates an {@link GLDrawable#isRealized() unrealized} dummy {@link GLDrawable}.
   * A dummy drawable is not visible on screen and will not be used to render directly to, it maybe on- or offscreen.
   * <p>
   * It is used to allow the creation of a {@link GLContext} to query information.
   * It also allows creation of framebuffer objects which are used for rendering or creating a shared GLContext w/o actually rendering to this dummy drawable's framebuffer.
   * </p>
   * @param deviceReq which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared device to be used, may be <code>null</code> for the platform's default device.
   * @param createNewDevice if <code>true</code> a new independent device instance is created from the <code>deviceReq</code>, otherwise <code>deviceReq</code> is used as-is and must be valid!
   * @param capsRequested the desired {@link GLCapabilitiesImmutable}, incl. it's {@link GLProfile}.
   *                      For shared context, same {@link GLCapabilitiesImmutable#getVisualID(com.jogamp.nativewindow.VisualIDHolder.VIDType) visual ID}
   *                      or {@link GLCapabilitiesImmutable caps}
   *                      across shared drawables will yield best compatibility.
   * @param chooser the custom chooser, may be null for default
   * @return the created unrealized dummy {@link GLDrawable}
   */
  public abstract GLDrawable createDummyDrawable(AbstractGraphicsDevice deviceReq, boolean createNewDevice, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser);

  /**
   * Creates a proxy {@link NativeSurface} w/ defined surface handle,
   * i.e. a {@link jogamp.nativewindow.WrappedSurface} or {@link jogamp.nativewindow.windows.GDISurface} instance.
   * <p>
   * It's {@link AbstractGraphicsConfiguration} is properly set according to the given
   * <code>windowHandle</code>'s native visualID if set or the given {@link GLCapabilitiesImmutable}.
   * </p>
   * <p>
   * Lifecycle (creation and destruction) of the given surface handle shall be handled by the caller
   * via {@link ProxySurface#createNotify()} and {@link ProxySurface#destroyNotify()}.
   * </p>
   * <p>
   * Such surface can be used to instantiate a GLDrawable. With the help of {@link GLAutoDrawableDelegate}
   * you will be able to implement a new native windowing system  binding almost on-the-fly,
   * see {@link com.jogamp.opengl.swt.GLCanvas}.
   * </p>
   * <p>
   * The resulting {@link GLOffscreenAutoDrawable} has it's own independent device instance using <code>device</code> details
   * which may be blocking depending on platform and windowing-toolkit requirements.
   * </p>
   *
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   *        Caller has to ensure it is compatible w/ the given <code>windowHandle</code>
   * @param screenIdx matching screen index of given <code>windowHandle</code>
   * @param windowHandle the native window handle
   * @param caps the requested GLCapabilties
   * @param chooser the custom chooser, may be null for default
   * @param upstream optional {@link UpstreamSurfaceHook} allowing control of the {@link ProxySurface}'s lifecycle and data it presents.
   * @return the created {@link ProxySurface} instance w/ defined surface handle.
   */
  public abstract ProxySurface createProxySurface(AbstractGraphicsDevice device,
                                                  int screenIdx,
                                                  long windowHandle,
                                                  GLCapabilitiesImmutable caps, GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstream);

  /**
   * Returns true if it is possible to create an <i>framebuffer object</i> (FBO).
   * <p>
   * FBO feature is implemented in OpenGL, hence it is {@link GLProfile} dependent.
   * </p>
   * <p>
   * FBO support is queried as described in {@link GLContext#hasBasicFBOSupport()}.
   * </p>
   *
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @param glp {@link GLProfile} to check for FBO capabilities
   * @see GLContext#hasBasicFBOSupport()
   */
  public abstract boolean canCreateFBO(AbstractGraphicsDevice device, GLProfile glp);

  /**
   * Returns true if it is possible to create an <i>pbuffer surface</i>.
   * <p>
   * Some older graphics cards do not have this capability,
   * as well as some new GL implementation, i.e. OpenGL 3 core on OSX.
   * </p>
   *
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   * @param glp {@link GLProfile} to check for FBO capabilities
   */
  public abstract boolean canCreateGLPbuffer(AbstractGraphicsDevice device, GLProfile glp);

  //----------------------------------------------------------------------
  // Methods for interacting with third-party OpenGL libraries

  /**
   * <P> Creates a GLContext object representing an existing OpenGL
   * context in an external (third-party) OpenGL-based library. This
   * GLContext object may be used to draw into this preexisting
   * context using its {@link GL} and {@link
   * com.jogamp.opengl.glu.GLU} objects. New contexts created through
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
   * @param device which {@link AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
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
