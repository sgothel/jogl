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

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.DefaultGraphicsScreen;
import com.jogamp.nativewindow.MutableSurface;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.common.os.PlatformPropsImpl;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLContextImpl.MappedGLVersion;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.SharedResourceRunner;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.GenericUpstreamSurfacelessHook;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.egl.EGL;

public class EGLDrawableFactory extends GLDrawableFactoryImpl {
    protected static final boolean DEBUG = GLDrawableFactoryImpl.DEBUG; // allow package access
    private static final boolean DEBUG_SHAREDCTX = DEBUG  || GLContext.DEBUG;

    static {
        Debug.initSingleton();
    }

    private static boolean eglDynamicLookupHelperInit = false;
    private static GLDynamicLookupHelper eglES1DynamicLookupHelper = null;
    private static GLDynamicLookupHelper eglES2DynamicLookupHelper = null;
    private static GLDynamicLookupHelper eglGLnDynamicLookupHelper = null;
    private static boolean isANGLE = false;
    private static boolean hasX11 = false;
    private static String defaultConnection = null;
    private static EGLGraphicsDevice defaultDevice = null;
    private static EGLFeatures defaultDeviceEGLFeatures = null;
    private static SharedResource defaultSharedResource = null;

    private static final boolean isANGLE(final GLDynamicLookupHelper dl) {
        if(Platform.OSType.WINDOWS == PlatformPropsImpl.OS_TYPE) {
            return dl.isFunctionAvailable("eglQuerySurfacePointerANGLE") ||
                   dl.isFunctionAvailable("glBlitFramebufferANGLE") ||
                   dl.isFunctionAvailable("glRenderbufferStorageMultisampleANGLE");
        } else {
            return false;
        }
    }

    private static final boolean includesES1(final GLDynamicLookupHelper dl) {
        return dl.isFunctionAvailable("glLoadIdentity") &&
               dl.isFunctionAvailable("glEnableClientState") &&
               dl.isFunctionAvailable("glColorPointer");
    }

    private static class EGLFeatures {
        public final String vendor;
        public final VersionNumber version;
        public final boolean hasGLAPI;
        public final boolean hasKHRCreateContext;
        public final boolean hasKHRSurfaceless;

        public EGLFeatures(final EGLGraphicsDevice device) {
            final long eglDisplay = device.getHandle();
            vendor = EGL.eglQueryString(eglDisplay, EGL.EGL_VENDOR);
            if(DEBUG_SHAREDCTX) {
                System.err.println("EGLFeatures on device "+device+", vendor "+vendor);
            }
            version = device.getEGLVersion();
            final boolean hasEGL_1_4 = version.compareTo(GLContext.Version1_4) >= 0;
            final boolean hasEGL_1_5 = version.compareTo(GLContext.Version1_5) >= 0;
            {
                boolean _hasGLAPI = false;
                final String eglClientAPIStr = EGL.eglQueryString(eglDisplay, EGL.EGL_CLIENT_APIS);
                if( hasEGL_1_4 ) {
                    final String[] eglClientAPIs = eglClientAPIStr.split("\\s");
                    for(int i=eglClientAPIs.length-1; !_hasGLAPI && i>=0; i--) {
                        _hasGLAPI = eglClientAPIs[i].equals("OpenGL");
                    }
                }
                hasGLAPI = _hasGLAPI;
                if(DEBUG_SHAREDCTX) {
                    System.err.println("  Client APIs: '"+eglClientAPIStr+"'; has EGL 1.4 "+hasEGL_1_4+" -> has OpenGL "+hasGLAPI);
                }
            }
            {
                final String extensions = EGLContext.getPlatformExtensionsStringImpl(device).toString();
                if( hasEGL_1_5 ) {
                    // subsumed in EGL 1.5
                    hasKHRCreateContext = true;
                    hasKHRSurfaceless = true;
                } else {
                    if( hasEGL_1_4 ) {
                        // requires EGL 1.4
                        hasKHRCreateContext = extensions.contains("EGL_KHR_create_context");
                    } else {
                        hasKHRCreateContext = false;
                    }
                    hasKHRSurfaceless = extensions.contains("EGL_KHR_surfaceless_context");
                }
                if(DEBUG_SHAREDCTX) {
                    System.err.println("  Extensions: "+extensions);
                    System.err.println("  KHR_create_context: "+hasKHRCreateContext);
                    System.err.println("  KHR_surfaceless_context: "+hasKHRSurfaceless);
                }
            }
        }
        public final String toString() {
            return "EGLFeatures[vendor "+vendor+", version "+version+
                   ", has[GL-API "+hasGLAPI+", KHR[CreateContext "+hasKHRCreateContext+", Surfaceless "+hasKHRSurfaceless+"]]]";
        }
    }

    static class EGLAcc extends EGL {
      protected static boolean resetProcAddressTable(final DynamicLookupHelper lookup) {
          return EGL.resetProcAddressTable(lookup);
      }
    }
    static final String eglInitializeFuncName = "eglInitialize";

