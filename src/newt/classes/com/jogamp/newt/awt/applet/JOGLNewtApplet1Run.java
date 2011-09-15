package com.jogamp.newt.awt.applet;

import java.applet.*;
import java.awt.Container;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.KeyListener;

import javax.media.opengl.*;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import java.awt.BorderLayout;

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
 *  Example of an applet tag using GearsES2 in an undecorated, translucent and always-on-top window: 
 *  <pre>
        &lt;applet width=1 height=1&gt;
           &lt;param name="java_arguments" value="-Dsun.java2d.noddraw=true"&gt;
           &lt;param name="gl_event_listener_class" value="com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2"&gt;
           &lt;param name="gl_profile" value="GL2"&gt;
           &lt;param name="gl_swap_interval" value="1"&gt;
           &lt;param name="gl_undecorated" value="true"&gt;
           &lt;param name="gl_alwaysontop" value="true"&gt;
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
    GLWindow glWindow;
    NewtCanvasAWT newtCanvasAWT;
    JOGLNewtAppletBase base;
    /** if valid glStandalone:=true (own window) ! */
    int glXd=Integer.MAX_VALUE, glYd=Integer.MAX_VALUE, glWidth=Integer.MAX_VALUE, glHeight=Integer.MAX_VALUE; 
    boolean glStandalone = false;

    public void init() {
        if(!(this instanceof Container)) {
            throw new RuntimeException("This Applet is not a AWT Container");
        }
        Container container = (Container) this; // have to think about that, we may use a Container

        String glEventListenerClazzName=null;
        String glProfileName=null;
        int glSwapInterval=0;
        boolean glDebug=false;
        boolean glTrace=false;
        boolean glUndecorated=false;
        boolean glAlwaysOnTop=false;
        boolean glOpaque=true;
        int glAlphaBits=0;
        int glNumMultisampleBuffer=0;
        boolean glNoDefaultKeyListener = false;
        try {
            glEventListenerClazzName = getParameter("gl_event_listener_class");
            glProfileName = getParameter("gl_profile");
            glSwapInterval = JOGLNewtAppletBase.str2Int(getParameter("gl_swap_interval"), glSwapInterval);
            glDebug = JOGLNewtAppletBase.str2Bool(getParameter("gl_debug"), glDebug);
            glTrace = JOGLNewtAppletBase.str2Bool(getParameter("gl_trace"), glTrace);
            glUndecorated = JOGLNewtAppletBase.str2Bool(getParameter("gl_undecorated"), glUndecorated);
            glAlwaysOnTop = JOGLNewtAppletBase.str2Bool(getParameter("gl_alwaysontop"), glAlwaysOnTop);
            glOpaque = JOGLNewtAppletBase.str2Bool(getParameter("gl_opaque"), glOpaque);
            glAlphaBits = JOGLNewtAppletBase.str2Int(getParameter("gl_alpha"), glAlphaBits);
            glNumMultisampleBuffer = JOGLNewtAppletBase.str2Int(getParameter("gl_multisamplebuffer"), glNumMultisampleBuffer); 
            glXd = JOGLNewtAppletBase.str2Int(getParameter("gl_dx"), glXd);
            glYd = JOGLNewtAppletBase.str2Int(getParameter("gl_dy"), glYd);
            glWidth = JOGLNewtAppletBase.str2Int(getParameter("gl_width"), glWidth);
            glHeight = JOGLNewtAppletBase.str2Int(getParameter("gl_height"), glHeight);
            glNoDefaultKeyListener = JOGLNewtAppletBase.str2Bool(getParameter("gl_nodefaultkeyListener"), glNoDefaultKeyListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(null==glEventListenerClazzName) {
            throw new RuntimeException("No applet parameter 'gl_event_listener_class'");
        }
        glStandalone = Integer.MAX_VALUE>glXd && Integer.MAX_VALUE>glYd && Integer.MAX_VALUE>glWidth && Integer.MAX_VALUE>glHeight;
        
        base = new JOGLNewtAppletBase(glEventListenerClazzName, 
                                      glSwapInterval,
                                      glNoDefaultKeyListener,
                                      glDebug,
                                      glTrace);

        try {
            GLProfile.initSingleton(false);
            GLCapabilities caps = new GLCapabilities(GLProfile.get(glProfileName));
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
            if(glStandalone) {
                newtCanvasAWT = null;
            } else {
                newtCanvasAWT = new NewtCanvasAWT(glWindow);
                container.setLayout(new BorderLayout());
                container.add(newtCanvasAWT, BorderLayout.CENTER);
            }
            base.init(glWindow);
            if(base.isValid()) {
                GLEventListener glEventListener = base.getGLEventListener();

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
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void start() {
        if(glStandalone) {
            glWindow.setSize(glWidth, glHeight);
            final java.awt.Point p0 = this.getLocationOnScreen();
            glWindow.setPosition(p0.x+glXd, p0.y+glYd);
        }
        base.start();
    }

    public void stop() {
        base.stop();
    }

    public void destroy() {
        glWindow.setVisible(false); // hide 1st
        if(!glStandalone) {
            glWindow.reparentWindow(null); // get out of newtCanvasAWT
            this.remove(newtCanvasAWT); // remove newtCanvasAWT
        }
        base.destroy(); // destroy glWindow unrecoverable
        base=null;
    }
}

