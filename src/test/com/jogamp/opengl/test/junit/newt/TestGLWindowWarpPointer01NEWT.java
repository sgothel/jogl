package com.jogamp.opengl.test.junit.newt;

import java.io.IOException;
import java.util.Random;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.Screen;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLWindowWarpPointer01NEWT  extends UITestCase {
    static GLProfile glp;
    static int width, height;
    static long durationPerTest = 2000; // ms

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glp = GLProfile.getDefault();
    }

    static GLWindow createWindow(Screen screen, GLCapabilitiesImmutable caps)
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

        GLEventListener demo = new GearsES2();
        glWindow.addGLEventListener(demo);

        glWindow.setSize(512, 512);
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
    public void testWarp01Center() throws InterruptedException {
        testWarpImpl(false);
    }
    
    @Test
    public void testWarp02Random() throws InterruptedException {
        testWarpImpl(true);
    }
    
    void testWarpImpl(final boolean random) throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        final GLWindow window1 = createWindow(null, caps); // local
        Assert.assertEquals(true,window1.isNativeValid());
        Assert.assertEquals(true,window1.isVisible());
        Animator animator = new Animator();
        animator.setUpdateFPSFrames(1, null);
        animator.add(window1);
        animator.start();
        AbstractGraphicsDevice device1 = window1.getScreen().getDisplay().getGraphicsDevice();

        System.err.println("GLProfiles window1: "+device1.getConnection()+": "+GLProfile.glAvailabilityToString(device1));
        
        window1.warpPointer(width / 2, height / 2);
        window1.requestFocus();
        
        window1.addMouseListener(new MouseAdapter() {
            void warpCenter() {
                window1.warpPointer(width / 2, height / 2);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
                warpCenter();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
            }
        });
        
        if( random ) {
            window1.addGLEventListener(new GLEventListener() {
                final Random r = new Random();
    
                void warpRandom(int width, int height) {
                    int x = r.nextInt(width);
                    int y = r.nextInt(height);
                    window1.warpPointer(x, y);
                }
                
                @Override
                public void init(GLAutoDrawable drawable) {}
    
                @Override
                public void dispose(GLAutoDrawable drawable) {}
    
                @Override
                public void display(GLAutoDrawable drawable) {
                    warpRandom(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
                }
    
                @Override
                public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
                
            });
        }
        
        while(animator.isAnimating() && animator.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }

        destroyWindow(window1);
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            }
        }
        System.out.println("durationPerTest: "+durationPerTest);
        String tstname = TestGLWindowWarpPointer01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
