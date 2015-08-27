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
package com.jogamp.newt.awt.applet;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Arrays;

import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.jawt.JAWTUtil;

import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.newt.awt.NewtCanvasAWT;
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
@SuppressWarnings("serial")
public class JOGLNewtApplet1Run extends Applet {
    public static final boolean DEBUG = JOGLNewtAppletBase.DEBUG;

    GLWindow glWindow = null;
    NewtCanvasAWT newtCanvasAWT = null;
    JOGLNewtAppletBase base = null;
    /** if valid glStandalone:=true (own window) ! */
    int glXd=Integer.MAX_VALUE, glYd=Integer.MAX_VALUE, glWidth=Integer.MAX_VALUE, glHeight=Integer.MAX_VALUE;

    @Override
    public void init() {
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.init() START - "+currentThreadName());
        }
        final Container container = this;

        String glEventListenerClazzName=null;
        String glProfileName=null;
        int glSwapInterval=1;
        boolean glDebug=false;
        boolean glTrace=false;
        boolean glUndecorated=false;
        boolean glAlwaysOnTop=false;
        boolean glCloseable=false;
        boolean glOpaque=true;
        int glAlphaBits=0;
        int glNumMultisampleBuffer=0;
        boolean glNoDefaultKeyListener = false;
        boolean appletDebugTestBorder = false;
        try {
            glEventListenerClazzName = getParameter("gl_event_listener_class");
            glProfileName = getParameter("gl_profile");
            glSwapInterval = JOGLNewtAppletBase.str2Int(getParameter("gl_swap_interval"), glSwapInterval);
            glDebug = JOGLNewtAppletBase.str2Bool(getParameter("gl_debug"), glDebug);
            glTrace = JOGLNewtAppletBase.str2Bool(getParameter("gl_trace"), glTrace);
            glUndecorated = JOGLNewtAppletBase.str2Bool(getParameter("gl_undecorated"), glUndecorated);
            glAlwaysOnTop = JOGLNewtAppletBase.str2Bool(getParameter("gl_alwaysontop"), glAlwaysOnTop);
            glCloseable = JOGLNewtAppletBase.str2Bool(getParameter("gl_closeable"), glCloseable);
            glOpaque = JOGLNewtAppletBase.str2Bool(getParameter("gl_opaque"), glOpaque);
            glAlphaBits = JOGLNewtAppletBase.str2Int(getParameter("gl_alpha"), glAlphaBits);
            glNumMultisampleBuffer = JOGLNewtAppletBase.str2Int(getParameter("gl_multisamplebuffer"), glNumMultisampleBuffer);
            glXd = JOGLNewtAppletBase.str2Int(getParameter("gl_dx"), glXd);
            glYd = JOGLNewtAppletBase.str2Int(getParameter("gl_dy"), glYd);
            glWidth = JOGLNewtAppletBase.str2Int(getParameter("gl_width"), glWidth);
            glHeight = JOGLNewtAppletBase.str2Int(getParameter("gl_height"), glHeight);
            glNoDefaultKeyListener = JOGLNewtAppletBase.str2Bool(getParameter("gl_nodefaultkeyListener"), glNoDefaultKeyListener);
            appletDebugTestBorder = JOGLNewtAppletBase.str2Bool(getParameter("appletDebugTestBorder"), appletDebugTestBorder);
        } catch (final Exception e) {
            e.printStackTrace();
        }
        if(null==glEventListenerClazzName) {
            throw new RuntimeException("No applet parameter 'gl_event_listener_class'");
        }
        final boolean glStandalone = Integer.MAX_VALUE>glXd && Integer.MAX_VALUE>glYd && Integer.MAX_VALUE>glWidth && Integer.MAX_VALUE>glHeight;
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run Configuration:");
            System.err.println("glStandalone: "+glStandalone);
            if(glStandalone) {
                System.err.println("pos-size: "+glXd+"/"+glYd+" "+glWidth+"x"+glHeight);
            }
            System.err.println("glEventListenerClazzName: "+glEventListenerClazzName);
            System.err.println("glProfileName: "+glProfileName);
            System.err.println("glSwapInterval: "+glSwapInterval);
            System.err.println("glDebug: "+glDebug);
            System.err.println("glTrace: "+glTrace);
            System.err.println("glUndecorated: "+glUndecorated);
            System.err.println("glAlwaysOnTop: "+glAlwaysOnTop);
            System.err.println("glCloseable: "+glCloseable);
            System.err.println("glOpaque: "+glOpaque);
            System.err.println("glAlphaBits: "+glAlphaBits);
            System.err.println("glNumMultisampleBuffer: "+glNumMultisampleBuffer);
            System.err.println("glNoDefaultKeyListener: "+glNoDefaultKeyListener);
        }

        base = new JOGLNewtAppletBase(glEventListenerClazzName,
                                      glSwapInterval,
                                      glNoDefaultKeyListener,
                                      glCloseable,
                                      glDebug,
                                      glTrace);

        try {
            final GLCapabilities caps = new GLCapabilities(GLProfile.get(glProfileName));
            caps.setAlphaBits(glAlphaBits);
            if(0<glNumMultisampleBuffer) {
                caps.setSampleBuffers(true);
                caps.setNumSamples(glNumMultisampleBuffer);
            }
            caps.setBackgroundOpaque(glOpaque);
            glWindow = GLWindow.create(caps);
            glWindow.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);
            glWindow.setUndecorated(glUndecorated);
            glWindow.setAlwaysOnTop(glAlwaysOnTop);
            glWindow.setDefaultCloseOperation(glCloseable ? WindowClosingMode.DISPOSE_ON_CLOSE : WindowClosingMode.DO_NOTHING_ON_CLOSE);
            container.setLayout(new BorderLayout());
            if(appletDebugTestBorder) {
                AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                    public void run() {
                        container.add(new Button("North"), BorderLayout.NORTH);
                        container.add(new Button("South"), BorderLayout.SOUTH);
                        container.add(new Button("East"), BorderLayout.EAST);
                        container.add(new Button("West"), BorderLayout.WEST);
                    } } );
            }
            base.init(glWindow);
            if(base.isValid()) {
                final GLEventListener glEventListener = base.getGLEventListener();

                if(glEventListener instanceof MouseListener) {
                    addMouseListener((MouseListener)glEventListener);
                }
                if(glEventListener instanceof MouseMotionListener) {
                    addMouseMotionListener((MouseMotionListener)glEventListener);
                }
                if(glEventListener instanceof KeyListener) {
                    addKeyListener((KeyListener)glEventListener);
                }
            }
            if( !glStandalone ) {
                AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                    public void run() {
                        newtCanvasAWT = new NewtCanvasAWT(glWindow);
                        newtCanvasAWT.setSkipJAWTDestroy(true); // Bug 910
                        container.add(newtCanvasAWT, BorderLayout.CENTER);
                        container.validate();
                    } } );
            }
        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.init() END - "+currentThreadName());
        }
    }

    private static String currentThreadName() { return "["+Thread.currentThread().getName()+", isAWT-EDT "+EventQueue.isDispatchThread()+"]"; }

    @Override
    public void start() {
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.start() START (isVisible "+isVisible()+", isDisplayable "+isDisplayable()+") - "+currentThreadName());
        }
        final java.awt.Point[] p0 = { null };
        AWTEDTExecutor.singleton.invoke(true, new Runnable() {
            public void run() {
                setVisible(true);
                p0[0] = getLocationOnScreen();
                if( null != newtCanvasAWT ) {
                    newtCanvasAWT.setFocusable(true);
                    newtCanvasAWT.requestFocus();
                }
            }
        });
        if( null == newtCanvasAWT ) {
            glWindow.requestFocus();
            glWindow.setSize(glWidth, glHeight);
            glWindow.setPosition(p0[0].x+glXd, p0[0].y+glYd);
        }
        if(DEBUG) {
            Component topC = this;
            while (null != topC.getParent()) {
                topC = topC.getParent();
            }
            System.err.println("JOGLNewtApplet1Run start:");
            System.err.println("TopComponent: "+topC.getLocation()+" rel, "+topC.getLocationOnScreen()+" screen, visible "+topC.isVisible()+", "+topC);
            System.err.println("Applet Pos: "+this.getLocation()+" rel, "+Arrays.toString(p0)+" screen, visible "+this.isVisible()+", "+this);
            if(null != newtCanvasAWT) {
                System.err.println("NewtCanvasAWT Pos: "+newtCanvasAWT.getLocation()+" rel, "+newtCanvasAWT.getLocationOnScreen()+" screen, visible "+newtCanvasAWT.isVisible()+", "+newtCanvasAWT);
            }
            System.err.println("GLWindow Pos: "+glWindow.getX()+"/"+glWindow.getY()+" rel, "+glWindow.getLocationOnScreen(null)+" screen");
            System.err.println("GLWindow: "+glWindow);
        }
        base.start();
        if( null != newtCanvasAWT &&
            newtCanvasAWT.isOffscreenLayerSurfaceEnabled() &&
            0 != ( JAWTUtil.JAWT_OSX_CALAYER_QUIRK_POSITION & JAWTUtil.getOSXCALayerQuirks() ) ) {
            // force relayout
            AWTEDTExecutor.singleton.invoke(true, new Runnable() {
                public void run() {
                    final int cW = newtCanvasAWT.getWidth();
                    final int cH = newtCanvasAWT.getHeight();
                    newtCanvasAWT.setSize(cW+1, cH+1);
                    newtCanvasAWT.setSize(cW, cH);
                } } );
        }
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
        AWTEDTExecutor.singleton.invoke(true, new Runnable() {
            public void run() {
                glWindow.setVisible(false); // hide 1st
                if( null != newtCanvasAWT ) {
                    newtCanvasAWT.setSkipJAWTDestroy(false); // Bug 910
                    remove(newtCanvasAWT); // remove newtCanvasAWT incl. glWindow.reparentWindow(null) if not done yet!
                    newtCanvasAWT.destroy();
                }
            } } );
        base.destroy(); // destroy glWindow unrecoverable
        base=null;
        glWindow=null;
        newtCanvasAWT=null;
        if(DEBUG) {
            System.err.println("JOGLNewtApplet1Run.destroy() END - "+currentThreadName());
        }
    }
}

