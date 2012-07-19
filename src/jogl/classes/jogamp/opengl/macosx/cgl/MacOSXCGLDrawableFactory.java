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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.ProxySurface.UpstreamSurfaceHook;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLProfile.ShutdownType;

import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.opengl.DesktopGLDynamicLookupHelper;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.WrappedSurface;
import com.jogamp.nativewindow.macosx.MacOSXGraphicsDevice;
import com.jogamp.opengl.GLExtensions;

public class MacOSXCGLDrawableFactory extends GLDrawableFactoryImpl {
  private static DesktopGLDynamicLookupHelper macOSXCGLDynamicLookupHelper = null;

  public MacOSXCGLDrawableFactory() {
    super();

    synchronized(MacOSXCGLDrawableFactory.class) {
        if(null==macOSXCGLDynamicLookupHelper) {
            DesktopGLDynamicLookupHelper tmp = null;
            try {
                tmp = new DesktopGLDynamicLookupHelper(new MacOSXCGLDynamicLibraryBundleInfo());
            } catch (GLException gle) {
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
            } catch (JogampRuntimeException jre) { /* n/a .. */ }
        }

        sharedMap = new HashMap<String, SharedResource>();
    }
  }

  @Override
  protected final void destroy(ShutdownType shutdownType) {
    if(null != sharedMap) {
        sharedMap.clear();
        sharedMap = null;
    }
    defaultDevice = null;
    /**
     * Pulling away the native library may cause havoc ..
     *
    if(ShutdownType.COMPLETE == shutdownType && null != macOSXCGLDynamicLookupHelper) {
        macOSXCGLDynamicLookupHelper.destroy();
        macOSXCGLDynamicLookupHelper = null;
    } */
  }

  @Override
  public GLDynamicLookupHelper getGLDynamicLookupHelper(int profile) {
      return macOSXCGLDynamicLookupHelper;
  }

  private HashMap<String, SharedResource> sharedMap = new HashMap<String, SharedResource>();
  private MacOSXGraphicsDevice defaultDevice;

  static class SharedResource {
      // private MacOSXCGLDrawable drawable;
      // private MacOSXCGLContext context;
      MacOSXGraphicsDevice device;
      boolean wasContextCreated;
      boolean hasNPOTTextures;
      boolean hasRECTTextures;
      boolean hasAppleFloatPixels;

      SharedResource(MacOSXGraphicsDevice device, boolean wasContextCreated,
                     boolean hasNPOTTextures, boolean hasRECTTextures, boolean hasAppletFloatPixels
                     /* MacOSXCGLDrawable draw, MacOSXCGLContext ctx */) {
          // drawable = draw;
          // context = ctx;
          this.device = device;
          this.wasContextCreated = wasContextCreated;
          this.hasNPOTTextures = hasNPOTTextures;
          this.hasRECTTextures = hasRECTTextures;
          this.hasAppleFloatPixels = hasAppletFloatPixels;
      }
      final MacOSXGraphicsDevice getDevice() { return device; }
      final boolean wasContextAvailable() { return wasContextCreated; }
      final boolean isNPOTTextureAvailable() { return hasNPOTTextures; }
      final boolean isRECTTextureAvailable() { return hasRECTTextures; }
      final boolean isAppleFloatPixelsAvailable() { return hasAppleFloatPixels; }
  }

