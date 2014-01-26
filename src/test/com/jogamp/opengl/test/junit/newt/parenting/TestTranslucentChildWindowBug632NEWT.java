package com.jogamp.opengl.test.junit.newt.parenting;

import java.io.IOException;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindow;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestTranslucentChildWindowBug632NEWT extends UITestCase {
    static long durationPerTest = 2*300;
    static GLProfile glp;
    static boolean opaque;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        opaque = false;
    }
    
    static GLWindow createParentWindow(GLCapabilitiesImmutable caps, int width, int height)
            throws InterruptedException
        {
            Assert.assertNotNull(caps);
            //
            // Create native windowing resources .. X11/Win/OSX
            // 
            GLWindow glWindow;
            glWindow = GLWindow.create(caps);
            Assert.assertNotNull(glWindow);
            
            glWindow.setTitle("NEWT Parenting Window Test");
            
            glWindow.addGLEventListener(new GearsES2(1));

            glWindow.setSize(width, height);
            glWindow.setVisible(true);
            Assert.assertEquals(true,glWindow.isVisible());
            Assert.assertEquals(true,glWindow.isNativeValid());
            
            return glWindow;
        }
    
    static GLWindow createNestedWindow(NativeWindow nativeParentWindow, GLCapabilitiesImmutable caps, int x, int y, int width, int height)
            throws InterruptedException {
         
        Assert.assertNotNull(nativeParentWindow);
        Assert.assertNotNull(caps);
         //
         // Create native windowing resources .. X11/Win/OSX
         // 
         GLWindow glWindow;
         glWindow = GLWindow.create(nativeParentWindow, caps);
         Assert.assertNotNull(glWindow);
         
         glWindow.setTitle("NEWT Parenting Window Test");
         
         glWindow.addGLEventListener(new GearsES2(1));

         glWindow.setPosition(x, y);
         glWindow.setSize(width, height);
         glWindow.setVisible(true);
         Assert.assertEquals(true,glWindow.isVisible());
         Assert.assertEquals(true,glWindow.isNativeValid());
         
         return glWindow;
    }
    
    static void destroyWindow(GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
            Assert.assertEquals(false,glWindow.isNativeValid());
        }
    }
    
    @Test
    public void testWindow00() throws InterruptedException {
        final Animator animator = new Animator();

        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        caps.setBackgroundOpaque(opaque);
        GLWindow window1 = createParentWindow(caps, 400, 400);
        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(true,window1.isVisible());
        animator.add(window1);
        
        GLWindow window2 = createNestedWindow(window1, caps, 400-300, 400-300, 300, 300);
        Assert.assertEquals(true,window2.isNativeValid());
        Assert.assertEquals(true,window2.isVisible());
        animator.add(window2);
        
        animator.start();
        
        AbstractGraphicsDevice device1 = window1.getScreen().getDisplay().getGraphicsDevice();

        System.err.println("GLProfiles window1: "+device1.getConnection()+": "+GLProfile.glAvailabilityToString(device1));

        Thread.sleep(durationPerTest/2);
        
        window1.setSize(512, 512);
        window2.setPosition(512-300, 512-300);
        
        Thread.sleep(durationPerTest/2);
        
        animator.stop();
        
        destroyWindow(window2);
        destroyWindow(window1);
    }
    
    public static void main(String[] args) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atol(args[++i], durationPerTest);
            }
        }
        String testName = TestTranslucentChildWindowBug632NEWT.class.getName();
        org.junit.runner.JUnitCore.main(testName);
    }    
}
