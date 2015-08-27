/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.newt.util.applet3;

import java.util.Locale;

import com.jogamp.plugin.applet.Applet3;
import com.jogamp.plugin.applet.Applet3Context;
import com.jogamp.plugin.ui.NativeWindowDownstream;
import com.jogamp.plugin.ui.NativeWindowUpstream;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

import com.jogamp.nativewindow.UpstreamWindowHookMutableSizePos;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.applet.JOGLNewtAppletBase;

/**
 * Simple GLEventListener deployment as an applet using JOGL. This demo must be
 * referenced from a web page via an &lt;applet&gt; tag.
 *
 *  <p>
 *  Example of an applet tag using GearsES2 within the applet area (normal case):
 *  <pre>
        &lt;applet width=100 height=100&gt;
           &lt;param name="java_arguments" value="-Dsun.java2d.noddraw=true"&gt;
           &lt;param name="gl_event_listener_class" value="com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2"&gt;
           &lt;param name="gl_profile" value="GL2"&gt;
           &lt;param name="gl_swap_interval" value="1"&gt;
           &lt;param name="gl_debug" value="false"&gt;
           &lt;param name="gl_trace" value="false"&gt;
           &lt;param name="jnlp_href" value="jogl-newt-applet-runner.jnlp"&gt;
        &lt;/applet&gt;Hello Gears !
 *  </pre>
 *  </p>
 *
 *  <p>
 *  Example of an applet tag using GearsES2 in an undecorated, translucent, closeable and always-on-top window:
 *  <pre>
        &lt;applet width=1 height=1&gt;
           &lt;param name="java_arguments" value="-Dsun.java2d.noddraw=true"&gt;
           &lt;param name="gl_event_listener_class" value="com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2"&gt;
           &lt;param name="gl_profile" value="GL2"&gt;
           &lt;param name="gl_swap_interval" value="1"&gt;
           &lt;param name="gl_undecorated" value="true"&gt;
           &lt;param name="gl_alwaysontop" value="true"&gt;
           &lt;param name="gl_closeable" value="true"&gt;
           &lt;param name="gl_alpha" value="1"&gt;
           &lt;param name="gl_multisamplebuffer" value="0"&gt;
           &lt;param name="gl_opaque" value="false"&gt;
           &lt;param name="gl_dx" value="10"&gt;
           &lt;param name="gl_dy" value="0"&gt;
           &lt;param name="gl_width" value="100"&gt;
           &lt;param name="gl_height" value="100"&gt;
           &lt;param name="gl_nodefaultkeyListener" value="true"&gt;
           &lt;param name="gl_debug" value="false"&gt;
           &lt;param name="gl_trace" value="false"&gt;
           &lt;param name="jnlp_href" value="jogl-newt-applet-runner.jnlp"&gt;
        &lt;/applet&gt;Hello Gears !
 *  </pre>
 *  </p>
 */
public class JOGLNewtApplet3Run implements Applet3 {
    public static final boolean DEBUG = JOGLNewtAppletBase.DEBUG;

    GLWindow glWindow = null;
    JOGLNewtAppletBase base = null;
    /** if valid glStandalone:=true (own window) ! */
    int glXd=Integer.MAX_VALUE, glYd=Integer.MAX_VALUE, glWidth=Integer.MAX_VALUE, glHeight=Integer.MAX_VALUE;
    Applet3Context ctx;
    boolean glStandalone = false;
    UpstreamWindowHookMutableSizePos upstreamSizePosHook;
    PointImmutable upstreamLocOnScreen;
    NativeWindow browserWin;

    final String getParameter(final String name) {
        return ctx.getParameter(name);
    }

