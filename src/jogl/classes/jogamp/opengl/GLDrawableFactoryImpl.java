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

package jogamp.opengl;

import java.nio.Buffer;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.OffscreenLayerSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.MutableSurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLFBODrawable;
import javax.media.opengl.GLOffscreenAutoDrawable;
import javax.media.opengl.GLProfile;

import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.nativewindow.DelegatedUpstreamSurfaceHookWithSurfaceSize;
import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;
import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.GLRendererQuirks;


/** Extends GLDrawableFactory with a few methods for handling
    typically software-accelerated offscreen rendering (Device
    Independent Bitmaps on Windows, pixmaps on X11). Direct access to
    these GLDrawables is not supplied directly to end users, though
    they may be instantiated by the GLJPanel implementation. */
public abstract class GLDrawableFactoryImpl extends GLDrawableFactory {
  protected static final boolean DEBUG = GLDrawableFactory.DEBUG; // allow package access

  protected GLDrawableFactoryImpl() {
    super();
  }

  /**
   * Returns the shared resource mapped to the <code>device</code> {@link AbstractGraphicsDevice#getConnection()},
   * either a pre-existing or newly created, or <code>null</code> if creation failed or not supported.<br>
   * Creation of the shared resource is tried only once.
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   */
  protected final SharedResourceRunner.Resource getOrCreateSharedResource(AbstractGraphicsDevice device) {
      try {
          device = validateDevice(device);
          if( null != device) {
              return getOrCreateSharedResourceImpl( device );
          }
      } catch (final GLException gle) {
          if(DEBUG) {
              System.err.println("Caught exception on thread "+getThreadName());
              gle.printStackTrace();
          }
      }
      return null;
  }
  protected abstract SharedResourceRunner.Resource getOrCreateSharedResourceImpl(AbstractGraphicsDevice device);

  /**
   * Returns the shared context mapped to the <code>device</code> {@link AbstractGraphicsDevice#getConnection()},
   * either a pre-existing or newly created, or <code>null</code> if creation failed or <b>not supported</b>.<br>
   * Creation of the shared context is tried only once.
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   */
  public final GLContext getOrCreateSharedContext(final AbstractGraphicsDevice device) {
      final SharedResourceRunner.Resource sr = getOrCreateSharedResource( device );
      if(null!=sr) {
        return sr.getContext();
      }
      return null;
  }

  @Override
  protected final boolean createSharedResourceImpl(final AbstractGraphicsDevice device) {
      final SharedResourceRunner.Resource sr = getOrCreateSharedResource( device );
      if(null!=sr) {
          return sr.isValid();
      }
      return false;
  }

  @Override
  public final GLRendererQuirks getRendererQuirks(final AbstractGraphicsDevice device) {
      final SharedResourceRunner.Resource sr = getOrCreateSharedResource( device );
      if(null!=sr) {
          return sr.getRendererQuirks();
      }
      return null;
  }

  /**
   * Returns the shared device mapped to the <code>device</code> {@link AbstractGraphicsDevice#getConnection()},
   * either a preexisting or newly created, or <code>null</code> if creation failed or not supported.<br>
   * Creation of the shared context is tried only once.
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared device to be used, may be <code>null</code> for the platform's default device.
   */
  protected final AbstractGraphicsDevice getOrCreateSharedDevice(final AbstractGraphicsDevice device) {
      final SharedResourceRunner.Resource sr = getOrCreateSharedResource( device );
      if(null!=sr) {
        return sr.getDevice();
      }
      return null;
  }

  /**
   * Returns the GLDynamicLookupHelper
   * @param profile if EGL/ES, profile <code>1</code> refers to ES1 and <code>2</code> to ES2,
   *        otherwise the profile is ignored.
   */
  public abstract GLDynamicLookupHelper getGLDynamicLookupHelper(int profile);