    public EGLDrawableFactory() {
        super();

        synchronized(EGLDrawableFactory.class) {
            if( eglDynamicLookupHelperInit ) {
                return;
            }
            eglDynamicLookupHelperInit = true;

            // Check for other underlying stuff ..
            final String nwt = NativeWindowFactory.getNativeWindowType(true);
            if(NativeWindowFactory.TYPE_X11 == nwt) {
                hasX11 = true;
                try {
                    ReflectionUtil.createInstance("jogamp.opengl.x11.glx.X11GLXGraphicsConfigurationFactory", EGLDrawableFactory.class.getClassLoader());
                } catch (final Exception jre) { /* n/a .. */ }
            } else {
                hasX11 = false;
            }
            defaultConnection = NativeWindowFactory.getDefaultDisplayConnection(nwt);

            /**
             * FIXME: Probably need to move EGL from a static model
             * to a dynamic one, where there can be 2 instances
             * for each ES profile with their own ProcAddressTable.
             *
             * Since EGL is designed to be static
             * we validate the function address of 'eglInitialize'
             * with all EGL/ES and EGL/GL combinations.
             * In case this address doesn't match the primary tuple EGL/ES2
             * the profile is skipped!
             */
            boolean eglTableReset = false;
            long eglInitializeAddress = 0;
            // Setup: eglES2DynamicLookupHelper[, eglES1DynamicLookupHelper]
            {
                GLDynamicLookupHelper tmp=null;
                try {
                    tmp = new GLDynamicLookupHelper(new EGLES2DynamicLibraryBundleInfo());
                } catch (final GLException gle) {
                    if(DEBUG) {
                        gle.printStackTrace();
                    }
                }
                if( null != tmp && tmp.isLibComplete() && true == ( eglTableReset = EGLAcc.resetProcAddressTable(tmp) ) ) {
                    eglInitializeAddress = tmp.dynamicLookupFunction(eglInitializeFuncName);
                    eglES2DynamicLookupHelper = tmp;
                    final boolean includesES1 = null == eglES1DynamicLookupHelper && includesES1(eglES2DynamicLookupHelper);
                    if(includesES1) {
                        eglES1DynamicLookupHelper = tmp;
                    }
                    final boolean isANGLEES2 = isANGLE(eglES2DynamicLookupHelper);
                    isANGLE |= isANGLEES2;
                    if (DEBUG || GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory: EGL ES2 - OK (includesES1 "+includesES1+", isANGLE: "+isANGLEES2+", eglInitialize 0x"+Long.toHexString(eglInitializeAddress)+")");
                        if(includesES1) {
                            System.err.println("Info: EGLDrawableFactory: EGL ES1 - OK (ES2 lib)");
                        }
                    }
                } else if (DEBUG || GLProfile.DEBUG) {
                    System.err.println("Info: EGLDrawableFactory: EGL ES2 - NOPE");
                }
            }
            // Setup: eglES1DynamicLookupHelper
            if( null == eglES1DynamicLookupHelper ) {
                GLDynamicLookupHelper tmp=null;
                try {
                    tmp = new GLDynamicLookupHelper(new EGLES1DynamicLibraryBundleInfo());
                } catch (final GLException gle) {
                    if(DEBUG) {
                        gle.printStackTrace();
                    }
                }
                if( null != tmp && tmp.isLibComplete() ) {
                    final boolean ok;
                    final long _eglInitializeAddress;
                    if( !eglTableReset ) {
                        if( true == ( eglTableReset = EGLAcc.resetProcAddressTable(tmp) ) ) {
                            _eglInitializeAddress = tmp.dynamicLookupFunction(eglInitializeFuncName);
                            eglInitializeAddress = _eglInitializeAddress;
                            ok = true;
                        } else {
                            _eglInitializeAddress = 0;
                            ok = false;
                        }
                    } else {
                        _eglInitializeAddress = tmp.dynamicLookupFunction(eglInitializeFuncName);
                        ok = _eglInitializeAddress == eglInitializeAddress;
                    }
                    if( ok ) {
                        eglES1DynamicLookupHelper = tmp;
                        final boolean isANGLEES1 = isANGLE(eglES1DynamicLookupHelper);
                        isANGLE |= isANGLEES1;
                        if (DEBUG || GLProfile.DEBUG) {
                            System.err.println("Info: EGLDrawableFactory: EGL ES1 - OK (isANGLE: "+isANGLEES1+", eglTableReset "+eglTableReset+", eglInitialize 0x"+Long.toHexString(_eglInitializeAddress)+")");
                        }
                    } else if (DEBUG || GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory: EGL ES1 - NOPE (ES1 proc, eglTableReset "+eglTableReset+", eglInitialize 0x"+Long.toHexString(_eglInitializeAddress)+")");
                    }
                } else if (DEBUG || GLProfile.DEBUG) {
                    System.err.println("Info: EGLDrawableFactory: EGL ES1 - NOPE (ES1 lib)");
                }
            }
            // Setup: eglGLnDynamicLookupHelper
            if( null == eglGLnDynamicLookupHelper ) {
                if( !GLProfile.disableOpenGLDesktop ) {
                    GLDynamicLookupHelper tmp=null;
                    try {
                        tmp = new GLDynamicLookupHelper(new EGLGLnDynamicLibraryBundleInfo());
                    } catch (final GLException gle) {
                        if(DEBUG) {
                            gle.printStackTrace();
                        }
                    }
                    if( null != tmp && tmp.isLibComplete() ) {
                        final boolean ok;
                        final long _eglInitializeAddress;
                        if( !eglTableReset ) {
                            if( true == ( eglTableReset = EGLAcc.resetProcAddressTable(tmp) ) ) {
                                _eglInitializeAddress = tmp.dynamicLookupFunction(eglInitializeFuncName);
                                eglInitializeAddress = _eglInitializeAddress;
                                ok = true;
                            } else {
                                _eglInitializeAddress = 0;
                                ok = false;
                            }
                        } else {
                            _eglInitializeAddress = tmp.dynamicLookupFunction(eglInitializeFuncName);
                            ok = _eglInitializeAddress == eglInitializeAddress;
                        }
                        if( ok ) {
                            eglGLnDynamicLookupHelper = tmp;
                            if (DEBUG || GLProfile.DEBUG) {
                                System.err.println("Info: EGLDrawableFactory: EGL GLn - OK (eglTableReset "+eglTableReset+", eglInitialize 0x"+Long.toHexString(_eglInitializeAddress)+")");
                            }
                        } else if (DEBUG || GLProfile.DEBUG) {
                            System.err.println("Info: EGLDrawableFactory: EGL GLn - NOPE (GLn proc, eglTableReset "+eglTableReset+", eglInitialize 0x"+Long.toHexString(_eglInitializeAddress)+")");
                        }
                    } else if (DEBUG || GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory: EGL GLn - NOPE (GLn lib)");
                    }
                } else if( DEBUG || GLProfile.DEBUG ) {
                    System.err.println("Info: EGLDrawableFactory: EGL Gln - disabled!");
                }
            }
            if( null != eglES2DynamicLookupHelper || null != eglES1DynamicLookupHelper || null != eglGLnDynamicLookupHelper ) {
                if(isANGLE && !GLProfile.enableANGLE) {
                    if(DEBUG || GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory.init - EGL/ES2 ANGLE disabled");
                    }
                } else {
                    if( isANGLE && ( DEBUG || GLProfile.DEBUG ) ) {
                        System.err.println("Info: EGLDrawableFactory.init - EGL/ES2 ANGLE enabled");
                    }
                    // Register our GraphicsConfigurationFactory implementations
                    // The act of constructing them causes them to be registered
                    EGLGraphicsConfigurationFactory.registerFactory();

                    // Note: defaultDevice.open() triggers eglInitialize(..) which crashed on Windows w/ Chrome/ANGLE, FF/ANGLE!
                    // Hence opening will happen later, eventually
                    defaultDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(EGL.EGL_DEFAULT_DISPLAY, defaultConnection, AbstractGraphicsDevice.DEFAULT_UNIT);

                    // Init shared resources off thread
                    // Will be released via ShutdownHook
                    sharedResourceImplementation = new SharedResourceImplementation();
                    sharedResourceRunner = new SharedResourceRunner(sharedResourceImplementation);
                    sharedResourceRunner.start();
                }
            }
        } // synchronized(EGLDrawableFactory.class)
    }

