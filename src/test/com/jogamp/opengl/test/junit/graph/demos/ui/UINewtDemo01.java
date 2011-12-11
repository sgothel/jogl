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

package com.jogamp.opengl.test.junit.graph.demos.ui;

import javax.media.opengl.FPSCounter;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.glsl.ShaderState;

/** Demonstrate the rendering of multiple outlines into one region/OutlineShape
 *  These Outlines are not necessary connected or contained.
 *  The output of this demo shows two identical shapes but the left one
 *  has some vertices with off-curve flag set to true, and the right allt he vertices 
 *  are on the curve. Demos the Res. Independent Nurbs based Curve rendering 
 *
 */
public class UINewtDemo01 {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    
    public static void main(String[] args) {
        GLProfile glp = GLProfile.getGL2ES2();
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        System.out.println("Requested: " + caps);
        
        final GLWindow window = GLWindow.create(caps);
        window.setPosition(10, 10);
        window.setSize(800, 400);
        window.setTitle("GPU UI Newt Demo 01");
        RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());
        UIGLListener01 uiGLListener = new UIGLListener01 (rs, DEBUG, TRACE);
        uiGLListener.attachInputListenerTo(window);        
        window.addGLEventListener(uiGLListener);

        window.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);        
        window.setVisible(true);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);
        animator.add(window);
        
        window.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent arg0) {
                if(arg0.getKeyCode() == KeyEvent.VK_F4) {
                    window.destroy();
                }
            }
        });
        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyed(WindowEvent e) {
                animator.stop();
            }
        });
                
        animator.start();
    }    
}