    @Override
    public NativeWindowDownstream createNativeWindow(final Applet3Context ctx, final NativeWindowUpstream upstreamWin) {
        this.ctx = ctx;

        String glProfileName=null;
        boolean glOpaque=true;
        int glAlphaBits=0;
        int glNumMultisampleBuffer=0;
        boolean glUndecorated=false;
        boolean glAlwaysOnTop=false;
        try {
            glProfileName = getParameter("gl_profile");
            glOpaque = JOGLNewtAppletBase.str2Bool(getParameter("gl_opaque"), glOpaque);
            glAlphaBits = JOGLNewtAppletBase.str2Int(getParameter("gl_alpha"), glAlphaBits);
            glNumMultisampleBuffer = JOGLNewtAppletBase.str2Int(getParameter("gl_multisamplebuffer"), glNumMultisampleBuffer);
            glXd = JOGLNewtAppletBase.str2Int(getParameter("gl_dx"), glXd);
            glYd = JOGLNewtAppletBase.str2Int(getParameter("gl_dy"), glYd);
            glWidth = JOGLNewtAppletBase.str2Int(getParameter("gl_width"), glWidth);
            glHeight = JOGLNewtAppletBase.str2Int(getParameter("gl_height"), glHeight);
            glUndecorated = JOGLNewtAppletBase.str2Bool(getParameter("gl_undecorated"), glUndecorated);
            glAlwaysOnTop = JOGLNewtAppletBase.str2Bool(getParameter("gl_alwaysontop"), glAlwaysOnTop);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        glStandalone = Integer.MAX_VALUE>glXd && Integer.MAX_VALUE>glYd && Integer.MAX_VALUE>glWidth && Integer.MAX_VALUE>glHeight;
        final GLCapabilities caps = new GLCapabilities(GLProfile.get(glProfileName));
        caps.setAlphaBits(glAlphaBits);
        if(0<glNumMultisampleBuffer) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(glNumMultisampleBuffer);
        }
        caps.setBackgroundOpaque(glOpaque);

        final AbstractGraphicsDevice aDevice = NativeWindowFactory.createDevice(upstreamWin.getDisplayConnection(),
                                                                                true /* own */); // open and own! (for upstreamLocOnScreen)
        final AbstractGraphicsScreen aScreen = NativeWindowFactory.createScreen(aDevice, upstreamWin.getScreenIndex());
        upstreamSizePosHook = new UpstreamWindowHookMutableSizePos(upstreamWin.getX(), upstreamWin.getY(),
                                                                   upstreamWin.getWidth(), upstreamWin.getHeight(),
                                                                   upstreamWin.getWidth(), upstreamWin.getHeight()); // FIXME: pixel-dim == window-dim 'for now' ?
        browserWin = NativeWindowFactory.createWrappedWindow(aScreen, 0 /* surfaceHandle */, upstreamWin.getWindowHandle(),
                                                            upstreamSizePosHook);
        upstreamLocOnScreen = NativeWindowFactory.getLocationOnScreen(browserWin);
        if(DEBUG) {
            System.err.println("JOGLNewtApplet3Run Configuration:");
            System.err.println("glStandalone: "+glStandalone);
            System.err.println("glProfileName: "+glProfileName);
            System.err.println("glOpaque: "+glOpaque);
            System.err.println("glAlphaBits: "+glAlphaBits);
            System.err.println("glNumMultisampleBuffer: "+glNumMultisampleBuffer);
            System.err.println("glUndecorated: "+glUndecorated);
            System.err.println("glAlwaysOnTop: "+glAlwaysOnTop);
            System.err.println("UpstreamWin: "+upstreamWin+", LOS "+upstreamLocOnScreen);
            if(glStandalone) {
                System.err.println("pos-size: "+glXd+"/"+glYd+" "+glWidth+"x"+glHeight);
            }
        }

        final Window w = NewtFactory.createWindow(glStandalone ? null : browserWin, caps);
        glWindow = GLWindow.create(w);
        glWindow.setUndecorated(glUndecorated);
        glWindow.setAlwaysOnTop(glAlwaysOnTop);
        glWindow.setSize(browserWin.getWidth(), browserWin.getHeight());

        return new NativeWindowDownstream() {
            @Override
            public void setVisible(final boolean v) {
                if( null != glWindow ) {
                    glWindow.setVisible(v);
                }
            }

            @Override
            public void setSize(final int width, final int height) {
                upstreamSizePosHook.setWinSize(width, height);
                if( null != glWindow ) {
                    glWindow.setSize(width, height);
                }
            }

            @Override
            public void requestFocus() {
                if( null != glWindow ) {
                    glWindow.requestFocus();
                }
            }

            @Override
            public void destroy() {
                if( null != glWindow ) {
                    glWindow.destroy();
                }
            }

            @Override
            public NativeWindowUpstream getParent() {
                return upstreamWin;
            }

            @Override
            public long getWindowHandle() {
                if( null != glWindow ) {
                    return glWindow.getWindowHandle();
                } else {
                    return 0;
                }
            }

            @Override
            public void display() {
                if( null != glWindow ) {
                    glWindow.display();
                }
            }

            @Override
            public void notifyPositionChanged(final NativeWindowUpstream nw) {
                upstreamSizePosHook.setWinPos(nw.getX(), nw.getY());
                if( null != glWindow ) {
                    glWindow.setPosition(nw.getX(), nw.getY());
                }
            }
        };
    }

