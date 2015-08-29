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
 */

package jogamp.opengl.egl;

import java.io.PrintStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.nativewindow.VisualIDHolder.VIDType;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.GLGraphicsConfigurationFactory;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.nio.PointerBuffer;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.egl.EGL;


/** Subclass of GraphicsConfigurationFactory used when non-AWT tookits
    are used on X11 platforms. Toolkits will likely need to delegate
    to this one to change the accepted and returned types of the
    GraphicsDevice and GraphicsConfiguration abstractions. */

public class EGLGraphicsConfigurationFactory extends GLGraphicsConfigurationFactory {
    static VisualIDHolder.VIDComparator EglCfgIDComparator = new VisualIDHolder.VIDComparator(VisualIDHolder.VIDType.EGL_CONFIG);
    static GraphicsConfigurationFactory nativeGraphicsConfigurationFactory = null;
    static GraphicsConfigurationFactory kdeglGraphicsConfigurationFactory = null;
    static GraphicsConfigurationFactory fallbackGraphicsConfigurationFactory = null;

    static void registerFactory() {
        final GraphicsConfigurationFactory eglFactory = new EGLGraphicsConfigurationFactory();

        // become the pre-selector for X11/.. to match the native visual id w/ EGL, if native ES is selected
        final String nwType = NativeWindowFactory.getNativeWindowType(false);
        if(NativeWindowFactory.TYPE_X11 == nwType) {
            nativeGraphicsConfigurationFactory = GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.x11.X11GraphicsDevice.class, GLCapabilitiesImmutable.class, eglFactory);
            if(null != nativeGraphicsConfigurationFactory) {
                fallbackGraphicsConfigurationFactory = nativeGraphicsConfigurationFactory;
            } else {
                fallbackGraphicsConfigurationFactory = GraphicsConfigurationFactory.getFactory(com.jogamp.nativewindow.x11.X11GraphicsDevice.class, CapabilitiesImmutable.class);
            }
        } /* else if(NativeWindowFactory.TYPE_WINDOWS == NativeWindowFactory.getNativeWindowType(false)) {
            nativeGraphicsConfigurationFactory = GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.windows.WindowsGraphicsDevice.class, eglFactory);
        } else if(NativeWindowFactory.TYPE_MACOSX == NativeWindowFactory.getNativeWindowType(false)) {
        } */

