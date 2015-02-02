/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.demos.gl4.awt;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.gl4.TriangleInstancedRendererWithShaderState;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.FPSAnimator;

/**
 * Test Instanced rendering demo TrianglesInstancedRenderer
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestInstancedReneringGL4AWT extends UITestCase {
	static GLProfile glp;
	static int width, height;
	static boolean shallUsePBuffer = false;
	static boolean shallUseBitmap = false;
	static boolean useMSAA = false;
	static int swapInterval = 0;
	static boolean useAnimator = true;
	static boolean manualTest = false;

	@BeforeClass
	public static void initClass() {
		if(GLProfile.isAvailable(GLProfile.GL4)) {
			glp = GLProfile.get(GLProfile.GL4);
			Assert.assertNotNull(glp);
			width = 640;
			height = 480;
		} else {
			setTestSupported(false);
		}
	}

	@AfterClass
	public static void releaseClass() {
	}

	protected void runTestGL(final GLCapabilities caps)
			throws AWTException, InterruptedException, InvocationTargetException
	{
		final JFrame frame = new JFrame("Swing GLJPanel");
		Assert.assertNotNull(frame);
		final GLJPanel glJPanel = new GLJPanel(caps);
		Assert.assertNotNull(glJPanel);
		final Dimension glc_sz = new Dimension(width, height);
		glJPanel.setMinimumSize(glc_sz);
		glJPanel.setPreferredSize(glc_sz);
		glJPanel.setSize(glc_sz);
		glJPanel.addGLEventListener(new TriangleInstancedRendererWithShaderState(null));
		final SnapshotGLEventListener snap = new SnapshotGLEventListener();
		glJPanel.addGLEventListener(snap);
		final FPSAnimator animator = useAnimator ? new FPSAnimator(glJPanel, 60) : null;
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				frame.getContentPane().add(glJPanel, BorderLayout.CENTER);
				frame.getContentPane().validate();
				frame.pack();
				frame.setVisible(true);
			} } ) ;
		if( useAnimator ) {
			animator.setUpdateFPSFrames(60, System.err);
			animator.start();
			Assert.assertEquals(true, animator.isAnimating());
		}
		final QuitAdapter quitAdapter = new QuitAdapter();
		new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), glJPanel).addTo(glJPanel);
		new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glJPanel).addTo(frame);
		final com.jogamp.newt.event.KeyListener kl = new com.jogamp.newt.event.KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if( e.isAutoRepeat() ) {
					return;
				}
				if(e.getKeyChar()=='m') {
					final GLCapabilitiesImmutable capsPre = glJPanel.getChosenGLCapabilities();
					final GLCapabilities capsNew = new GLCapabilities(capsPre.getGLProfile());
					capsNew.copyFrom(capsPre);
					final boolean msaa;
					if( capsPre.getSampleBuffers() ) {
						capsNew.setSampleBuffers(false);
						capsNew.setDoubleBuffered(false);
						msaa = false;
					} else {
						capsNew.setSampleBuffers(true);
						capsNew.setNumSamples(4);
						msaa = true;
					}
					System.err.println("[set MSAA "+msaa+" Caps had]: "+capsPre);
					System.err.println("[set MSAA "+msaa+" Caps new]: "+capsNew);
					System.err.println("XXX-A1: "+animator.toString());
//					glJPanel.setRequestedGLCapabilities(capsNew);
					System.err.println("XXX-A2: "+animator.toString());
					System.err.println("XXX: "+glJPanel.toString());
				} else if(e.getKeyChar()=='b') {
					final GLCapabilitiesImmutable capsPre = glJPanel.getChosenGLCapabilities();
					final GLCapabilities capsNew = new GLCapabilities(capsPre.getGLProfile());
					capsNew.copyFrom(capsPre);
					final boolean bmp;
					if( capsPre.isBitmap() ) {
						capsNew.setBitmap(false); // auto-choose
						bmp = false;
					} else {
						capsNew.setBitmap(true);
						capsNew.setFBO(false);
						capsNew.setPBuffer(false);
						bmp = true;
					}
					System.err.println("[set Bitmap "+bmp+" Caps had]: "+capsPre);
					System.err.println("[set Bitmap "+bmp+" Caps new]: "+capsNew);
					System.err.println("XXX-A1: "+animator.toString());
//					glJPanel.setRequestedGLCapabilities(capsNew);
					System.err.println("XXX-A2: "+animator.toString());
					System.err.println("XXX: "+glJPanel.toString());
				}
			} };
			new AWTKeyAdapter(kl, glJPanel).addTo(glJPanel);
			final long t0 = System.currentTimeMillis();
			long t1 = t0;
			boolean triggerSnap = false;
			while(!quitAdapter.shouldQuit() && t1 - t0 < duration) {
				Thread.sleep(100);
				t1 = System.currentTimeMillis();
				snap.getDisplayCount();
				if( !triggerSnap && snap.getDisplayCount() > 1 ) {
					// Snapshot only after one frame has been rendered to suite FBO MSAA!
					snap.setMakeSnapshot();
					triggerSnap = true;
				}
			}
			Assert.assertNotNull(frame);
			Assert.assertNotNull(glJPanel);
			Assert.assertNotNull(animator);
			if( useAnimator ) {
				animator.stop();
				Assert.assertEquals(false, animator.isAnimating());
			}
			SwingUtilities.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					frame.setVisible(false);
					frame.getContentPane().remove(glJPanel);
					frame.remove(glJPanel);
					glJPanel.destroy();
					frame.dispose();
				} } );
	}

	@Test
	public void test01_DefaultMsaa()
			throws AWTException, InterruptedException, InvocationTargetException
	{
		if( manualTest ) {
			return;
		}
		final GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL4));
		caps.setNumSamples(4);
		caps.setSampleBuffers(true);
		runTestGL(caps);
	}

	static long duration = 500; // ms

	public static void main(final String args[]) {
		for(int i=0; i<args.length; i++) {
			if(args[i].equals("-time")) {
				i++;
				duration = MiscUtils.atol(args[i], duration);
			} else if(args[i].equals("-vsync")) {
				i++;
				swapInterval = MiscUtils.atoi(args[i], swapInterval);
			} else if(args[i].equals("-msaa")) {
				useMSAA = true;
			} else if(args[i].equals("-noanim")) {
				useAnimator = false;
			} else if(args[i].equals("-pbuffer")) {
				shallUsePBuffer = true;
			} else if(args[i].equals("-bitmap")) {
				shallUseBitmap = true;
			} else if(args[i].equals("-manual")) {
				manualTest = true;
			}
		}
		System.err.println("swapInterval "+swapInterval);
		System.err.println("useMSAA "+useMSAA);
		System.err.println("useAnimator "+useAnimator);
		System.err.println("shallUsePBuffer "+shallUsePBuffer);
		System.err.println("shallUseBitmap "+shallUseBitmap);
		System.err.println("manualTest "+manualTest);
		org.junit.runner.JUnitCore.main(TestInstancedReneringGL4AWT.class.getName());
	}
}
