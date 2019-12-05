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

package com.jogamp.opengl.demos;

import java.io.IOException;
import java.lang.reflect.Constructor;

import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.TraceMouseAdapter;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.opengl.util.NEWTDemoListener;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.demos.es2.RedSquareES2;

/**
 * <p>
 * The demo code uses {@link NEWTDemoListener} functionality.
 * </p>
 * <p>
 * Manual invocation via main allows setting each tests's duration in milliseconds, e.g.{@code -duration 10000} and many more, see {@link #main(String[])}
 * </p>
 */
public class Launcher0 {
    static int screenIdx = 0;
    static PointImmutable wpos;
    static DimensionImmutable wsize = new Dimension(640, 480), rwsize=null;
    static float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

    static String demoName = "com.jogamp.opengl.demos.es2.GearsES2";
    static long duration = 500; // ms
    static boolean opaque = true;
    static int forceAlpha = -1;
    static boolean undecorated = false;
    static boolean alwaysOnTop = false;
    static boolean alwaysOnBottom = false;
    static boolean resizable = true;
    static boolean sticky = false;
    static boolean max_vert= false;
    static boolean max_horz= false;
    static boolean fullscreen = false;
    static int swapInterval = 1;
    static boolean waitForKey = false;
    static boolean mouseVisible = true;
    static boolean mouseConfined = false;
    static boolean useMultiplePointerIcon = false;
    static boolean showFPS = true;
    static boolean forceES2 = false;
    static boolean forceES3 = false;
    static boolean forceGL3 = false;
    static boolean forceGL2 = false;
    static boolean useDoubleBuffer = true;
    static boolean forceDebug = false;
    static boolean forceTrace = false;
    static boolean traceMouse = false;
    static boolean exclusiveContext = false;
    static boolean useAnimator = true;