    @Override
    protected final boolean isComplete() {
        return null != sharedResourceImplementation; // null != eglES2DynamicLookupHelper || null != eglES1DynamicLookupHelper || ..;
    }


    @Override
    protected final void shutdownImpl() {
        if( DEBUG ) {
            System.err.println("EGLDrawableFactory.shutdown");
        }
        if(null != sharedResourceRunner) {
            sharedResourceRunner.stop();
            sharedResourceRunner = null;
        }
        if(null != sharedResourceImplementation) {
            sharedResourceImplementation.clear();
            sharedResourceImplementation = null;
        }

        if(null != defaultDevice) {
            defaultDevice.close();
            defaultDevice = null;
        }
        /**
         * Pulling away the native library may cause havoc ..
         */
        if(null != eglES1DynamicLookupHelper) {
            // eglES1DynamicLookupHelper.destroy();
            eglES1DynamicLookupHelper = null;
        }
        if(null != eglES2DynamicLookupHelper) {
            // eglES2DynamicLookupHelper.destroy();
            eglES2DynamicLookupHelper = null;
        }
        if(null != eglGLnDynamicLookupHelper) {
            // eglGLDynamicLookupHelper.destroy();
            eglGLnDynamicLookupHelper = null;
        }
        EGLGraphicsConfigurationFactory.unregisterFactory();
        EGLDisplayUtil.shutdown(DEBUG);
    }

    private void dumpMap() {
        synchronized(sharedResourceImplementation) {
            final Map<String /* uniqueId */, SharedResourceRunner.Resource> sharedMap = sharedResourceImplementation.getSharedMap();
            System.err.println("EGLDrawableFactory.MapGLVersion.map "+sharedMap.size());
            int i=0;
            final Set<String> keys = sharedMap.keySet();
            for(final Iterator<String> keyI = keys.iterator(); keyI.hasNext(); i++) {
                final String key = keyI.next();
                final SharedResource sr = (SharedResource) sharedMap.get(key);
                System.err.println("EGLDrawableFactory.MapGLVersion.map["+i+"] "+key+" -> "+sr.getDevice()+", avail "+sr.isAvailable+", "+
                                   "es1 [avail "+sr.isAvailableES1+", quirks "+sr.rendererQuirksES1+", ctp "+EGLContext.getGLVersion(1, 0, sr.ctpES1, null)+"], "+
                                   "es2 [avail "+sr.isAvailableES2+", quirks "+sr.rendererQuirksES2+", ctp "+EGLContext.getGLVersion(2, 0, sr.ctpES2, null)+"], "+
                                   "es3 [avail "+sr.isAvailableES3+", quirks "+sr.rendererQuirksES3+", ctp "+EGLContext.getGLVersion(2, 0, sr.ctpES3, null)+"], "+
                                   "gln [avail "+sr.isAvailableGLn+", quirks "+sr.rendererQuirksGLn+", ctp "+EGLContext.getGLVersion(3, 0, sr.ctpGLn, null)+"]");
            }
            ;
        }
    }

    private SharedResourceImplementation sharedResourceImplementation;
    private SharedResourceRunner sharedResourceRunner;

    static class SharedResource implements SharedResourceRunner.Resource {
      private EGLGraphicsDevice device;
      final boolean isAvailable;
      final boolean isAvailableES1;
      final boolean isAvailableES2;
      final boolean isAvailableES3;
      final boolean isAvailableGLn;
      final GLRendererQuirks rendererQuirksES1;
      final GLRendererQuirks rendererQuirksES2;
      final GLRendererQuirks rendererQuirksES3;
      final GLRendererQuirks rendererQuirksGLn;
      final int ctpES1;
      final int ctpES2;
      final int ctpES3;
      final int ctpGLn;

      SharedResource(final EGLGraphicsDevice dev,
                     final boolean isAvailableES1, final GLRendererQuirks rendererQuirksES1, final int ctpES1,
                     final boolean isAvailableES2, final GLRendererQuirks rendererQuirksES2, final int ctpES2,
                     final boolean isAvailableES3, final GLRendererQuirks rendererQuirksES3, final int ctpES3,
                     final boolean isAvailableGLn, final GLRendererQuirks rendererQuirksGLn, final int ctpGLn) {
          this.device = dev;
          this.isAvailable = isAvailableES1 || isAvailableES2 || isAvailableES3 || isAvailableGLn;

          this.isAvailableES1 = isAvailableES1;
          this.rendererQuirksES1 = rendererQuirksES1;
          this.ctpES1 = ctpES1;
          this.isAvailableES2 = isAvailableES2;
          this.rendererQuirksES2 = rendererQuirksES2;
          this.ctpES2 = ctpES2;
          this.isAvailableES3 = isAvailableES3;
          this.rendererQuirksES3 = rendererQuirksES3;
          this.ctpES3 = ctpES3;
          this.isAvailableGLn = isAvailableGLn;
          this.rendererQuirksGLn = rendererQuirksGLn;
          this.ctpGLn = ctpGLn;
      }

      @Override
      public final boolean isAvailable() {
          return isAvailable;
      }
      @Override
      public final EGLGraphicsDevice getDevice() { return device; }

