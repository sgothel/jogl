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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.MutableSurface;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
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

import jogamp.nativewindow.WrappedSurface;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLGraphicsConfigurationUtil;
import jogamp.opengl.SharedResourceRunner;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLRendererQuirks;

public class EGLDrawableFactory extends GLDrawableFactoryImpl {
    protected static final boolean DEBUG = GLDrawableFactoryImpl.DEBUG; // allow package access
    
    /* package */ static final boolean QUERY_EGL_ES_NATIVE_TK = Debug.isPropertyDefined("jogl.debug.EGLDrawableFactory.QueryNativeTK", true);
    
    private static GLDynamicLookupHelper eglES1DynamicLookupHelper = null;
    private static GLDynamicLookupHelper eglES2DynamicLookupHelper = null;

    private static final boolean isANGLE(GLDynamicLookupHelper dl) {
        if(Platform.OSType.WINDOWS == Platform.OS_TYPE) {
            return dl.isFunctionAvailable("eglQuerySurfacePointerANGLE") ||
                   dl.isFunctionAvailable("glBlitFramebufferANGLE") ||
                   dl.isFunctionAvailable("glRenderbufferStorageMultisampleANGLE");
        } else {
            return false;
        }
    }

    private static final boolean includesES1(GLDynamicLookupHelper dl) {
        return dl.isFunctionAvailable("glLoadIdentity") &&
               dl.isFunctionAvailable("glEnableClientState") &&
               dl.isFunctionAvailable("glColorPointer");
    }
    
    public EGLDrawableFactory() {
        super();

        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        EGLGraphicsConfigurationFactory.registerFactory();

        // Check for other underlying stuff ..
        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(true)) {
            hasX11 = true;
            try {
                ReflectionUtil.createInstance("jogamp.opengl.x11.glx.X11GLXGraphicsConfigurationFactory", EGLDrawableFactory.class.getClassLoader());
            } catch (Exception jre) { /* n/a .. */ }
        }

        // FIXME: Probably need to move EGL from a static model
        // to a dynamic one, where there can be 2 instances
        // for each ES profile with their own ProcAddressTable.