    @Override
    public void init(final Applet3Context ctx) {
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.init() START - "+currentThreadName());
        }
        this.ctx = ctx;
        String glEventListenerClazzName=null;
        int glSwapInterval=1;
        boolean glDebug=false;
        boolean glTrace=false;
        boolean glNoDefaultKeyListener = false;
        boolean glCloseable=false;

        try {
            glEventListenerClazzName = getParameter("gl_event_listener_class");
            glSwapInterval = JOGLNewtAppletBase.str2Int(getParameter("gl_swap_interval"), glSwapInterval);
            glDebug = JOGLNewtAppletBase.str2Bool(getParameter("gl_debug"), glDebug);
            glTrace = JOGLNewtAppletBase.str2Bool(getParameter("gl_trace"), glTrace);
            glNoDefaultKeyListener = JOGLNewtAppletBase.str2Bool(getParameter("gl_nodefaultkeyListener"), glNoDefaultKeyListener);
            glCloseable = JOGLNewtAppletBase.str2Bool(getParameter("gl_closeable"), glCloseable);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if(null==glEventListenerClazzName) {
            throw new RuntimeException("No applet parameter 'gl_event_listener_class'");
        }
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run Configuration:");
            System.err.println("glEventListenerClazzName: "+glEventListenerClazzName);
            System.err.println("glSwapInterval: "+glSwapInterval);
            System.err.println("glDebug: "+glDebug);
            System.err.println("glTrace: "+glTrace);
            System.err.println("glNoDefaultKeyListener: "+glNoDefaultKeyListener);
            System.err.println("glCloseable: "+glCloseable);
        }

        base = new JOGLNewtAppletBase(glEventListenerClazzName,
                                      glSwapInterval,
                                      glNoDefaultKeyListener,
                                      glCloseable,
                                      glDebug,
                                      glTrace);

        try {
            glWindow.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);
            glWindow.setDefaultCloseOperation(glCloseable ? WindowClosingMode.DISPOSE_ON_CLOSE : WindowClosingMode.DO_NOTHING_ON_CLOSE);
            base.init(glWindow);
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.init() END - "+currentThreadName());
        }
    }

    private static String currentThreadName() { return "["+Thread.currentThread().getName()+"]"; }

    @Override
    public void start() {
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.start() START (isVisible "+glWindow.isVisible()+") - "+currentThreadName());
        }
        if( glStandalone ) {
            glWindow.setSize(glWidth, glHeight);
            glWindow.setPosition(upstreamLocOnScreen.getX()+glXd, upstreamLocOnScreen.getY()+glYd);
            glWindow.setVisible(true);
            glWindow.requestFocus();
        }
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run start:");
            System.err.println("GLWindow Pos: "+glWindow.getX()+"/"+glWindow.getY()+" rel, "+glWindow.getLocationOnScreen(null)+" screen");
            System.err.println("GLWindow: "+glWindow);
        }
        base.start();
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.start() END - "+currentThreadName());
        }
    }

    @Override
    public void stop() {
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.stop() START - "+currentThreadName());
        }
        base.stop();
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.stop() END - "+currentThreadName());
        }
    }

    @Override
    public void destroy() {
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.destroy() START - "+currentThreadName());
        }
        glWindow.setVisible(false); // hide 1st
        base.destroy(); // destroy glWindow unrecoverable
        base=null;
        glWindow=null;
        browserWin.destroy(); // make sure the open display connection gets closed!
        browserWin = null;
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.destroy() END - "+currentThreadName());
        }
    }

    @Override
    public String getAppletInfo() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public String[][] getParameterInfo() {
        return null;
    }

}