    public void runTest() throws InterruptedException {
        final GLProfile glp;
        if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES3) {
            glp = GLProfile.get(GLProfile.GLES3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else if(forceGL2) {
            glp = GLProfile.get(GLProfile.GL2);
        } else {
            glp = GLProfile.getGL2ES2();
        }
        final GLCapabilities caps = new GLCapabilities( glp );
        caps.setBackgroundOpaque(opaque);
        if(-1 < forceAlpha) {
            caps.setAlphaBits(forceAlpha);
        }
        caps.setDoubleBuffered(useDoubleBuffer);

        System.err.println("requested: vsync "+swapInterval+", "+caps);
        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, screenIdx);
        final GLWindow glWindow = GLWindow.create(screen, caps);

        glWindow.setSurfaceScale(reqSurfacePixelScale);
        final float[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new float[2]);
        glWindow.setSize(wsize.getWidth(), wsize.getHeight());
        if(null != wpos) {
            glWindow.setPosition(wpos.getX(), wpos.getY());
        }
        glWindow.setUndecorated(undecorated);
        glWindow.setAlwaysOnTop(alwaysOnTop);
        glWindow.setAlwaysOnBottom(alwaysOnBottom);
        glWindow.setResizable(resizable);
        glWindow.setSticky(sticky);
        glWindow.setMaximized(max_horz, max_vert);
        glWindow.setFullscreen(fullscreen);
        glWindow.setPointerVisible(mouseVisible);
        glWindow.confinePointer(mouseConfined);

        final GLEventListener demo;
        {
            GLEventListener _demo = null;
            try {
                final Class<?> demoClazz = ReflectionUtil.getClass(demoName, true, Launcher0.class.getClassLoader());
                try {
                    // with swapInterval
                    System.err.println("Loading "+demoName+"("+swapInterval+")");
                    final Constructor<?> ctr = ReflectionUtil.getConstructor(demoClazz, int.class);
                    _demo = (GLEventListener) ReflectionUtil.createInstance(ctr, swapInterval);
                } catch( final Exception e ) {
                    System.err.println(e.getMessage()+" using.0: <"+demoName+">");
                }
                if( null == _demo ) {
                    // without swapInterval
                    System.err.println("Loading "+demoName+"()");
                    _demo = (GLEventListener) ReflectionUtil.createInstance(demoClazz);
                }
            } catch( final Exception e ) {
                System.err.println(e.getMessage()+" using.1: <"+demoName+">");
            }
            if( null == _demo ) {
                System.err.println("Loading RedSquareES2()");
                _demo = new RedSquareES2();
            }
            demo = _demo;
        }
        System.out.println("Choosen demo "+demo.getClass().getName());
        if( forceDebug || forceTrace ) {
            glWindow.addGLEventListener(new GLEventListener() {
                @Override
                public void init(final GLAutoDrawable drawable) {
                    GL _gl = drawable.getGL();
                    if(forceDebug) {
                        try {
                            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, _gl, null) );
                        } catch (final Exception e) {e.printStackTrace();}
                    }

                    if(forceTrace) {
                        try {
                            // Trace ..
                            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, _gl, new Object[] { System.err } ) );
                        } catch (final Exception e) {e.printStackTrace();}
                    }
                }
                @Override
                public void dispose(final GLAutoDrawable drawable) {}
                @Override
                public void display(final GLAutoDrawable drawable) {}
                @Override
                public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
            });
        }

        if( null != demo ) {
            glWindow.addGLEventListener(demo);
        }

        if(waitForKey) {
            glWindow.addGLEventListener(new GLEventListener() {
                public void init(final GLAutoDrawable drawable) { }
                public void dispose(final GLAutoDrawable drawable) { }
                public void display(final GLAutoDrawable drawable) {
                    final GLAnimatorControl  actrl = drawable.getAnimator();
                    if(waitForKey && actrl.getTotalFPSFrames() == 60*3) {
                        MiscUtils.waitForKey("3s mark");
                        actrl.resetFPSCounter();
                        waitForKey = false;
                    }
                }
                public void reshape(final GLAutoDrawable drawable, final int x, final int y,
                        final int width, final int height) { }
            });
        }

        final Animator animator = useAnimator ? new Animator() : null;
        if( useAnimator ) {
            animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
            animator.setExclusiveContext(exclusiveContext);
        }

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(final WindowEvent e) {
                System.err.println("window resized: "+glWindow.getBounds()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                NEWTDemoListener.setTitle(glWindow);
            }
            public void windowMoved(final WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getBounds()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                NEWTDemoListener.setTitle(glWindow);
            }
        });

        final NEWTDemoListener newtDemoListener;
        {
            final PointerIcon[] pointerIcon = useMultiplePointerIcon ? NEWTDemoListener.createPointerIcons(glWindow.getScreen().getDisplay()) : null;
            newtDemoListener = new NEWTDemoListener(glWindow, pointerIcon);
        }
        newtDemoListener.quitAdapterEnable(true);
        glWindow.addKeyListener(newtDemoListener);
        if( traceMouse ) {
            glWindow.addMouseListener(new TraceMouseAdapter());
        }
        glWindow.addMouseListener(newtDemoListener);
        glWindow.addWindowListener(newtDemoListener);

        if( useAnimator ) {
            animator.add(glWindow);
            animator.start();
        }

        glWindow.setVisible(true);
        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        } else {
            glWindow.setUpdateFPSFrames(60, showFPS ? System.err : null);
        }

        System.err.println("Window Current State   : "+glWindow.getStateMaskString());
        System.err.println("Window Supported States: "+glWindow.getSupportedStateMaskString());
        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

        final float[] hasSurfacePixelScale1 = glWindow.getCurrentSurfaceScale(new float[2]);
        System.err.println("HiDPI PixelScale: "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                           valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                           hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        NEWTDemoListener.setTitle(glWindow);

        if( null != rwsize ) {
            Thread.sleep(500); // 500ms delay
            glWindow.setSize(rwsize.getWidth(), rwsize.getHeight());
            System.err.println("window resize pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!newtDemoListener.shouldQuit() && t1-t0<duration) {
            if(!useAnimator) {
                glWindow.display();
                Thread.yield();
            } else {
                Thread.sleep(100);
            }
            t1 = System.currentTimeMillis();
        }

        if( useAnimator ) {
            animator.stop();
        }
        glWindow.destroy();
    }

    public static void main(final String args[]) throws IOException {
        int x=0, y=0, w=640, h=480, rw=-1, rh=-1;
        boolean usePos = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-demo") && i+1<args.length) {
                demoName = args[++i];
            } else if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-translucent")) {
                opaque = false;
            } else if(args[i].equals("-forceAlpha")) {
                i++;
                forceAlpha = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-undecorated")) {
                undecorated = true;
            } else if(args[i].equals("-atop")) {
                alwaysOnTop = true;
            } else if(args[i].equals("-abottom")) {
                alwaysOnBottom = true;
            } else if(args[i].equals("-noresize")) {
                resizable = false;
            } else if(args[i].equals("-sticky")) {
                sticky = true;
            } else if(args[i].equals("-maxv")) {
                max_vert = true;
            } else if(args[i].equals("-maxh")) {
                max_horz = true;
            } else if(args[i].equals("-fullscreen")) {
                fullscreen = true;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-single")) {
                useDoubleBuffer = false;
            } else if(args[i].equals("-exclctx")) {
                exclusiveContext = true;
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-es3")) {
                forceES3 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-gl2")) {
                forceGL2 = true;
            } else if(args[i].equals("-debug")) {
                forceDebug = true;
            } else if(args[i].equals("-trace")) {
                forceTrace = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-mouseInvisible")) {
                mouseVisible = false;
            } else if(args[i].equals("-mouseConfine")) {
                mouseConfined = true;
            } else if(args[i].equals("-pointerIcon")) {
                useMultiplePointerIcon = true;
            } else if(args[i].equals("-showFPS")) {
                showFPS = true;
            } else if(args[i].equals("-width")) {
                i++;
                w = MiscUtils.atoi(args[i], w);
            } else if(args[i].equals("-height")) {
                i++;
                h = MiscUtils.atoi(args[i], h);
            } else if(args[i].equals("-x")) {
                i++;
                x = MiscUtils.atoi(args[i], x);
                usePos = true;
            } else if(args[i].equals("-y")) {
                i++;
                y = MiscUtils.atoi(args[i], y);
                usePos = true;
            } else if(args[i].equals("-pixelScale")) {
                i++;
                final float pS = MiscUtils.atof(args[i], reqSurfacePixelScale[0]);
                reqSurfacePixelScale[0] = pS;
                reqSurfacePixelScale[1] = pS;
            } else if(args[i].equals("-rwidth")) {
                i++;
                rw = MiscUtils.atoi(args[i], rw);
            } else if(args[i].equals("-rheight")) {
                i++;
                rh = MiscUtils.atoi(args[i], rh);
            } else if(args[i].equals("-screen")) {
                i++;
                screenIdx = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-traceMouse")) {
                traceMouse = true;
            }
        }
        wsize = new Dimension(w, h);
        if( 0 < rw && 0 < rh ) {
            rwsize = new Dimension(rw, rh);
        }

        if(usePos) {
            wpos = new Point(x, y);
        }
        System.err.println("demo "+demoName);
        System.err.println("position "+wpos);
        System.err.println("size "+wsize);
        System.err.println("resize "+rwsize);
        System.err.println("screen "+screenIdx);
        System.err.println("translucent "+(!opaque));
        System.err.println("forceAlpha "+forceAlpha);
        System.err.println("undecorated "+undecorated);
        System.err.println("atop "+alwaysOnTop);
        System.err.println("abottom "+alwaysOnBottom);
        System.err.println("resizable "+resizable);
        System.err.println("sticky "+sticky);
        System.err.println("max_vert "+max_vert);
        System.err.println("max_horz "+max_horz);
        System.err.println("fullscreen "+fullscreen);
        System.err.println("mouseVisible "+mouseVisible);
        System.err.println("mouseConfined "+mouseConfined);
        System.err.println("pointerIcon "+useMultiplePointerIcon);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceES3 "+forceES3);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("forceGL2 "+forceGL2);
        System.err.println("forceDebug "+forceDebug);
        System.err.println("forceTrace "+forceTrace);
        System.err.println("useDoubleBuffer "+useDoubleBuffer);
        System.err.println("swapInterval "+swapInterval);
        System.err.println("exclusiveContext "+exclusiveContext);
        System.err.println("useAnimator "+useAnimator);
        System.err.println("traceMouse "+traceMouse);

        if(waitForKey) {
            MiscUtils.waitForKey("Start");
        }

        final Launcher0 l = new Launcher0();
        try {
            System.err.println("Start-Demo");
            l.runTest();
            System.err.println("End-Demo");
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        if(waitForKey) {
            MiscUtils.waitForKey("End-Pre-Shutdown");
        }
        System.err.println("End-Pre-Shutdown");
        GLProfile.shutdown();
        System.err.println("End-Post-Shutdown");
    }
}