      @Override
      public AbstractGraphicsScreen getScreen() {
          return null;
      }
      @Override
      public GLDrawableImpl getDrawable() {
          return null;
      }
      @Override
      public GLContextImpl getContext() {
          return null;
      }
      @Override
      public GLRendererQuirks getRendererQuirks(final GLProfile glp) {
          if( null == glp ) {
              if( null != rendererQuirksES3 ) {
                  return rendererQuirksES3;
              } else if( null != rendererQuirksES2 ) {
                  return rendererQuirksES2;
              } else if( null != rendererQuirksES1 ) {
                  return rendererQuirksES1;
              } else {
                  return rendererQuirksGLn;
              }
          } else if( !glp.isGLES() ) {
              return rendererQuirksGLn;
          } else if( glp.isGLES1() ) {
              return rendererQuirksES1;
          } else if( glp.isGLES2() ) {
              return rendererQuirksES2;
          } else /* if( glp.isGLES3() ) */ {
              return rendererQuirksES3;
          }
      }
  }

  class SharedResourceImplementation extends SharedResourceRunner.AImplementation {
        @Override
        public boolean isDeviceSupported(final AbstractGraphicsDevice device) {
            return null != sharedResourceImplementation; // null != eglES2DynamicLookupHelper || null != eglES1DynamicLookupHelper || ..
        }

        @Override
        public SharedResourceRunner.Resource createSharedResource(final AbstractGraphicsDevice adevice) {
            adevice.lock();
            try {
                return createEGLSharedResourceImpl(adevice);
            } catch (final Throwable t) {
                throw new GLException("EGLGLXDrawableFactory - Could not initialize shared resources for "+adevice, t);
            } finally {
                adevice.unlock();
            }
        }

        private SharedResource createEGLSharedResourceImpl(final AbstractGraphicsDevice adevice) {
            if ( DEBUG_SHAREDCTX ) {
                System.err.println("EGLDrawableFactory.MapGLVersions: device "+adevice);
            }

            final boolean initDefaultDevice;
            if( 0 == defaultDevice.getHandle() ) { // Note: GLProfile always triggers EGL device initialization first!
                initDefaultDevice = true;
                defaultDevice.open();
                defaultDeviceEGLFeatures = new EGLFeatures(defaultDevice);
                if ( DEBUG_SHAREDCTX ) {
                    System.err.println("EGLDrawableFactory.MapGLVersions: defaultDevice "+defaultDevice);
                    System.err.println("EGLDrawableFactory.MapGLVersions: defaultDevice EGLFeatures "+defaultDeviceEGLFeatures);
                }
                // Probe for GLRendererQuirks.SingletonEGLDisplayOnly
                final boolean singletonEGLDisplayOnlyVendor, singletonEGLDisplayOnlyProbe;
                if( defaultDeviceEGLFeatures.vendor.contains("NVIDIA") ) { // OpenGL ES 3.1 NVIDIA 355.06 unstable
                    singletonEGLDisplayOnlyVendor=true;
                    singletonEGLDisplayOnlyProbe=false;
                } else {
                    singletonEGLDisplayOnlyVendor=false;
                    final long secondEGLDisplay = EGL.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY);
                    singletonEGLDisplayOnlyProbe = EGL.EGL_NO_DISPLAY == secondEGLDisplay;
                }
                if( singletonEGLDisplayOnlyVendor || singletonEGLDisplayOnlyProbe ) {
                    final int quirk = GLRendererQuirks.SingletonEGLDisplayOnly;
                    GLRendererQuirks.addStickyDeviceQuirk(adevice, quirk);
                    EGLDisplayUtil.setSingletonEGLDisplayOnly(true);
                    if ( DEBUG_SHAREDCTX ) {
                        if( singletonEGLDisplayOnlyVendor ) {
                            System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Vendor: "+defaultDeviceEGLFeatures);
                        } else if( singletonEGLDisplayOnlyProbe ) {
                            System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Second eglGetDisplay(EGL_DEFAULT_DISPLAY) failed");
                        }
                    }
                }
            } else {
                initDefaultDevice = false;
                if( null == defaultSharedResource ) {
                    throw new InternalError("XXX: defaultDevice "+defaultDevice+", adevice "+adevice);
                }
            }

            final boolean[] mappedToDefaultDevice = { false };
            final GLRendererQuirks[] rendererQuirksES1 = new GLRendererQuirks[] { null };
            final GLRendererQuirks[] rendererQuirksES2 = new GLRendererQuirks[] { null };
            final GLRendererQuirks[] rendererQuirksES3 = new GLRendererQuirks[] { null };
            final GLRendererQuirks[] rendererQuirksGLn = new GLRendererQuirks[] { null };
            final int[] ctpES1 = new int[] { 0 };
            final int[] ctpES2 = new int[] { 0 };
            final int[] ctpES3 = new int[] { 0 };
            final int[] ctpGLn = new int[] { 0 };
            final boolean[] madeCurrentES1 = { false };
            final boolean[] madeCurrentES2 = { false };
            final boolean[] madeCurrentES3 = { false };
            final boolean[] madeCurrentGLn = { false };

            final GLContextImpl.MappedGLVersionListener mvl = new GLContextImpl.MappedGLVersionListener() {
                @Override
                public void glVersionMapped(final MappedGLVersion e) {
                    if ( DEBUG_SHAREDCTX ) {
                        System.err.println("EGLDrawableFactory.MapGLVersions: Mapped: "+e);
                    }
                    if ( EGLContext.isGLES2ES3(e.ctxVersion.getMajor(), e.ctxOptions) ) {
                        if( e.ctxVersion.getMajor() == 3 ) {
                            madeCurrentES3[0] = true;
                            rendererQuirksES3[0] = e.quirks;
                            ctpES3[0] = e.ctxOptions;
                        }
                        madeCurrentES2[0] = true;
                        rendererQuirksES2[0] = e.quirks;
                        ctpES2[0] = e.ctxOptions;
                    } else if ( EGLContext.isGLES1(e.ctxVersion.getMajor(), e.ctxOptions) ) {
                        madeCurrentES1[0] = true;
                        rendererQuirksES1[0] = e.quirks;
                        ctpES1[0] = e.ctxOptions;
                    } else if( EGLContext.isGLDesktop(e.ctxOptions) ) {
                        madeCurrentGLn[0] = true;
                        rendererQuirksGLn[0] = e.quirks;
                        ctpGLn[0] = e.ctxOptions;
                    }
                }
            };
            final SharedResource sr;
            final EGLGraphicsDevice[] eglDevice = { null };
            final boolean mapSuccess;
            EGLContext.setMappedGLVersionListener(mvl);
            try {
                // Query triggers profile mapping!
                mapSuccess = mapAvailableEGLESConfig(adevice, mappedToDefaultDevice, eglDevice);
            } finally {
                EGLContext.setMappedGLVersionListener(null);
            }