  //---------------------------------------------------------------------------
  // Dispatching GLDrawable construction in respect to the NativeSurface Capabilities
  //
  @Override
  public final GLDrawable createGLDrawable(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    final MutableGraphicsConfiguration config = (MutableGraphicsConfiguration) target.getGraphicsConfiguration();
    final GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    final AbstractGraphicsDevice adevice = config.getScreen().getDevice();
    final boolean isFBOAvailable = GLContext.isFBOAvailable(adevice, chosenCaps.getGLProfile());
    GLDrawable result = null;
    adevice.lock();
    try {
        final OffscreenLayerSurface ols = NativeWindowFactory.getOffscreenLayerSurface(target, true);
        if(null != ols) {
            final GLCapabilitiesImmutable chosenCapsMod = GLGraphicsConfigurationUtil.fixOffscreenGLCapabilities(chosenCaps, this, adevice);

            // layered surface -> Offscreen/[FBO|PBuffer]
            if( !chosenCapsMod.isFBO() && !chosenCapsMod.isPBuffer() ) {
                throw new GLException("Neither FBO nor Pbuffer is available for "+chosenCapsMod+", "+target);
            }
            config.setChosenCapabilities(chosenCapsMod);
            ols.setChosenCapabilities(chosenCapsMod);
            if(DEBUG) {
                System.err.println("GLDrawableFactoryImpl.createGLDrawable -> OnscreenDrawable -> Offscreen-Layer");
                System.err.println("chosenCaps:    "+chosenCaps);
                System.err.println("chosenCapsMod: "+chosenCapsMod);
                System.err.println("OffscreenLayerSurface: **** "+ols);
                System.err.println("Target: **** "+target);
                Thread.dumpStack();
            }
            if( ! ( target instanceof MutableSurface ) ) {
                throw new IllegalArgumentException("Passed NativeSurface must implement SurfaceChangeable for offscreen layered surface: "+target);
            }
            if( chosenCapsMod.isFBO() ) {
                result = createFBODrawableImpl(target, chosenCapsMod, 0);
            } else {
                result = createOffscreenDrawableImpl(target);
            }
        } else if(chosenCaps.isOnscreen()) {
            // onscreen
            final GLCapabilitiesImmutable chosenCapsMod = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
            config.setChosenCapabilities(chosenCapsMod);
            if(DEBUG) {
                System.err.println("GLDrawableFactoryImpl.createGLDrawable -> OnscreenDrawable: "+target);
            }
            result = createOnscreenDrawableImpl(target);
        } else {
            // offscreen
            if(DEBUG) {
                System.err.println("GLDrawableFactoryImpl.createGLDrawable -> OffScreenDrawable, FBO chosen / avail, PBuffer: "+
                                   chosenCaps.isFBO()+" / "+isFBOAvailable+", "+chosenCaps.isPBuffer()+": "+target);
            }
            if( ! ( target instanceof MutableSurface ) ) {
                throw new IllegalArgumentException("Passed NativeSurface must implement MutableSurface for offscreen: "+target);
            }
            if( chosenCaps.isFBO() && isFBOAvailable ) {
                // need to hook-up a native dummy surface since source may not have & use minimum GLCapabilities for it w/ same profile
                final ProxySurface dummySurface = createDummySurfaceImpl(adevice, false, new GLCapabilities(chosenCaps.getGLProfile()), (GLCapabilitiesImmutable)config.getRequestedCapabilities(), null, 64, 64);
                dummySurface.setUpstreamSurfaceHook(new DelegatedUpstreamSurfaceHookWithSurfaceSize(dummySurface.getUpstreamSurfaceHook(), target));
                result = createFBODrawableImpl(dummySurface, chosenCaps, 0);
            } else {
                result = createOffscreenDrawableImpl(target);
            }
        }
    } finally {
        adevice.unlock();
    }
    if(DEBUG) {
        System.err.println("GLDrawableFactoryImpl.createGLDrawable: "+result);
    }
    return result;
  }

  //---------------------------------------------------------------------------
  //
  // Onscreen GLDrawable construction
  //

