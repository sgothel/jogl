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
import javax.media.nativewindow.SurfaceChangeable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.MutableGraphicsConfiguration;

/** Extends GLDrawableFactory with a few methods for handling
    typically software-accelerated offscreen rendering (Device
    Independent Bitmaps on Windows, pixmaps on X11). Direct access to
    these GLDrawables is not supplied directly to end users, though
    they may be instantiated by the GLJPanel implementation. */
public abstract class GLDrawableFactoryImpl extends GLDrawableFactory {
  protected static final boolean DEBUG = GLDrawableImpl.DEBUG;

  protected GLDrawableFactoryImpl() {
    super();
  }

  /**
   * Returns the shared context mapped to the <code>device</code> {@link AbstractGraphicsDevice#getConnection()},
   * either a pre-existing or newly created, or <code>null</code> if creation failed or not supported.<br>
   * Creation of the shared context is tried only once.
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared the target device, may be <code>null</code> for the platform's default device.
   */
  public final GLContext getOrCreateSharedContext(AbstractGraphicsDevice device) {
      device = validateDevice(device);
      if(null!=device) {
        return getOrCreateSharedContextImpl(device);
      }
      return null;
  }
  protected abstract GLContext getOrCreateSharedContextImpl(AbstractGraphicsDevice device);
  
  /**
   * Returns the shared device mapped to the <code>device</code> {@link AbstractGraphicsDevice#getConnection()},
   * either a preexisting or newly created, or <code>null</code> if creation failed or not supported.<br>
   * Creation of the shared context is tried only once.
   *
   * @param device which {@link javax.media.nativewindow.AbstractGraphicsDevice#getConnection() connection} denotes the shared device to be used, may be <code>null</code> for the platform's default device.
   */
  protected final AbstractGraphicsDevice getOrCreateSharedDevice(AbstractGraphicsDevice device) {
      if(null==device) {
          device = getDefaultDevice();
          if(null==device) {
              throw new InternalError("no default device");
          }
          if (GLProfile.DEBUG) {
              System.err.println("Info: GLDrawableFactoryImpl.getOrCreateSharedContext: using default device : "+device);
          }
      } else if( !getIsDeviceCompatible(device) ) {
          if (GLProfile.DEBUG) {
              System.err.println("Info: GLDrawableFactoryImpl.getOrCreateSharedContext: device not compatible : "+device);
          }
          return null;
      }
      return getOrCreateSharedDeviceImpl(device);
  }
  protected abstract AbstractGraphicsDevice getOrCreateSharedDeviceImpl(AbstractGraphicsDevice device);

  /** 
   * Returns the GLDynamicLookupHelper
   * @param profile if EGL/ES, profile <code>1</code> refers to ES1 and <code>2</code> to ES2,
   *        otherwise the profile is ignored.
   */
  public abstract GLDynamicLookupHelper getGLDynamicLookupHelper(int profile);

