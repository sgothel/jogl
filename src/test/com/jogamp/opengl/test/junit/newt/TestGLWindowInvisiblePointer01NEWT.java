package com.jogamp.opengl.test.junit.newt;

import java.io.IOException;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es1.GearsES1;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLWindowInvisiblePointer01NEWT  extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static long durationPerTest = 4000; // ms

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(final Screen screen, final GLCapabilitiesImmutable caps)
        throws InterruptedException
    {
        Assert.assertNotNull(caps);
        //
        // Create native windowing resources .. X11/Win/OSX
        //
        GLWindow glWindow;
        if(null!=screen) {
            glWindow = GLWindow.create(screen, caps);
            Assert.assertNotNull(glWindow);
        } else {
            glWindow = GLWindow.create(caps);
            Assert.assertNotNull(glWindow);
        }
        glWindow.setUpdateFPSFrames(1, null);

        final GLEventListener demo = new GearsES1();
        glWindow.addGLEventListener(demo);

        glWindow.setSize(512, 512);
        glWindow.setVisible(true);
        Assert.assertEquals(true,glWindow.isVisible());
        Assert.assertEquals(true,glWindow.isNativeValid());

        return glWindow;
    }

    static void destroyWindow(final GLWindow glWindow) {
        if(null!=glWindow) {
            glWindow.destroy();
            Assert.assertEquals(false,glWindow.isNativeValid());
        }
    }

    @Test
    public void testWindow00() throws InterruptedException {
        final GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final GLWindow window1 = createWindow(null, caps); // local
        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(true,window1.isVisible());
        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(1, null);
        animator.add(window1);
        animator.start();
        final AbstractGraphicsDevice device1 = window1.getScreen().getDisplay().getGraphicsDevice();

        System.err.println("GLProfiles window1: "+device1.getConnection()+": "+GLProfile.glAvailabilityToString(device1));

        window1.warpPointer(width / 2, height / 2);
        window1.requestFocus();
        while(animator.isAnimating() && animator.getTotalFPSDuration()<durationPerTest) {
        	final boolean pointerVisibleNewVal = (animator.getTotalFPSDuration()/100)%2==0;
        	window1.setPointerVisible(pointerVisibleNewVal);
        	Assert.assertEquals(pointerVisibleNewVal,window1.isPointerVisible());
            Thread.sleep(100);
        }

        destroyWindow(window1);
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        final String tstname = TestGLWindowInvisiblePointer01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
