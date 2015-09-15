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
package com.jogamp.newt.util.applet;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.WindowClosingProtocol.WindowClosingMode;
import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLPipelineFactory;

import jogamp.newt.Debug;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.opengl.util.NEWTDemoListener;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;


/** Shows how to deploy an applet using JOGL. This demo must be
    referenced from a web page via an &lt;applet&gt; tag. */

public class JOGLNewtAppletBase implements KeyListener, GLEventListener {
    public static final boolean DEBUG = Debug.debug("Applet");

    String glEventListenerClazzName;
    int glSwapInterval;
    boolean noDefaultKeyListener;
    boolean glClosable;
    boolean glDebug;
    boolean glTrace;

    GLEventListener glEventListener = null;
    GLWindow glWindow = null;
    Animator glAnimator=null;
    boolean isValid = false;
    NativeWindow parentWin;

    public JOGLNewtAppletBase(final String glEventListenerClazzName,
                              final int glSwapInterval,
                              final boolean noDefaultKeyListener,
                              final boolean glClosable,
                              final boolean glDebug,
                              final boolean glTrace) {

        this.glEventListenerClazzName=glEventListenerClazzName;
        this.glSwapInterval=glSwapInterval;
        this.noDefaultKeyListener = noDefaultKeyListener;
        this.glClosable = glClosable;
        this.glDebug = glDebug;
        this.glTrace = glTrace;
    }

    public GLEventListener getGLEventListener() { return glEventListener; }
    public GLWindow getGLWindow() { return glWindow; }
    public Animator getGLAnimator() { return glAnimator; }
    public boolean isValid() { return isValid; }

    public static boolean str2Bool(final String str, final boolean def) {
        if(null==str) return def;
        try {
            return Boolean.valueOf(str).booleanValue();
        } catch (final Exception ex) { ex.printStackTrace(); }
        return def;
    }

    public static int str2Int(final String str, final int def) {
        if(null==str) return def;
        try {
            return Integer.parseInt(str);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return def;
    }

    public static GLEventListener createInstance(final String clazzName) {
        Object instance = null;

        try {
            final Class<?> clazz = AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {
                @Override
                public Class<?> run() {
                    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    Class<?> clazz = null;
                    try {
                        clazz = Class.forName(clazzName, false, cl);
                    } catch (final Throwable t) {
                        t.printStackTrace();
                    }
                    return clazz;
                }
            });
            instance = clazz.newInstance();
        } catch (final Throwable t) {
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

    public static boolean setField(final Object instance, final String fieldName, final Object value) {
        try {
            final Field f = instance.getClass().getField(fieldName);
            if(f.getType().isInstance(value)) {
                f.set(instance, value);
                return true;
            } else {
                System.out.println(instance.getClass()+" '"+fieldName+"' field not assignable with "+value.getClass()+", it's a: "+f.getType());
            }
        } catch (final NoSuchFieldException nsfe) {
            System.out.println(instance.getClass()+" has no '"+fieldName+"' field");
        } catch (final Throwable t) {
            t.printStackTrace();
        }
        return false;
    }

    public void init(final GLWindow glWindow) {
        init(Thread.currentThread().getThreadGroup(), glWindow);
    }

    public void init(final ThreadGroup tg, final GLWindow glWindow) {
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

            glWindow.addWindowListener(reparentHomeListener);

            if(!noDefaultKeyListener) {
                glWindow.addKeyListener(this);
                final NEWTDemoListener newtDemoListener = new NEWTDemoListener(glWindow);
                glWindow.addKeyListener(newtDemoListener);
                glWindow.addMouseListener(newtDemoListener);
            }

            glWindow.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);

            // glAnimator = new FPSAnimator(canvas, 60);
            glAnimator = new Animator();
            glAnimator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD); // No AWT thread involved!
            glAnimator.setThreadGroup(tg);
            glAnimator.add(glWindow);
            glAnimator.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, null);

        } catch (final Throwable t) {
            throw new RuntimeException(t);
        }
        isValid = true;
    }

    private final WindowListener reparentHomeListener = new WindowAdapter() {
        // Closing action: back to parent!
        @Override
        public void windowDestroyNotify(final WindowEvent e) {
            if( isValid() && WindowClosingMode.DO_NOTHING_ON_CLOSE == glWindow.getDefaultCloseOperation() &&
                null == glWindow.getParent() && null != parentWin && 0 != parentWin.getWindowHandle() )
            {
                // we may be called directly by the native EDT
                new InterruptSource.Thread(null, new Runnable() {
                   @Override
                   public void run() {
                    if( glWindow.isNativeValid() && null != parentWin && 0 != parentWin.getWindowHandle() ) {
                        glWindow.reparentWindow(parentWin, -1, -1, Window.REPARENT_HINT_BECOMES_VISIBLE);
                    }
                   }
                }).start();
            }
        } };

    public void start() {
        if(isValid) {
            glWindow.setVisible(true);
            glWindow.sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
            glAnimator.start();
            parentWin = glWindow.getParent();
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

    @Override
    public void init(final GLAutoDrawable drawable) {
        GL _gl = drawable.getGL();

        if(glDebug) {
            try {
                _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, _gl, null) );
            } catch (final Exception e) {e.printStackTrace();}
        }

        if(glTrace) {
            try {
                // Trace ..
                _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, _gl, new Object[] { System.err } ) );
            } catch (final Exception e) {e.printStackTrace();}
        }
        _gl.setSwapInterval(glSwapInterval);
    }
    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
    }
    @Override
    public void display(final GLAutoDrawable drawable) {
    }
    @Override
    public void dispose(final GLAutoDrawable drawable) {
    }

    // ***********************************************************************************
    // ***********************************************************************************
    // ***********************************************************************************

    @Override
    public void keyPressed(final KeyEvent e) {
       if( !e.isPrintableKey() || e.isAutoRepeat() ) {
           return;
       }

       if(e.getKeyChar()=='r' && 0==e.getModifiers() && null!=parentWin) {
           e.setConsumed(true);
           glWindow.invokeOnNewThread(null, false, new Runnable() {
               public void run() {
                   if(null == glWindow.getParent()) {
                        glWindow.reparentWindow(parentWin, -1, -1, 0 /* hints */);
                  } else {
                       final InsetsImmutable insets = glWindow.getInsets();
                       final int x, y;
                       if ( 0 >= insets.getTopHeight() ) {
                           // fail safe ..
                           x = 32;
                           y = 32;
                       } else {
                           x = insets.getLeftWidth();
                           y = insets.getTopHeight();
                       }
                       glWindow.reparentWindow(null, x, y, 0 /* hints */);
                       glWindow.setDefaultCloseOperation( glClosable ? WindowClosingMode.DISPOSE_ON_CLOSE : WindowClosingMode.DO_NOTHING_ON_CLOSE );
                   }
               } } );
       }
    }

    @Override
    public void keyReleased(final KeyEvent e) {
    }

}

