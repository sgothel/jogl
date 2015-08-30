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

package jogamp.opengl.macosx.cgl;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.macosx.OSXDummyUpstreamSurfaceHook;
import jogamp.opengl.DesktopGLDynamicLookupHelper;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.SharedResourceRunner;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.GenericUpstreamSurfacelessHook;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLRendererQuirks;

public class MacOSXCGLDrawableFactory extends GLDrawableFactoryImpl {
  private static final boolean DEBUG_SHAREDCTX = DEBUG  || GLContext.DEBUG;

  private static DesktopGLDynamicLookupHelper macOSXCGLDynamicLookupHelper = null;

  public MacOSXCGLDrawableFactory() {
    super();

    synchronized(MacOSXCGLDrawableFactory.class) {
        if(null==macOSXCGLDynamicLookupHelper) {
            DesktopGLDynamicLookupHelper tmp = null;
            try {
                tmp = new DesktopGLDynamicLookupHelper(new MacOSXCGLDynamicLibraryBundleInfo());
            } catch (final GLException gle) {
                if(DEBUG) {
                    gle.printStackTrace();
                }
            }
            if(null!=tmp && tmp.isLibComplete()) {
                macOSXCGLDynamicLookupHelper = tmp;
                /** FIXME ??
                CGL.getCGLProcAddressTable().reset(macOSXCGLDynamicLookupHelper);
                */
            }
        }
    }

    defaultDevice = new MacOSXGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);