            if( mappedToDefaultDevice[0] ) {
                EGLContext.remapAvailableGLVersions(defaultDevice, adevice);
                sr = defaultSharedResource;
            } else {
                if( hasX11 ) {
                    handleDontCloseX11DisplayQuirk(rendererQuirksES1[0]);
                    handleDontCloseX11DisplayQuirk(rendererQuirksGLn[0]);
                    handleDontCloseX11DisplayQuirk(rendererQuirksES3[0]);
                    handleDontCloseX11DisplayQuirk(rendererQuirksES2[0]);
                }
                sr = new SharedResource(eglDevice[0],
                            madeCurrentES1[0], rendererQuirksES1[0], ctpES1[0],
                            madeCurrentES2[0], rendererQuirksES2[0], ctpES2[0],
                            madeCurrentES3[0], rendererQuirksES3[0], ctpES3[0],
                            madeCurrentGLn[0], rendererQuirksGLn[0], ctpGLn[0]);
                if( initDefaultDevice ) {
                    defaultSharedResource = sr;
                }
            }

            if ( DEBUG_SHAREDCTX ) {
                System.err.println("EGLDrawableFactory.MapGLVersions: mapSuccess "+mapSuccess+", mappedToDefaultDevice "+mappedToDefaultDevice[0]);
                System.err.println("EGLDrawableFactory.MapGLVersions: defDevice  : " + defaultDevice);
                System.err.println("EGLDrawableFactory.MapGLVersions: adevice    : " + adevice);
                System.err.println("EGLDrawableFactory.MapGLVersions: eglDevice  : " + sr.device);
                System.err.println("EGLDrawableFactory.MapGLVersions: context ES1: " + sr.isAvailableES1 + ", quirks "+sr.rendererQuirksES1);
                System.err.println("EGLDrawableFactory.MapGLVersions: context ES2: " + sr.isAvailableES2 + ", quirks "+sr.rendererQuirksES2);
                System.err.println("EGLDrawableFactory.MapGLVersions: context ES3: " + sr.isAvailableES3 + ", quirks "+sr.rendererQuirksES3);
                System.err.println("EGLDrawableFactory.MapGLVersions: context GLn: " + sr.isAvailableGLn + ", quirks "+sr.rendererQuirksGLn);
                dumpMap();
            }
            return sr;
        }

        private void handleDontCloseX11DisplayQuirk(final GLRendererQuirks quirks) {
            if( null != quirks && quirks.exist( GLRendererQuirks.DontCloseX11Display ) ) {
                jogamp.nativewindow.x11.X11Util.markAllDisplaysUnclosable();
            }
        }

