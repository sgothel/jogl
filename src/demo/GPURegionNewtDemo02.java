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

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.HwRegionRenderer;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

/** Demonstrate the rendering of multiple OutlineShapes
 *  into one region
 *
 */
public class GPURegionNewtDemo02 {
	private static void create(){
		new RegionsNewtWindow();
	}
	public static void main(String[] args) {
		create();
	}
}

class RegionsNewtWindow {
	RegionGLListener regionGLListener = null; 

	public RegionsNewtWindow(){
		createWindow();
	}
	private void createWindow() {
		GLProfile.initSingleton(true);
		GLProfile glp = GLProfile.getGL2ES2();
		GLCapabilities caps = new GLCapabilities(glp);
        System.out.println("Requested: "+caps);
		final GLWindow window = GLWindow.create(caps);
		window.setPosition(10, 10);
		window.setSize(500, 500);

		window.setTitle("GPU Curve Region Newt Demo 02 - r2t0 msaa0");
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
		
		OutlineShape[] outlineShapes = new OutlineShape[2];
		
		public RegionGLListener() {}
		
		private void createTestOutline(){
			float offset = 0;
			outlineShapes[0] = new OutlineShape(SVertex.factory());
			outlineShapes[0].addVertex(0.0f,-10.0f,true);
			outlineShapes[0].addVertex(15.0f,-10.0f, true);
			outlineShapes[0].addVertex(10.0f,5.0f, false);
			outlineShapes[0].addVertex(15.0f,10.0f, true);
			outlineShapes[0].addVertex(6.0f,15.0f, false);
			outlineShapes[0].addVertex(5.0f,8.0f, false);
			outlineShapes[0].addVertex(0.0f,10.0f,true);
			outlineShapes[0].closeLastOutline();
			outlineShapes[0].addEmptyOutline();
			outlineShapes[0].addVertex(5.0f,-5.0f,true);
			outlineShapes[0].addVertex(10.0f,-5.0f, false);
			outlineShapes[0].addVertex(10.0f,0.0f, true);
			outlineShapes[0].addVertex(5.0f,0.0f, false);
			outlineShapes[0].closeLastOutline();
			
			/** Same shape as above but without any off-curve vertices */
			outlineShapes[1] = new OutlineShape(SVertex.factory());
			offset = 30;
			outlineShapes[1].addVertex(offset+0.0f,-10.0f, true);
			outlineShapes[1].addVertex(offset+17.0f,-10.0f, true);
			outlineShapes[1].addVertex(offset+11.0f,5.0f, true);
			outlineShapes[1].addVertex(offset+16.0f,10.0f, true);
			outlineShapes[1].addVertex(offset+7.0f,15.0f, true);
			outlineShapes[1].addVertex(offset+6.0f,8.0f, true);
			outlineShapes[1].addVertex(offset+0.0f,10.0f, true);
			outlineShapes[1].closeLastOutline();
			outlineShapes[1].addEmptyOutline();
			outlineShapes[1].addVertex(offset+5.0f,0.0f, true);
			outlineShapes[1].addVertex(offset+5.0f,-5.0f, true);
			outlineShapes[1].addVertex(offset+10.0f,-5.0f, true);
			outlineShapes[1].addVertex(offset+10.0f,0.0f, true);
			outlineShapes[1].closeLastOutline();
		}

		public void init(GLAutoDrawable drawable) {
			GL2ES2 gl = drawable.getGL().getGL2ES2();
			gl.setSwapInterval(1);
			gl.glEnable(GL2ES2.GL_DEPTH_TEST);
			regionRenderer = new HwRegionRenderer(drawable.getContext());
            regionRenderer.setAlpha(1.0f);
            regionRenderer.setColor(0.0f, 0.0f, 0.0f);
            MSAATool.dump(drawable);
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
				regionRenderer.renderOutlineShapes(outlineShapes, new float[]{0,0,0});
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
