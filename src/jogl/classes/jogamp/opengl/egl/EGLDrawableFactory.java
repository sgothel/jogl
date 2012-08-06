/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package jogamp.opengl.egl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.MutableSurface;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.ProxySurface.UpstreamSurfaceHook;
import javax.media.nativewindow.VisualIDHolder.VIDType;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLProfile.ShutdownType;

import jogamp.opengl.Debug;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.WrappedSurface;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

public class EGLDrawableFactory extends GLDrawableFactoryImpl {
    /* package */ static final boolean QUERY_EGL_ES = !Debug.isPropertyDefined("jogl.debug.EGLDrawableFactory.DontQuery", true);
    /* package */ static final boolean QUERY_EGL_ES_NATIVE_TK = Debug.isPropertyDefined("jogl.debug.EGLDrawableFactory.QueryNativeTK", true);
    
    private static GLDynamicLookupHelper eglES1DynamicLookupHelper = null;
    private static GLDynamicLookupHelper eglES2DynamicLookupHelper = null;
    private static boolean isANGLE = false;

    private static final boolean isANGLE(GLDynamicLookupHelper dl) {
        if(Platform.OSType.WINDOWS == Platform.OS_TYPE) {
            final boolean r = 0 != dl.dynamicLookupFunction("eglQuerySurfacePointerANGLE") ||
                              0 != dl.dynamicLookupFunction("glBlitFramebufferANGLE") ||
                              0 != dl.dynamicLookupFunction("glRenderbufferStorageMultisampleANGLE");
            return r;
        } else {
            return false;
        }
    }

    public EGLDrawableFactory() {
        super();

        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        EGLGraphicsConfigurationFactory.registerFactory();

        // Check for other underlying stuff ..
        if(NativeWindowFactory.TYPE_X11.equals(NativeWindowFactory.getNativeWindowType(true))) {
            try {
                ReflectionUtil.createInstance("jogamp.opengl.x11.glx.X11GLXGraphicsConfigurationFactory", EGLDrawableFactory.class.getClassLoader());
            } catch (JogampRuntimeException jre) { /* n/a .. */ }
        }

        defaultDevice = new EGLGraphicsDevice(AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);

        // FIXME: Probably need to move EGL from a static model
        // to a dynamic one, where there can be 2 instances
        // for each ES profile with their own ProcAddressTable.

        synchronized(EGLDrawableFactory.class) {
            /**
             * Currently AMD's EGL impl. crashes at eglGetDisplay(EGL_DEFAULT_DISPLAY)
             *
            // Check Desktop ES2 Availability first (AMD, ..)
            if(null==eglES2DynamicLookupHelper) {
                GLDynamicLookupHelper tmp=null;
                try {
                    tmp = new GLDynamicLookupHelper(new DesktopES2DynamicLibraryBundleInfo());
                } catch (GLException gle) {
                    if(DEBUG) {
                        gle.printStackTrace();
                    }
                }
                if(null!=tmp && tmp.isLibComplete()) {
                    eglES2DynamicLookupHelper = tmp;
                    EGL.resetProcAddressTable(eglES2DynamicLookupHelper);
                    if (GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory: Desktop ES2 - OK");
                    }
                } else if (GLProfile.DEBUG) {
                    System.err.println("Info: EGLDrawableFactory: Desktop ES2 - NOPE");
                }
            } */
            final boolean hasDesktopES2 = null != eglES2DynamicLookupHelper;

            if(!hasDesktopES2 && null==eglES1DynamicLookupHelper) {
                GLDynamicLookupHelper tmp=null;
                try {
                    tmp = new GLDynamicLookupHelper(new EGLES1DynamicLibraryBundleInfo());
                } catch (GLException gle) {
                    if(DEBUG) {
                        gle.printStackTrace();
                    }
                }
                if(null!=tmp && tmp.isLibComplete()) {
                    eglES1DynamicLookupHelper = tmp;
                    EGL.resetProcAddressTable(eglES1DynamicLookupHelper);
                    final boolean isANGLEES1 = isANGLE(eglES1DynamicLookupHelper);
                    isANGLE |= isANGLEES1;
                    if (GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory: EGL ES1 - OK, isANGLE: "+isANGLEES1);
                    }
                } else if (GLProfile.DEBUG) {
                    System.err.println("Info: EGLDrawableFactory: EGL ES1 - NOPE");
                }
            }
            if(!hasDesktopES2 && null==eglES2DynamicLookupHelper) {
                GLDynamicLookupHelper tmp=null;
                try {
                    tmp = new GLDynamicLookupHelper(new EGLES2DynamicLibraryBundleInfo());
                } catch (GLException gle) {
                    if(DEBUG) {
                        gle.printStackTrace();
                    }
                }
                if(null!=tmp && tmp.isLibComplete()) {
                    eglES2DynamicLookupHelper = tmp;
                    EGL.resetProcAddressTable(eglES2DynamicLookupHelper);
                    final boolean isANGLEES2 = isANGLE(eglES2DynamicLookupHelper);
                    isANGLE |= isANGLEES2;
                    if (GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory: EGL ES2 - OK, isANGLE: "+isANGLEES2);
                    }
                } else if (GLProfile.DEBUG) {
                    System.err.println("Info: EGLDrawableFactory: EGL ES2 - NOPE");
                }
            }
        }
    }

