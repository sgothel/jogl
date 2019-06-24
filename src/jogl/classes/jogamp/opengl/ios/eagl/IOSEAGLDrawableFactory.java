/**
 * Copyright 2019 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.opengl.ios.eagl;

import java.nio.Buffer;
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
import com.jogamp.nativewindow.ios.IOSGraphicsDevice;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.WrappedSurface;
import jogamp.nativewindow.ios.IOSDummyUpstreamSurfaceHook;
import jogamp.nativewindow.ios.IOSUtil;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.SharedResourceRunner;

import com.jogamp.nativewindow.GenericUpstreamSurfacelessHook;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.Attachment.StorageDefinition;

public class IOSEAGLDrawableFactory extends GLDrawableFactoryImpl {
  private static final boolean DEBUG_SHAREDCTX = DEBUG  || GLContext.DEBUG;

  private static GLDynamicLookupHelper iosEAGLDynamicLookupHelper = null;

  public IOSEAGLDrawableFactory() {
    super();

    synchronized(IOSEAGLDrawableFactory.class) {
        if(null==iosEAGLDynamicLookupHelper) {
            GLDynamicLookupHelper tmp = null;
            try {
                tmp = new GLDynamicLookupHelper(new IOSEAGLDynamicLibraryBundleInfo());
            } catch (final GLException gle) {
                if(DEBUG) {
                    gle.printStackTrace();
                }
            }
            if(null!=tmp && tmp.isLibComplete()) {
                iosEAGLDynamicLookupHelper = tmp;
            }
        }
    }

    defaultDevice = new IOSGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);

    if(null!=iosEAGLDynamicLookupHelper) {
        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        IOSEAGLGraphicsConfigurationFactory.registerFactory();
        sharedMap = new HashMap<String, SharedResource>();
    }
  }

  @Override
  protected final boolean isComplete() {
      return null != iosEAGLDynamicLookupHelper;
  }

  @Override
  protected final void shutdownImpl() {
    if( DEBUG ) {
        System.err.println("IOSEAGLDrawableFactory.shutdown");
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
    iosEAGLDynamicLookupHelper = null;
  }

  @Override
  public final GLDynamicLookupHelper getGLDynamicLookupHelper(final int majorVersion, final int contextOptions) {
      return iosEAGLDynamicLookupHelper;
  }

  private HashMap<String, SharedResource> sharedMap = new HashMap<String, SharedResource>();
  private IOSGraphicsDevice defaultDevice;

  static class SharedResource implements SharedResourceRunner.Resource {
      // private IOSEAGLDrawable drawable;
      // private IOSEAGLContext context;
      private final GLRendererQuirks glRendererQuirks;
      IOSGraphicsDevice device;
      boolean valid;
      boolean hasNPOTTextures;
      boolean hasRECTTextures;
      boolean hasAppleFloatPixels;

      SharedResource(final IOSGraphicsDevice device, final boolean valid,
                     final boolean hasNPOTTextures, final boolean hasRECTTextures, final boolean hasAppletFloatPixels
                     /* IOSEAGLDrawable draw, IOSEAGLContext ctx */, final GLRendererQuirks glRendererQuirks) {
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
      public final IOSGraphicsDevice getDevice() { return device; }
      // final IOSEAGLContext getContext() { return context; }
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
      if(null!=iosEAGLDynamicLookupHelper && device instanceof IOSGraphicsDevice) {
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
        final IOSGraphicsDevice device = new IOSGraphicsDevice(adevice.getUnitID());
        GLDrawable drawable = null;
        GLDrawable zeroDrawable = null;
        GLContextImpl context = null;
        boolean contextIsCurrent = false;
        device.lock();
        try {
            final GLProfile glp = GLProfile.get(device, GLProfile.GL_PROFILE_LIST_MAX_MOBILE, false);
            if (null == glp) {
                throw new GLException("Couldn't get default GLProfile for device: "+device);
            }
            final GLCapabilitiesImmutable caps = new GLCapabilities(glp);
            // drawable = createSurfacelessFBODrawable(device, caps, 64, 64);
            drawable = createSurfacelessDrawable(device, caps, 64, 64);

            drawable.setRealized(true);

            context = (IOSEAGLContext) drawable.createContext(null);
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
            throw new GLException("IOSEAGLDrawableFactory - Could not initialize shared resources for "+adevice, t);
        } finally {
            if( null != context ) {
                try {
                    context.destroy();
                } catch (final GLException gle) {
                    if ( DEBUG_SHAREDCTX ) {
                        System.err.println("IOSEAGLDrawableFactory.createShared: INFO: destroy caught exception:");
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
   * This factory never supports native desktop OpenGL profiles.
   * </p>
   */
  @Override
  public final boolean hasOpenGLDesktopSupport() { return false; }

  /**
   * {@inheritDoc}
   * <p>
   * This factory always supports native GLES profiles.
   * </p>
   */
  @Override
  public final boolean hasOpenGLESSupport() { return true; }

  /**
   * {@inheritDoc}
   * <p>
   * Always returns false.
   * </p>
   */
  @Override
  public final boolean hasMajorMinorCreateContextARB() { return false; }

  @Override
  protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(final AbstractGraphicsDevice device) {
      return IOSEAGLGraphicsConfiguration.getAvailableCapabilities(this, device);
  }

  @Override
  protected final OnscreenFBOColorbufferStorageDefinition getOnscreenFBOColorbufStorageDef() { return onscreenGLDrawableFBOColorRenderbufferStorageDef; }
  private final OnscreenFBOColorbufferStorageDefinition onscreenGLDrawableFBOColorRenderbufferStorageDef = new OnscreenFBOColorbufferStorageDefinition() {
                @Override
                public final void setStorage(final GL gl, final Attachment a) {
                    final int samples = ((FBObject.RenderAttachment) a).getSamples();
                    if( samples > 0 ) {
                        gl.glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, samples, a.format, a.getWidth(), a.getHeight());
                    } else {
                        // gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, a.format, a.getWidth(), a.getHeight());
                        final long eaglLayer = IOSUtil.GetCAEAGLLayer(gl.getContext().getGLDrawable().getNativeSurface().getSurfaceHandle());
                        EAGL.eaglBindDrawableStorageToRenderbuffer(gl.getContext().getHandle(), GL.GL_RENDERBUFFER, eaglLayer);
                        if( DEBUG ) {
                            System.err.println("EAGL.eaglBindDrawableStorageToRenderbuffer: ctx 0x"+Long.toHexString(gl.getContext().getHandle()) +
                                               ", eaglLayer 0x"+Long.toHexString(eaglLayer));
                        }
                    }
                }
                @Override
                public final boolean isDoubleBufferSupported() {
                    return false;
                }
                @Override
                public final int getTextureUnit() {
                    return -1; // force using a color renderbuffer
                } };

  /**
   * {@inheritDoc}
   * <p>
   * This IOS <i>onscreen drawable</i> implementation actually is only a dummy implementation
   * </p>
   */
  @Override
  protected GLDrawableImpl createOnscreenDrawableImpl(final NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new IOSOnscreenEAGLDrawable(this, target);
  }

  @Override
  protected GLDrawableImpl createOffscreenDrawableImpl(final NativeSurface target) {
    throw new GLException("Only FBO is supported for offscreen");
  }

  @Override
  public boolean canCreateGLPbuffer(final AbstractGraphicsDevice device, final GLProfile glp) {
    return false;
  }

  @Override
  protected ProxySurface createMutableSurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                  final GLCapabilitiesImmutable capsChosen, final GLCapabilitiesImmutable capsRequested,
                                                  final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstreamHook) {
    final IOSGraphicsDevice device;
    if( createNewDevice || !(deviceReq instanceof IOSGraphicsDevice) ) {
        device = new IOSGraphicsDevice(deviceReq.getUnitID());
    } else {
        device = (IOSGraphicsDevice)deviceReq;
    }
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
    final IOSEAGLGraphicsConfiguration config = IOSEAGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen, true);
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
                                    new IOSDummyUpstreamSurfaceHook(width, height));
  }

  @Override
  public final ProxySurface createSurfacelessImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
          GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
    chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
    return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, new GenericUpstreamSurfacelessHook(width, height));
  }

  @Override
  protected ProxySurface createProxySurfaceImpl(final AbstractGraphicsDevice deviceReq, final int screenIdx, final long windowHandle, final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstream) {
    final IOSGraphicsDevice device = new IOSGraphicsDevice(deviceReq.getUnitID());
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, screenIdx);
    final IOSEAGLGraphicsConfiguration config = IOSEAGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, true);
    return new WrappedSurface(config, windowHandle, upstream, true);
  }

  @Override
  protected GLContext createExternalGLContextImpl() {
    throw new GLException("Not implemented");
  }

  @Override
  public boolean canCreateExternalGLDrawable(final AbstractGraphicsDevice device) {
    return false;
  }

  @Override
  protected GLDrawable createExternalGLDrawableImpl() {
    throw new GLException("Not implemented");
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
    // final FloatBuffer rampNIO = Buffers.newDirectFloatBuffer(ramp);
    return false; // TODO CGL.setGammaRamp(ramp.length, rampNIO, rampNIO, rampNIO);
  }

  @Override
  protected Buffer getGammaRamp(final NativeSurface surface) {
    return ShortBuffer.allocate(0); // return a dummy gamma ramp default for reset
  }

  @Override
  protected void resetGammaRamp(final NativeSurface surface, final Buffer originalGammaRamp) {
    // TODO CGL.resetGammaRamp();
  }

  @Override
  protected final void resetGammaRamp(final DeviceScreenID deviceScreenID, final Buffer originalGammaRamp) {
      // TODO CGL.resetGammaRamp();
  }

}