  //---------------------------------------------------------------------------
  // Dispatching GLDrawable construction in respect to the NativeSurface Capabilities
  //
  public GLDrawable createGLDrawable(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    final MutableGraphicsConfiguration config = (MutableGraphicsConfiguration) target.getGraphicsConfiguration();
    GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    AbstractGraphicsDevice adevice = config.getScreen().getDevice();
    GLDrawable result = null;
    adevice.lock();
    try {
        final OffscreenLayerSurface ols = NativeWindowFactory.getOffscreenLayerSurface(target, true);
        if(null != ols) {
            // layered surface -> Offscreen/PBuffer
            final GLCapabilities chosenCapsMod = (GLCapabilities) chosenCaps.cloneMutable();
            chosenCapsMod.setOnscreen(false);
            chosenCapsMod.setPBuffer(canCreateGLPbuffer(adevice));
            config.setChosenCapabilities(chosenCapsMod);
            if(DEBUG) {
                System.err.println("GLDrawableFactoryImpl.createGLDrawable -> OnscreenDrawable -> Offscreen-Layer: "+target);
            }
            if( ! ( target instanceof SurfaceChangeable ) ) {
                throw new IllegalArgumentException("Passed NativeSurface must implement SurfaceChangeable for offscreen layered surface: "+target);
            }
            result = createOffscreenDrawableImpl(target);            
        } else if(chosenCaps.isOnscreen()) {
            // onscreen
            if(DEBUG) {
                System.err.println("GLDrawableFactoryImpl.createGLDrawable -> OnscreenDrawable: "+target);
            }
            result = createOnscreenDrawableImpl(target);
        } else {
            // offscreen
            if(DEBUG) {
                System.err.println("GLDrawableFactoryImpl.createGLDrawable -> OffScreenDrawable (PBuffer: "+chosenCaps.isPBuffer()+"): "+target);
            }
            if( ! ( target instanceof SurfaceChangeable ) ) {
                throw new IllegalArgumentException("Passed NativeSurface must implement SurfaceChangeable for offscreen: "+target);
            }
            result = createOffscreenDrawableImpl(target);
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
  // PBuffer GLDrawable construction 
  //

  public abstract boolean canCreateGLPbuffer(AbstractGraphicsDevice device);

  public GLPbuffer createGLPbuffer(AbstractGraphicsDevice deviceReq,
                                   GLCapabilitiesImmutable capsRequested,
                                   GLCapabilitiesChooser chooser,
                                   int width,
                                   int height,
                                   GLContext shareWith) {
    if(height<=0 || height<=0) {
        throw new GLException("Width and height of pbuffer must be positive (were (" +
                        width + ", " + height + "))");
    }

    AbstractGraphicsDevice device = getOrCreateSharedDevice(deviceReq);
    if(null == device) {
        throw new GLException("No shared device for requested: "+deviceReq);
    }

    if (!canCreateGLPbuffer(device)) {
        throw new GLException("Pbuffer support not available with device: "+device);
    }
    
    GLCapabilitiesImmutable capsChosen = GLGraphicsConfigurationUtil.fixGLPBufferGLCapabilities(capsRequested);
    GLDrawableImpl drawable = null;
    device.lock();
    try {
        drawable = (GLDrawableImpl) createGLDrawable( createOffscreenSurfaceImpl(device, capsChosen, capsRequested, chooser, width, height) );
        if(null != drawable) {
            drawable.setRealized(true);
        }
    } finally {
        device.unlock();
    }

    if(null==drawable) {
        throw new GLException("Could not create Pbuffer drawable for: "+device+", "+capsChosen+", "+width+"x"+height);
    }
    return new GLPbufferImpl( drawable, shareWith);
  }


  //---------------------------------------------------------------------------
  //
  // Offscreen GLDrawable construction 
  //

  protected abstract GLDrawableImpl createOffscreenDrawableImpl(NativeSurface target) ;

  public GLDrawable createOffscreenDrawable(AbstractGraphicsDevice deviceReq,
                                            GLCapabilitiesImmutable capsRequested,
                                            GLCapabilitiesChooser chooser,
                                            int width,
                                            int height) {
    if(width<=0 || height<=0) {
        throw new GLException("Width and height of pbuffer must be positive (were (" +
                        width + ", " + height + "))");
    }
    AbstractGraphicsDevice device = getOrCreateSharedDevice(deviceReq);
    if(null == device) {
        throw new GLException("No shared device for requested: "+deviceReq);
    }
    GLCapabilitiesImmutable capsChosen = GLGraphicsConfigurationUtil.fixOffScreenGLCapabilities(capsRequested, canCreateGLPbuffer(deviceReq));

    device.lock();
    try {
        return createGLDrawable( createOffscreenSurfaceImpl(device, capsChosen, capsRequested, chooser, width, height) );
    } finally {
        device.unlock();
    }
  }

  public NativeSurface createOffscreenSurface(AbstractGraphicsDevice deviceReq,
                                              GLCapabilitiesImmutable capsRequested,
                                              GLCapabilitiesChooser chooser,
                                              int width, int height) {
    AbstractGraphicsDevice device = getOrCreateSharedDevice(deviceReq);
    if(null == device) {
        throw new GLException("No shared device for requested: "+deviceReq);
    }
    GLCapabilitiesImmutable capsChosen = GLGraphicsConfigurationUtil.fixOffScreenGLCapabilities(capsRequested, canCreateGLPbuffer(deviceReq));

    device.lock();
    try {
        return createOffscreenSurfaceImpl(device, capsChosen, capsRequested, chooser, width, height);
    } finally {
        device.unlock();
    }
  }

  /**
   * creates an offscreen NativeSurface, which must implement SurfaceChangeable as well,
   * so the windowing system related implementation is able to set the surface handle.
   */
  protected abstract NativeSurface createOffscreenSurfaceImpl(AbstractGraphicsDevice device,
                                                              GLCapabilitiesImmutable capabilities, GLCapabilitiesImmutable capsRequested,
                                                              GLCapabilitiesChooser chooser,
                                                              int width, int height);

  public ProxySurface createProxySurface(AbstractGraphicsDevice device, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser) {
    if(null == device) {
        throw new GLException("No shared device for requested: "+device);
    }

    device.lock();
    try {
        return createProxySurfaceImpl(device, windowHandle, capsRequested, chooser);
    } finally {
        device.unlock();
    }
  }  
  
  protected abstract ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice device, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser);

  //---------------------------------------------------------------------------
  //
  // External GLDrawable construction
  //

  protected abstract GLContext createExternalGLContextImpl();
  
  public GLContext createExternalGLContext() {
    NativeWindowFactory.getDefaultToolkitLock().lock();
    try {
        return createExternalGLContextImpl();
    } finally {
        NativeWindowFactory.getDefaultToolkitLock().unlock();
    }
  }

  protected abstract GLDrawable createExternalGLDrawableImpl();

  public GLDrawable createExternalGLDrawable() {
    NativeWindowFactory.getDefaultToolkitLock().lock();
    try {
        return createExternalGLDrawableImpl();
    } finally {
        NativeWindowFactory.getDefaultToolkitLock().unlock();
    }
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
  public static GLDrawableFactoryImpl getFactoryImpl(GLProfile glp) {
    return (GLDrawableFactoryImpl) getFactory(glp);
  }

  //---------------------------------------------------------------------------
  // Support for Java2D/JOGL bridge on Mac OS X; the external
  // GLDrawable mechanism in the public API is sufficient to
  // implement this functionality on all other platforms
  //

  public abstract boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device);

  public abstract GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
    throws GLException;

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
  public boolean setDisplayGamma(float gamma, float brightness, float contrast) throws IllegalArgumentException {
    if ((brightness < -1.0f) || (brightness > 1.0f)) {
      throw new IllegalArgumentException("Brightness must be between -1.0 and 1.0");
    }
    if (contrast < 0) {
      throw new IllegalArgumentException("Contrast must be greater than 0.0");
    }
    // FIXME: ensure gamma is > 1.0? Are smaller / negative values legal?
    int rampLength = getGammaRampLength();
    if (rampLength == 0) {
      return false;
    }
    float[] gammaRamp = new float[rampLength];
    for (int i = 0; i < rampLength; i++) {
      float intensity = (float) i / (float) (rampLength - 1);
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
    registerGammaShutdownHook();
    return setGammaRamp(gammaRamp);
  }

  public synchronized void resetDisplayGamma() {
    if (gammaShutdownHook == null) {
      throw new IllegalArgumentException("Should not call this unless setDisplayGamma called first");
    }
    resetGammaRamp(originalGammaRamp);
    unregisterGammaShutdownHook();
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
  protected boolean setGammaRamp(float[] ramp) {
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
  protected void resetGammaRamp(Buffer originalGammaRamp) {
  }

  // Shutdown hook mechanism for resetting gamma
  private boolean gammaShutdownHookRegistered;
  private Thread  gammaShutdownHook;
  private Buffer  originalGammaRamp;
  private synchronized void registerGammaShutdownHook() {
    if (gammaShutdownHookRegistered)
      return;
    if (gammaShutdownHook == null) {
      gammaShutdownHook = new Thread(new Runnable() {
          public void run() {
            synchronized (GLDrawableFactoryImpl.this) {
              resetGammaRamp(originalGammaRamp);
            }
          }
        });
      originalGammaRamp = getGammaRamp();
    }
    Runtime.getRuntime().addShutdownHook(gammaShutdownHook);
    gammaShutdownHookRegistered = true;
  }

  private synchronized void unregisterGammaShutdownHook() {
    if (!gammaShutdownHookRegistered)
      return;
    if (gammaShutdownHook == null) {
      throw new InternalError("Error in gamma shutdown hook logic");
    }
    Runtime.getRuntime().removeShutdownHook(gammaShutdownHook);
    gammaShutdownHookRegistered = false;
    // Leave the original gamma ramp data alone
  }
}