  protected abstract GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target);

  //---------------------------------------------------------------------------
  //
  // PBuffer Offscreen GLAutoDrawable construction
  //

  @Override
  public abstract boolean canCreateGLPbuffer(AbstractGraphicsDevice device, GLProfile glp);

  //---------------------------------------------------------------------------
  //
  // Offscreen GLDrawable construction
  //

  @Override
  public final boolean canCreateFBO(final AbstractGraphicsDevice deviceReq, final GLProfile glp) {
    final AbstractGraphicsDevice device = getOrCreateSharedDevice(deviceReq);
    if(null == device) {
        throw new GLException("No shared device for requested: "+deviceReq);
    }
    return GLContext.isFBOAvailable(device, glp);
  }

  @Override
  public final GLOffscreenAutoDrawable createOffscreenAutoDrawable(final AbstractGraphicsDevice deviceReq,
                                                             final GLCapabilitiesImmutable capsRequested,
                                                             final GLCapabilitiesChooser chooser,
                                                             final int width, final int height) {
    final GLDrawable drawable = createOffscreenDrawable( deviceReq, capsRequested, chooser, width, height );
    drawable.setRealized(true);
    if(drawable instanceof GLFBODrawableImpl) {
        return new GLOffscreenAutoDrawableImpl.FBOImpl( (GLFBODrawableImpl)drawable, null, null, null );
    }
    return new GLOffscreenAutoDrawableImpl( drawable, null, null, null);
  }

  @Override
  public final GLAutoDrawable createDummyAutoDrawable(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice, final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser) {
      final GLDrawable drawable = createDummyDrawable(deviceReq, createNewDevice, capsRequested, chooser);
      drawable.setRealized(true);
      final GLAutoDrawable sharedDrawable = new GLAutoDrawableDelegate(drawable, null, null, true /*ownDevice*/, null) { };
      return sharedDrawable;
  }

  @Override
  public final GLDrawable createOffscreenDrawable(final AbstractGraphicsDevice deviceReq,
                                            final GLCapabilitiesImmutable capsRequested,
                                            final GLCapabilitiesChooser chooser,
                                            final int width, final int height) {
    if(width<=0 || height<=0) {
        throw new GLException("initial size must be positive (were (" + width + " x " + height + "))");
    }
    final AbstractGraphicsDevice device = getOrCreateSharedDevice(deviceReq);
    if(null == device) {
        throw new GLException("No shared device for requested: "+deviceReq);
    }

    final GLCapabilitiesImmutable capsChosen = GLGraphicsConfigurationUtil.fixOffscreenGLCapabilities(capsRequested, this, device);

    if( capsChosen.isFBO() ) {
        // Use minimum GLCapabilities for the dummy surface w/ same profile
        final ProxySurface dummySurface = createDummySurfaceImpl(device, true, new GLCapabilities(capsChosen.getGLProfile()), capsRequested, null, width, height);
        final GLDrawableImpl dummyDrawable = createOnscreenDrawableImpl(dummySurface);
        return new GLFBODrawableImpl.ResizeableImpl(this, dummyDrawable, dummySurface, capsChosen, 0);
    }
    return createOffscreenDrawableImpl( createMutableSurfaceImpl(device, true, capsChosen, capsRequested, chooser,
                                                                 new UpstreamSurfaceHookMutableSize(width, height) ) );
  }

  @Override
  public final GLDrawable createDummyDrawable(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice, final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser) {
    final AbstractGraphicsDevice device = createNewDevice ? getOrCreateSharedDevice(deviceReq) : deviceReq;
    if(null == device) {
        throw new GLException("No shared device for requested: "+deviceReq+", createNewDevice "+createNewDevice);
    }
    if( !createNewDevice ) {
        device.lock();
    }
    try {
        final ProxySurface dummySurface = createDummySurfaceImpl(device, createNewDevice, capsRequested, capsRequested, chooser, 64, 64);
        return createOnscreenDrawableImpl(dummySurface);
    } finally {
        if( !createNewDevice ) {
            device.unlock();
        }
    }
  }

  /** Creates a platform independent unrealized FBO offscreen GLDrawable */
  protected final GLFBODrawable createFBODrawableImpl(final NativeSurface dummySurface, final GLCapabilitiesImmutable fboCaps, final int textureUnit) {
    final GLDrawableImpl dummyDrawable = createOnscreenDrawableImpl(dummySurface);
    return new GLFBODrawableImpl(this, dummyDrawable, dummySurface, fboCaps, textureUnit);
  }

  /** Creates a platform dependent unrealized offscreen pbuffer/pixmap GLDrawable instance */
  protected abstract GLDrawableImpl createOffscreenDrawableImpl(NativeSurface target) ;

  /**
   * Creates a mutable {@link ProxySurface} w/o defined surface handle.
   * <p>
   * It's {@link AbstractGraphicsConfiguration} is properly set according to the given {@link GLCapabilitiesImmutable}.
   * </p>
   * <p>
   * Lifecycle (destruction) of the TBD surface handle shall be handled by the caller.
   * </p>
   * @param device a valid platform dependent target device.
   * @param createNewDevice if <code>true</code> a new independent device instance is created using <code>device</code> details,
   *                        otherwise <code>device</code> instance is used as-is.
   * @param capsChosen
   * @param capsRequested
   * @param chooser the custom chooser, may be null for default
   * @param upstreamHook surface size information and optional control of the surface's lifecycle
   * @return the created {@link MutableSurface} instance w/o defined surface handle
   */
  protected abstract ProxySurface createMutableSurfaceImpl(AbstractGraphicsDevice device, boolean createNewDevice,
                                                           GLCapabilitiesImmutable capsChosen,
                                                           GLCapabilitiesImmutable capsRequested,
                                                           GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstreamHook);

  /**
   * A dummy surface is not visible on screen and will not be used to render directly to,
   * it maybe on- or offscreen.
   * <p>
   * It is used to allow the creation of a {@link GLDrawable} and {@link GLContext} to query information.
   * It also allows creation of framebuffer objects which are used for rendering or using a shared GLContext w/o actually rendering to a usable framebuffer.
   * </p>
   * <p>
   * Creates a new independent device instance using <code>deviceReq</code> details.
   * </p>
   * @param deviceReq which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared device to be used, may be <code>null</code> for the platform's default device.
   * @param requestedCaps
   * @param chooser the custom chooser, may be null for default
   * @param width the initial width as returned by {@link NativeSurface#getSurfaceWidth()}, not the actual dummy surface width.
   *        The latter is platform specific and small
   * @param height the initial height as returned by {@link NativeSurface#getSurfaceHeight()}, not the actual dummy surface height,
   *        The latter is platform specific and small
   *
   * @return the created {@link ProxySurface} instance w/o defined surface handle but platform specific {@link UpstreamSurfaceHook}.
   */
  public final ProxySurface createDummySurface(final AbstractGraphicsDevice deviceReq, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser,
                                          final int width, final int height) {
    final AbstractGraphicsDevice device = getOrCreateSharedDevice(deviceReq);
    if(null == device) {
        throw new GLException("No shared device for requested: "+deviceReq);
    }
    return createDummySurfaceImpl(device, true, requestedCaps, requestedCaps, chooser, width, height);
  }

  /**
   * A dummy surface is not visible on screen and will not be used to render directly to,
   * it maybe on- or offscreen.
   * <p>
   * It is used to allow the creation of a {@link GLDrawable} and {@link GLContext} to query information.
   * It also allows creation of framebuffer objects which are used for rendering or using a shared GLContext w/o actually rendering to a usable framebuffer.
   * </p>
   * @param device a valid platform dependent target device.
   * @param createNewDevice if <code>true</code> a new device instance is created using <code>device</code> details,
   *                        otherwise <code>device</code> instance is used as-is.
   * @param chosenCaps
   * @param requestedCaps
   * @param chooser the custom chooser, may be null for default
   * @param width the initial width as returned by {@link NativeSurface#getSurfaceWidth()}, not the actual dummy surface width.
   *        The latter is platform specific and small
   * @param height the initial height as returned by {@link NativeSurface#getSurfaceHeight()}, not the actual dummy surface height,
   *        The latter is platform specific and small
   * @return the created {@link ProxySurface} instance w/o defined surface handle but platform specific {@link UpstreamSurfaceHook}.
   */
  public abstract ProxySurface createDummySurfaceImpl(AbstractGraphicsDevice device, boolean createNewDevice,
                                                      GLCapabilitiesImmutable chosenCaps, GLCapabilitiesImmutable requestedCaps, GLCapabilitiesChooser chooser, int width, int height);

  //---------------------------------------------------------------------------
  //
  // ProxySurface (Wrapped pre-existing native surface) construction
  //

  @Override
  public ProxySurface createProxySurface(final AbstractGraphicsDevice deviceReq, final int screenIdx, final long windowHandle,
                                         final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstream) {
    final AbstractGraphicsDevice device = getOrCreateSharedDevice(deviceReq);
    if(null == device) {
        throw new GLException("No shared device for requested: "+deviceReq);
    }
    if(0 == windowHandle) {
        throw new IllegalArgumentException("Null windowHandle");
    }

    device.lock();
    try {
        return createProxySurfaceImpl(device, screenIdx, windowHandle, capsRequested, chooser, upstream);
    } finally {
        device.unlock();
    }
  }

  /**
   * Creates a {@link ProxySurface} with a set surface handle.
   * <p>
   * Implementation is also required to allocate it's own {@link AbstractGraphicsDevice} instance.
   * </p>
 * @param upstream TODO
   */
  protected abstract ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice deviceReq, int screenIdx, long windowHandle,
                                                         GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstream);

  //---------------------------------------------------------------------------
  //
  // External GLDrawable construction
  //

  protected abstract GLContext createExternalGLContextImpl();

  @Override
  public GLContext createExternalGLContext() {
    return createExternalGLContextImpl();
  }

  protected abstract GLDrawable createExternalGLDrawableImpl();

  @Override
  public GLDrawable createExternalGLDrawable() {
    return createExternalGLDrawableImpl();
  }


  //---------------------------------------------------------------------------
  //
  // GLDrawableFactoryImpl details
  //

  /**
   * Returns the sole GLDrawableFactoryImpl instance.
   *
   * @param glProfile GLProfile to determine the factory type, ie EGLDrawableFactory,
   *                or one of the native GLDrawableFactory's, ie X11/GLX, Windows/WGL or MacOSX/CGL.
   */
  public static GLDrawableFactoryImpl getFactoryImpl(final GLProfile glp) {
    return (GLDrawableFactoryImpl) getFactory(glp);
  }

  //----------------------------------------------------------------------
  // Gamma adjustment support
  // Thanks to the LWJGL team for illustrating how to make these
  // adjustments on various OSs.

  /*
   * Portions Copyright (c) 2002-2004 LWJGL Project
   * All rights reserved.
   *
   * Redistribution and use in source and binary forms, with or without
   * modification, are permitted provided that the following conditions are
   * met:
   *
   * * Redistributions of source code must retain the above copyright
   *   notice, this list of conditions and the following disclaimer.
   *
   * * Redistributions in binary form must reproduce the above copyright
   *   notice, this list of conditions and the following disclaimer in the
   *   documentation and/or other materials provided with the distribution.
   *
   * * Neither the name of 'LWJGL' nor the names of
   *   its contributors may be used to endorse or promote products derived
   *   from this software without specific prior written permission.
   *
   * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
   * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
   * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
   * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
   * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
   * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
   * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
   * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
   * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
   */

  /**
   * Sets the gamma, brightness, and contrast of the current main
   * display. Returns true if the settings were changed, false if
   * not. If this method returns true, the display settings will
   * automatically be reset upon JVM exit (assuming the JVM does not
   * crash); if the user wishes to change the display settings back to
   * normal ahead of time, use resetDisplayGamma(). Throws
   * IllegalArgumentException if any of the parameters were
   * out-of-bounds.
   *
   * @param gamma The gamma value, typically > 1.0 (default value is
   *   1.0)
   * @param brightness The brightness value between -1.0 and 1.0,
   *   inclusive (default value is 0)
   * @param contrast The contrast, greater than 0.0 (default value is 1)
   * @throws IllegalArgumentException if any of the parameters were
   *   out-of-bounds
   */
  public boolean setDisplayGamma(final float gamma, final float brightness, final float contrast) throws IllegalArgumentException {
    if ((brightness < -1.0f) || (brightness > 1.0f)) {
      throw new IllegalArgumentException("Brightness must be between -1.0 and 1.0");
    }
    if (contrast < 0) {
      throw new IllegalArgumentException("Contrast must be greater than 0.0");
    }
    // FIXME: ensure gamma is > 1.0? Are smaller / negative values legal?
    final int rampLength = getGammaRampLength();
    if (rampLength == 0) {
      return false;
    }
    final float[] gammaRamp = new float[rampLength];
    for (int i = 0; i < rampLength; i++) {
      final float intensity = (float) i / (float) (rampLength - 1);
      // apply gamma
      float rampEntry = (float) java.lang.Math.pow(intensity, gamma);
      // apply brightness
      rampEntry += brightness;
      // apply contrast
      rampEntry = (rampEntry - 0.5f) * contrast + 0.5f;
      // Clamp entry to [0, 1]
      if (rampEntry > 1.0f)
        rampEntry = 1.0f;
      else if (rampEntry < 0.0f)
        rampEntry = 0.0f;
      gammaRamp[i] = rampEntry;
    }
    if( !needsGammaRampReset ) {
        originalGammaRamp = getGammaRamp();
        needsGammaRampReset = true;
    }
    return setGammaRamp(gammaRamp);
  }

  @Override
  public synchronized void resetDisplayGamma() {
    if( needsGammaRampReset ) {
        resetGammaRamp(originalGammaRamp);
        needsGammaRampReset = false;
    }
  }

  //------------------------------------------------------
  // Gamma-related methods to be implemented by subclasses
  //

  /** Returns the length of the computed gamma ramp for this OS and
      hardware. Returns 0 if gamma changes are not supported. */
  protected int getGammaRampLength() {
    return 0;
  }

  /** Sets the gamma ramp for the main screen. Returns false if gamma
      ramp changes were not supported. */
  protected boolean setGammaRamp(final float[] ramp) {
    return false;
  }

  /** Gets the current gamma ramp. This is basically an opaque value
      used only on some platforms to reset the gamma ramp to its
      original settings. */
  protected Buffer getGammaRamp() {
    return null;
  }

  /** Resets the gamma ramp, potentially using the specified Buffer as
      data to restore the original values. */
  protected void resetGammaRamp(final Buffer originalGammaRamp) {
  }

  // Shutdown hook mechanism for resetting gamma
  private volatile Buffer originalGammaRamp;
  private volatile boolean needsGammaRampReset = false;
}
