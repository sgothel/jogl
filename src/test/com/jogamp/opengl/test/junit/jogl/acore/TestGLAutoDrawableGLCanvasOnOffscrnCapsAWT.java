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
 
package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.IOException;

import javax.media.nativewindow.CapabilitiesImmutable;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.opengl.GLGraphicsConfigurationUtil;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Tests using an AWT {@link GLCanvas} {@link GLAutoDrawable auto drawable} for on- and offscreen cases.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLAutoDrawableGLCanvasOnOffscrnCapsAWT extends UITestCase {
    static final int widthStep = 800/4;
    static final int heightStep = 600/4;
    static boolean waitForKey = false;
    volatile int szStep = 2;

    static GLCapabilities getCaps(String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        }
        return new GLCapabilities(GLProfile.get(profile));
    }
    
    static void setGLCanvasSize(final Frame frame, final GLCanvas glc, final int width, final int height) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    final Dimension new_sz = new Dimension(width, height);
                    glc.setMinimumSize(new_sz);
                    glc.setPreferredSize(new_sz);
                    glc.setSize(new_sz);
                    frame.pack();
                    frame.validate();
                } } );
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }       
    }
    
    static interface MyGLEventListener extends GLEventListener {
        void setMakeSnapshot();
    }
    
    void doTest(GLCapabilitiesImmutable reqGLCaps, GLEventListener demo) throws InterruptedException {
        if(reqGLCaps.isOnscreen() && JAWTUtil.isOffscreenLayerRequired()) {
            System.err.println("onscreen layer n/a");
            return;
        }
        if(!reqGLCaps.isOnscreen() && !JAWTUtil.isOffscreenLayerSupported()) {
            System.err.println("offscreen layer n/a");
            return;
        }
        System.out.println("Requested  GL Caps: "+reqGLCaps);
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(reqGLCaps.getGLProfile());
        final GLCapabilitiesImmutable expGLCaps = GLGraphicsConfigurationUtil.fixGLCapabilities(reqGLCaps, factory, null);
        System.out.println("Expected   GL Caps: "+expGLCaps);
        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final GLCanvas glad = new GLCanvas(reqGLCaps); // will implicit trigger offscreen layer - if !onscreen && supported
        Assert.assertNotNull(glad);
        Dimension glc_sz = new Dimension(widthStep*szStep, heightStep*szStep);
        glad.setMinimumSize(glc_sz);
        glad.setPreferredSize(glc_sz);
        glad.setSize(glc_sz);
        final Frame frame = new Frame(getSimpleTestName("."));
        Assert.assertNotNull(frame);
        frame.add(glad);        
        
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.pack();
                    frame.setVisible(true);
                }});
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glad, true));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glad, true));
        System.out.println("Window: "+glad.getClass().getName());
        
        // Check caps of NativeWindow config w/o GL
        final CapabilitiesImmutable chosenCaps = glad.getChosenGLCapabilities();
        System.out.println("Window Caps Pre_GL: "+chosenCaps);
        Assert.assertNotNull(chosenCaps);
        Assert.assertTrue(chosenCaps.getGreenBits()>5);
        Assert.assertTrue(chosenCaps.getBlueBits()>5);
        Assert.assertTrue(chosenCaps.getRedBits()>5);

        glad.display(); // force native context creation
        
        //
        // Create native OpenGL resources .. XGL/WGL/CGL .. 
        // equivalent to GLAutoDrawable methods: setVisible(true)
        //         
        {
            final GLDrawable actualDrawable = glad.getDelegatedDrawable();
            Assert.assertNotNull(actualDrawable);
            System.out.println("Drawable    Pre-GL(0): "+actualDrawable.getClass().getName()+", "+actualDrawable.getNativeSurface().getClass().getName());
        }
        
        System.out.println("Window Caps PostGL   : "+glad.getChosenGLCapabilities());
        System.out.println("Drawable   Post-GL(1): "+glad.getClass().getName()+", "+glad.getNativeSurface().getClass().getName());
        
        // Check caps of GLDrawable after realization
        final GLCapabilitiesImmutable chosenGLCaps = glad.getChosenGLCapabilities();
        System.out.println("Chosen     GL Caps(1): "+chosenGLCaps);
        Assert.assertNotNull(chosenGLCaps);
        Assert.assertTrue(chosenGLCaps.getGreenBits()>5);
        Assert.assertTrue(chosenGLCaps.getBlueBits()>5);
        Assert.assertTrue(chosenGLCaps.getRedBits()>5);
        Assert.assertTrue(chosenGLCaps.getDepthBits()>4);
        Assert.assertEquals(expGLCaps.isOnscreen(), chosenGLCaps.isOnscreen());
        Assert.assertEquals(expGLCaps.isFBO(), chosenGLCaps.isFBO());
        Assert.assertEquals(expGLCaps.isPBuffer(), chosenGLCaps.isPBuffer());
        Assert.assertEquals(expGLCaps.isBitmap(), chosenGLCaps.isBitmap());
        /** Single/Double buffer cannot be checked since result may vary .. 
        if(chosenGLCaps.isOnscreen() || chosenGLCaps.isFBO()) {
            // dbl buffer may be disabled w/ offscreen pbuffer and bitmap
            Assert.assertEquals(expGLCaps.getDoubleBuffered(), chosenGLCaps.getDoubleBuffered());
        } */

        {
            GLContext context = glad.getContext();
            System.out.println("Chosen     GL CTX (2): "+context.getGLVersion());
            Assert.assertNotNull(context);
            Assert.assertTrue(context.isCreated());
        }
        
        System.out.println("Chosen     GL Caps(2): "+glad.getChosenGLCapabilities());
        System.out.println("Drawable   Post-GL(2): "+glad.getClass().getName()+", "+glad.getNativeSurface().getClass().getName());
        
        glad.addGLEventListener(demo);
        
        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        glad.addGLEventListener(snapshotGLEventListener);
        
        glad.display(); // initial resize/display
        
        // 1 - szStep = 2
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getWidth()+"x"+glad.getHeight(), 
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();
        
        // 2, 3 (resize + display)
        szStep = 1;
        setGLCanvasSize(frame, glad, widthStep*szStep, heightStep*szStep);
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getWidth()+"x"+glad.getHeight(), 
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        glad.display();
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();
                
        // 4, 5 (resize + display)
        szStep = 4;
        setGLCanvasSize(frame, glad, widthStep*szStep, heightStep*szStep);
        Assert.assertTrue("Size not reached: Expected "+(widthStep*szStep)+"x"+(heightStep*szStep)+", Is "+glad.getWidth()+"x"+glad.getHeight(), 
                          AWTRobotUtil.waitForSize(glad, widthStep*szStep, heightStep*szStep));
        glad.display();
        snapshotGLEventListener.setMakeSnapshot();
        glad.display();
        
        Thread.sleep(50);
        
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.remove(glad);
                    frame.dispose();
                }});
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }        
        System.out.println("Fin: "+glad);
    }

    @Test
    public void testAvailableInfo() {
        GLDrawableFactory f = GLDrawableFactory.getDesktopFactory();
        if(null != f) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(f.getDefaultDevice(), null, true).toString());
        }
        f = GLDrawableFactory.getEGLFactory();
        if(null != f) {
            System.err.println(JoglVersion.getDefaultOpenGLInfo(f.getDefaultDevice(), null, true).toString());
        }
    }
    
    @Test
    public void testGL2OnScreenDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OnScreenDblBufStencil() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setStencilBits(1);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OnScreenDblBufMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OnScreenDblBufStencilMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setStencilBits(1);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OffScreenAutoDblBuf() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        doTest(reqGLCaps, new GearsES2(1));
    }

    @Test
    public void testGL2OffScreenFBODblBufStencil() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setStencilBits(1);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OffScreenFBODblBufMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OffScreenFBODblBufStencilMSAA() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setFBO(true);
        reqGLCaps.setStencilBits(1);
        reqGLCaps.setSampleBuffers(true);
        reqGLCaps.setNumSamples(4);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    @Test
    public void testGL2OffScreenPbuffer() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2);
        if(null == reqGLCaps) return;
        reqGLCaps.setOnscreen(false);
        reqGLCaps.setPBuffer(true);
        doTest(reqGLCaps, new GearsES2(1));
    }
    
    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        if(waitForKey) {
            UITestCase.waitForKey("Start");
        }
        org.junit.runner.JUnitCore.main(TestGLAutoDrawableGLCanvasOnOffscrnCapsAWT.class.getName());
    }

}
