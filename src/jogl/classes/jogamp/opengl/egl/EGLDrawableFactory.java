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
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLProfile.ShutdownType;

import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;

import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.WrappedSurface;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

public class EGLDrawableFactory extends GLDrawableFactoryImpl {
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
                final long eglDisplay = sr.device.getHandle();
                if(EGL.EGL_NO_DISPLAY != eglDisplay) {
                    EGLDisplayUtil.eglTerminate(eglDisplay);
                }
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

    /**
    private boolean isEGLContextAvailable(EGLGraphicsDevice sharedDevice, String profile) {
        boolean madeCurrent = false;
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(profile));
        caps.setRedBits(5); caps.setGreenBits(5); caps.setBlueBits(5); caps.setAlphaBits(0);
        caps.setDoubleBuffered(false);
        caps.setOnscreen(false);
        caps.setPBuffer(true);
        final EGLDrawable drawable = (EGLDrawable) createGLDrawable( createOffscreenSurfaceImpl(sharedDevice, caps, caps, null, 64, 64) );
        if(null!=drawable) {
            final EGLContext context = (EGLContext) drawable.createContext(null);
            if (null != context) {
                context.setSynchronized(true);
                try {
                    context.makeCurrent(); // could cause exception
                    madeCurrent = context.isCurrent();
                } catch (GLException gle) {
                    if (DEBUG) {
                        System.err.println("EGLDrawableFactory.createShared: INFO: makeCurrent failed");
                        gle.printStackTrace();
                    }
                } finally {
                    context.destroy();
                }
            }
            drawable.destroy();
        }
        return madeCurrent;
    } */

    /* package */ SharedResource getOrCreateEGLSharedResource(AbstractGraphicsDevice adevice) {
        if(null == eglES1DynamicLookupHelper && null == eglES2DynamicLookupHelper) {
            return null;
        }
        String connection = adevice.getConnection();
        SharedResource sr;
        synchronized(sharedMap) {
            sr = sharedMap.get(connection);
        }
        if(null==sr) {
            long eglDisplay = EGLDisplayUtil.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY);
            if (eglDisplay == EGL.EGL_NO_DISPLAY) {
                throw new GLException("Failed to created EGL default display: error 0x"+Integer.toHexString(EGL.eglGetError()));
            } else if(DEBUG) {
                System.err.println("EGLDrawableFactory.createShared: eglDisplay(EGL_DEFAULT_DISPLAY): 0x"+Long.toHexString(eglDisplay));
            }
            if (!EGLDisplayUtil.eglInitialize(eglDisplay, null, null)) {
                throw new GLException("eglInitialize failed"+", error 0x"+Integer.toHexString(EGL.eglGetError()));
            }
            final EGLGraphicsDevice sharedDevice = new EGLGraphicsDevice(eglDisplay, connection, adevice.getUnitID());
            // final boolean madeCurrentES1 = isEGLContextAvailable(sharedDevice, GLProfile.GLES1);
            // final boolean madeCurrentES2 = isEGLContextAvailable(sharedDevice, GLProfile.GLES2);
            final boolean madeCurrentES1 = true; // FIXME
            final boolean madeCurrentES2 = true; // FIXME
            sr = new SharedResource(sharedDevice, madeCurrentES1, madeCurrentES2);
            synchronized(sharedMap) {
                sharedMap.put(connection, sr);
            }
            if (DEBUG) {
                System.err.println("EGLDrawableFactory.createShared: device:  " + sharedDevice);
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
        return new EGLOnscreenDrawable(this, target);
    }

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
    protected NativeSurface createOffscreenSurfaceImpl(AbstractGraphicsDevice device, GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser, int width, int height) {
        WrappedSurface ns = new WrappedSurface(EGLGraphicsConfigurationFactory.createOffscreenGraphicsConfiguration(device, capsChosen, capsRequested, chooser));
        ns.surfaceSizeChanged(width, height);
        return ns;
    }

    @Override
    protected ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice adevice, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser) {
        // FIXME device/windowHandle -> screen ?!
        EGLGraphicsDevice device = (EGLGraphicsDevice) adevice;
        DefaultGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
        EGLGraphicsConfiguration cfg = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, VisualIDHolder.VID_UNDEFINED, false);
        WrappedSurface ns = new WrappedSurface(cfg, windowHandle);
        return ns;
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