    @Override
    protected final void destroy(ShutdownType shutdownType) {
        if(null != sharedMap) {
            Collection<SharedResource> srl = sharedMap.values();
            for(Iterator<SharedResource> sri = srl.iterator(); sri.hasNext(); ) {
                SharedResource sr = sri.next();
                if(DEBUG) {
                    System.err.println("EGLDrawableFactory.destroy("+shutdownType+"): "+sr.device.toString());
                }
                sr.device.close();
            }
            sharedMap.clear();
            sharedMap = null;
        }
        defaultDevice = null;
        /**
         * Pulling away the native library may cause havoc ..
         */
        if(ShutdownType.COMPLETE == shutdownType) {
            if(null != eglES1DynamicLookupHelper) {
                // eglES1DynamicLookupHelper.destroy();
                eglES1DynamicLookupHelper = null;
            }
            if(null != eglES2DynamicLookupHelper) {
                // eglES2DynamicLookupHelper.destroy();
                eglES2DynamicLookupHelper = null;
            }
        }
        EGLGraphicsConfigurationFactory.unregisterFactory();
    }

    private HashMap<String /*connection*/, SharedResource> sharedMap = new HashMap<String /*connection*/, SharedResource>();
    private EGLGraphicsDevice defaultDevice;

    static class SharedResource {
      private final EGLGraphicsDevice device;
      // private final EGLDrawable drawable;
      // private final EGLContext contextES1;
      // private final EGLContext contextES2;
      private final boolean wasES1ContextCreated;
      private final boolean wasES2ContextCreated;

      SharedResource(EGLGraphicsDevice dev, boolean wasContextES1Created, boolean wasContextES2Created
                     /*EGLDrawable draw, EGLContext ctxES1, EGLContext ctxES2 */) {
          this.device = dev;
          // this.drawable = draw;
          // this.contextES1 = ctxES1;
          // this.contextES2 = ctxES2;
          this.wasES1ContextCreated = wasContextES1Created;
          this.wasES2ContextCreated = wasContextES2Created;
      }
      final EGLGraphicsDevice getDevice() { return device; }
      // final EGLDrawable getDrawable() { return drawable; }
      // final EGLContext getContextES1() { return contextES1; }
      // final EGLContext getContextES2() { return contextES2; }
      final boolean wasES1ContextAvailable() { return wasES1ContextCreated; }
      final boolean wasES2ContextAvailable() { return wasES2ContextCreated; }      
    }

