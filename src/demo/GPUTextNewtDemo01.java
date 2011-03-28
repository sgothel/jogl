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

import java.io.IOException;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

public class GPUTextNewtDemo01 {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    
	public static void main(String[] args) {
	    GPUTextNewtDemo01 test = new GPUTextNewtDemo01();
	    test.testMe();
	}
	
	TextGLListener textGLListener = null; 
	GLWindow window;
	public void testMe() {
		GLProfile.initSingleton(true);
		GLProfile glp = GLProfile.getGL2ES2();
		GLCapabilities caps = new GLCapabilities(glp);
		caps.setAlphaBits(4);
	    caps.setSampleBuffers(true);
		caps.setNumSamples(4); // 2 samples is not enough ..
		System.out.println("Requested: "+caps);
		
		window = GLWindow.create(caps);
		
		window.setPosition(10, 10);
		window.setSize(500, 500);

		window.setTitle("GPU Text Newt Demo 01 - r2t0 msaa1");
		textGLListener = new TextGLListener();
		textGLListener.attachTo(window);

		window.setVisible(true);
		FPSAnimator animator = new FPSAnimator(10);
		// Animator animator = new Animator();
		animator.add(window);
		animator.start();
	}
	
	private class TextGLListener extends GPUTextGLListenerBase01 {
	    public TextGLListener() {
		    super(SVertex.factory(), Region.SINGLE_PASS, DEBUG, TRACE);
            setMatrix(-10, 10, 0f, -70, 0);		    
		}
		
		public void init(GLAutoDrawable drawable) {
            GL2ES2 gl = drawable.getGL().getGL2ES2();
		    
            super.init(drawable);
            
			gl.setSwapInterval(1);
			gl.glEnable(GL2ES2.GL_DEPTH_TEST);
			textRenderer.init(gl);
			textRenderer.setAlpha(gl, 1.0f);
			textRenderer.setColor(gl, 0.0f, 0.0f, 0.0f);
			//gl.glSampleCoverage(0.95f, false);
			//gl.glEnable(GL2GL3.GL_SAMPLE_COVERAGE); // sample coverage doesn't really make a difference to lines
			//gl.glEnable(GL2GL3.GL_SAMPLE_ALPHA_TO_COVERAGE);
			//gl.glEnable(GL2GL3.GL_SAMPLE_ALPHA_TO_ONE);
			MSAATool.dump(drawable);
		}
		
		public void display(GLAutoDrawable drawable) {
			super.display(drawable);
			
			if(printScreen){
				try {
					super.printScreen(window, "./","r2t0-msaa1", false);
					printScreen=false;
				} catch (GLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