        private boolean mapAvailableEGLESConfig(final AbstractGraphicsDevice adevice,
                                                final boolean[] mapsADeviceToDefaultDevice,
                                                final EGLGraphicsDevice[] resEGLDevice) {
            final int majorVersion = 2;
            final int minorVersion = 0;
            final int ctxProfile = EGLContext.CTX_PROFILE_ES;
            final String profileString = EGLContext.getGLProfile(majorVersion, minorVersion, ctxProfile);

            if ( !GLProfile.isAvailable(adevice, profileString) ) {
                if ( DEBUG_SHAREDCTX ) {
                    System.err.println("EGLDrawableFactory.MapGLVersions: "+profileString+" n/a on "+adevice);
                }
                return false;
            }
            final GLProfile glp = GLProfile.get(adevice, profileString) ;
            final GLDrawableFactoryImpl desktopFactory = (GLDrawableFactoryImpl) GLDrawableFactory.getDesktopFactory();

            final GLCapabilities reqCapsAny = new GLCapabilities(glp);
            reqCapsAny.setRedBits(5); reqCapsAny.setGreenBits(5); reqCapsAny.setBlueBits(5); reqCapsAny.setAlphaBits(0);
            reqCapsAny.setDoubleBuffered(false);
            final GLCapabilitiesImmutable reqCapsPBuffer = GLGraphicsConfigurationUtil.fixGLPBufferGLCapabilities(reqCapsAny);
            final List<GLCapabilitiesImmutable> defaultDevicePBufferCapsL = getAvailableEGLConfigs(defaultDevice, reqCapsPBuffer);
            final boolean defaultDeviceHasPBuffer = defaultDevicePBufferCapsL.size() > 0;

            final boolean useDefaultDevice = adevice == defaultDevice;

            mapsADeviceToDefaultDevice[0] = !useDefaultDevice &&
                                            null != defaultSharedResource && defaultSharedResource.isAvailable &&
                                            defaultConnection.equals(adevice.getConnection());

            if ( DEBUG_SHAREDCTX ) {
                System.err.println("EGLDrawableFactory.MapGLVersions: "+profileString+" ( "+majorVersion+" ), "+
                        "mapsADeviceToDefaultDevice "+mapsADeviceToDefaultDevice[0]+
                        " (useDefaultDevice "+useDefaultDevice+", defaultDeviceHasPBuffer "+defaultDeviceHasPBuffer+", hasDesktopFactory "+(null != desktopFactory)+
                        ", isEGLGraphicsDevice "+(adevice instanceof EGLGraphicsDevice)+")");
            }

            if( mapsADeviceToDefaultDevice[0] ) {
                return true;
            }

            final boolean defaultNoSurfacelessCtx = GLRendererQuirks.existStickyDeviceQuirk(defaultDevice, GLRendererQuirks.NoSurfacelessCtx);
            boolean success = false;
            final boolean hasKHRSurfacelessTried;
            if( defaultDeviceEGLFeatures.hasKHRSurfaceless && !defaultNoSurfacelessCtx ) {
                hasKHRSurfacelessTried = true;
                final AbstractGraphicsDevice zdevice = useDefaultDevice ? defaultDevice : adevice; // reuse
                final EGLSurface zeroSurface = createSurfacelessImpl(zdevice, false, reqCapsAny, reqCapsAny, null, 64, 64);
                resEGLDevice[0] = (EGLGraphicsDevice) zeroSurface.getGraphicsConfiguration().getScreen().getDevice();
                if ( DEBUG_SHAREDCTX ) {
                    System.err.println("EGLDrawableFactory-MapGLVersions.0: "+resEGLDevice[0]);
                }
                EGLDrawable zeroDrawable = null;
                EGLContext context = null;
                boolean hasException = false;
                try {
                    zeroDrawable = (EGLDrawable) createOnscreenDrawableImpl ( zeroSurface );
                    zeroDrawable.setRealized(true);

                    context = (EGLContext) zeroDrawable.createContext(null);
                    if (null == context) {
                        throw new GLException("Couldn't create shared context for drawable: "+zeroDrawable);
                    }
                    // Triggers initial mapping, if not done yet
                    if( GLContext.CONTEXT_NOT_CURRENT != context.makeCurrent() ) { // could cause exception
                        // context.isCurrent() !
                        final GL gl = context.getGL();
                        final String glVersionString = gl.glGetString(GL.GL_VERSION);
                        if(null != glVersionString) {
                            success = true;
                        } else {
                            setNoSurfacelessCtxQuirk(context);
                        }
                    } else if ( DEBUG_SHAREDCTX ) {
                        System.err.println("EGLDrawableFactory-MapGLVersions.0: NOT_CURRENT: "+resEGLDevice[0]+", "+context);
                    }
                } catch (final Throwable t) {
                    hasException = true;
                    if ( DEBUG_SHAREDCTX ) {
                        System.err.println("EGLDrawableFactory-MapGLVersions.0: INFO: context create/makeCurrent failed");
                        t.printStackTrace();
                    }
                } finally {
                    if( null != context ) {
                        try {
                            context.destroy();
                        } catch (final GLException gle) {
                            if ( DEBUG_SHAREDCTX ) {
                                System.err.println("EGLDrawableFactory-MapGLVersions.0: INFO: destroy caught exception:");
                                gle.printStackTrace();
                            }
                        }
                    }
                    if( null != zeroDrawable ) {
                        zeroDrawable.setRealized(false);
                    }
                    if( null != zeroSurface ) {
                        zeroSurface.destroyNotify();
                    }
                    if( success || hasException ) { // cont. using device if !success
                        if( defaultDevice != resEGLDevice[0] ) { // don't close default device
                            if(null != resEGLDevice[0]) {
                                resEGLDevice[0].close();
                            }
                        }
                    }
                }
                if( success ) {
                    return true;
                }
            } else { // hasKHRSurfaceless
                hasKHRSurfacelessTried = false;
            }
            EGLFeatures eglFeatures = null;
            NativeSurface surface = null;
            EGLDrawable drawable = null;
            GLDrawable zeroDrawable = null;
            EGLContext context = null;
            ProxySurface upstreamSurface = null; // X11, GLX, ..
            ProxySurface downstreamSurface = null; // EGL
            try {
                if( useDefaultDevice && defaultDeviceHasPBuffer ) {
                    // Map any non EGL device to EGL default shared resources (default behavior), using a pbuffer surface
                    resEGLDevice[0] = defaultDevice; // reuse
                    eglFeatures = defaultDeviceEGLFeatures;
                    if ( DEBUG_SHAREDCTX ) {
                        System.err.println("EGLDrawableFactory-MapGLVersions.1: "+resEGLDevice[0]);
                        System.err.println("EGLDrawableFactory-MapGLVersions.1: "+eglFeatures);
                    }

                    downstreamSurface = createDummySurfaceImpl(resEGLDevice[0], false, reqCapsPBuffer, reqCapsPBuffer, null, 64, 64);
                    if( null != downstreamSurface ) {
                        downstreamSurface.createNotify();
                        surface = downstreamSurface;
                    }
                } else if( adevice != defaultDevice ) {
                    // Create a true mapping of given device to EGL
                    upstreamSurface = desktopFactory.createDummySurface(adevice, reqCapsAny, null, 64, 64); // X11, WGL, .. dummy window
                    if(null != upstreamSurface) {
                        upstreamSurface.createNotify();
                        resEGLDevice[0] = EGLDisplayUtil.eglCreateEGLGraphicsDevice(upstreamSurface);
                        resEGLDevice[0].open();
                        eglFeatures = new EGLFeatures(resEGLDevice[0]);
                        if ( DEBUG_SHAREDCTX ) {
                            System.err.println("EGLDrawableFactory-MapGLVersions.2: "+resEGLDevice[0]);
                            System.err.println("EGLDrawableFactory-MapGLVersions.2: "+eglFeatures);
                        }
                        surface = upstreamSurface;
                    }
                }

                if(null != surface) {
                    drawable = (EGLDrawable) createOnscreenDrawableImpl ( surface );
                    drawable.setRealized(true);

                    context = (EGLContext) drawable.createContext(null);
                    if (null == context) {
                        throw new GLException("Couldn't create shared context for drawable: "+drawable);
                    }

                    // Triggers initial mapping, if not done yet
                    if( GLContext.CONTEXT_NOT_CURRENT != context.makeCurrent() ) { // could cause exception
                        // context.isCurrent() !
                        final GL gl = context.getGL();
                        final String glVersionString = gl.glGetString(GL.GL_VERSION);
                        if(null != glVersionString) {
                            success = true;
                            if( !hasKHRSurfacelessTried && eglFeatures.hasKHRSurfaceless &&
                                ( context.isGLES() || context.getGLVersionNumber().compareTo(GLContext.Version3_0) >= 0 )
                              )
                            {
                                if( probeSurfacelessCtx(context, false /* restoreDrawable */) ) {
                                    zeroDrawable = context.getGLDrawable();
                                }
                            } else {
                                setNoSurfacelessCtxQuirk(context);
                            }
                        } else if ( DEBUG_SHAREDCTX ) {
                            System.err.println("EGLDrawableFactory-MapGLVersions.12: NULL VERSION: "+resEGLDevice[0]+", "+context.getGLVersion());
                        }
                    } else if ( DEBUG_SHAREDCTX ) {
                        System.err.println("EGLDrawableFactory-MapGLVersions.12: NOT_CURRENT: "+resEGLDevice[0]+", "+context);
                    }
                }
            } catch (final Throwable t) {
                if ( DEBUG_SHAREDCTX ) {
                    System.err.println("EGLDrawableFactory-MapGLVersions.12: INFO: context create/makeCurrent failed");
                    t.printStackTrace();
                }
                success = false;
            } finally {
                if( null != context ) {
                    try {
                        context.destroy();
                    } catch (final GLException gle) {
                        if ( DEBUG_SHAREDCTX ) {
                            System.err.println("EGLDrawableFactory-MapGLVersions.12: INFO: destroy caught exception:");
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
                if( null != downstreamSurface ) {
                    downstreamSurface.destroyNotify();
                }
                if( defaultDevice != resEGLDevice[0] ) { // don't close default device
                    if(null != resEGLDevice[0]) {
                        resEGLDevice[0].close();
                    }
                }
                if(null != upstreamSurface) {
                    upstreamSurface.destroyNotify();
                }
            }
            return success;
        }

        @Override
        public void releaseSharedResource(final SharedResourceRunner.Resource shared) {
            final SharedResource sr = (SharedResource) shared;
            if ( DEBUG_SHAREDCTX ) {
                System.err.println("Shutdown Shared:");
                System.err.println("Device  : " + sr.device);
                ExceptionUtils.dumpStack(System.err);
            }

            if (null != sr.device) {
                // Issues eglTerminate(), which may cause SIGSEGV w/ NVIDIA 343.36 w/ TestGLProfile01NEWT
                // May cause JVM SIGSEGV:
                sr.device.close();
                sr.device = null;
            }
        }
    }

    public final boolean hasDefaultDeviceKHRCreateContext() {
        return defaultDeviceEGLFeatures.hasKHRCreateContext;
    }
    /**
     * {@inheritDoc}
     * <p>
     * This factory may support native desktop OpenGL if {@link EGL#EGL_CLIENT_APIS} contains {@code OpenGL}
     * <i>and</i> if {@code EGL_KHR_create_context} extension is supported.
     * </p>
     */
    @Override
    public final boolean hasOpenGLDesktopSupport() {
        /**
         * It has been experienced w/ Mesa 10.3.2 (EGL 1.4/Gallium)
         * that even though initial OpenGL context can be created w/o 'EGL_KHR_create_context',
         * switching the API via 'eglBindAPI(EGL_OpenGL_API)' the latter 'eglCreateContext(..)' fails w/ EGL_BAD_ACCESS.
         * Hence we require both: OpenGL API support _and_  'EGL_KHR_create_context'.
         */
        return null != eglGLnDynamicLookupHelper &&
               defaultDeviceEGLFeatures.hasGLAPI && defaultDeviceEGLFeatures.hasKHRCreateContext;
    }

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
     * Return true if  {@code EGL_KHR_create_context} extension is supported,
     * see {@link #hasDefaultDeviceKHRCreateContext()}.
     * </p>
     */
    @Override
    public final boolean hasMajorMinorCreateContextARB() {
        return hasDefaultDeviceKHRCreateContext();
    }

    @Override
    public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
    }

    @Override
    public final boolean getIsDeviceCompatible(final AbstractGraphicsDevice device) {
      // via mappings (X11/WGL/.. -> EGL) we shall be able to handle all types.
      return null != sharedResourceImplementation ; // null!=eglES2DynamicLookupHelper || null!=eglES1DynamicLookupHelper || ..;
    }

    private static List<GLCapabilitiesImmutable> getAvailableEGLConfigs(final EGLGraphicsDevice eglDisplay, final GLCapabilitiesImmutable caps) {
        final IntBuffer numConfigs = Buffers.newDirectIntBuffer(1);
        if(!EGL.eglGetConfigs(eglDisplay.getHandle(), null, 0, numConfigs)) {
            throw new GLException("EGLDrawableFactory.getAvailableEGLConfigs: Get maxConfigs (eglGetConfigs) call failed, error "+EGLContext.toHexString(EGL.eglGetError()));
        }
        if(0 < numConfigs.get(0)) {
            final PointerBuffer configs = PointerBuffer.allocateDirect(numConfigs.get(0));
            final IntBuffer attrs = EGLGraphicsConfiguration.GLCapabilities2AttribList(caps);
            final int winattrmask = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(caps);
            if( EGL.eglChooseConfig(eglDisplay.getHandle(), attrs, configs, configs.capacity(), numConfigs) && numConfigs.get(0) > 0) {
                return EGLGraphicsConfigurationFactory.eglConfigs2GLCaps(eglDisplay, caps.getGLProfile(), configs, numConfigs.get(0), winattrmask, false /* forceTransparentFlag */, false /* onlyFirstValid */);
            }
        }
        return new ArrayList<GLCapabilitiesImmutable>(0);
    }

    static void dumpEGLInfo(final String prefix, final long eglDisplay) {
        final String eglVendor = EGL.eglQueryString(eglDisplay, EGL.EGL_VENDOR);
        final String eglClientAPIs = EGL.eglQueryString(eglDisplay, EGL.EGL_CLIENT_APIS);
        final String eglClientVersion = EGL.eglQueryString(EGL.EGL_NO_DISPLAY, EGL.EGL_VERSION);
        final String eglServerVersion = EGL.eglQueryString(eglDisplay, EGL.EGL_VERSION);
        System.err.println(prefix+"EGL vendor "+eglVendor+", version [client "+eglClientVersion+", server "+eglServerVersion+"], clientAPIs "+eglClientAPIs);
    }

    @Override
    protected final SharedResource getOrCreateSharedResourceImpl(final AbstractGraphicsDevice adevice) {
        return (SharedResource) sharedResourceRunner.getOrCreateShared(adevice);
    }

    @Override
    protected final Thread getSharedResourceThread() {
        return sharedResourceRunner.start();
    }

    public final boolean isANGLE() {
        return isANGLE;
    }

    @Override
    public final GLDynamicLookupHelper getGLDynamicLookupHelper(final int majorVersion, final int contextOptions) {
        final GLDynamicLookupHelper res;
        if ( EGLContext.isGLES2ES3(majorVersion, contextOptions) ) {
            res = eglES2DynamicLookupHelper;
        } else if ( EGLContext.isGLES1(majorVersion, contextOptions) ) {
            res = eglES1DynamicLookupHelper;
        } else if( EGLContext.isGLDesktop(contextOptions) ) {
            res = eglGLnDynamicLookupHelper;
        } else {
            throw new IllegalArgumentException("neither GLES1, GLES2, GLES3 nor desktop GL has been specified: "+majorVersion+" ("+EGLContext.getGLProfile(new StringBuilder(), contextOptions).toString());
        }
        if( DEBUG_SHAREDCTX ) {
            if( null == res ) {
                System.err.println("EGLDrawableFactory.getGLDynamicLookupHelper: NULL for profile "+majorVersion+" ("+EGLContext.getGLProfile(new StringBuilder(), contextOptions).toString());
            }
        }
        return res;
    }

    @Override
    protected List<GLCapabilitiesImmutable> getAvailableCapabilitiesImpl(final AbstractGraphicsDevice device) {
        if(null == sharedResourceImplementation) { // null == eglES1DynamicLookupHelper && null == eglES2DynamicLookupHelper || ..
            return new ArrayList<GLCapabilitiesImmutable>(); // null
        }
        return EGLGraphicsConfigurationFactory.getAvailableCapabilities(this, device);
    }

    @Override
    protected GLDrawableImpl createOnscreenDrawableImpl(final NativeSurface target) {
        if (target == null) {
          throw new IllegalArgumentException("Null target");
        }
        return new EGLDrawable(this, EGLSurface.get(target));
    }

    @Override
    protected GLDrawableImpl createOffscreenDrawableImpl(final NativeSurface target) {
        if (target == null) {
          throw new IllegalArgumentException("Null target");
        }
        final AbstractGraphicsConfiguration config = target.getGraphicsConfiguration();
        final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
        if(!caps.isPBuffer()) {
            throw new GLException("Non pbuffer not yet implemented");
        }
        // PBuffer GLDrawable Creation
        return new EGLDrawable(this, EGLSurface.get(target));
    }

    @Override
    public boolean canCreateGLPbuffer(final AbstractGraphicsDevice device, final GLProfile glp) {
        // SharedResource sr = getOrCreateEGLSharedResource(device);
        // return sr.hasES1PBuffer() || sr.hasES2PBuffer();
        return true;
    }

    private final EGLGraphicsConfiguration evalConfig(final boolean[] ownDevice, final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                      final GLCapabilitiesImmutable capsChosen, final GLCapabilitiesImmutable capsRequested,
                                                      final GLCapabilitiesChooser chooser) {
        final EGLGraphicsDevice device;
        if( createNewDevice || ! (deviceReq instanceof EGLGraphicsDevice) ) {
            device = EGLDisplayUtil.eglCreateEGLGraphicsDevice(deviceReq);
            device.open();
            ownDevice[0] = true;
        } else {
            device = (EGLGraphicsDevice) deviceReq;
            ownDevice[0] = false;
        }
        final DefaultGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
        final EGLGraphicsConfiguration config = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen, VisualIDHolder.VID_UNDEFINED, false);
        if(null == config) {
            throw new GLException("Choosing GraphicsConfiguration failed w/ "+capsChosen+" on "+screen);
        }
        return config;
    }

    @Override
    protected final EGLSurface createMutableSurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                          final GLCapabilitiesImmutable capsChosen, final GLCapabilitiesImmutable capsRequested,
                                                          final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstreamHook) {
        final boolean[] ownDevice = { false };
        final EGLGraphicsConfiguration config = evalConfig(ownDevice, deviceReq, createNewDevice, capsChosen, capsRequested, chooser);
        return EGLSurface.createWrapped(config, 0, upstreamHook, ownDevice[0]);
    }

