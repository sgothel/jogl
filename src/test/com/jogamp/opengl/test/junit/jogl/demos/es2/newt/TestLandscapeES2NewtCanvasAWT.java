/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
 
package com.jogamp.opengl.test.junit.jogl.demos.es2.newt;

import java.awt.Frame;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.jogl.demos.es2.LandscapeES2;
import com.jogamp.opengl.test.junit.newt.parenting.NewtAWTReparentingKeyAdapter;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;
import javax.swing.SwingUtilities;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestLandscapeES2NewtCanvasAWT extends UITestCase {    
    static DimensionImmutable wsize = new Dimension(500, 290);
    
    static long duration = 500; // ms
    static int swapInterval = 1;
    static boolean shallUseOffscreenFBOLayer = false;
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean mainRun = false;
    static boolean exclusiveContext = false;
    static boolean useAnimator = true;
    
    // public enum ResizeBy { GLWindow, Component, Frame };
    protected void runTestGL(final GLCapabilitiesImmutable caps) throws InterruptedException, InvocationTargetException {
        System.err.println("requested: vsync "+swapInterval+", "+caps);
        Display dpy = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(dpy, 0);
        final GLWindow glWindow = GLWindow.create(screen, caps);
        
        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow);
        if ( shallUseOffscreenFBOLayer ) {
            newtCanvasAWT.setShallUseOffscreenLayer(true);
        }
        
        final Frame frame = new Frame("AWT Parent Frame");
        {
            java.awt.Dimension d = new java.awt.Dimension(wsize.getWidth(), wsize.getHeight());
            frame.setSize(d);
        }
        frame.add(newtCanvasAWT);
        frame.setTitle("Gears NewtCanvasAWT Test (translucent "+!caps.isBackgroundOpaque()+"), swapInterval "+swapInterval+", size "+wsize);
        
        final LandscapeES2 demo = new LandscapeES2(swapInterval);
        glWindow.addGLEventListener(demo);
        
        final Animator animator = useAnimator ? new Animator() : null;
        if( useAnimator ) {
            animator.setModeBits(false, Animator.MODE_EXPECT_AWT_RENDERING_THREAD);
            animator.setExclusiveContext(exclusiveContext);
        }
        
        final QuitAdapter quitAdapter = new QuitAdapter();
        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(WindowEvent e) {
                System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight());
            }
            public void windowMoved(WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight());
            }            
        });
        
        glWindow.addKeyListener(new NewtAWTReparentingKeyAdapter(frame, newtCanvasAWT, glWindow, quitAdapter));
        
        if( useAnimator ) {
            animator.add(glWindow);
            animator.start();
        }

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.validate();
               frame.setVisible(true);               
           }
        });        
        
        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, System.err);
        }
        
        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getWidth()+"x"+glWindow.getHeight()+", "+glWindow.getInsets());
                
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        if( useAnimator ) {
            animator.stop();
        }
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {               
               frame.dispose();               
           }
        });
        glWindow.destroy();
    }

    @Test
    public void test01GL2ES2() throws InterruptedException, InvocationTargetException {
        final GLProfile glp;
        if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else {
            glp = GLProfile.getGL2ES2();
        }
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps);
    }

    @Test
    public void test02GL3() throws InterruptedException, InvocationTargetException {
        if(mainRun) return;
        
        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps);
    }
    
    public static void main(String args[]) throws IOException {
        mainRun = true;
        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-layeredFBO")) {
                shallUseOffscreenFBOLayer = true;
            } else if(args[i].equals("-exclctx")) {
                exclusiveContext = true;
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            }
        }
        
        System.err.println("size "+wsize);
        System.err.println("shallUseOffscreenFBOLayer     "+shallUseOffscreenFBOLayer);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("swapInterval "+swapInterval);
        System.err.println("exclusiveContext "+exclusiveContext);
        System.err.println("useAnimator "+useAnimator);

        org.junit.runner.JUnitCore.main(TestLandscapeES2NewtCanvasAWT.class.getName());
    }
}
