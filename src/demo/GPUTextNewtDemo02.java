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
package demo;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;

public class GPUTextNewtDemo02 {
    /**
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
        GPUTextNewtDemo02 test = new GPUTextNewtDemo02();
        test.testMe();
        
    }
    
    GLWindow window;
	TextGLListener textGLListener = null; 

	public void testMe() {
		GLProfile.initSingleton(true);
		GLProfile glp = GLProfile.get(GLProfile.GL3bc);
		
		GLCapabilities caps = new GLCapabilities(glp);
		caps.setAlphaBits(4);		
		System.out.println("Requested: "+caps);
		
		window = GLWindow.create(caps);
		
		window.setPosition(10, 10);
        window.setSize(800, 400);		

        window.setTitle("GPU Text Newt Demo 02 - r2t1 msaa0");
        textGLListener = new TextGLListener();
        textGLListener.attachTo(window);

        window.enablePerfLog(true);
		window.setVisible(true);

		// FPSAnimator animator = new FPSAnimator(60);
        Animator animator = new Animator();		
		animator.add(window);
		animator.start();
	}
	
	private class TextGLListener extends GPUTextGLListenerBase01 {
        public TextGLListener() {
            super(SVertex.factory(), Region.TWO_PASS, DEBUG, TRACE);
            // FIXME: Rami will fix FBO size !!
            // setMatrix(-10, 10, 0f, -100, 400);   
            // setMatrix(-80, -30, 0f, -100, window.getWidth()*3);
            setMatrix(-400, -30, 0f, -500, window.getWidth()*3);
        }
	    
		public void init(GLAutoDrawable drawable) {
            GL3 gl = drawable.getGL().getGL3();
            
            super.init(drawable);
            
			gl.setSwapInterval(1);
			gl.glEnable(GL3.GL_DEPTH_TEST);
			textRenderer.init(gl);
			textRenderer.setAlpha(gl, 1.0f);
			textRenderer.setColor(gl, 0.0f, 0.0f, 0.0f);
			gl.glDisable(GL.GL_MULTISAMPLE); // this state usually doesn't matter in driver - but document here: no MSAA 
			//gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL3.GL_NICEST);
			MSAATool.dump(drawable);
		}
	}
}
