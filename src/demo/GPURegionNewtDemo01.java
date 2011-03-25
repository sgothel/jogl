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
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.HwRegionRenderer;
import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

/** Demonstrate the rendering of multiple outlines into one region/OutlineShape
 *  These Outlines are not necessary connected or contained.
 *  The output of this demo shows two identical shapes but the left one
 *  has some vertices with off-curve flag set to true, and the right allt he vertices 
 *  are on the curve. Demos the Res. Independent Nurbs based Curve rendering 
 *
 */
public class GPURegionNewtDemo01 {
	private static void create(){
		new RegionNewtWindow();
	}
	public static void main(String[] args) {
		create();
	}
}

class RegionNewtWindow {
	RegionGLListener regionGLListener = null; 

	public RegionNewtWindow(){
		createWindow();
	}
	private void createWindow() {
		GLProfile.initSingleton(true);
		GLProfile glp = GLProfile.getGL2ES2();
		GLCapabilities caps = new GLCapabilities(glp);
		caps.setDoubleBuffered(true);
		caps.setSampleBuffers(true);
		caps.setNumSamples(4);
		System.out.println(caps);
		final GLWindow window = GLWindow.create(caps);
		window.setPosition(10, 10);
		window.setSize(500, 500);

		window.setTitle("GPU Curve Region Newt Demo 01");
		regionGLListener = new RegionGLListener();
		window.addGLEventListener(regionGLListener);

		window.setVisible(true);

		window.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent arg0) {
				if(arg0.getKeyCode() == KeyEvent.VK_1){
					regionGLListener.zoomIn();
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_2){
					regionGLListener.zoomOut();
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_UP){
					regionGLListener.move(0, -1);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_DOWN){
					regionGLListener.move(0, 1);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_LEFT){
					regionGLListener.move(1, 0);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_RIGHT){
					regionGLListener.move(-1, 0);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_0){
					regionGLListener.rotate(1);
				}
				else if(arg0.getKeyCode() == KeyEvent.VK_9){
					regionGLListener.rotate(-1);
				}
			}
			public void keyTyped(KeyEvent arg0) {}
			public void keyReleased(KeyEvent arg0) {}
		});

		FPSAnimator animator = new FPSAnimator(60);
		animator.add(window);
		window.addWindowListener(new WindowAdapter() {
			public void windowDestroyNotify(WindowEvent arg0) {
				System.exit(0);
			};
		});
		animator.start();
	}
	private class RegionGLListener implements GLEventListener{
		HwRegionRenderer regionRenderer = null;
		
		OutlineShape outlineShape = null;
		
		public RegionGLListener() {}
		
		private void createTestOutline(){
			float offset = 0;
			outlineShape = new OutlineShape(SVertex.factory());
			outlineShape.addVertex(0.0f,-10.0f, true);
			outlineShape.addVertex(15.0f,-10.0f, true);
			outlineShape.addVertex(10.0f,5.0f, false);
			outlineShape.addVertex(15.0f,10.0f, true);
			outlineShape.addVertex(6.0f,15.0f, false);
			outlineShape.addVertex(5.0f,8.0f, false);
			outlineShape.addVertex(0.0f,10.0f,true);
			outlineShape.closeLastOutline();
			outlineShape.addEmptyOutline();
			outlineShape.addVertex(5.0f,-5.0f,true);
			outlineShape.addVertex(10.0f,-5.0f, false);
			outlineShape.addVertex(10.0f,0.0f, true);
			outlineShape.addVertex(5.0f,0.0f, false);
			outlineShape.closeLastOutline();
			
			/** Same shape as above but without any off-curve vertices */
			outlineShape.addEmptyOutline();
			offset = 30;
			outlineShape.addVertex(offset+0.0f,-10.0f, true);
			outlineShape.addVertex(offset+17.0f,-10.0f, true);
			outlineShape.addVertex(offset+11.0f,5.0f, true);
			outlineShape.addVertex(offset+16.0f,10.0f, true);
			outlineShape.addVertex(offset+7.0f,15.0f, true);
			outlineShape.addVertex(offset+6.0f,8.0f, true);
			outlineShape.addVertex(offset+0.0f,10.0f, true);
			outlineShape.closeLastOutline();
			outlineShape.addEmptyOutline();
			outlineShape.addVertex(offset+5.0f,0.0f, true);
			outlineShape.addVertex(offset+5.0f,-5.0f, true);
			outlineShape.addVertex(offset+10.0f,-5.0f, true);
			outlineShape.addVertex(offset+10.0f,0.0f, true);
			outlineShape.closeLastOutline();
		}

		public void init(GLAutoDrawable drawable) {
			GL2ES2 gl = drawable.getGL().getGL2ES2();
			gl.setSwapInterval(1);
			gl.glEnable(GL2ES2.GL_DEPTH_TEST);
			regionRenderer = new HwRegionRenderer(drawable.getContext());
			createTestOutline();
		}

		float ang = 0;
		float zoom = -70;
		float xTran = -20;
		float yTran = 5;

		public void display(GLAutoDrawable drawable) {
			GL2ES2 gl = drawable.getGL().getGL2ES2();

			gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
			gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

			regionRenderer.resetMatrix();
			regionRenderer.translate(xTran, yTran, zoom);
			regionRenderer.rotate(ang, 0, 1, 0);

			try {
				regionRenderer.setAlpha(1.0f);
				regionRenderer.setColor(0.0f, 0.0f, 1.0f);
				
				regionRenderer.renderOutlineShape(outlineShape, new float[]{0,0,0});
			} catch (Exception e) { 
				e.printStackTrace();
			}
		}
		public void reshape(GLAutoDrawable drawable, int xstart, int ystart, int width, int height){
			GL2ES2 gl = drawable.getGL().getGL2ES2();
			gl.glViewport(xstart, ystart, width, height);

			regionRenderer.reshape(drawable, 45.0f, (float)width / (float)height, 0.1f, 7000.0f);
		}

		public void zoomIn(){
			zoom++;
		}
		public void zoomOut(){
			zoom--;
		}
		public void move(float x, float y){
			xTran += x;
			yTran += y;
		}
		public void rotate(float delta){
			ang+= delta;
			ang%=360;
		}
		public void dispose(GLAutoDrawable arg0) {
			regionRenderer.clearCached();
			
		}
	}
}
