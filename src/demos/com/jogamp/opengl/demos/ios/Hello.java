/**
 * Copyright 2019 Gothel Software e.K. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY Gothel Software e.K. ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Gothel Software e.K. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Gothel Software e.K.
 */
package com.jogamp.opengl.demos.ios;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.UpstreamWindowHookMutableSizePos;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.es2.RedSquareES2;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

import jogamp.nativewindow.WrappedWindow;
import jogamp.nativewindow.ios.IOSUtil;
import jogamp.opengl.GLDrawableFactoryImpl;

import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

public class Hello {

    private static int parseInt(final String s, final int def) {
        try {
            return Integer.parseInt(s);
        } catch (final NumberFormatException nfe) {}
        return def;
    }

    public static void main(final String[] args) {
        int width = 832, height = 480; // ipad pro 11: 2388x1668 px (scale: 2)
        int fboDepthBits = -1; // CAEAGLLayer fails with depth 16 + 24 in Simulation; -1 means don't change
        boolean exitJVM = false;
        String demoName = "com.jogamp.opengl.demos.es2.GearsES2";
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-exit")) {
                exitJVM = true;
            } else if(args[i].equals("-demo") && i+1<args.length) {
                demoName = args[++i];
            } else if(args[i].equals("-width") && i+1<args.length) {
                width = parseInt(args[++i], width);
            } else if(args[i].equals("-height") && i+1<args.length) {
                height = parseInt(args[++i], height);
            } else if(args[i].equals("-fboDepthBits") && i+1<args.length) {
                fboDepthBits = parseInt(args[++i], fboDepthBits);
            } else {
                System.err.println("ignoring arg["+i+"]: "+args[i]);
            }
        }
        System.out.println("Hello JogAmp World: exitJVM "+exitJVM+", size "+width+"x"+height+", fboDepthBits "+fboDepthBits+", demo "+demoName);
        System.out.println("os.name: <"+System.getProperty("os.name")+">");
        System.out.println("os.version: <"+System.getProperty("os.version")+">");
        System.out.println("os.arch: <"+System.getProperty("os.arch")+">");
        System.out.println("java.vendor: <"+System.getProperty("java.vendor")+">");
        System.out.println("java.vendor.url: <"+System.getProperty("java.vendor.url")+">");
        System.out.println("java.version: <"+System.getProperty("java.version")+">");
        System.out.println("java.vm.name: <"+System.getProperty("java.vm.name")+">");
        System.out.println("java.runtime.name: <"+System.getProperty("java.runtime.name")+">");
        System.out.println("");
        System.out.println(VersionUtil.getPlatformInfo());
        System.out.println("");
        System.out.println("Version Info:");
        System.out.println(GlueGenVersion.getInstance());
        System.out.println("");
        System.out.println("Full Manifest:");
        System.out.println(GlueGenVersion.getInstance().getFullManifestInfo(null));

        System.out.println("");
        System.err.println("mark-01");
        System.err.println("");
        System.err.println(JoglVersion.getInstance());
        System.err.println("");
        System.err.println("mark-02");
        System.err.println("");
        GLProfile.initSingleton();
        System.err.println("");
        System.err.println("mark-03");
        System.out.println("");
        System.out.println(JoglVersion.getDefaultOpenGLInfo(GLProfile.getDefaultDevice(), null, true));
        System.out.println("");
        System.err.println("mark-04");
        System.err.println("");

        GLAutoDrawableDelegate glad = null;
        final long uiWindow = IOSUtil.CreateUIWindow(0, 0, width, height);
        try {
            // 1) Config ..
            final GLProfile glp = GLProfile.getGL2ES2();
            final GLCapabilities reqCaps = new GLCapabilities(glp);
            if( 0 <= fboDepthBits) {
                reqCaps.setDepthBits(fboDepthBits);
            }
            System.out.println("Requested GL Caps: "+reqCaps);
            final GLDrawableFactoryImpl factory = (GLDrawableFactoryImpl) GLDrawableFactory.getFactory(glp);

            // 2) Create native window and wrap it around out NativeWindow structure
            final long uiView = IOSUtil.GetUIView(uiWindow, true);
            final long caeaglLayer = IOSUtil.GetCAEAGLLayer(uiView);
            System.out.println("EAGL: UIWindow 0x"+Long.toHexString(uiWindow));
            System.out.println("EAGL: UIView 0x"+Long.toHexString(uiView));
            System.out.println("EAGL: EAGLLayer 0x"+Long.toHexString(caeaglLayer));
            System.out.println("isUIWindow "+IOSUtil.isUIWindow(uiWindow)+", isUIView "+IOSUtil.isUIView(uiView)+
                               ", isCAEAGLLayer "+IOSUtil.isCAEAGLLayer(caeaglLayer));
            final AbstractGraphicsScreen aScreen = NativeWindowFactory.createScreen(NativeWindowFactory.createDevice(null, true /* own */), -1);
            final UpstreamWindowHookMutableSizePos hook = new UpstreamWindowHookMutableSizePos(0, 0, width, height, width, height);
            final MutableGraphicsConfiguration config = new MutableGraphicsConfiguration(aScreen, reqCaps, reqCaps);
            final WrappedWindow nativeWindow = new WrappedWindow(config, uiView, hook, true, uiWindow);

            // 3) Create a GLDrawable ..
            final GLDrawable drawable = factory.createGLDrawable(nativeWindow);
            drawable.setRealized(true);
            // final GLOffscreenAutoDrawable glad = factory.createOffscreenAutoDrawable(aScreen.getDevice(), reqCaps, null, width, height);
            glad = new GLAutoDrawableDelegate(drawable, null, nativeWindow, false, null) {
                    @Override
                    protected void destroyImplInLock() {
                        super.destroyImplInLock();  // destroys drawable/context
                        nativeWindow.destroy(); // destroys the actual window, incl. the device
                        IOSUtil.DestroyUIWindow(uiWindow);
                    }
                };
            glad.display(); // force native context creation

            // Check caps of GLDrawable after realization
            final GLCapabilitiesImmutable chosenCaps = glad.getChosenGLCapabilities();
            System.out.println("Choosen   GL Caps: "+chosenCaps);

            GLEventListener demo = null;
            {
                try {
                    demo = (GLEventListener) ReflectionUtil.createInstance(demoName, Hello.class.getClassLoader());
                } catch( final Exception e ) {
                    System.err.println(e.getMessage()+" using: <"+demoName+">");
                }
                if( null == demo ) {
                    demo = new RedSquareES2();
                }
            }
            System.out.println("Choosen demo "+demo.getClass().getName());
            glad.addGLEventListener(demo);

            final Animator animator = new Animator();
            // animator.setExclusiveContext(exclusiveContext);
            animator.setUpdateFPSFrames(60, System.err);
            animator.add(glad);
            animator.start();

            for(int i=0; i<10; i++) { // 10s
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            animator.stop();

        } finally {
            if( null != glad ) {
                glad.destroy();
            }
        }

        System.err.println("");
        System.err.println("mark-05");
        System.err.println("");

        if( exitJVM ) {
            System.exit(0);
        }
    }
}