  @Override
  public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
  }

  @Override
  public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      if(null!=macOSXCGLDynamicLookupHelper && device instanceof MacOSXGraphicsDevice) {
          return true;
      }
      return false;
  }

  private HashSet<String> devicesTried = new HashSet<String>();

  private boolean getDeviceTried(String connection) {
      synchronized (devicesTried) {
          return devicesTried.contains(connection);
      }
  }
  private void addDeviceTried(String connection) {
      synchronized (devicesTried) {
          devicesTried.add(connection);
      }
  }
  private void removeDeviceTried(String connection) {
      synchronized (devicesTried) {
          devicesTried.remove(connection);
      }
  }

  /* package */ SharedResource getOrCreateOSXSharedResource(AbstractGraphicsDevice adevice) {
    final String connection = adevice.getConnection();
    SharedResource sr;
    synchronized(sharedMap) {
        sr = sharedMap.get(connection);
    }
    if(null==sr && !getDeviceTried(connection)) {
        addDeviceTried(connection);
        final MacOSXGraphicsDevice sharedDevice = new MacOSXGraphicsDevice(adevice.getUnitID());
        boolean madeCurrent = false;
        boolean hasNPOTTextures = false;
        boolean hasRECTTextures = false;
        boolean hasAppleFloatPixels = false;
        {
            GLProfile glp = GLProfile.get(sharedDevice, GLProfile.GL_PROFILE_LIST_MIN_DESKTOP, false);
            if (null == glp) {
                throw new GLException("Couldn't get default GLProfile for device: "+sharedDevice);
            }
            final GLDrawableImpl sharedDrawable = createOnscreenDrawableImpl(createDummySurfaceImpl(sharedDevice, false, new GLCapabilities(glp), null, 64, 64));
            sharedDrawable.setRealized(true);
            
            final GLContextImpl sharedContext  = (GLContextImpl) sharedDrawable.createContext(null);
            if (null == sharedContext) {
                throw new GLException("Couldn't create shared context for drawable: "+sharedDrawable);
            }
            
            try {
                sharedContext.makeCurrent(); // could cause exception
                madeCurrent = sharedContext.isCurrent();
                if(madeCurrent) {
                    GL gl = sharedContext.getGL();
                    hasNPOTTextures = gl.isNPOTTextureAvailable();
                    hasRECTTextures = gl.isExtensionAvailable(GLExtensions.EXT_texture_rectangle);
                    hasAppleFloatPixels = gl.isExtensionAvailable(GLExtensions.APPLE_float_pixels);
                }
            } catch (GLException gle) {
                if (DEBUG) {
                    System.err.println("MacOSXCGLDrawableFactory.createShared: INFO: makeCurrent catched exception:");
                    gle.printStackTrace();
                }
            } finally {
                try {
                    sharedContext.destroy();
                } catch (GLException gle) {
                    if (DEBUG) {
                        System.err.println("MacOSXCGLDrawableFactory.createShared: INFO: destroy catched exception:");
                        gle.printStackTrace();
                    }
                }
            }
            sharedDrawable.setRealized(false);
        }
        sr = new SharedResource(sharedDevice, madeCurrent, hasNPOTTextures, hasRECTTextures, hasAppleFloatPixels);
        synchronized(sharedMap) {
            sharedMap.put(connection, sr);
        }
        removeDeviceTried(connection);
        if (DEBUG) {
            System.err.println("MacOSXCGLDrawableFactory.createShared: device:  " + sharedDevice);
            System.err.println("MacOSXCGLDrawableFactory.createShared: context: madeCurrent " + madeCurrent + ", NPOT "+hasNPOTTextures+
                               ", RECT "+hasRECTTextures+", FloatPixels "+hasAppleFloatPixels);
        }
    }
    return sr;
  }

  @Override
  protected final Thread getSharedResourceThread() {
    return null;
  }

  @Override
  protected final boolean createSharedResource(AbstractGraphicsDevice device) {
    try {
        SharedResource sr = getOrCreateOSXSharedResource(device);
        if(null!=sr) {
            return sr.wasContextAvailable();
        }
    } catch (GLException gle) {
        if(DEBUG) {
            System.err.println("Catched Exception while MaxOSXCGL Shared Resource initialization:");
            gle.printStackTrace();
        }
    }
    return false;
  }

  @Override
  protected final GLContext getOrCreateSharedContextImpl(AbstractGraphicsDevice device) {
      // FIXME: not implemented .. needs a dummy OSX surface
      return null;
  }

  @Override
  protected AbstractGraphicsDevice getOrCreateSharedDeviceImpl(AbstractGraphicsDevice device) {
      SharedResource sr = getOrCreateOSXSharedResource(device);
      if(null!=sr) {
          return sr.getDevice();
      }
      return null;
  }

  @Override
  protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(AbstractGraphicsDevice device) {
      return MacOSXCGLGraphicsConfiguration.getAvailableCapabilities(this, device);
  }

  @Override
  protected GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
    if (target == null) {
      throw new IllegalArgumentException("Null target");
    }
    return new MacOSXOnscreenCGLDrawable(this, target);
  }

  @Override
  protected GLDrawableImpl createOffscreenDrawableImpl(NativeSurface target) {
    AbstractGraphicsConfiguration config = target.getGraphicsConfiguration();
    GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    if(!caps.isPBuffer()) {
        return new MacOSXOffscreenCGLDrawable(this, target);
    }
    return new MacOSXPbufferCGLDrawable(this, target);
  }

  @Override
  public boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
    return true;
  }

  @Override
  protected ProxySurface createMutableSurfaceImpl(AbstractGraphicsDevice deviceReq, boolean createNewDevice, 
                                                  GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested, 
                                                  GLCapabilitiesChooser chooser, int width, int height, UpstreamSurfaceHook lifecycleHook) {
    final MacOSXGraphicsDevice device;
    if(createNewDevice) {
        device = new MacOSXGraphicsDevice(deviceReq.getUnitID());
    } else {
        device = (MacOSXGraphicsDevice)deviceReq;
    }
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
    final MacOSXCGLGraphicsConfiguration config = MacOSXCGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen, true);
    if(null == config) {
        throw new GLException("Choosing GraphicsConfiguration failed w/ "+capsChosen+" on "+screen); 
    }    
    return new WrappedSurface(config, 0, width, height, lifecycleHook);
  }

  @Override
  public final ProxySurface createDummySurfaceImpl(AbstractGraphicsDevice deviceReq, boolean createNewDevice, 
                                                   GLCapabilitiesImmutable requestedCaps, GLCapabilitiesChooser chooser, int width, int height) {
    final GLCapabilitiesImmutable chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(requestedCaps);
    return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, width, height, dummySurfaceLifecycleHook); 
  }  
  private static final ProxySurface.UpstreamSurfaceHook dummySurfaceLifecycleHook = new ProxySurface.UpstreamSurfaceHook() {
    long nsWindow = 0;
    @Override
    public final void create(ProxySurface s) {
        if(0 == nsWindow && 0 == s.getSurfaceHandle()) {
            nsWindow = OSXUtil.CreateNSWindow(0, 0, s.getWidth(), s.getHeight());
            if(0 == nsWindow) {
                throw new GLException("Error NS window 0");
            }
            long nsView = OSXUtil.GetNSView(nsWindow);
            if(0 == nsView) {
                throw new GLException("Error NS view 0");
            }
            s.setSurfaceHandle(nsView);
            s.setImplBitfield(ProxySurface.INVISIBLE_WINDOW);
            if(DEBUG) {
                System.err.println("MacOSXCGLDrawableFactory.dummySurfaceLifecycleHook.create: "+s);
            }
        }
    }
    @Override
    public final void destroy(ProxySurface s) {
        if(0 != nsWindow && 0 != s.getSurfaceHandle()) {
            OSXUtil.DestroyNSWindow(nsWindow);
            nsWindow = 0;
            s.setSurfaceHandle(0);
            if(DEBUG) {
                System.err.println("MacOSXCGLDrawableFactory.dummySurfaceLifecycleHook.destroy: "+s);
            }
        }
    }
    @Override
    public final int getWidth(ProxySurface s) {
        return s.initialWidth;
    }
    @Override
    public final int getHeight(ProxySurface s) {
        return s.initialHeight;
    }
    
    @Override
    public String toString() {
       return "MacOSXLSurfaceLifecycleHook[]";
    }
    
  };
  
  @Override
  protected ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice deviceReq, int screenIdx, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstream) {
    final MacOSXGraphicsDevice device = new MacOSXGraphicsDevice(deviceReq.getUnitID());
    final AbstractGraphicsScreen screen = new DefaultGraphicsScreen(device, screenIdx);
    final MacOSXCGLGraphicsConfiguration config = MacOSXCGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, true); 
    return new WrappedSurface(config, windowHandle, 0, 0, upstream);
  }

  @Override
  protected GLContext createExternalGLContextImpl() {
    return MacOSXExternalCGLContext.create(this);
  }

  @Override
  public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
    return false;
  }

  @Override
  protected GLDrawable createExternalGLDrawableImpl() {
    // FIXME
    throw new GLException("Not yet implemented");
  }

  @Override
  public boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
    return false;
  }

  @Override
  public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
    throws GLException {
    throw new GLException("not supported in non AWT enviroment");
  }

  //------------------------------------------------------
  // Gamma-related functionality
  //

  private static final int GAMMA_RAMP_LENGTH = 256;

  /** Returns the length of the computed gamma ramp for this OS and
      hardware. Returns 0 if gamma changes are not supported. */
  @Override
  protected int getGammaRampLength() {
    return GAMMA_RAMP_LENGTH;
  }

  @Override
  protected boolean setGammaRamp(float[] ramp) {
    return CGL.setGammaRamp(ramp.length,
                            ramp, 0,
                            ramp, 0,
                            ramp, 0);
  }

  @Override
  protected Buffer getGammaRamp() {
    return null;
  }

  @Override
  protected void resetGammaRamp(Buffer originalGammaRamp) {
    CGL.resetGammaRamp();
  }
}
