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
package com.jogamp.opengl.test.junit.graph.demos;

import javax.media.opengl.FPSCounter;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.Renderer;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.glsl.ShaderState;

public class GPUTextNewtDemo02 {
    /**
     * FIXME:
     * 
     * If DEBUG is enabled:
     *  
     * Caused by: javax.media.opengl.GLException: Thread[main-Display-X11_:0.0-1-EDT-1,5,main] glGetError() returned the following error codes after a call to glFramebufferRenderbuffer(<int> 0x8D40, <int> 0x1902, <int> 0x8D41, <int> 0x1): GL_INVALID_ENUM ( 1280 0x500), 
     * at javax.media.opengl.DebugGL4bc.checkGLGetError(DebugGL4bc.java:33961)
     * at javax.media.opengl.DebugGL4bc.glFramebufferRenderbuffer(DebugGL4bc.java:33077)
     * at jogamp.graph.curve.opengl.VBORegion2PGL3.initFBOTexture(VBORegion2PGL3.java:295)
     */
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    
    public static void main(String[] args) {
        GLProfile.initSingleton(true);
        GLProfile glp = GLProfile.getGL2ES2();
        
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);        
        System.out.println("Requested: "+caps);
        
        final GLWindow window = GLWindow.create(caps);
        
        window.setPosition(10, 10);
        window.setSize(800, 400);        
        window.setTitle("GPU Text Newt Demo 02 - r2t1 msaa0");
        
        RenderState rs = Renderer.createRenderState(new ShaderState(), SVertex.factory());
        GPUTextGLListener0A textGLListener = new GPUTextGLListener0A(rs, Region.TWO_PASS, window.getWidth()*3, DEBUG, TRACE);
        // ((TextRenderer)textGLListener.getRenderer()).setCacheLimit(32);
        textGLListener.attachInputListenerTo(window);
        window.addGLEventListener(textGLListener);
        
        window.setUpdateFPSFrames(FPSCounter.DEFAULT_FRAMES_PER_INTERVAL, System.err);        
        window.setVisible(true);
        // FPSAnimator animator = new FPSAnimator(60);
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