    @Override
    public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
    }

    @Override
    public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      // via mappings (X11/WGL/.. -> EGL) we shall be able to handle all types.
      return null!=eglES2DynamicLookupHelper || null!=eglES1DynamicLookupHelper;
    }

    private boolean isEGLContextAvailable(AbstractGraphicsDevice adevice, EGLGraphicsDevice sharedEGLDevice, String profileString) {
        if( !GLProfile.isAvailable(adevice, profileString) ) {
            return false;
        }
        final GLProfile glp = GLProfile.get(adevice, profileString) ;
        final GLDrawableFactoryImpl desktopFactory = (GLDrawableFactoryImpl) GLDrawableFactory.getDesktopFactory();
        EGLGraphicsDevice eglDevice = null;
        NativeSurface surface = null;
        ProxySurface upstreamSurface = null; // X11, GLX, ..
        boolean success = false;
        boolean deviceFromUpstreamSurface = false;
        try {            
            final GLCapabilities caps = new GLCapabilities(glp);
            caps.setRedBits(5); caps.setGreenBits(5); caps.setBlueBits(5); caps.setAlphaBits(0);
            if(adevice instanceof EGLGraphicsDevice || null == desktopFactory || !QUERY_EGL_ES_NATIVE_TK) {
                eglDevice = sharedEGLDevice; // reuse
                surface = createDummySurfaceImpl(eglDevice, false, caps, null, 64, 64); // egl pbuffer offscreen
                upstreamSurface = (ProxySurface)surface;
                upstreamSurface.createNotify();
                deviceFromUpstreamSurface = false;
            } else {
                surface = desktopFactory.createDummySurface(adevice, caps, null, 64, 64); // X11, WGL, .. dummy window
                upstreamSurface = ( surface instanceof ProxySurface ) ? (ProxySurface)surface : null ;
                if(null != upstreamSurface) {
                    upstreamSurface.createNotify();
                }                    
                eglDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(surface, true);
                deviceFromUpstreamSurface = true;
            }
            
            final EGLDrawable drawable = (EGLDrawable) createOnscreenDrawableImpl ( surface );
            drawable.setRealized(true);
            final EGLContext context = (EGLContext) drawable.createContext(null);
            if (null != context) {
                try {
                    context.makeCurrent(); // could cause exception
                    success = context.isCurrent();
                    if(success) {
                        final String glVersion = context.getGL().glGetString(GL.GL_VERSION);
                        if(null == glVersion) {
                            // Oops .. something is wrong
                            if(DEBUG) {
                                System.err.println("EGLDrawableFactory.isEGLContextAvailable: "+eglDevice+", "+context.getGLVersion()+" - VERSION is null, dropping availability!");                                
                            }
                            success = false;
                        }
                    }
                    if(success) {
                        context.mapCurrentAvailableGLVersion(eglDevice);
                        if(eglDevice != adevice) {
                            context.mapCurrentAvailableGLVersion(adevice);
                        }
                    }
                } catch (GLException gle) {
                    if (DEBUG) {
                        System.err.println("EGLDrawableFactory.createShared: INFO: context create/makeCurrent failed");
                        gle.printStackTrace();
                    }
                } finally {
                    context.destroy();
                }
            }
            drawable.setRealized(false);
        } catch (Throwable t) {
            if(DEBUG) {
                System.err.println("Catched Exception:");
                t.printStackTrace();
            }
            success = false;
        } finally {
            if(eglDevice == sharedEGLDevice) {
                if(null != upstreamSurface) {
                    upstreamSurface.destroyNotify();
                }                
            } else if( deviceFromUpstreamSurface ) {
                if(null != eglDevice) {
                    eglDevice.close();
                }
                if(null != upstreamSurface) {
                    upstreamSurface.destroyNotify();
                }
            } else {
                if(null != upstreamSurface) {
                    upstreamSurface.destroyNotify();
                }                
                if(null != eglDevice) {
                    eglDevice.close();
                }
            }
        }
        return success;
    }

    /* package */ SharedResource getOrCreateEGLSharedResource(AbstractGraphicsDevice adevice) {
        if(null == eglES1DynamicLookupHelper && null == eglES2DynamicLookupHelper) {
            return null;
        }
        final String connection = adevice.getConnection();
        SharedResource sr;
        synchronized(sharedMap) {
            sr = sharedMap.get(connection);
        }
        if(null==sr) {
            final boolean madeCurrentES1;            
            final boolean madeCurrentES2;
            final EGLGraphicsDevice sharedDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(EGL.EGL_DEFAULT_DISPLAY, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
            
            if(QUERY_EGL_ES) {
                madeCurrentES1 = isEGLContextAvailable(adevice, sharedDevice, GLProfile.GLES1);
                madeCurrentES2 = isEGLContextAvailable(adevice, sharedDevice, GLProfile.GLES2);                
            } else {            
                madeCurrentES1 = true;            
                madeCurrentES2 = true;
                EGLContext.mapStaticGLESVersion(sharedDevice, 1);
                if(sharedDevice != adevice) {
                    EGLContext.mapStaticGLESVersion(adevice, 1);
                }
                EGLContext.mapStaticGLESVersion(sharedDevice, 2);
                if(sharedDevice != adevice) {
                    EGLContext.mapStaticGLESVersion(adevice, 2);
                }
            }
            
            if( !EGLContext.getAvailableGLVersionsSet(adevice) ) {
                // Even though we override the non EGL native mapping intentionally,
                // avoid exception due to double 'set' - carefull exception of the rule. 
                EGLContext.setAvailableGLVersionsSet(adevice);
            }
            sr = new SharedResource(sharedDevice, madeCurrentES1, madeCurrentES2);
            
            synchronized(sharedMap) {
                sharedMap.put(connection, sr);
                if(adevice != sharedDevice) {
                    sharedMap.put(sharedDevice.getConnection(), sr);
                }
            }
            if (DEBUG) {
                System.err.println("EGLDrawableFactory.createShared: devices:  queried " + QUERY_EGL_ES + "[nativeTK "+QUERY_EGL_ES_NATIVE_TK+"], " + adevice + ", " + sharedDevice);
                System.err.println("EGLDrawableFactory.createShared: context ES1: " + madeCurrentES1);
                System.err.println("EGLDrawableFactory.createShared: context ES2: " + madeCurrentES2);
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
            SharedResource sr = getOrCreateEGLSharedResource(device);
            if(null!=sr) {
                return sr.wasES1ContextAvailable() || sr.wasES2ContextAvailable();
            }
        } catch (GLException gle) {
            if(DEBUG) {
                System.err.println("Catched Exception while EGL Shared Resource initialization");
                gle.printStackTrace();
            }
        }
        return false;
    }

    @Override
    protected final GLContext getOrCreateSharedContextImpl(AbstractGraphicsDevice device) {
        return null; // n/a for EGL .. since we don't keep the resources
    }

    @Override
    protected AbstractGraphicsDevice getOrCreateSharedDeviceImpl(AbstractGraphicsDevice device) {
        SharedResource sr = getOrCreateEGLSharedResource(device);
        if(null!=sr) {
            return sr.getDevice();
        }
        return null;
    }

    public boolean isANGLE() {
        return isANGLE;
    }

    @Override
    public GLDynamicLookupHelper getGLDynamicLookupHelper(int esProfile) {
        if (2==esProfile) {
            return eglES2DynamicLookupHelper;
        } else if (1==esProfile) {
            return eglES1DynamicLookupHelper;
        } else {
            throw new GLException("Unsupported: ES"+esProfile);
        }
    }

    @Override
    protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(AbstractGraphicsDevice device) {
        if(null == eglES1DynamicLookupHelper && null == eglES2DynamicLookupHelper) {
            return new ArrayList<GLCapabilitiesImmutable>(); // null
        }
        return EGLGraphicsConfigurationFactory.getAvailableCapabilities(this, device);
    }

    @Override
    protected GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
        if (target == null) {
          throw new IllegalArgumentException("Null target");
        }
        return new EGLOnscreenDrawable(this, getEGLSurface(target));
    }
    
    protected static NativeSurface getEGLSurface(NativeSurface surface) {
        AbstractGraphicsConfiguration aConfig = surface.getGraphicsConfiguration();
        AbstractGraphicsDevice aDevice = aConfig.getScreen().getDevice();
        if( aDevice instanceof EGLGraphicsDevice && aConfig instanceof EGLGraphicsConfiguration ) {
            // already in native EGL format
            if(DEBUG) {
                System.err.println(getThreadName() + ": getEGLSurface - already in EGL format - use as-is: "+aConfig);
            }
            return surface;
        }
        // create EGL instance out of platform native types
        final EGLGraphicsDevice eglDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(surface, true);
        final AbstractGraphicsScreen eglScreen = new DefaultGraphicsScreen(eglDevice, aConfig.getScreen().getIndex());
        final GLCapabilitiesImmutable capsRequested = (GLCapabilitiesImmutable) aConfig.getRequestedCapabilities();
        final EGLGraphicsConfiguration eglConfig;
        if( aConfig instanceof EGLGraphicsConfiguration ) {
            // Config is already in EGL type - reuse ..
            final EGLGLCapabilities capsChosen = (EGLGLCapabilities) aConfig.getChosenCapabilities();
            if( 0 == capsChosen.getEGLConfig() ) {
                // 'refresh' the native EGLConfig handle
                capsChosen.setEGLConfig(EGLGraphicsConfiguration.EGLConfigId2EGLConfig(eglDevice.getHandle(), capsChosen.getEGLConfigID()));
                if( 0 == capsChosen.getEGLConfig() ) {
                    throw new GLException("Refreshing native EGLConfig handle failed: "+capsChosen+" of "+aConfig);
                }
            }
            eglConfig  = new EGLGraphicsConfiguration(eglScreen, capsChosen, capsRequested, null);
            if(DEBUG) {
                System.err.println(getThreadName() + ": getEGLSurface - Reusing chosenCaps: "+eglConfig);
            }
        } else {
            eglConfig = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
                    capsRequested, capsRequested, null, eglScreen, aConfig.getVisualID(VIDType.NATIVE), false);

            if (null == eglConfig) {
                throw new GLException("Couldn't create EGLGraphicsConfiguration from "+eglScreen);
            } else if(DEBUG) {
                System.err.println(getThreadName() + ": getEGLSurface - Chosen eglConfig: "+eglConfig);
            }
        }
        return new WrappedSurface(eglConfig, EGL.EGL_NO_SURFACE, surface.getWidth(), surface.getHeight(), new EGLUpstreamSurfaceHook(surface));
    }
    static String getThreadName() { return Thread.currentThread().getName(); }

    @Override
    protected GLDrawableImpl createOffscreenDrawableImpl(NativeSurface target) {
        if (target == null) {
          throw new IllegalArgumentException("Null target");
        }
        AbstractGraphicsConfiguration config = target.getGraphicsConfiguration();
        GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        if(!caps.isPBuffer()) {
            throw new GLException("Non pbuffer not yet implemented");
        }
        // PBuffer GLDrawable Creation
        return new EGLPbufferDrawable(this, target);
    }

    @Override
    public boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
        return true;
    }

    @Override
    protected ProxySurface createMutableSurfaceImpl(AbstractGraphicsDevice deviceReq, boolean createNewDevice, 
                                                    GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested, 
                                                    GLCapabilitiesChooser chooser, int width, int height, UpstreamSurfaceHook lifecycleHook) {
        final EGLGraphicsDevice device;
        if(createNewDevice) {
            final EGLGraphicsDevice eglDeviceReq = (EGLGraphicsDevice) deviceReq;
            device = EGLDisplayUtil.eglCreateEGLGraphicsDevice(eglDeviceReq.getNativeDisplayID(), deviceReq.getConnection(), deviceReq.getUnitID());
        } else {
            device = (EGLGraphicsDevice) deviceReq;
        }
        final DefaultGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
        final EGLGraphicsConfiguration config = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen, VisualIDHolder.VID_UNDEFINED, false);
        if(null == config) {
            throw new GLException("Choosing GraphicsConfiguration failed w/ "+capsChosen+" on "+screen); 
        }    
        return new WrappedSurface(config, 0, width, height, lifecycleHook);
    }
    
    @Override
    public final ProxySurface createDummySurfaceImpl(AbstractGraphicsDevice deviceReq, boolean createNewDevice, 
                                                     GLCapabilitiesImmutable requestedCaps, GLCapabilitiesChooser chooser, int width, int height) {
        final GLCapabilitiesImmutable chosenCaps = GLGraphicsConfigurationUtil.fixOffscreenGLCapabilities(requestedCaps, false, canCreateGLPbuffer(deviceReq));        
        return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, width, height, dummySurfaceLifecycleHook);
    }
    private static final ProxySurface.UpstreamSurfaceHook dummySurfaceLifecycleHook = new ProxySurface.UpstreamSurfaceHook() {
        @Override
        public final void create(ProxySurface s) {
            if( EGL.EGL_NO_SURFACE == s.getSurfaceHandle() ) {
                final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) s.getGraphicsConfiguration().getScreen().getDevice();
                if(0 == eglDevice.getHandle()) {
                    eglDevice.open();
                    s.setImplBitfield(ProxySurface.OWN_DEVICE);
                }
                createPBufferSurfaceImpl(s, false);
                if(DEBUG) {
                    System.err.println("EGLDrawableFactory.dummySurfaceLifecycleHook.create: "+s);
                }
            }
        }
        @Override
        public final void destroy(ProxySurface s) {
            if( EGL.EGL_NO_SURFACE != s.getSurfaceHandle() ) {
                final EGLGraphicsConfiguration config = (EGLGraphicsConfiguration) s.getGraphicsConfiguration();
                final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) config.getScreen().getDevice();
                EGL.eglDestroySurface(eglDevice.getHandle(), s.getSurfaceHandle());
                s.setSurfaceHandle(EGL.EGL_NO_SURFACE);
                if( 0 != ( ProxySurface.OWN_DEVICE & s.getImplBitfield() ) ) {
                    eglDevice.close();
                }
                if(DEBUG) {
                    System.err.println("EGLDrawableFactory.dummySurfaceLifecycleHook.create: "+s);
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
            return "EGLSurfaceLifecycleHook[]";
        }
        
    };
    
    /**
     * @param ms {@link MutableSurface} which dimensions and config are being used to create the pbuffer surface. 
     *           It will also hold the resulting pbuffer surface handle. 
     * @param useTexture
     * @return the passed {@link MutableSurface} which now has the EGL pbuffer surface set as it's handle
     */
    protected static MutableSurface createPBufferSurfaceImpl(MutableSurface ms, boolean useTexture) {
        final EGLGraphicsConfiguration config = (EGLGraphicsConfiguration) ms.getGraphicsConfiguration();
        final EGLGraphicsDevice eglDevice = (EGLGraphicsDevice) config.getScreen().getDevice();
        final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        final int texFormat;

        if(useTexture) {
            texFormat = caps.getAlphaBits() > 0 ? EGL.EGL_TEXTURE_RGBA : EGL.EGL_TEXTURE_RGB ;
        } else {
            texFormat = EGL.EGL_NO_TEXTURE;
        }

        if (DEBUG) {
          System.out.println("Pbuffer config: " + config);
        }

        final int[] attrs = EGLGraphicsConfiguration.CreatePBufferSurfaceAttribList(ms.getWidth(), ms.getHeight(), texFormat);
        final long surf = EGL.eglCreatePbufferSurface(eglDevice.getHandle(), config.getNativeConfig(), attrs, 0);
        if (EGL.EGL_NO_SURFACE==surf) {
            throw new GLException("Creation of window surface (eglCreatePbufferSurface) failed, dim "+ms.getWidth()+"x"+ms.getHeight()+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        } else if(DEBUG) {
            System.err.println("PBuffer setSurface result: eglSurface 0x"+Long.toHexString(surf));
        }
        ms.setSurfaceHandle(surf);
        return ms;
    }

    @Override
    protected ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice deviceReq, int screenIdx, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstream) {
        final EGLGraphicsDevice eglDeviceReq = (EGLGraphicsDevice) deviceReq;
        final EGLGraphicsDevice device = EGLDisplayUtil.eglCreateEGLGraphicsDevice(eglDeviceReq.getNativeDisplayID(), deviceReq.getConnection(), deviceReq.getUnitID());
        final DefaultGraphicsScreen screen = new DefaultGraphicsScreen(device, screenIdx);
        final EGLGraphicsConfiguration cfg = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, VisualIDHolder.VID_UNDEFINED, false);
        return new WrappedSurface(cfg, windowHandle, 0, 0, upstream);
    }

    @Override
    protected GLContext createExternalGLContextImpl() {
        AbstractGraphicsScreen absScreen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_EGL);
        return new EGLExternalContext(absScreen);
    }

    @Override
    public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
        return false;
    }

    @Override
    protected GLDrawable createExternalGLDrawableImpl() {
        throw new GLException("Not yet implemented");
    }

    @Override
    public boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
        return false;
    }

    @Override
    public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
        throws GLException {
        throw new GLException("Unimplemented on this platform");
    }
}
