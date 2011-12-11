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

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.media.nativewindow.NativeWindow;
import javax.media.opengl.FPSCounter;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPipelineFactory;

import jogamp.newt.Debug;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;


/** Shows how to deploy an applet using JOGL. This demo must be
    referenced from a web page via an &lt;applet&gt; tag. */

public class JOGLNewtAppletBase implements KeyListener, GLEventListener {
    public static final boolean DEBUG = Debug.debug("Applet");
    
    String glEventListenerClazzName;
    int glSwapInterval;
    boolean noDefaultKeyListener;
    boolean glDebug;
    boolean glTrace;

    GLEventListener glEventListener = null;
    GLWindow glWindow = null;
    Animator glAnimator=null;
    boolean isValid = false;
    NativeWindow awtParent;

    public JOGLNewtAppletBase(String glEventListenerClazzName, 
                              int glSwapInterval,
                              boolean noDefaultKeyListener,
                              boolean glDebug,
                              boolean glTrace) {
    
        this.glEventListenerClazzName=glEventListenerClazzName;
        this.glSwapInterval=glSwapInterval;
        this.noDefaultKeyListener = noDefaultKeyListener;
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

    public static GLEventListener createInstance(final String clazzName) {
        Object instance = null;

        try {
            final Class<?> clazz = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
                public Class<?> run() {
                    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(clazzName, false, cl);
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    return clazz;
                }
            });
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
        isValid = false;
        this.glWindow = glWindow;

        glEventListener = createInstance(glEventListenerClazzName);
        if(null == glEventListener) {
            return;
        }

        try {
            if(!setField(glEventListener, "window", glWindow)) {
                setField(glEventListener, "glWindow", glWindow);
            }

            glWindow.addGLEventListener(this);
            glWindow.addGLEventListener(glEventListener);

            if(glEventListener instanceof WindowListener) {
                glWindow.addWindowListener((WindowListener)glEventListener);
            }

            if(glEventListener instanceof MouseListener) {
                glWindow.addMouseListener((MouseListener)glEventListener);
            }

            if(glEventListener instanceof KeyListener) {
                glWindow.addKeyListener((KeyListener)glEventListener);
            }
            
            if(!noDefaultKeyListener) {
                glWindow.addKeyListener(this);
            }

            glWindow.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);

            // glAnimator = new FPSAnimator(canvas, 60);
            glAnimator = new Animator(tg, glWindow);
            glAnimator.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, null);
            
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
        isValid = true;
    }

    public void start() {
        if(isValid) {
            glWindow.setVisible(true);
            glWindow.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
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
}