        // become the selector for KD/EGL ..
        kdeglGraphicsConfigurationFactory = GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.egl.EGLGraphicsDevice.class, GLCapabilitiesImmutable.class, eglFactory);
    }

    static void unregisterFactory() {
        final String nwType = NativeWindowFactory.getNativeWindowType(false);
        if(NativeWindowFactory.TYPE_X11 == nwType) {
            GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.x11.X11GraphicsDevice.class, GLCapabilitiesImmutable.class, nativeGraphicsConfigurationFactory);
        } /* else if(NativeWindowFactory.TYPE_WINDOWS == NativeWindowFactory.getNativeWindowType(false)) {
            GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.windows.WindowsGraphicsDevice.class, nativeGraphicsConfigurationFactory);
        } else if(NativeWindowFactory.TYPE_MACOSX == NativeWindowFactory.getNativeWindowType(false)) {
        } */
        nativeGraphicsConfigurationFactory = null;
        fallbackGraphicsConfigurationFactory = null;

        GraphicsConfigurationFactory.registerFactory(com.jogamp.nativewindow.egl.EGLGraphicsDevice.class, GLCapabilitiesImmutable.class, kdeglGraphicsConfigurationFactory);
        kdeglGraphicsConfigurationFactory = null;
    }

    private EGLGraphicsConfigurationFactory() {
    }

    @Override
    protected AbstractGraphicsConfiguration chooseGraphicsConfigurationImpl (
            final CapabilitiesImmutable capsChosen, final CapabilitiesImmutable capsRequested,
            final CapabilitiesChooser chooser, final AbstractGraphicsScreen absScreen, final int nativeVisualID) {
        if (absScreen == null) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only AbstractGraphicsDevice objects");
        }

        if (! (capsChosen instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - chosen");
        }
        final GLCapabilitiesImmutable glCapsChosen = (GLCapabilitiesImmutable) capsChosen;

        if (! (capsRequested instanceof GLCapabilitiesImmutable) ) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilities objects - requested");
        }

        if (chooser != null &&
            !(chooser instanceof GLCapabilitiesChooser)) {
            throw new IllegalArgumentException("This NativeWindowFactory accepts only GLCapabilitiesChooser objects");
        }

        final AbstractGraphicsDevice absDevice = absScreen.getDevice();
        if(null==absDevice) {
            throw new GLException("Null AbstractGraphicsDevice");
        }

        AbstractGraphicsConfiguration cfg = null;

        if( absDevice instanceof EGLGraphicsDevice ) {
            cfg = chooseGraphicsConfigurationStatic((GLCapabilitiesImmutable) capsChosen,
                                                    (GLCapabilitiesImmutable) capsRequested,
                                                    (GLCapabilitiesChooser) chooser,
                                                    absScreen, nativeVisualID, false);
        } else {
            // handle non native cases (X11, ..)
            if(null == fallbackGraphicsConfigurationFactory) {
                throw new InternalError("Native fallback GraphicsConfigurationFactory is null, but call issued for device: "+absDevice+" of type "+absDevice.getClass().getSimpleName());
            }

            if(glCapsChosen.getGLProfile().usesNativeGLES()) {
                if(DEBUG) {
                    System.err.println("EGLGraphicsConfigurationFactory.choose..: Handle native device "+absDevice.getClass().getSimpleName());
                }
                cfg = chooseGraphicsConfigurationStatic((GLCapabilitiesImmutable) capsChosen,
                                                        (GLCapabilitiesImmutable) capsRequested,
                                                        (GLCapabilitiesChooser) chooser,
                                                        absScreen, nativeVisualID, false);
                if(null == cfg || VisualIDHolder.VID_UNDEFINED == cfg.getVisualID(VIDType.NATIVE)) {
                    cfg = null;
                    if(DEBUG) {
                        System.err.println("EGLGraphicsConfigurationFactory.choose..: No native visual ID, fallback ..");
                    }
                }
            }
            if(null == cfg) {
                // fwd to native config factory (only X11 for now)
                if(DEBUG) {
                    System.err.println("EGLGraphicsConfigurationFactory.choose..: Delegate to "+fallbackGraphicsConfigurationFactory.getClass().getSimpleName());
                }
                cfg = fallbackGraphicsConfigurationFactory.chooseGraphicsConfiguration(capsChosen, capsRequested, chooser, absScreen, nativeVisualID);
            }
        }
        return cfg;
    }

    protected static List<GLCapabilitiesImmutable> getAvailableCapabilities(final EGLDrawableFactory factory, final AbstractGraphicsDevice device) {
        final EGLDrawableFactory.SharedResource sharedResource = factory.getOrCreateSharedResourceImpl(device);
        if(null == sharedResource) {
            throw new GLException("Shared resource for device n/a: "+device);
        }
        final EGLGraphicsDevice eglDevice = sharedResource.getDevice();
        final long eglDisplay = eglDevice.getHandle();
        if(0 == eglDisplay) {
            throw new GLException("null eglDisplay");
        }
        List<GLCapabilitiesImmutable> availableCaps = null;
        final IntBuffer numConfigs = Buffers.newDirectIntBuffer(1);

        if(!EGL.eglGetConfigs(eglDisplay, null, 0, numConfigs)) {
            throw new GLException("Graphics configuration get maxConfigs (eglGetConfigs) call failed, error "+toHexString(EGL.eglGetError()));
        }
        if(0 == numConfigs.get(0)) {
            throw new GLException("Graphics configuration get maxConfigs (eglGetConfigs) no configs");
        }

        final PointerBuffer configs = PointerBuffer.allocateDirect(numConfigs.get(0));

        if(!EGL.eglGetConfigs(eglDisplay, configs, configs.capacity(), numConfigs)) {
            throw new GLException("Graphics configuration get all configs (eglGetConfigs) call failed, error "+toHexString(EGL.eglGetError()));
        }
        if (numConfigs.get(0) > 0) {
            availableCaps = eglConfigs2GLCaps(eglDevice, null, configs, numConfigs.get(0), GLGraphicsConfigurationUtil.ALL_BITS, false /* forceTransparentFlag */, false /* onlyFirstValid */);
            if( null != availableCaps && availableCaps.size() > 1) {
                Collections.sort(availableCaps, EglCfgIDComparator);
            }
        }
        return availableCaps;
    }

    public static EGLGraphicsConfiguration chooseGraphicsConfigurationStatic(GLCapabilitiesImmutable capsChosen,
                                                                             final GLCapabilitiesImmutable capsReq,
                                                                             final GLCapabilitiesChooser chooser,
                                                                             final AbstractGraphicsScreen absScreen, final int nativeVisualID,
                                                                             final boolean forceTransparentFlag) {
        if (capsChosen == null) {
            capsChosen = new GLCapabilities(null);
        }

        if(null==absScreen) {
            throw new GLException("Null AbstractGraphicsScreen");
        }
        final AbstractGraphicsDevice absDevice = absScreen.getDevice();
        if(null==absDevice) {
            throw new GLException("Null AbstractGraphicsDevice");
        }

        final EGLGraphicsDevice eglDevice;
        final boolean ownEGLDisplay;
        if( absDevice instanceof EGLGraphicsDevice ) {
            eglDevice = (EGLGraphicsDevice) absDevice;
            if (eglDevice.getHandle() == EGL.EGL_NO_DISPLAY) {
                throw new GLException("Invalid EGL display: "+eglDevice);
            }
            ownEGLDisplay = false;
        } else {
            eglDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(absDevice);
            eglDevice.open();
            ownEGLDisplay = true;
        }

        final GLProfile glp = capsChosen.getGLProfile();
        capsChosen = GLGraphicsConfigurationUtil.fixGLCapabilities( capsChosen, GLDrawableFactory.getEGLFactory(), absDevice);
        EGLGraphicsConfiguration res = eglChooseConfig(eglDevice, capsChosen, capsReq, chooser, absScreen, nativeVisualID, forceTransparentFlag);
        if(null==res) {
            if(DEBUG) {
                System.err.println("eglChooseConfig failed with given capabilities "+capsChosen);
            }

            // Last try .. add a fixed embedded profile [ATI, Nokia, Intel, ..]
            //
            // rgb888 - d16, s4
            final GLCapabilities fixedCaps = new GLCapabilities(glp);
            fixedCaps.setSampleBuffers(true);
            fixedCaps.setNumSamples(4);
            fixedCaps.setRedBits(8);
            fixedCaps.setGreenBits(8);
            fixedCaps.setBlueBits(8);
            fixedCaps.setDepthBits(16);
            if( !capsChosen.isOnscreen() ) {
                fixedCaps.setOnscreen(false);
                fixedCaps.setPBuffer(capsChosen.isPBuffer());
                fixedCaps.setFBO(capsChosen.isFBO());
            }
            if(DEBUG) {
                System.err.println("trying fixed caps (1): "+fixedCaps);
            }
            res = eglChooseConfig(eglDevice, fixedCaps, capsReq, chooser, absScreen, nativeVisualID, false);
        }
        if(null==res) {
            //
            // rgb565 - d16, s0
            final GLCapabilities fixedCaps = new GLCapabilities(glp);
            fixedCaps.setRedBits(5);
            fixedCaps.setGreenBits(6);
            fixedCaps.setBlueBits(5);
            fixedCaps.setDepthBits(16);
            if( !capsChosen.isOnscreen() ) {
                fixedCaps.setOnscreen(false);
                fixedCaps.setPBuffer(capsChosen.isPBuffer());
                fixedCaps.setFBO(capsChosen.isFBO());
            }
            if(DEBUG) {
                System.err.println("trying fixed caps (2): "+fixedCaps);
            }
            res = eglChooseConfig(eglDevice, fixedCaps, capsReq, chooser, absScreen, nativeVisualID, false);
        }
        if(null==res) {
            //
            // rgb565 - d16, s4
            final GLCapabilities fixedCaps = new GLCapabilities(glp);
            fixedCaps.setSampleBuffers(true);
            fixedCaps.setNumSamples(4);
            fixedCaps.setRedBits(5);
            fixedCaps.setGreenBits(6);
            fixedCaps.setBlueBits(5);
            fixedCaps.setDepthBits(16);
            if( !capsChosen.isOnscreen() ) {
                fixedCaps.setOnscreen(false);
                fixedCaps.setPBuffer(capsChosen.isPBuffer());
                fixedCaps.setFBO(capsChosen.isFBO());
            }
            if(DEBUG) {
                System.err.println("trying fixed caps (3): "+fixedCaps);
            }
            res = eglChooseConfig(eglDevice, fixedCaps, capsReq, chooser, absScreen, nativeVisualID, false);
        }
        if(null==res) {
            throw new GLException("Graphics configuration failed [direct caps, eglGetConfig/chooser and fixed-caps(1-3)]");
        }
        if(ownEGLDisplay) {
            ((EGLGLCapabilities) res.getChosenCapabilities()).setEGLConfig(0); // eglDisplay: EOL
            eglDevice.close();
        }
        return res;
    }


    static EGLGraphicsConfiguration eglChooseConfig(final EGLGraphicsDevice device,
                                                    final GLCapabilitiesImmutable capsChosen, final GLCapabilitiesImmutable capsRequested,
                                                    final GLCapabilitiesChooser chooser,
                                                    final AbstractGraphicsScreen absScreen,
                                                    final int nativeVisualID, final boolean forceTransparentFlag) {
        final long eglDisplay = device.getHandle();
        final GLProfile glp = capsChosen.getGLProfile();
        final int winattrmask = GLGraphicsConfigurationUtil.getExclusiveWinAttributeBits(capsChosen);
        List<GLCapabilitiesImmutable> availableCaps = null;
        int recommendedIndex = -1;
        final IntBuffer numConfigs = Buffers.newDirectIntBuffer(1);

        if(!EGL.eglGetConfigs(eglDisplay, null, 0, numConfigs)) {
            throw new GLException("EGLGraphicsConfiguration.eglChooseConfig: Get maxConfigs (eglGetConfigs) call failed, error "+toHexString(EGL.eglGetError()));
        }
        if(0 == numConfigs.get(0)) {
            throw new GLException("EGLGraphicsConfiguration.eglChooseConfig: Get maxConfigs (eglGetConfigs) no configs");
        }
        final int numEGLConfigs = numConfigs.get(0);
        if (DEBUG) {
            System.err.println("EGLGraphicsConfiguration.eglChooseConfig: eglChooseConfig eglDisplay "+toHexString(eglDisplay)+
                               ", nativeVisualID "+toHexString(nativeVisualID)+
                               ", capsChosen "+capsChosen+", winbits "+GLGraphicsConfigurationUtil.winAttributeBits2String(null, winattrmask).toString()+
                               ", fboAvail "+GLContext.isFBOAvailable(device, glp)+
                               ", device "+device+", "+device.getUniqueID()+
                               ", numEGLConfigs "+numEGLConfigs);
        }

        final IntBuffer attrs = EGLGraphicsConfiguration.GLCapabilities2AttribList(capsChosen);
        final PointerBuffer configs = PointerBuffer.allocateDirect(numConfigs.get(0));

        // 1st choice: get GLCapabilities based on users GLCapabilities
        //             setting recommendedIndex as preferred choice
        // skipped if nativeVisualID is given
        final boolean hasEGLChosenCaps;
        if( VisualIDHolder.VID_UNDEFINED == nativeVisualID ) {
            if( !EGL.eglChooseConfig(eglDisplay, attrs, configs, configs.capacity(), numConfigs) ) {
                if(DEBUG) {
                    System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #1 eglChooseConfig: false");
                }
                numConfigs.put(0, 0);
                hasEGLChosenCaps = false;
            } else {
                hasEGLChosenCaps = numConfigs.get(0)>0;
            }
        } else {
            if(DEBUG) {
                System.err.println("EGLGraphicsConfiguration.eglChooseConfig: Skipped due to given visualID: "+toHexString(nativeVisualID));
            }
            hasEGLChosenCaps = false;
        }
        final boolean useRecommendedIndex = hasEGLChosenCaps && !forceTransparentFlag && capsChosen.isBackgroundOpaque(); // only use recommended idx if not translucent
        final boolean skipCapsChooser = null == chooser && useRecommendedIndex; // fast path: skip choosing if using recommended idx and null chooser is used
        if( hasEGLChosenCaps ) {
            availableCaps = eglConfigs2GLCaps(device, glp, configs, numConfigs.get(0), winattrmask, forceTransparentFlag, skipCapsChooser /* onlyFirsValid */);
            if(availableCaps.size() > 0) {
                final long recommendedEGLConfig =  configs.get(0);
                recommendedIndex = 0;
                if (DEBUG) {
                    System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #1 eglChooseConfig: recommended fbcfg " + toHexString(recommendedEGLConfig) + ", idx " + recommendedIndex);
                    System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #1 useRecommendedIndex "+useRecommendedIndex+", skipCapsChooser "+skipCapsChooser);
                    System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #1 fbcfg caps " + availableCaps.get(recommendedIndex));
                }
            } else if (DEBUG) {
                System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #1 eglChooseConfig: no caps for recommended fbcfg " + toHexString(configs.get(0)));
                System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #1 useRecommendedIndex "+useRecommendedIndex+", skipCapsChooser "+skipCapsChooser);
            }
        } else if (DEBUG) {
            System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #1 eglChooseConfig: no configs");
            System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #1 useRecommendedIndex "+useRecommendedIndex+", skipCapsChooser "+skipCapsChooser);
        }

        // 2nd choice: get all GLCapabilities available, no preferred recommendedIndex available
        if( null == availableCaps || 0 == availableCaps.size() ) {
            // reset ..
            recommendedIndex = -1;

            if(!EGL.eglGetConfigs(eglDisplay, configs, configs.capacity(), numConfigs)) {
                throw new GLException("EGLGraphicsConfiguration.eglChooseConfig: #2 Get all configs (eglGetConfigs) call failed, error "+toHexString(EGL.eglGetError()));
            }
            if (numConfigs.get(0) > 0) {
                availableCaps = eglConfigs2GLCaps(device, glp, configs, numConfigs.get(0), winattrmask, forceTransparentFlag, false /* onlyFirsValid */);
            }
        }

        if( null == availableCaps || 0 == availableCaps.size() ) {
            if(DEBUG) {
                // FIXME: this happens on a ATI PC Emulation ..
                System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #2 Graphics configuration 1st choice and 2nd choice failed - no configs");
                availableCaps = eglConfigs2GLCaps(device, glp, configs, numConfigs.get(0), GLGraphicsConfigurationUtil.ALL_BITS, forceTransparentFlag, false /* onlyFirsValid */);
                printCaps("AllCaps", availableCaps, System.err);
            }
            return null;
        }

        if(DEBUG) {
            System.err.println("EGLGraphicsConfiguration.eglChooseConfig: got configs: "+availableCaps.size());
            for(int i=0; i<availableCaps.size(); i++) {
                System.err.println(i+": "+availableCaps.get(i));
            }
        }

        if( VisualIDHolder.VID_UNDEFINED != nativeVisualID ) { // implies !hasEGLChosenCaps
            final List<GLCapabilitiesImmutable> removedCaps = new ArrayList<GLCapabilitiesImmutable>();
            for(int i=0; i<availableCaps.size(); ) {
                final GLCapabilitiesImmutable aCap = availableCaps.get(i);
                if(aCap.getVisualID(VIDType.NATIVE) != nativeVisualID) {
                    if(DEBUG) { System.err.println("Remove["+i+"] (mismatch VisualID): "+aCap); }
                    removedCaps.add(availableCaps.remove(i));
                } else if( 0 == aCap.getDepthBits() && 0 < capsChosen.getDepthBits() ) {
                    // Hack for HiSilicon/Vivante/Immersion.16 Renderer ..
                    if(DEBUG) { System.err.println("Remove["+i+"] (mismatch depth-bits): "+aCap); }
                    removedCaps.add(availableCaps.remove(i));
                } else {
                    i++;
                }
            }
            if(0==availableCaps.size()) {
                availableCaps = removedCaps;
                if(DEBUG) {
                    System.err.println("EGLGraphicsConfiguration.eglChooseConfig: post filter nativeVisualID "+toHexString(nativeVisualID)+" no config found, revert to all");
                }
            } else if(DEBUG) {
                System.err.println("EGLGraphicsConfiguration.eglChooseConfig: post filter nativeVisualID "+toHexString(nativeVisualID)+" got configs: "+availableCaps.size());
                for(int i=0; i<availableCaps.size(); i++) {
                    System.err.println(i+": "+availableCaps.get(i));
                }
            }
        }

        final int chosenIndex;
        if( skipCapsChooser && 0 <= recommendedIndex ) {
            chosenIndex = recommendedIndex;
        } else {
            chosenIndex = chooseCapabilities(chooser, capsChosen, availableCaps, recommendedIndex);
        }
        if ( 0 > chosenIndex ) {
            if (DEBUG) {
                System.err.println("EGLGraphicsConfiguration.eglChooseConfig: #2 chooseCapabilities failed");
            }
            return null;
        }
        final EGLGLCapabilities chosenCaps = (EGLGLCapabilities) availableCaps.get(chosenIndex);
        final EGLGraphicsConfiguration res = new EGLGraphicsConfiguration(absScreen, chosenCaps, capsRequested, chooser);
        if (DEBUG) {
            System.err.println("EGLGraphicsConfiguration.eglChooseConfig: X chosen :"+chosenIndex+", eglConfig: "+toHexString(chosenCaps.getEGLConfig())+": "+res);
        }
        return res;
    }

    static List<GLCapabilitiesImmutable> eglConfigs2GLCaps(final EGLGraphicsDevice device, final GLProfile glp, final PointerBuffer configs, final int num, final int winattrmask, final boolean forceTransparentFlag, final boolean onlyFirstValid) {
        final GLRendererQuirks defaultQuirks = GLRendererQuirks.getStickyDeviceQuirks( GLDrawableFactory.getEGLFactory().getDefaultDevice() );
        final List<GLCapabilitiesImmutable> bucket = new ArrayList<GLCapabilitiesImmutable>(num);
        for(int i=0; i<num; i++) {
            final GLCapabilitiesImmutable caps = EGLGraphicsConfiguration.EGLConfig2Capabilities(defaultQuirks, device, glp, configs.get(i), winattrmask, forceTransparentFlag);
            if(null != caps) {
                bucket.add(caps);
                if(onlyFirstValid) {
                    break;
                }
            }
        }
        return bucket;
    }

    static void printCaps(final String prefix, final List<GLCapabilitiesImmutable> caps, final PrintStream out) {
        for(int i=0; i<caps.size(); i++) {
            out.println(prefix+"["+i+"] "+caps.get(i));
        }
    }
}

