package com.jogamp.newt.awt.applet;

import java.lang.reflect.*;

import javax.media.nativewindow.NativeWindow;
import javax.media.opengl.*;
import com.jogamp.opengl.util.*;

import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.GLWindow;

/** Shows how to deploy an applet using JOGL. This demo must be
    referenced from a web page via an &lt;applet&gt; tag. */

public class JOGLNewtAppletBase extends WindowAdapter implements KeyListener, MouseListener, GLEventListener {
    String glEventListenerClazzName;
    int glSwapInterval;
    boolean glDebug;
    boolean glTrace;

    GLEventListener glEventListener = null;
    GLWindow glWindow = null;
    Animator glAnimator=null;
    boolean isValid = false;
    NativeWindow awtParent;

    public JOGLNewtAppletBase(String glEventListenerClazzName, 
                              int glSwapInterval,
                              boolean glDebug,
                              boolean glTrace) {
    
        this.glEventListenerClazzName=glEventListenerClazzName;
        this.glSwapInterval=glSwapInterval;
        this.glDebug = glDebug;
        this.glTrace = glTrace;
    }

    public GLEventListener getGLEventListener() { return glEventListener; }
    public GLWindow getGLWindow() { return glWindow; }
    public Animator getGLAnimator() { return glAnimator; }
    public boolean isValid() { return isValid; }

    public static boolean str2Bool(String str, boolean def) {
        if(null==str) return def;
        try {
            return Boolean.valueOf(str).booleanValue();
        } catch (Exception ex) { ex.printStackTrace(); }
        return def;
    }

    public static int str2Int(String str, int def) {
        if(null==str) return def;
        try {
            return Integer.parseInt(str);
        } catch (Exception ex) { ex.printStackTrace(); }
        return def;
    }

    public static GLEventListener createInstance(String clazzName) {
        Object instance = null;

        try {
            Class<?> clazz = Class.forName(clazzName);
            instance = clazz.newInstance();
        } catch (Throwable t) {
            t.printStackTrace();
            throw new RuntimeException("Error while instantiating demo: "+clazzName);
        }
        if( null == instance ) {
            throw new RuntimeException("Null GLEventListener: "+clazzName);
        }
        if( !(instance instanceof GLEventListener) ) {
            throw new RuntimeException("Not a GLEventListener: "+clazzName);
        }
        return (GLEventListener) instance;
    }

    public static boolean setField(Object instance, String fieldName, Object value) {
        try {
            Field f = instance.getClass().getField(fieldName);
            if(f.getType().isInstance(value)) {
                f.set(instance, value);
                return true;
            } else {
                System.out.println(instance.getClass()+" '"+fieldName+"' field not assignable with "+value.getClass()+", it's a: "+f.getType());
            }
        } catch (NoSuchFieldException nsfe) {
            System.out.println(instance.getClass()+" has no '"+fieldName+"' field");
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    public void init(GLWindow glWindow) {
        init(Thread.currentThread().getThreadGroup(), glWindow);
    }

    public void init(ThreadGroup tg, GLWindow glWindow) {
        this.glWindow = glWindow;

        glEventListener = createInstance(glEventListenerClazzName);

        try {
            if(!setField(glEventListener, "window", glWindow)) {
                setField(glEventListener, "glWindow", glWindow);
            }

            glWindow.addGLEventListener(this);
            glWindow.addGLEventListener(glEventListener);

            if(glEventListener instanceof WindowListener) {
                glWindow.addWindowListener((WindowListener)glEventListener);
            }
            glWindow.addWindowListener(this);

            if(glEventListener instanceof MouseListener) {
                glWindow.addMouseListener((MouseListener)glEventListener);
            }
            glWindow.addMouseListener(this);

            if(glEventListener instanceof KeyListener) {
                glWindow.addKeyListener((KeyListener)glEventListener);
            }
            glWindow.addKeyListener(this);

            glWindow.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);

            // glAnimator = new FPSAnimator(canvas, 60);
            glAnimator = new Animator(tg, glWindow);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        isValid = true;
    }

    public void start() {
        if(isValid) {
            glWindow.setVisible(true);
            glAnimator.start();
            awtParent = glWindow.getParent();
        }
    }

    public void stop() {
        if(null!=glAnimator) {
            glAnimator.stop();
            glWindow.setVisible(false);
        }
    }

    public void destroy() {
        isValid = false;
        if(null!=glAnimator) {
            glAnimator.stop();
            glAnimator.remove(glWindow);
            glAnimator=null;
        }
        if(null!=glWindow) {
            glWindow.destroy();
            glWindow=null;
        }
    }

    // ***********************************************************************************
    // ***********************************************************************************
    // ***********************************************************************************

    public void init(GLAutoDrawable drawable) {
        GL _gl = drawable.getGL();

        if(glDebug) {
            try {
                _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", null, _gl, null) );
            } catch (Exception e) {e.printStackTrace();} 
        }

        if(glTrace) {
            try {
                // Trace ..
                _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", null, _gl, new Object[] { System.err } ) );
            } catch (Exception e) {e.printStackTrace();} 
        }

        if(glSwapInterval>=0) {
            _gl.setSwapInterval(glSwapInterval);
        }
    }
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }
    public void display(GLAutoDrawable drawable) {
    }
    public void dispose(GLAutoDrawable drawable) {
    }

    // ***********************************************************************************
    // ***********************************************************************************
    // ***********************************************************************************

    public void keyPressed(KeyEvent e) { 
    }
    public void keyReleased(KeyEvent e) { 
    }
    public void keyTyped(KeyEvent e) {
       if(e.getKeyChar()=='d') {
            glWindow.setUndecorated(!glWindow.isUndecorated());
       } if(e.getKeyChar()=='f') {
            glWindow.setFullscreen(!glWindow.isFullscreen());
       } else if(e.getKeyChar()=='a') {
            glWindow.setAlwaysOnTop(!glWindow.isAlwaysOnTop());
       } else if(e.getKeyChar()=='r' && null!=awtParent) {
            if(null == glWindow.getParent()) {
                glWindow.reparentWindow(awtParent);
            } else {
                glWindow.reparentWindow(null);
            }
       }
    }

    // ***********************************************************************************
    // ***********************************************************************************
    // ***********************************************************************************

    public void mouseClicked(MouseEvent e) {
    }
    public void mouseEntered(MouseEvent e) {
    }
    public void mouseExited(MouseEvent e) {
    }
    public void mousePressed(MouseEvent e) {
    }
    public void mouseReleased(MouseEvent e) {
    }
    public void mouseMoved(MouseEvent e) {
    }
    public void mouseDragged(MouseEvent e) {
    }
    public void mouseWheelMoved(MouseEvent e) {
    }

}