    @Override
    public final EGLSurface createDummySurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                     GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
        chosenCaps = GLGraphicsConfigurationUtil.fixGLPBufferGLCapabilities(chosenCaps); // complete validation in EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(..) above
        return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, new EGLDummyUpstreamSurfaceHook(width, height));
    }

    @Override
    public final EGLSurface createSurfacelessImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                    GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
        chosenCaps = GLGraphicsConfigurationUtil.fixOnscreenGLCapabilities(chosenCaps);
        final boolean[] ownDevice = { false };
        final EGLGraphicsConfiguration config = evalConfig(ownDevice, deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser);
        return EGLSurface.createSurfaceless(config, new GenericUpstreamSurfacelessHook(width, height), ownDevice[0]);
    }

    /**
     * @param ms {@link MutableSurface} which dimensions and config are being used to create the pbuffer surface.
     *           It will also hold the resulting pbuffer surface handle.
     * @param useTexture
     * @return the passed {@link MutableSurface} which now has the EGL pbuffer surface set as it's handle
     */
    protected static MutableSurface createPBufferSurfaceImpl(final MutableSurface ms, final boolean useTexture) {
        return null;
    }
    protected static long createPBufferSurfaceImpl(final EGLGraphicsConfiguration config, final int width, final int height, final boolean useTexture) {
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

        final IntBuffer attrs = EGLGraphicsConfiguration.CreatePBufferSurfaceAttribList(width, height, texFormat);
        final long surf = EGL.eglCreatePbufferSurface(eglDevice.getHandle(), config.getNativeConfig(), attrs);
        if (EGL.EGL_NO_SURFACE==surf) {
            throw new GLException("Creation of window surface (eglCreatePbufferSurface) failed, dim "+width+"x"+height+", "+eglDevice+", "+config+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        } else if(DEBUG) {
            System.err.println("PBuffer setSurface result: eglSurface 0x"+Long.toHexString(surf));
        }
        return surf;
    }

    @Override
    protected EGLSurface createProxySurfaceImpl(final AbstractGraphicsDevice deviceReq, final int screenIdx, final long windowHandle,
                                                  final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser,
                                                  final UpstreamSurfaceHook upstream) {
        final EGLGraphicsDevice device = EGLDisplayUtil.eglCreateEGLGraphicsDevice(deviceReq);
        device.open();
        final DefaultGraphicsScreen screen = new DefaultGraphicsScreen(device, screenIdx);
        final EGLGraphicsConfiguration cfg = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, VisualIDHolder.VID_UNDEFINED, false);
        return EGLSurface.createWrapped(cfg, windowHandle, upstream, true);
    }

    @Override
    protected GLContext createExternalGLContextImpl() {
        final AbstractGraphicsScreen absScreen = DefaultGraphicsScreen.createDefault(NativeWindowFactory.TYPE_EGL);
        return new EGLExternalContext(absScreen);
    }

    @Override
    public boolean canCreateExternalGLDrawable(final AbstractGraphicsDevice device) {
        return false;
    }

    @Override
    protected GLDrawable createExternalGLDrawableImpl() {
        throw new GLException("Not yet implemented");
    }
}
