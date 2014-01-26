/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
 
package com.jogamp.opengl.test.junit.jogl.awt;

import java.lang.reflect.InvocationTargetException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;


import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import javax.swing.JFrame;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import static org.junit.Assume.*;
import static javax.swing.SwingUtilities.*;

/**
 * Tests context creation + display on various kinds of Window implementations.
 * @author Michael Bien, et. al.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug551AWT extends UITestCase {

    static void checkGraphicsEnvironment() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge =GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();

        //write graphics devices to log 
        System.err.println("number of graphics devices " + gs.length);
        for(int i =0 ; i < gs.length; i++) {
            System.err.println(gs[i].toString());
        }

        //check for bounds
        for (int j = 0; j < gs.length; j++) { 
            GraphicsDevice gd = gs[j];
            GraphicsConfiguration[] gc = gd.getConfigurations();
            for (int i=0; i < gc.length; i++) {
                System.err.println("graphics configuration for device " + j + " is: " + gc[i].getBounds());
                virtualBounds = virtualBounds.union(gc[i].getBounds());
            }            
        }

    }
    
    protected void runTestGL() throws InterruptedException, InvocationTargetException {
        final Window window = new JFrame(this.getSimpleTestName(" - "));
        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
        
        // final array as mutable container hack
        final GLCanvas[] glCanvas = new GLCanvas[1];

        Runnable test = new Runnable() {
            public void run() {
                glCanvas[0] = new GLCanvas(caps);
                glCanvas[0].addGLEventListener(new GearsES2());
                window.add(glCanvas[0]);

                // Revalidate size/layout.
                // Always validate if component added/removed.
                // Ensure 1st paint of GLCanvas will have a valid size, hence drawable gets created.
                window.setSize(512, 512);
                window.validate();

                window.setVisible(true);
                glCanvas[0].display();
            }
        };

        Runnable cleanup = new Runnable() {
            public void run() {
                System.out.println("cleaning up...");
                window.setVisible(false);
                try {
                    window.removeAll();
                } catch (Throwable t) {
                    assumeNoException(t);
                    t.printStackTrace();
                }
                window.dispose();
            }

        };

        // AWT / Swing on EDT..
        invokeAndWait(test);

        Animator animator = new Animator(glCanvas[0]);
        animator.start();
        Thread.sleep(1000);
        animator.stop();

        // AWT / Swing on EDT..
        invokeAndWait(cleanup);
    }

    // @Test
    public void test01Plain() throws InterruptedException, InvocationTargetException {
        runTestGL();
    }

    @Test
    public void test02WithCheckGraphicsEnvironment() throws InterruptedException, InvocationTargetException {
        checkGraphicsEnvironment();
        runTestGL();
    }

    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestBug551AWT.class.getName());
    }
}