        synchronized(EGLDrawableFactory.class) {
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
                    if (DEBUG || GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory: EGL ES1 - OK, isANGLE: "+isANGLEES1);
                    }
                } else if (DEBUG || GLProfile.DEBUG) {
                    System.err.println("Info: EGLDrawableFactory: EGL ES1 - NOPE (ES1 lib)");
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
                    final boolean includesES1 = null == eglES1DynamicLookupHelper && includesES1(eglES2DynamicLookupHelper);
                    if(includesES1) {
                        eglES1DynamicLookupHelper = tmp;
                    }
                    final boolean isANGLEES2 = isANGLE(eglES2DynamicLookupHelper);
                    isANGLE |= isANGLEES2;
                    if (DEBUG || GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory: EGL ES2 - OK (includesES1 "+includesES1+", isANGLE: "+isANGLEES2+")");
                        if(includesES1) {
                            System.err.println("Info: EGLDrawableFactory: EGL ES1 - OK (ES2 lib)");
                        }
                    }
                } else if (DEBUG || GLProfile.DEBUG) {
                    System.err.println("Info: EGLDrawableFactory: EGL ES2 - NOPE");
                }
            }
            if( null != eglES2DynamicLookupHelper || null != eglES1DynamicLookupHelper ) {
                if(isANGLE && !enableANGLE) {
                    if(DEBUG || GLProfile.DEBUG) {
                        System.err.println("Info: EGLDrawableFactory.init - EGL/ES2 ANGLE disabled");
                    }
                } else {
                    if( isANGLE && ( DEBUG || GLProfile.DEBUG ) ) {
                        System.err.println("Info: EGLDrawableFactory.init - EGL/ES2 ANGLE enabled");
                    }                    
                    sharedMap = new HashMap<String /*uniqueKey*/, SharedResource>();
                    sharedMapCreateAttempt = new HashSet<String>();
                    
                    // FIXME: Following triggers eglInitialize(..) which crashed on Windows w/ Chrome/Angle, FF/Angle!
                    defaultDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(EGL.EGL_DEFAULT_DISPLAY, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT);
                }
            }
        }
    }

    @Override
    protected final boolean isComplete() {
        return null != sharedMap; // null != eglES2DynamicLookupHelper || null != eglES1DynamicLookupHelper;
    }
  
    
    @Override
    protected final void destroy() {
        if(null != sharedMap) {
            if(DEBUG) {
                System.err.println("EGLDrawableFactory.destroy() .. ");
                dumpMap();
            }
            Collection<SharedResource> srl = sharedMap.values();
            for(Iterator<SharedResource> sri = srl.iterator(); sri.hasNext(); ) {
                SharedResource sr = sri.next();
                if(DEBUG) {
                    System.err.println("EGLDrawableFactory.destroy(): "+sr.device.toString());
                }
                sr.device.close();
            }
            sharedMap.clear();
            sharedMapCreateAttempt.clear();
            sharedMap = null;
            sharedMapCreateAttempt = null;
        }
        if(null != defaultSharedResource) {
            defaultSharedResource = null;
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
        EGLGraphicsConfigurationFactory.unregisterFactory();
        EGLDisplayUtil.shutdown(DEBUG);
    }
    
    private void dumpMap() {
        synchronized(sharedMap) {
            System.err.println("EGLDrawableFactory.map "+sharedMap.size());
            int i=0;
            Set<String> keys = sharedMap.keySet();
            for(Iterator<String> keyI = keys.iterator(); keyI.hasNext(); i++) {
                String key = keyI.next();
                SharedResource sr = sharedMap.get(key);
                System.err.println("EGLDrawableFactory.map["+i+"] "+key+" -> "+sr.getDevice()+", "+
                                   "es1   [avail "+sr.wasES1ContextCreated+", pbuffer "+sr.hasPBufferES1+", quirks "+sr.rendererQuirksES1+", ctp "+EGLContext.getGLVersion(1, 0, sr.ctpES1, null)+"], "+
                                   "es2/3 [es2 "+sr.wasES2ContextCreated+", es3 "+sr.wasES3ContextCreated+", [pbuffer "+sr.hasPBufferES3ES2+", quirks "+sr.rendererQuirksES3ES2+", ctp "+EGLContext.getGLVersion(2, 0, sr.ctpES3ES2, null)+"]]");
            }
            ;
        }
    }

    private HashMap<String /*uniqueKey*/, SharedResource> sharedMap = null;
    private HashSet<String> sharedMapCreateAttempt = null;    
    private EGLGraphicsDevice defaultDevice = null;
    private SharedResource defaultSharedResource = null;
    private boolean isANGLE = false;
    private boolean hasX11 = false;

    static class SharedResource implements SharedResourceRunner.Resource {
      private final EGLGraphicsDevice device;
      // private final EGLContext contextES1;
      // private final EGLContext contextES2;
      // private final EGLContext contextES3;
      private final boolean wasES1ContextCreated;
      private final boolean wasES2ContextCreated;
      private final boolean wasES3ContextCreated;
      private final GLRendererQuirks rendererQuirksES1;
      private final GLRendererQuirks rendererQuirksES3ES2;
      private final int ctpES1;
      private final int ctpES3ES2;
      private final boolean hasPBufferES1;
      private final boolean hasPBufferES3ES2;

      SharedResource(EGLGraphicsDevice dev, 
                     boolean wasContextES1Created, boolean hasPBufferES1, GLRendererQuirks rendererQuirksES1, int ctpES1,  
                     boolean wasContextES2Created, boolean wasContextES3Created, 
                     boolean hasPBufferES3ES2, GLRendererQuirks rendererQuirksES3ES2, int ctpES3ES2) {
          this.device = dev;
          // this.contextES1 = ctxES1;
          this.wasES1ContextCreated = wasContextES1Created;
          this.hasPBufferES1= hasPBufferES1;
          this.rendererQuirksES1 = rendererQuirksES1;
          this.ctpES1 = ctpES1;
          
          // this.contextES2 = ctxES2;
          // this.contextES3 = ctxES3;
          this.wasES2ContextCreated = wasContextES2Created;
          this.wasES3ContextCreated = wasContextES3Created;
          this.hasPBufferES3ES2= hasPBufferES3ES2;
          this.rendererQuirksES3ES2 = rendererQuirksES3ES2;
          this.ctpES3ES2 = ctpES3ES2;
      }
      @Override
      public final boolean isValid() {
          return wasES1ContextCreated || wasES2ContextCreated || wasES3ContextCreated;
      }
      @Override
      public final EGLGraphicsDevice getDevice() { return device; }
      // final EGLContext getContextES1() { return contextES1; }
      // final EGLContext getContextES2() { return contextES2; }
      // final EGLContext getContextES3() { return contextES3; }
      
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
      public GLRendererQuirks getRendererQuirks() {
          return null != rendererQuirksES3ES2 ? rendererQuirksES3ES2 : rendererQuirksES1 ;      
      }
    }

    @Override
    public final AbstractGraphicsDevice getDefaultDevice() {
      return defaultDevice;
    }

    @Override
    public final boolean getIsDeviceCompatible(AbstractGraphicsDevice device) {
      // via mappings (X11/WGL/.. -> EGL) we shall be able to handle all types.
      return null != sharedMap ; // null!=eglES2DynamicLookupHelper || null!=eglES1DynamicLookupHelper;
    }

    private static List<GLCapabilitiesImmutable> getAvailableEGLConfigs(EGLGraphicsDevice eglDisplay, GLCapabilitiesImmutable caps) {
        final IntBuffer numConfigs = Buffers.newDirectIntBuffer(1);
        if(!EGL.eglGetConfigs(eglDisplay.getHandle(), null, 0, numConfigs)) {
            throw new GLException("EGLDrawableFactory.getAvailableEGLConfigs: Get maxConfigs (eglGetConfigs) call failed, error "+EGLContext.toHexString(EGL.eglGetError()));
        }
        if(0 < numConfigs.get(0)) {
            final PointerBuffer configs = PointerBuffer.allocateDirect(numConfigs.get(0));
            final IntBuffer attrs = EGLGraphicsConfiguration.GLCapabilities2AttribList(caps);
            final int winattrmask = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(caps);
            if( EGL.eglChooseConfig(eglDisplay.getHandle(), attrs, configs, configs.capacity(), numConfigs) && numConfigs.get(0) > 0) {
                return EGLGraphicsConfigurationFactory.eglConfigs2GLCaps(eglDisplay, caps.getGLProfile(), configs, numConfigs.get(0), winattrmask, false /* forceTransparentFlag */);
            }
        }
        return new ArrayList<GLCapabilitiesImmutable>(0);
    }
    
    private boolean mapAvailableEGLESConfig(AbstractGraphicsDevice adevice, int esProfile, 
                                            boolean[] hasPBuffer, GLRendererQuirks[] rendererQuirks, int[] ctp) {
        final String profileString;
        switch( esProfile ) {
            case 3:
                profileString = GLProfile.GLES3; break; 
            case 2:
                profileString = GLProfile.GLES2; break; 
            case 1: 
                profileString = GLProfile.GLES1; break;
            default: 
                throw new GLException("Invalid ES profile number "+esProfile);
        }
        if ( !GLProfile.isAvailable(adevice, profileString) ) {
            if( DEBUG ) {
                System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: "+profileString+" n/a on "+adevice);
            }
            return false;
        }
        final GLProfile glp = GLProfile.get(adevice, profileString) ;
        final GLDrawableFactoryImpl desktopFactory = (GLDrawableFactoryImpl) GLDrawableFactory.getDesktopFactory();
        final boolean mapsADeviceToDefaultDevice = !QUERY_EGL_ES_NATIVE_TK || null == desktopFactory || adevice instanceof EGLGraphicsDevice ;
        if( DEBUG ) {
            System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: "+profileString+" ( "+esProfile+" ), "+
                               "defaultSharedResourceSet "+(null!=defaultSharedResource)+", mapsADeviceToDefaultDevice "+mapsADeviceToDefaultDevice+
                               " (QUERY_EGL_ES_NATIVE_TK "+QUERY_EGL_ES_NATIVE_TK+", hasDesktopFactory "+(null != desktopFactory)+
                               ", isEGLGraphicsDevice "+(adevice instanceof EGLGraphicsDevice)+")");
        }

        EGLGraphicsDevice eglDevice = null;
        NativeSurface surface = null;
        ProxySurface upstreamSurface = null; // X11, GLX, ..
        boolean success = false;
        boolean deviceFromUpstreamSurface = false;
        try {            
            final GLCapabilities reqCapsAny = new GLCapabilities(glp);
            reqCapsAny.setRedBits(5); reqCapsAny.setGreenBits(5); reqCapsAny.setBlueBits(5); reqCapsAny.setAlphaBits(0);
            reqCapsAny.setDoubleBuffered(false);
            
            if( mapsADeviceToDefaultDevice ) {
                // In this branch, any non EGL device is mapped to EGL default shared resources (default behavior).
                // Only one default shared resource instance is ever be created. 
                final GLCapabilitiesImmutable reqCapsPBuffer = GLGraphicsConfigurationUtil.fixGLPBufferGLCapabilities(reqCapsAny);
                final List<GLCapabilitiesImmutable> availablePBufferCapsL = getAvailableEGLConfigs(defaultDevice, reqCapsPBuffer);
                hasPBuffer[0] = availablePBufferCapsL.size() > 0;
                
                // 1st case: adevice is not the EGL default device, map default shared resources
                if( adevice != defaultDevice ) {
                    if(null == defaultSharedResource) {
                        return false;
                    }                    
                    switch(esProfile) {
                        case 3:
                            if( !defaultSharedResource.wasES3ContextCreated ) {
                                return false;
                            }
                            rendererQuirks[0] = defaultSharedResource.rendererQuirksES3ES2;
                            ctp[0] = defaultSharedResource.ctpES3ES2;
                            break;
                        case 2: 
                            if( !defaultSharedResource.wasES2ContextCreated ) {
                                return false;
                            }
                            rendererQuirks[0] = defaultSharedResource.rendererQuirksES3ES2;
                            ctp[0] = defaultSharedResource.ctpES3ES2;
                            break;
                        case 1: 
                            if( !defaultSharedResource.wasES1ContextCreated ) {
                                return false;
                            }
                            rendererQuirks[0] = defaultSharedResource.rendererQuirksES1;
                            ctp[0] = defaultSharedResource.ctpES1;
                            break;
                    }
                    EGLContext.mapStaticGLVersion(adevice, esProfile, 0, ctp[0]);
                    return true;
                }
                
                // attempt to created the default shared resources ..
                
                eglDevice = defaultDevice; // reuse
                
                if( hasPBuffer[0] ) {
                    // 2nd case create defaultDevice shared resource using pbuffer surface
                    surface = createDummySurfaceImpl(eglDevice, false, reqCapsPBuffer, reqCapsPBuffer, null, 64, 64); // egl pbuffer offscreen
                    upstreamSurface = (ProxySurface)surface;
                    upstreamSurface.createNotify();
                    deviceFromUpstreamSurface = false;
                } else {
                    // 3rd case fake creation of defaultDevice shared resource, no pbuffer available
                    final List<GLCapabilitiesImmutable> capsAnyL = getAvailableEGLConfigs(eglDevice, reqCapsAny);
                    if(capsAnyL.size() > 0) {
                        final GLCapabilitiesImmutable chosenCaps = capsAnyL.get(0);
                        EGLContext.mapStaticGLESVersion(eglDevice, chosenCaps);
                        success = true;
                    }
                    if(DEBUG) {
                        System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig() no pbuffer config available, detected !pbuffer config: "+success);
                        EGLGraphicsConfigurationFactory.printCaps("!PBufferCaps", capsAnyL, System.err);
                    }                    
                }                
            } else {
                // 4th case always creates a true mapping of given device to EGL                
                surface = desktopFactory.createDummySurface(adevice, reqCapsAny, null, 64, 64); // X11, WGL, .. dummy window
                upstreamSurface = ( surface instanceof ProxySurface ) ? (ProxySurface)surface : null ;
                if(null != upstreamSurface) {
                    upstreamSurface.createNotify();
                }                    
                eglDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(surface);
                deviceFromUpstreamSurface = true;
                hasPBuffer[0] = true;
            }

            if(null != surface) {
                final EGLDrawable drawable = (EGLDrawable) createOnscreenDrawableImpl ( surface ); // works w/ implicit pbuffer surface via proxy-hook
                drawable.setRealized(true);
                final EGLContext context = (EGLContext) drawable.createContext(null);
                if (null != context) {
                    try {
                        context.makeCurrent(); // could cause exception
                        if(context.isCurrent()) {
                            final String glVersion = context.getGL().glGetString(GL.GL_VERSION);
                            if(null != glVersion) {                                
                                context.mapCurrentAvailableGLVersion(eglDevice);
                                if(eglDevice != adevice) {
                                    context.mapCurrentAvailableGLVersion(adevice);
                                }
                                rendererQuirks[0] = context.getRendererQuirks();
                                ctp[0] = context.getContextOptions();
                                success = true;
                            } else {
                                // Oops .. something is wrong
                                if(DEBUG) {
                                    System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: "+eglDevice+", "+context.getGLVersion()+" - VERSION is null, dropping availability!");                                
                                }
                            }
                        }
                    } catch (GLException gle) {
                        if (DEBUG) {
                            System.err.println("EGLDrawableFactory.mapAvailableEGLESConfig: INFO: context create/makeCurrent failed");
                            gle.printStackTrace();
                        }
                    } finally {
                        context.destroy();
                    }
                }
                drawable.setRealized(false);
            }
        } catch (Throwable t) {
            if(DEBUG) {
                System.err.println("Catched Exception on thread "+getThreadName()); 
                t.printStackTrace();
            }
            success = false;
        } finally {
            if(eglDevice == defaultDevice) {
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

    private final boolean needsToCreateSharedResource(String key, SharedResource[] existing) {
        synchronized(sharedMap) {
            final SharedResource sr = sharedMap.get(key);
            if( null == sr ) {
                final boolean createAttempted = sharedMapCreateAttempt.contains(key);
                if(!createAttempted) {
                    sharedMapCreateAttempt.add(key);
                }
                return !createAttempted;
            } else {
                if(null != existing) {
                    existing[0] = sr;
                }
                return false;
            }
        }        
    }
    
    @Override
    protected final SharedResource getOrCreateSharedResourceImpl(AbstractGraphicsDevice adevice) {
        if(null == sharedMap) { // null == eglES1DynamicLookupHelper && null == eglES2DynamicLookupHelper
            return null;
        }

        if( needsToCreateSharedResource(defaultDevice.getUniqueID(), null) ) {
            if (DEBUG) {
                System.err.println("EGLDrawableFactory.createShared: (defaultDevice): req. device: "+adevice+", defaultDevice "+defaultDevice);
                Thread.dumpStack();
            }
            if(null != defaultSharedResource) {
                dumpMap();
                throw new InternalError("defaultSharedResource already exist: "+defaultSharedResource);
            }
            defaultSharedResource = createEGLSharedResourceImpl(defaultDevice);            
        }
        
        final String key = adevice.getUniqueID();
        if( defaultDevice.getUniqueID().equals(key) ) {
            return defaultSharedResource;
        } else {
            if( null == defaultSharedResource) { // defaultDevice must be initialized before host-device 
                dumpMap();
                throw new InternalError("defaultSharedResource does not exist");            
            }
            final SharedResource[] existing = new SharedResource[] { null };
            if ( !needsToCreateSharedResource(key, existing) ) {
                return existing[0];
            }            
            return createEGLSharedResourceImpl(adevice);
        }
    }
    
    private SharedResource createEGLSharedResourceImpl(AbstractGraphicsDevice adevice) {
        final boolean madeCurrentES1;            
        final boolean madeCurrentES2;
        final boolean madeCurrentES3;
        boolean[] hasPBufferES1 = new boolean[] { false };
        boolean[] hasPBufferES3ES2 = new boolean[] { false };
        // EGLContext[] eglCtxES1 = new EGLContext[] { null };
        // EGLContext[] eglCtxES2 = new EGLContext[] { null };
        GLRendererQuirks[] rendererQuirksES1 = new GLRendererQuirks[] { null };
        GLRendererQuirks[] rendererQuirksES3ES2 = new GLRendererQuirks[] { null };
        int[] ctpES1 = new int[] { -1 };
        int[] ctpES3ES2 = new int[] { -1 };
        
        
        if (DEBUG) {
            System.err.println("EGLDrawableFactory.createShared(): device "+adevice);
        }
        
        if( null != eglES1DynamicLookupHelper ) {
            madeCurrentES1 = mapAvailableEGLESConfig(adevice, 1, hasPBufferES1, rendererQuirksES1, ctpES1);
        } else {
            madeCurrentES1 = false;
        }
        if( null != eglES2DynamicLookupHelper ) {
            madeCurrentES3 = mapAvailableEGLESConfig(adevice, 3, hasPBufferES3ES2, rendererQuirksES3ES2, ctpES3ES2);
            if( madeCurrentES3 ) {
                madeCurrentES2 = true;
                EGLContext.mapStaticGLVersion(adevice, 2, 0, ctpES3ES2[0]);
            } else {
                madeCurrentES2 = mapAvailableEGLESConfig(adevice, 2, hasPBufferES3ES2, rendererQuirksES3ES2, ctpES3ES2);
            }
        } else {
            madeCurrentES2 = false;
            madeCurrentES3 = false;
        }
        
        if( !EGLContext.getAvailableGLVersionsSet(adevice) ) {
            // Even though we override the non EGL native mapping intentionally,
            // avoid exception due to double 'set' - carefull exception of the rule. 
            EGLContext.setAvailableGLVersionsSet(adevice);
        }
        if( hasX11 ) {
            handleDontCloseX11DisplayQuirk(rendererQuirksES1[0]);
            handleDontCloseX11DisplayQuirk(rendererQuirksES3ES2[0]);
        }
        final SharedResource sr = new SharedResource(defaultDevice, madeCurrentES1, hasPBufferES1[0], rendererQuirksES1[0], ctpES1[0],
                                                                    madeCurrentES2, madeCurrentES3, hasPBufferES3ES2[0], rendererQuirksES3ES2[0], ctpES3ES2[0]);
        
        synchronized(sharedMap) {
            sharedMap.put(adevice.getUniqueID(), sr);
        }
        if (DEBUG) {
            System.err.println("EGLDrawableFactory.createShared: devices: queried nativeTK "+QUERY_EGL_ES_NATIVE_TK+", adevice " + adevice + ", defaultDevice " + defaultDevice);
            System.err.println("EGLDrawableFactory.createShared: context ES1: " + madeCurrentES1 + ", hasPBuffer "+hasPBufferES1[0]);
            System.err.println("EGLDrawableFactory.createShared: context ES2: " + madeCurrentES2 + ", hasPBuffer "+hasPBufferES3ES2[0]);
            System.err.println("EGLDrawableFactory.createShared: context ES3: " + madeCurrentES3 + ", hasPBuffer "+hasPBufferES3ES2[0]);
            dumpMap();
        }
        return sr;
    }
    
    private void handleDontCloseX11DisplayQuirk(GLRendererQuirks quirks) {
        if( null != quirks && quirks.exist( GLRendererQuirks.DontCloseX11Display ) ) {
            jogamp.nativewindow.x11.X11Util.markAllDisplaysUnclosable();
        }
    }

    @Override
    protected final Thread getSharedResourceThread() {
        return null;
    }

    public final boolean isANGLE() {
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
        if(null == sharedMap) { // null == eglES1DynamicLookupHelper && null == eglES2DynamicLookupHelper
            return new ArrayList<GLCapabilitiesImmutable>(); // null
        }
        return EGLGraphicsConfigurationFactory.getAvailableCapabilities(this, device);
    }

    @Override
    protected GLDrawableImpl createOnscreenDrawableImpl(NativeSurface target) {
        if (target == null) {
          throw new IllegalArgumentException("Null target");
        }
        return new EGLOnscreenDrawable(this, EGLWrappedSurface.get(target));
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
        return new EGLPbufferDrawable(this, EGLWrappedSurface.get(target));
    }

    @Override
    public boolean canCreateGLPbuffer(AbstractGraphicsDevice device, GLProfile glp) {
        // SharedResource sr = getOrCreateEGLSharedResource(device);
        // return sr.hasES1PBuffer() || sr.hasES2PBuffer();
        return true;
    }

    @Override
    protected ProxySurface createMutableSurfaceImpl(AbstractGraphicsDevice deviceReq, boolean createNewDevice, 
                                                    GLCapabilitiesImmutable capsChosen, GLCapabilitiesImmutable capsRequested, 
                                                    GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstreamHook) {
        final boolean ownDevice;
        final EGLGraphicsDevice device;
        if( createNewDevice || ! (deviceReq instanceof EGLGraphicsDevice) ) {
            final long nativeDisplayID = ( deviceReq instanceof EGLGraphicsDevice) ?
                    ( (EGLGraphicsDevice) deviceReq ).getNativeDisplayID() : deviceReq.getHandle() ;
            device = EGLDisplayUtil.eglCreateEGLGraphicsDevice(nativeDisplayID, deviceReq.getConnection(), deviceReq.getUnitID());
            ownDevice = true;
        } else {
            device = (EGLGraphicsDevice) deviceReq;
            ownDevice = false;
        }
        final DefaultGraphicsScreen screen = new DefaultGraphicsScreen(device, 0);
        final EGLGraphicsConfiguration config = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsChosen, capsRequested, chooser, screen, VisualIDHolder.VID_UNDEFINED, false);
        if(null == config) {
            throw new GLException("Choosing GraphicsConfiguration failed w/ "+capsChosen+" on "+screen); 
        }    
        return new WrappedSurface(config, 0, upstreamHook, ownDevice);
    }
    
    @Override
    public final ProxySurface createDummySurfaceImpl(AbstractGraphicsDevice deviceReq, boolean createNewDevice, 
                                                     GLCapabilitiesImmutable chosenCaps, GLCapabilitiesImmutable requestedCaps, GLCapabilitiesChooser chooser, int width, int height) {
        chosenCaps = GLGraphicsConfigurationUtil.fixOffscreenBitOnly(chosenCaps); // complete validation in EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(..) above         
        return createMutableSurfaceImpl(deviceReq, createNewDevice, chosenCaps, requestedCaps, chooser, new EGLDummyUpstreamSurfaceHook(width, height));
    }
    
    /**
     * @param ms {@link MutableSurface} which dimensions and config are being used to create the pbuffer surface. 
     *           It will also hold the resulting pbuffer surface handle. 
     * @param useTexture
     * @return the passed {@link MutableSurface} which now has the EGL pbuffer surface set as it's handle
     */
    protected static MutableSurface createPBufferSurfaceImpl(MutableSurface ms, boolean useTexture) {
        return null;
    }
    protected static long createPBufferSurfaceImpl(EGLGraphicsConfiguration config, int width, int height, boolean useTexture) {
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
    protected ProxySurface createProxySurfaceImpl(AbstractGraphicsDevice deviceReq, int screenIdx, long windowHandle, GLCapabilitiesImmutable capsRequested, GLCapabilitiesChooser chooser, UpstreamSurfaceHook upstream) {
        final EGLGraphicsDevice eglDeviceReq = (EGLGraphicsDevice) deviceReq;
        final EGLGraphicsDevice device = EGLDisplayUtil.eglCreateEGLGraphicsDevice(eglDeviceReq.getNativeDisplayID(), deviceReq.getConnection(), deviceReq.getUnitID());
        final DefaultGraphicsScreen screen = new DefaultGraphicsScreen(device, screenIdx);
        final EGLGraphicsConfiguration cfg = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(capsRequested, capsRequested, chooser, screen, VisualIDHolder.VID_UNDEFINED, false);
        return new WrappedSurface(cfg, windowHandle, upstream, true);
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
}
