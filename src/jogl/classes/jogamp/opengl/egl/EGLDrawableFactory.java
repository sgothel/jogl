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
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.GenericUpstreamSurfacelessHook;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.egl.EGL;

public class EGLDrawableFactory extends GLDrawableFactoryImpl {
    protected static final boolean DEBUG = GLDrawableFactoryImpl.DEBUG; // allow package access
    private static final boolean DEBUG_SHAREDCTX = DEBUG  || GLContext.DEBUG;

    /* package */ static final boolean QUERY_EGL_ES_NATIVE_TK;

    static {
        Debug.initSingleton();
        QUERY_EGL_ES_NATIVE_TK = PropertyAccess.isPropertyDefined("jogl.debug.EGLDrawableFactory.QueryNativeTK", true);
    }

    private static boolean eglDynamicLookupHelperInit = false;
    private static GLDynamicLookupHelper eglES1DynamicLookupHelper = null;
    private static GLDynamicLookupHelper eglES2DynamicLookupHelper = null;
    private static GLDynamicLookupHelper eglGLnDynamicLookupHelper = null;

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
            if(DEBUG) {
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
                    for(int i=eglClientAPIs.length-1; i>=0; i--) {
                        _hasGLAPI = eglClientAPIs[i].equals("OpenGL");
                    }
                }
                hasGLAPI = _hasGLAPI;
                if(DEBUG) {
                    System.err.println("  Client APIs: "+eglClientAPIStr+"; has EGL 1.4 "+hasEGL_1_4+" -> has OpenGL "+hasGLAPI);
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
                if(DEBUG) {
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
            if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(true)) {
                hasX11 = true;
                try {
                    ReflectionUtil.createInstance("jogamp.opengl.x11.glx.X11GLXGraphicsConfigurationFactory", EGLDrawableFactory.class.getClassLoader());
                } catch (final Exception jre) { /* n/a .. */ }
            }

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

                    // FIXME: defaultDevice.open() triggers eglInitialize(..) which crashed on Windows w/ Chrome/ANGLE, FF/ANGLE!
                    defaultDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(EGL.EGL_DEFAULT_DISPLAY, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);

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
            System.err.println("EGLDrawableFactory.map "+sharedMap.size());
            int i=0;
            final Set<String> keys = sharedMap.keySet();
            for(final Iterator<String> keyI = keys.iterator(); keyI.hasNext(); i++) {
                final String key = keyI.next();
                final SharedResource sr = (SharedResource) sharedMap.get(key);
                System.err.println("EGLDrawableFactory.map["+i+"] "+key+" -> "+sr.getDevice()+", avail "+sr.isAvailable+
                                   "gln   [quirks "+sr.rendererQuirksGLn+", ctp "+EGLContext.getGLVersion(3, 0, sr.ctpGLn, null)+"], "+
                                   "es1   [quirks "+sr.rendererQuirksES1+", ctp "+EGLContext.getGLVersion(1, 0, sr.ctpES1, null)+"], "+
                                   "es2/3 [quirks "+sr.rendererQuirksES3ES2+", ctp "+EGLContext.getGLVersion(2, 0, sr.ctpES3ES2, null)+"]");
            }
            ;
        }
    }

    private boolean isANGLE = false;
    private boolean hasX11 = false;
    private EGLGraphicsDevice defaultDevice = null;
    private EGLFeatures defaultDeviceEGLFeatures;
    private SharedResourceImplementation sharedResourceImplementation;
    private SharedResourceRunner sharedResourceRunner;

    static class SharedResource implements SharedResourceRunner.Resource {
      private EGLGraphicsDevice device;
      // private final EGLContext contextES1;
      // private final EGLContext contextES2;
      // private final EGLContext contextES3;
      final boolean isAvailable;
      final GLRendererQuirks rendererQuirksGLn;
      final GLRendererQuirks rendererQuirksES1;
      final GLRendererQuirks rendererQuirksES3ES2;
      final int ctpGLn;
      final int ctpES1;
      final int ctpES3ES2;

      SharedResource(final EGLGraphicsDevice dev, final boolean isAvailable,
                     final GLRendererQuirks rendererQuirksGLn, final int ctpGLn,
                     final GLRendererQuirks rendererQuirksES1, final int ctpES1,
                     final GLRendererQuirks rendererQuirksES3ES2, final int ctpES3ES2) {
          this.device = dev;
          this.isAvailable = isAvailable;

          this.rendererQuirksGLn = rendererQuirksGLn;
          this.ctpGLn = ctpGLn;

          this.rendererQuirksES1 = rendererQuirksES1;
          this.ctpES1 = ctpES1;

          this.rendererQuirksES3ES2 = rendererQuirksES3ES2;
          this.ctpES3ES2 = ctpES3ES2;
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
              if( null != rendererQuirksES3ES2 ) {
                  return rendererQuirksES3ES2;
              } else if( null != rendererQuirksES1 ) {
                  return rendererQuirksES1;
              } else {
                  return rendererQuirksGLn;
              }
          } else if( !glp.isGLES() ) {
              return rendererQuirksGLn;
          } else if( glp.isGLES1() ) {
              return rendererQuirksES1;
          } else {
              return rendererQuirksES3ES2;
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
            final GLRendererQuirks[] rendererQuirksES1 = new GLRendererQuirks[] { null };
            final GLRendererQuirks[] rendererQuirksES3ES2 = new GLRendererQuirks[] { null };
            final GLRendererQuirks[] rendererQuirksGLn = new GLRendererQuirks[] { null };
            final int[] ctpES1 = new int[] { EGLContext.CTX_PROFILE_ES };
            final int[] ctpES3ES2 = new int[] { EGLContext.CTX_PROFILE_ES };
            final int[] ctpGLn = new int[] { EGLContext.CTX_PROFILE_CORE };

            if ( DEBUG_SHAREDCTX ) {
                System.err.println("EGLDrawableFactory.createShared(): device "+adevice);
            }

            boolean madeCurrentES1 = false;
            boolean madeCurrentES2 = false;
            boolean madeCurrentES3 = false;
            boolean madeCurrentGLn = false;

            if( null != eglGLnDynamicLookupHelper ) {
                // OpenGL 3.1 core -> GL3, will utilize normal desktop profile mapping
                final int[] major = { 3 };
                final int[] minor = { 1 }; // FIXME: No minor version probing for ES currently!
                madeCurrentGLn = mapAvailableEGLESConfig(adevice, major, minor,
                                                         ctpGLn, rendererQuirksGLn) && 0 != major[0];
            } else {
                madeCurrentGLn = false;
            }
            EGLContext.setAvailableGLVersionsSet(adevice, true);

            if( null != eglES1DynamicLookupHelper ) {
                final int[] major = { 1 };
                final int[] minor = { 0 };
                madeCurrentES1 = mapAvailableEGLESConfig(adevice, major, minor,
                                                         ctpES1, rendererQuirksES1) && 1 == major[0];
            } else {
                madeCurrentES1 = false;
            }
            if( null != eglES2DynamicLookupHelper ) {
                // ES3 Query
                final int[] major = { 3 };
                final int[] minor = { 0 };
                madeCurrentES3 = mapAvailableEGLESConfig(adevice, major, minor,
                                                         ctpES3ES2, rendererQuirksES3ES2) && 3 == major[0];
                if( !madeCurrentES3 ) {
                    // ES2 Query, may result in ES3
                    major[0] = 2;
                    if( mapAvailableEGLESConfig(adevice, major, minor,
                                                ctpES3ES2, rendererQuirksES3ES2) )
                    {
                        switch( major[0] ) {
                            case 2: madeCurrentES2 = true; break;
                            case 3: madeCurrentES3 = true; break;
                            default: throw new InternalError("XXXX Got "+major[0]);
                        }
                    }
                }
            }

            if( hasX11 ) {
                handleDontCloseX11DisplayQuirk(rendererQuirksES1[0]);
                handleDontCloseX11DisplayQuirk(rendererQuirksES3ES2[0]);
            }
            final SharedResource sr = new SharedResource(defaultDevice,
                                                         madeCurrentGLn || madeCurrentES1 || madeCurrentES2 || madeCurrentES3,
                                                         rendererQuirksGLn[0], ctpGLn[0],
                                                         rendererQuirksES1[0], ctpES1[0],
                                                         rendererQuirksES3ES2[0], ctpES3ES2[0]);

            if ( DEBUG_SHAREDCTX ) {
                System.err.println("EGLDrawableFactory.createShared: devices: queried nativeTK "+QUERY_EGL_ES_NATIVE_TK+", adevice " + adevice + ", defaultDevice " + defaultDevice);
                System.err.println("EGLDrawableFactory.createShared: context GLn: " + madeCurrentGLn + ", quirks "+rendererQuirksGLn[0]);
                System.err.println("EGLDrawableFactory.createShared: context ES1: " + madeCurrentES1 + ", quirks "+rendererQuirksES1[0]);
                System.err.println("EGLDrawableFactory.createShared: context ES2: " + madeCurrentES2 + ", quirks "+rendererQuirksES3ES2[0]);
                System.err.println("EGLDrawableFactory.createShared: context ES3: " + madeCurrentES3 + ", quirks "+rendererQuirksES3ES2[0]);
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
                                                final int[] majorVersion, final int[] minorVersion,
                                                final int[] ctxProfile, final GLRendererQuirks[] rendererQuirks) {
            final String profileString = EGLContext.getGLProfile(majorVersion[0], minorVersion[0], ctxProfile[0]);

            if ( !GLProfile.isAvailable(adevice, profileString) ) {
                if ( DEBUG_SHAREDCTX ) {
                    System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: "+profileString+" n/a on "+adevice);
                }
                return false;
            }
            final GLProfile glp = GLProfile.get(adevice, profileString) ;
            final GLDrawableFactoryImpl desktopFactory = (GLDrawableFactoryImpl) GLDrawableFactory.getDesktopFactory();
            final boolean initDefaultDevice = 0 == defaultDevice.getHandle(); // Note: GLProfile always triggers EGL device initialization first!
            final boolean mapsADeviceToDefaultDevice = !QUERY_EGL_ES_NATIVE_TK || initDefaultDevice ||
                                                       null == desktopFactory;
                                                       // FIXME || adevice instanceof EGLGraphicsDevice ;
            if ( DEBUG_SHAREDCTX ) {
                System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: "+profileString+" ( "+majorVersion[0]+" ), "+
                        "mapsADeviceToDefaultDevice "+mapsADeviceToDefaultDevice+
                        " (QUERY_EGL_ES_NATIVE_TK "+QUERY_EGL_ES_NATIVE_TK+", initDefaultDevice "+initDefaultDevice+", hasDesktopFactory "+(null != desktopFactory)+
                        ", isEGLGraphicsDevice "+(adevice instanceof EGLGraphicsDevice)+")");
            }

            boolean hasPBuffer;
            EGLGraphicsDevice eglDevice = null;
            EGLFeatures eglFeatures = null;
            NativeSurface surface = null;
            ProxySurface upstreamSurface = null; // X11, GLX, ..
            ProxySurface downstreamSurface = null; // EGL
            boolean success = false;
            try {
                final GLCapabilities reqCapsAny = new GLCapabilities(glp);
                reqCapsAny.setRedBits(5); reqCapsAny.setGreenBits(5); reqCapsAny.setBlueBits(5); reqCapsAny.setAlphaBits(0);
                reqCapsAny.setDoubleBuffered(false);

                if( mapsADeviceToDefaultDevice ) {
                    // In this branch, any non EGL device is mapped to EGL default shared resources (default behavior).
                    // Only one default shared resource instance is ever be created.
                    if( initDefaultDevice ) {
                        defaultDevice.open();
                        defaultDeviceEGLFeatures = new EGLFeatures(defaultDevice);

                        // Probe for GLRendererQuirks.SingletonEGLDisplayOnly
                        final long secondEGLDisplay = EGL.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY);
                        if ( EGL.EGL_NO_DISPLAY == secondEGLDisplay ) {
                            final int quirk = GLRendererQuirks.SingletonEGLDisplayOnly;
                            GLRendererQuirks.addStickyDeviceQuirk(adevice, quirk);
                            EGLDisplayUtil.setSingletonEGLDisplayOnly(true);
                            if ( DEBUG_SHAREDCTX ) {
                                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Second eglGetDisplay(EGL_DEFAULT_DISPLAY) failed");
                            }
                        }
                    }
                    eglDevice = defaultDevice; // reuse
                    eglFeatures = defaultDeviceEGLFeatures;
                    if ( DEBUG_SHAREDCTX ) {
                        System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig.0: "+eglFeatures);
                    }

                    if( !glp.isGLES() && !eglFeatures.hasGLAPI ) {
                        if ( DEBUG_SHAREDCTX ) {
                            System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig() OpenGL API not supported (1)");
                        }
                    } else {
                        final GLCapabilitiesImmutable reqCapsPBuffer = GLGraphicsConfigurationUtil.fixGLPBufferGLCapabilities(reqCapsAny);
                        final List<GLCapabilitiesImmutable> availablePBufferCapsL = getAvailableEGLConfigs(eglDevice, reqCapsPBuffer);
                        hasPBuffer = availablePBufferCapsL.size() > 0;

                        // attempt to created the default shared resources ..
                        if( hasPBuffer ) {
                            // 2nd case create defaultDevice shared resource using pbuffer surface
                            downstreamSurface = createDummySurfaceImpl(eglDevice, false, reqCapsPBuffer, reqCapsPBuffer, null, 64, 64); // egl pbuffer offscreen
                            if( null != downstreamSurface ) {
                                downstreamSurface.createNotify();
                                surface = downstreamSurface;
                            }
                        } else {
                            // 3rd case fake creation of defaultDevice shared resource, no pbuffer available
                            final List<GLCapabilitiesImmutable> capsAnyL = getAvailableEGLConfigs(eglDevice, reqCapsAny);
                            if(capsAnyL.size() > 0) {
                                final GLCapabilitiesImmutable chosenCaps = capsAnyL.get(0);
                                EGLContext.mapStaticGLESVersion(eglDevice, chosenCaps);
                                success = true;
                            }
                            if ( DEBUG_SHAREDCTX ) {
                                System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig() no pbuffer config available, detected !pbuffer config: "+success);
                                EGLGraphicsConfigurationFactory.printCaps("!PBufferCaps", capsAnyL, System.err);
                            }
                        }
                    }
                } else {
                    // 4th case always creates a true mapping of given device to EGL
                    upstreamSurface = desktopFactory.createDummySurface(adevice, reqCapsAny, null, 64, 64); // X11, WGL, .. dummy window
                    if(null != upstreamSurface) {
                        upstreamSurface.createNotify();
                        eglDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(upstreamSurface);
                        eglDevice.open();
                        eglFeatures = new EGLFeatures(eglDevice);
                        if ( DEBUG_SHAREDCTX ) {
                            System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig.1: "+eglFeatures);
                        }
                        if( !glp.isGLES() && !eglFeatures.hasGLAPI ) {
                            if ( DEBUG_SHAREDCTX ) {
                                System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig() OpenGL API not supported (2)");
                            }
                            // disposed at finalized: eglDevice, upstreamSurface
                        } else {
                            hasPBuffer = true;
                            surface = upstreamSurface;
                        }
                    }
                }

                if(null != surface) {
                    EGLDrawable drawable = null;
                    GLDrawable zeroDrawable = null;
                    EGLContext context = null;
                    try {
                        drawable = (EGLDrawable) createOnscreenDrawableImpl ( surface );
                        drawable.setRealized(true);

                        context = (EGLContext) drawable.createContext(null);
                        if (null == context) {
                            throw new GLException("Couldn't create shared context for drawable: "+drawable);
                        }

                        if( GLContext.CONTEXT_NOT_CURRENT != context.makeCurrent() ) { // could cause exception
                            // context.isCurrent() !
                            final String glVersionString = context.getGL().glGetString(GL.GL_VERSION);
                            if(null != glVersionString) {
                                context.mapCurrentAvailableGLESVersion(eglDevice);
                                if(eglDevice != adevice) {
                                    context.mapCurrentAvailableGLESVersion(adevice);
                                }

                                if( eglFeatures.hasKHRSurfaceless &&
                                    ( context.isGLES() || context.getGLVersionNumber().compareTo(GLContext.Version3_0) >= 0 )
                                  )
                                {
                                    if( probeSurfacelessCtx(context, false /* restoreDrawable */) ) {
                                        zeroDrawable = context.getGLDrawable();
                                    }
                                } else {
                                    setNoSurfacelessCtxQuirk(context);
                                }
                                rendererQuirks[0] = context.getRendererQuirks();
                                ctxProfile[0] = context.getContextOptions();
                                majorVersion[0] = context.getGLVersionNumber().getMajor();
                                minorVersion[0] = context.getGLVersionNumber().getMinor();
                                success = true;
                            } else {
                                // Oops .. something is wrong
                                if ( DEBUG_SHAREDCTX ) {
                                    System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: "+eglDevice+", "+context.getGLVersion()+" - VERSION is null, dropping availability!");
                                }
                            }
                        }
                    } catch (final Throwable t) {
                        if ( DEBUG_SHAREDCTX ) {
                            System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: INFO: context create/makeCurrent failed");
                            t.printStackTrace();
                        }
                    } finally {
                        if( null != context ) {
                            try {
                                context.destroy();
                            } catch (final GLException gle) {
                                if ( DEBUG_SHAREDCTX ) {
                                    System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: INFO: destroy caught exception:");
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
                    }
                }
            } catch (final Throwable t) {
                if ( DEBUG_SHAREDCTX ) {
                    System.err.println("Caught exception on thread "+getThreadName());
                    t.printStackTrace();
                }
                success = false;
            } finally {
                if(null != downstreamSurface) {
                    downstreamSurface.destroyNotify();
                }
                if( defaultDevice != eglDevice ) { // don't close default device
                    if(null != eglDevice) {
                        eglDevice.close();
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
    public final boolean hasOpenGLAPISupport() {
        return defaultDeviceEGLFeatures.hasGLAPI;
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
            final long nativeDisplayID = ( deviceReq instanceof EGLGraphicsDevice) ?
                    ( (EGLGraphicsDevice) deviceReq ).getNativeDisplayID() : deviceReq.getHandle() ;
            device = EGLDisplayUtil.eglCreateEGLGraphicsDevice(nativeDisplayID, deviceReq.getConnection(), deviceReq.getUnitID());
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
    protected final ProxySurface createMutableSurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                          final GLCapabilitiesImmutable capsChosen, final GLCapabilitiesImmutable capsRequested,
                                                          final GLCapabilitiesChooser chooser, final UpstreamSurfaceHook upstreamHook) {
        final boolean[] ownDevice = { false };
        final EGLGraphicsConfiguration config = evalConfig(ownDevice, deviceReq, createNewDevice, capsChosen, capsRequested, chooser);
        return EGLSurface.createWrapped(config, 0, upstreamHook, ownDevice[0]);
    }

    @Override
    public final ProxySurface createDummySurfaceImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
                                                     GLCapabilitiesImmutable chosenCaps, final GLCapabilitiesImmutable requestedCaps, final GLCapabilitiesChooser chooser, final int width, final int height) {
        chosenCaps = GLGraphicsConfigurationUtil.fixGLPBufferGLCapabilities(chosenCaps); // complete validation in EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(..) above
        return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, new EGLDummyUpstreamSurfaceHook(width, height));
    }

    @Override
    public final ProxySurface createSurfacelessImpl(final AbstractGraphicsDevice deviceReq, final boolean createNewDevice,
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
    protected ProxySurface createProxySurfaceImpl(final AbstractGraphicsDevice deviceReq, final int screenIdx, final long windowHandle,
                                                  final GLCapabilitiesImmutable capsRequested, final GLCapabilitiesChooser chooser,
                                                  final UpstreamSurfaceHook upstream) {
        final EGLGraphicsDevice eglDeviceReq = (EGLGraphicsDevice) deviceReq;
        final EGLGraphicsDevice device = EGLDisplayUtil.eglCreateEGLGraphicsDevice(eglDeviceReq.getNativeDisplayID(), deviceReq.getConnection(), deviceReq.getUnitID());
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
