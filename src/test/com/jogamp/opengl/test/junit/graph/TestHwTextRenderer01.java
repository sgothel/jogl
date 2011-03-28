package test.com.jogamp.opengl.test.junit.graph;

import java.io.IOException;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GL;
import javax.media.opengl.GL3;
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

	static GLWindow createWindow(String title, GLCapabilities caps, int width, int height, boolean onscreen) {
		Assert.assertNotNull(caps);
		caps.setOnscreen(onscreen);

		GLWindow window = GLWindow.create(caps);
		window.setSize(width, height);
		window.setPosition(10, 10);
		window.setTitle(title);
		Assert.assertNotNull(window);
		window.setVisible(true);
		//window.setAutoSwapBufferMode(false);

		return window;
	}

	@Test
	public void testTextRendererR2T01() throws InterruptedException {
		GLProfile glp = GLProfile.get(GLProfile.GL3bc);
		GLCapabilities caps = new GLCapabilities(glp);
		caps.setAlphaBits(4);	

		GLWindow window = createWindow("r2t1msaa0", caps, 400,400,true);
		TextR2TGLListener textGLListener = new TextR2TGLListener(Region.TWO_PASS);
		textGLListener.setTech(-10, 10, 0f, -1000, 400);
		textGLListener.attachTo(window);

		FPSAnimator animator = new FPSAnimator(60);
		animator.add(window);
		animator.start();
		
		window.getAnimator().resume();
		
		while(!textGLListener.isPrinted()){
			Thread.sleep(100);
		}
		animator.stop();
		destroyWindow(window); 
		
		Thread.sleep(1000);
	}
	
	@Test
	public void testTextRendererR2T02() throws InterruptedException {
		GLProfile glp = GLProfile.get(GLProfile.GL3bc);
		GLCapabilities caps = new GLCapabilities(glp);
		caps.setAlphaBits(4);	

		GLWindow window = createWindow("r2t1msaa0", caps, 400,400,true);
		TextR2TGLListener textGLListener = new TextR2TGLListener(Region.TWO_PASS);
		textGLListener.setTech(-111, 74, 0, -380, 900);
		textGLListener.attachTo(window);
		
		while(!window.isRealized()){
			
		}

		FPSAnimator animator = new FPSAnimator(60);
		animator.add(window);
		animator.start();
		
		window.getAnimator().resume();
		
		while(!textGLListener.isPrinted()){
			Thread.sleep(100);
		}
		animator.stop();
		destroyWindow(window);  
	}


	private class TextR2TGLListener extends GPUTextGLListenerBase01 {
		GLWindow glwindow;
		public TextR2TGLListener(int type) {
			super(SVertex.factory(), type, false, false);
		}
		
		public void setTech(float xt, float yt, float angle, int zoom, int fboSize){
			setMatrix(xt, yt, angle, zoom, fboSize);       
		}

		public void init(GLAutoDrawable drawable) {
			GL3 gl = drawable.getGL().getGL3();
			super.init(drawable);
			gl.setSwapInterval(1);
			gl.glEnable(GL3.GL_DEPTH_TEST);
			textRenderer.init(gl);
			textRenderer.setAlpha(gl, 1.0f);
			textRenderer.setColor(gl, 0.0f, 0.0f, 0.0f);
			gl.glDisable(GL.GL_MULTISAMPLE); 
		}
		public void attachTo(GLWindow window) {
			super.attachTo(window);
			glwindow = window;
		}

		public boolean isPrinted(){
			return !printScreen;
		}

		public void display(GLAutoDrawable drawable) {
			super.display(drawable);

			try {
				if(printScreen){
					printScreen(glwindow, "./", "r2t1msaa0", false);
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