    if(null!=macOSXCGLDynamicLookupHelper) {
        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        MacOSXCGLGraphicsConfigurationFactory.registerFactory();
        if(GLProfile.isAWTAvailable()) {
            try {
              ReflectionUtil.callStaticMethod("jogamp.opengl.macosx.cgl.awt.MacOSXAWTCGLGraphicsConfigurationFactory",
                                              "registerFactory", null, null, getClass().getClassLoader());
            } catch (final Exception jre) { /* n/a .. */ }
        }

        sharedMap = new HashMap<String, SharedResource>();
    }
  }

  @Override
  protected final boolean isComplete() {
      return null != macOSXCGLDynamicLookupHelper;
  }

  @Override
  protected final void shutdownImpl() {
    if( DEBUG ) {
        System.err.println("MacOSXCGLDrawableFactory.shutdown");
    }
    if(null != sharedMap) {
        sharedMap.clear();
        sharedMap = null;
    }
    defaultDevice = null;
    /**
     * Pulling away the native library may cause havoc ..
     *
      macOSXCGLDynamicLookupHelper.destroy();
     */
    macOSXCGLDynamicLookupHelper = null;
  }

  @Override
  public final GLDynamicLookupHelper getGLDynamicLookupHelper(final int majorVersion, final int contextOptions) {
      return macOSXCGLDynamicLookupHelper;
  }

  private HashMap<String, SharedResource> sharedMap = new HashMap<String, SharedResource>();
  private MacOSXGraphicsDevice defaultDevice;

  static class SharedResource implements SharedResourceRunner.Resource {
      // private MacOSXCGLDrawable drawable;
      // private MacOSXCGLContext context;
      private final GLRendererQuirks glRendererQuirks;
      MacOSXGraphicsDevice device;
      boolean valid;
      boolean hasNPOTTextures;
      boolean hasRECTTextures;
      boolean hasAppleFloatPixels;

      SharedResource(final MacOSXGraphicsDevice device, final boolean valid,
                     final boolean hasNPOTTextures, final boolean hasRECTTextures, final boolean hasAppletFloatPixels
                     /* MacOSXCGLDrawable draw, MacOSXCGLContext ctx */, final GLRendererQuirks glRendererQuirks) {
          // drawable = draw;
          // this.context = ctx;
          this.glRendererQuirks = glRendererQuirks;
          this.device = device;
          this.valid = valid;
          this.hasNPOTTextures = hasNPOTTextures;
          this.hasRECTTextures = hasRECTTextures;
          this.hasAppleFloatPixels = hasAppletFloatPixels;
      }
      @Override
      public final boolean isAvailable() {
          return valid;
      }
      @Override
      public final MacOSXGraphicsDevice getDevice() { return device; }
      // final MacOSXCGLContext getContext() { return context; }
      final boolean isNPOTTextureAvailable() { return hasNPOTTextures; }
      final boolean isRECTTextureAvailable() { return hasRECTTextures; }
      final boolean isAppleFloatPixelsAvailable() { return hasAppleFloatPixels; }
      @Override
      public final AbstractGraphicsScreen getScreen() {
          return null;
      }
      @Override
      public final GLDrawableImpl getDrawable() {
          return null;
      }
      @Override
      public GLContextImpl getContext() {
          return null;
      }
      @Override
      public GLRendererQuirks getRendererQuirks(final GLProfile glp) {
          return glRendererQuirks;
      }
  }

  @Override
  public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
  }

  @Override
  public final boolean getIsDeviceCompatible(final AbstractGraphicsDevice device) {
      if(null!=macOSXCGLDynamicLookupHelper && device instanceof MacOSXGraphicsDevice) {
          return true;
      }
      return false;
  }

  private final HashSet<String> devicesTried = new HashSet<String>();

  private boolean getDeviceTried(final String connection) {
      synchronized (devicesTried) {
          return devicesTried.contains(connection);
      }
  }
  private void addDeviceTried(final String connection) {
      synchronized (devicesTried) {
          devicesTried.add(connection);
      }
  }
  private void removeDeviceTried(final String connection) {
      synchronized (devicesTried) {
          devicesTried.remove(connection);
      }
  }

  @Override
  protected final SharedResource getOrCreateSharedResourceImpl(final AbstractGraphicsDevice adevice) {
    final String connection = adevice.getConnection();
    SharedResource sr;
    synchronized(sharedMap) {
        sr = sharedMap.get(connection);
    }
    if(null==sr && !getDeviceTried(connection)) {
        addDeviceTried(connection);
        final MacOSXGraphicsDevice device = new MacOSXGraphicsDevice(adevice.getUnitID());
        GLDrawable drawable = null;
        GLDrawable zeroDrawable = null;
        GLContextImpl context = null;
        boolean contextIsCurrent = false;
        device.lock();
        try {
            final GLProfile glp = GLProfile.get(device, GLProfile.GL_PROFILE_LIST_MIN_DESKTOP, false);
            if (null == glp) {
                throw new GLException("Couldn't get default GLProfile for device: "+device);
            }
            final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
            drawable = createOnscreenDrawableImpl(createDummySurfaceImpl(device, false, caps, caps, null, 64, 64));
            drawable.setRealized(true);

            context = (MacOSXCGLContext) drawable.createContext(null);
            if (null == context) {
                throw new GLException("Couldn't create shared context for drawable: "+drawable);
            }
            contextIsCurrent = GLContext.CONTEXT_NOT_CURRENT != context.makeCurrent();

            final boolean allowsSurfacelessCtx;
            final boolean hasNPOTTextures;
            final boolean hasRECTTextures;
            final boolean hasAppleFloatPixels;
            final GLRendererQuirks glRendererQuirks;
            if( contextIsCurrent ) {
                // We allow probing surfaceless for even the compatible 2.1 context,
                // which we probably have right here - since OSX may support this.
                // Otherwise, we cannot map the quirk to the device.
                if( probeSurfacelessCtx(context, false /* restoreDrawable */) ) {
                    allowsSurfacelessCtx = true;
                    zeroDrawable = context.getGLDrawable();
                } else {
                    allowsSurfacelessCtx = false;
                }
                final GL gl = context.getGL();
                hasNPOTTextures = gl.isNPOTTextureAvailable();
                hasRECTTextures = gl.isExtensionAvailable(GLExtensions.EXT_texture_rectangle);
                hasAppleFloatPixels = gl.isExtensionAvailable(GLExtensions.APPLE_float_pixels);
                glRendererQuirks = context.getRendererQuirks();
            } else {
                allowsSurfacelessCtx = false;
                hasNPOTTextures = false;
                hasRECTTextures = false;
                hasAppleFloatPixels = false;
                glRendererQuirks = null;
            }
            sr = new SharedResource(device, contextIsCurrent, hasNPOTTextures, hasRECTTextures, hasAppleFloatPixels, glRendererQuirks);
            if ( DEBUG_SHAREDCTX ) {
                System.err.println("SharedDevice:  " + device);
                System.err.println("SharedContext: " + context + ", madeCurrent " + contextIsCurrent);
                System.err.println("  NPOT "+hasNPOTTextures+", RECT "+hasRECTTextures+", FloatPixels "+hasAppleFloatPixels);
                System.err.println("  allowsSurfacelessCtx "+allowsSurfacelessCtx);
                System.err.println("  glRendererQuirks "+glRendererQuirks);
            }
            synchronized(sharedMap) {
                sharedMap.put(connection, sr);
            }
        } catch (final Throwable t) {
            throw new GLException("MacOSXCGLDrawableFactory - Could not initialize shared resources for "+adevice, t);
        } finally {
            if( null != context ) {
                try {
                    context.destroy();
                } catch (final GLException gle) {
                    if ( DEBUG_SHAREDCTX ) {
                        System.err.println("MacOSXCGLDrawableFactory.createShared: INFO: destroy caught exception:");
                        gle.printStackTrace();
                    }
                }
            }
            if( null != zeroDrawable ) {
                zeroDrawable.setRealized(false);
            }
            if( null != drawable ) {
                drawable.setRealized(false);
            }
            device.unlock();
            removeDeviceTried(connection);
        }
    }
    return sr;
  }

  @Override
  protected final Thread getSharedResourceThread() {
    return null;
  }

  /**
   * {@inheritDoc}
   * <p>
   * This factory always supports native desktop OpenGL profiles.
   * </p>
   */
  @Override
  public final boolean hasOpenGLDesktopSupport() { return true; }

  /**
   * {@inheritDoc}
   * <p>
   * This factory never supports native GLES profiles.
   * </p>
   */
  @Override
  public final boolean hasOpenGLESSupport() { return false; }

  /**
   * {@inheritDoc}
   * <p>
   * Always returns true.
   * </p>
   */
  @Override
  public final boolean hasMajorMinorCreateContextARB() { return true; }

  @Override
  protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(final AbstractGraphicsDevice device) {
      return MacOSXCGLGraphicsConfiguration.getAvailableCapabilities(this, device);
  }

  @Override
  protected GLDrawableImpl createOnscreenDrawableImpl(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new MacOSXOnscreenCGLDrawable(this, target);
  }

  @Override
  protected GLDrawableImpl createOffscreenDrawableImpl(final NativeSurface target) {
    final MutableGraphicsConfiguration config = (MutableGraphicsConfiguration) target.getGraphicsConfiguration();
    final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if(!caps.isPBuffer()) {
        // Actual implementation is using PBuffer ...
        final GLCapabilities modCaps = (GLCapabilities) caps.cloneMutable();
        modCaps.setPBuffer(true);
        modCaps.setBitmap(false);
        config.setChosenCapabilities(modCaps);
        return new MacOSXOffscreenCGLDrawable(this, target);
    }
    return new MacOSXPbufferCGLDrawable(this, target);
  }

  @Override
  public boolean canCreateGLPbuffer(final AbstractGraphicsDevice device, final GLProfile glp) {
    if( glp.isGL2() ) {
        // OSX only supports pbuffer w/ compatible, non-core, context.
        return true;
    } else {
        return false;
    }
  }

  @Override
  protected ProxySurface createMutableSurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                  final GLCapabilitiesImmutable capsChosen, final GLCapabilitiesImmutable capsRequested,
                                                  final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstreamHook) {
    final MacOSXGraphicsDevice device;
    if( createNewDevice || !(deviceReq instanceof MacOSXGraphicsDevice) ) {
        device = new MacOSXGraphicsDevice(deviceReq.getUnitID());
    } else {
        device = (MacOSXGraphicsDevice)deviceReq;
    }
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
    final MacOSXCGLGraphicsConfiguration config = MacOSXCGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen, true);
    if(null == config) {
        throw new GLException("Choosing GraphicsConfiguration failed w/ "+capsChosen+" on "+screen);
    }
    return new WrappedSurface(config, 0, upstreamHook, createNewDevice);
  }

  @Override
  public final ProxySurface createDummySurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                   GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
    chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
    return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser,
                                    new OSXDummyUpstreamSurfaceHook(width, height));
  }

  @Override
  public final ProxySurface createSurfacelessImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
          GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
    chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
    return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, new GenericUpstreamSurfacelessHook(width, height));
  }

  @Override
  protected ProxySurface createProxySurfaceImpl(final AbstractGraphicsDevice deviceReq, final int screenIdx, final long windowHandle, final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstream) {
    final MacOSXGraphicsDevice device = new MacOSXGraphicsDevice(deviceReq.getUnitID());
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, screenIdx);
    final MacOSXCGLGraphicsConfiguration config = MacOSXCGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, true);
    return new WrappedSurface(config, windowHandle, upstream, true);
  }

  @Override
  protected GLContext createExternalGLContextImpl() {
    return MacOSXExternalCGLContext.create(this);
  }

  @Override
  public boolean canCreateExternalGLDrawable(final AbstractGraphicsDevice device) {
    return false;
  }

  @Override
  protected GLDrawable createExternalGLDrawableImpl() {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  //------------------------------------------------------
  // Gamma-related functionality
  //

  private static final int GAMMA_RAMP_LENGTH = 256;

  /** Returns the length of the computed gamma ramp for this OS and
      hardware. Returns 0 if gamma changes are not supported. */
  @Override
  protected int getGammaRampLength(final NativeSurface surface) {
    return GAMMA_RAMP_LENGTH;
  }

  @Override
  protected boolean setGammaRamp(final NativeSurface surface, final float[] ramp) {
    final FloatBuffer rampNIO = Buffers.newDirectFloatBuffer(ramp);
    return CGL.setGammaRamp(ramp.length, rampNIO, rampNIO, rampNIO);
  }

  @Override
  protected Buffer getGammaRamp(final NativeSurface surface) {
    return ShortBuffer.allocate(0); // return a dummy gamma ramp default for reset
  }

  @Override
  protected void resetGammaRamp(final NativeSurface surface, final Buffer originalGammaRamp) {
    CGL.resetGammaRamp();
  }

  @Override
  protected final void resetGammaRamp(final DeviceScreenID deviceScreenID, final Buffer originalGammaRamp) {
      CGL.resetGammaRamp();
  }

}
