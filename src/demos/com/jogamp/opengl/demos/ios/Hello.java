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
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.common.GlueGenVersion;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.es2.RedSquareES2;
import com.jogamp.opengl.util.Animator;

import jogamp.nativewindow.ios.IOSUtil;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

public class Hello {

    private static int parseInt(final String s, final int def) {
        try {
            return Integer.parseInt(s);
        } catch (final NumberFormatException nfe) {}
        return def;
    }
    private static float parseFloat(final String s, final float def) {
        try {
            return Float.parseFloat(s);
        } catch (final NumberFormatException nfe) {}
        return def;
    }

    public static void main(final String[] args) {
        final float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

        int secondsDuration = 10; // 10s
        int width = 832, height = 480; // ipad pro 11: 2388x1668 px (scale: 2)
        int fboDepthBits = -1; // CAEAGLLayer fails with depth 16 + 24 in Simulation; -1 means don't change
        boolean translucent = false;
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
            } else if(args[i].equals("-pixelScale") && i+1<args.length) {
                reqSurfacePixelScale[0] = parseFloat(args[++i], reqSurfacePixelScale[0]);
                reqSurfacePixelScale[1] = reqSurfacePixelScale[0];
            } else if(args[i].equals("-seconds") && i+1<args.length) {
                secondsDuration = parseInt(args[++i], secondsDuration);
            } else if(args[i].equals("-translucent")) {
                translucent = true;
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

        GLWindow glWindow = null;
        try {
            // 1) Config ..
            final GLProfile glp = GLProfile.getGL2ES2();
            final GLCapabilities reqCaps = new GLCapabilities(glp);
            if( 0 <= fboDepthBits) {
                reqCaps.setDepthBits(fboDepthBits);
            }
            reqCaps.setBackgroundOpaque(!translucent);
            System.out.println("Requested GL Caps: "+reqCaps);

            // 2) Create newt native window
            final Display dpy = NewtFactory.createDisplay(null);
            final Screen screen = NewtFactory.createScreen(dpy, 0);
            glWindow = GLWindow.create(screen, reqCaps);
            glWindow.setSurfaceScale(reqSurfacePixelScale);
            final float[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new float[2]);
            glWindow.setSize(width, height);
            glWindow.setPosition(0, 0);
            final GLEventListener tracker = new GLEventListener() {
                void printInfo(final String prefix, final GLAutoDrawable d, final String postfix) {
                    System.out.print(prefix+": drawable "+d.getSurfaceWidth()+"x"+d.getSurfaceHeight());
                    if(null != postfix) {
                        System.out.println(" - "+postfix);
                    } else {
                        System.out.println();
                    }
                }
                @Override
                public void init(final GLAutoDrawable drawable) {
                    printInfo("GLEvent::Init", drawable, null);
                }

                @Override
                public void dispose(final GLAutoDrawable drawable) {
                    printInfo("GLEvent::Dispose", drawable, null);
                }

                @Override
                public void display(final GLAutoDrawable drawable) {
                }

                @Override
                public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                    printInfo("GLEvent::Reshape", drawable, "reshape["+x+"/"+y+" "+width+"x"+height+"]");
                }
            };
            glWindow.addGLEventListener(tracker);
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
            glWindow.addGLEventListener(demo);
            glWindow.setVisible(true); // force native context creation

            // Check caps of GLDrawable after realization
            System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
            System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
            System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+"[wu] "+glWindow.getWidth()+"x"+glWindow.getHeight()+"[wu] "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+"[px], "+glWindow.getInsets());

            final float[] hasSurfacePixelScale1 = glWindow.getCurrentSurfaceScale(new float[2]);
            System.err.println("HiDPI PixelScale: "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                               valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                               hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
            {
                final long uiWindow = glWindow.getWindowHandle();
                final long uiView = IOSUtil.GetUIView(uiWindow, true);
                final long caeaglLayer = IOSUtil.GetCAEAGLLayer(uiView);
                System.out.println("EAGL: UIWindow 0x"+Long.toHexString(uiWindow));
                System.out.println("EAGL: UIView 0x"+Long.toHexString(uiView));
                System.out.println("EAGL: EAGLLayer 0x"+Long.toHexString(caeaglLayer));
                System.out.println("isUIWindow "+IOSUtil.isUIWindow(uiWindow)+", isUIView "+IOSUtil.isUIView(uiView)+
                                   ", isCAEAGLLayer "+IOSUtil.isCAEAGLLayer(caeaglLayer));
            }

            final Animator animator = new Animator(0 /* w/o AWT */);
            // animator.setExclusiveContext(exclusiveContext);
            animator.setUpdateFPSFrames(60, System.err);
            animator.add(glWindow);
            animator.start();

            for(int i=0; i<secondsDuration; i++) {
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            animator.stop();

        } finally {
            System.err.println("");
            System.err.println("mark-05");
            System.err.println("");

            if( null != glWindow ) {
                glWindow.destroy();
            }
        }

        System.err.println("");
        System.err.println("mark-06");
        System.err.println("");

        if( exitJVM ) {
            System.exit(0);
        }
    }
}
