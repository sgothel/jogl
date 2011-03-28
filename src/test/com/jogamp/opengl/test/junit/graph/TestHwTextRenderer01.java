package test.com.jogamp.opengl.test.junit.graph;

import java.io.IOException;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.FPSAnimator;

import demo.GPUTextGLListenerBase01;

public class TestHwTextRenderer01 {

	@BeforeClass
	public static void initClass() {
		GLProfile.initSingleton(true);
		NativeWindowFactory.initSingleton(true);
	}

	static void destroyWindow(Window window) {
		if(null!=window) {
			window.destroy();
		}
	}

	static GLWindow createWindow(String title, GLCapabilities caps, int width, int height) {
		Assert.assertNotNull(caps);

		GLWindow window = GLWindow.create(caps);
		window.setSize(width, height);
		window.setPosition(10, 10);
		window.setTitle(title);
		Assert.assertNotNull(window);
		window.setVisible(true);

		return window;
	}

	@Test
	public void testTextRendererR2T01() throws InterruptedException {
		GLProfile glp = GLProfile.get(GLProfile.GL3bc);
		GLCapabilities caps = new GLCapabilities(glp);
		caps.setAlphaBits(4);	

		GLWindow window = createWindow("r2t1msaa0", caps, 400,400);
		TextGLListener textGLListener = new TextGLListener(Region.TWO_PASS);
		textGLListener.setTech(-10, 10, 0f, -1000, 400);
		textGLListener.attachTo(window);

		FPSAnimator animator = new FPSAnimator(10);
		animator.add(window);
		animator.start();
		
		while(!textGLListener.isPrinted()){
			Thread.sleep(100);
		}
		
		textGLListener.resetPrinting();
		textGLListener.setTech(-111, 74, 0, -380, 900);
		Thread.sleep(100);
		while(!textGLListener.isPrinted()){
			Thread.sleep(100);
		}
		
		animator.stop();
		destroyWindow(window); 
		
		Thread.sleep(1000);
	}
	
	//@Test
	public void testTextRendererMSAA01() throws InterruptedException {
		GLProfile glp = GLProfile.get(GLProfile.GL2ES2);
		GLCapabilities caps = new GLCapabilities(glp);
		//caps.setAlphaBits(4);	
		caps.setSampleBuffers(true);
		caps.setNumSamples(4);

		GLWindow window = createWindow("r2t0msaa1", caps, 400,0);
		TextGLListener textGLListener = new TextGLListener(Region.SINGLE_PASS);
		textGLListener.setTech(-10, 10, 0f, -1000, 0);
		textGLListener.attachTo(window);

		FPSAnimator animator = new FPSAnimator(10);
		animator.add(window);
		animator.start();
		
		while(!textGLListener.isPrinted()){
			Thread.sleep(100);
		}
		
		//textGLListener.resetPrinting();
		//textGLListener.setTech(-111, 74, 0, -380, 0);
		//Thread.sleep(100);
		//while(!textGLListener.isPrinted()){
		//	Thread.sleep(100);
		//}
		
		animator.stop();
		destroyWindow(window); 
		
		Thread.sleep(1000);
	}
	
	private class TextGLListener extends GPUTextGLListenerBase01 {
		GLWindow glwindow;
		public TextGLListener(int type) {
			super(SVertex.factory(), type, false, false);
		}
		
		public void setTech(float xt, float yt, float angle, int zoom, int fboSize){
			setMatrix(xt, yt, angle, zoom, fboSize);       
		}

		public void init(GLAutoDrawable drawable) {
			GL2ES2 gl = drawable.getGL().getGL2ES2();
			super.init(drawable);
			gl.setSwapInterval(1);
			gl.glEnable(GL.GL_DEPTH_TEST);
			textRenderer.init(gl);
			textRenderer.setAlpha(gl, 1.0f);
			textRenderer.setColor(gl, 0.0f, 0.0f, 0.0f);
		}
		public void attachTo(GLWindow window) {
			super.attachTo(window);
			glwindow = window;
		}

		public boolean isPrinted(){
			return !printScreen;
		}
		
		public void resetPrinting(){
			printScreen = true;
		}

		public void display(GLAutoDrawable drawable) {
			super.display(drawable);

			try {
				if(printScreen){
					printScreen(glwindow, "./", glwindow.getTitle(), false);
					printScreen = false;
				}
			} catch (GLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
