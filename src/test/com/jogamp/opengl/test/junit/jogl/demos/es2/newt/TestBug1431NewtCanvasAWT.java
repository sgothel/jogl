package com.jogamp.opengl.test.junit.jogl.demos.es2.newt;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;

import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Self-contained example (within a single class only to keep it simple)
 * displaying a rotating quad
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug1431NewtCanvasAWT extends UITestCase {

    public static class JOGLQuadNewt implements GLEventListener {
        final int[] exp_gl_viewport = { -1, -1, -1, -1 };
        final int[] has_gl_viewport = { -1, -1, -1, -1 };
        private float rotateT = 0.0f;

        @Override
        public void display(final GLAutoDrawable gLDrawable)
        {
            final GL2 gl = gLDrawable.getGL().getGL2();
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
            gl.glLoadIdentity();
            gl.glTranslatef(0.0f, 0.0f, -5.0f);

            // rotate about the three axes
            gl.glRotatef(rotateT, 1.0f, 0.0f, 0.0f);
            gl.glRotatef(rotateT, 0.0f, 1.0f, 0.0f);
            gl.glRotatef(rotateT, 0.0f, 0.0f, 1.0f);

            // Draw A Quad
            gl.glBegin(GL2ES3.GL_QUADS);
            gl.glColor3f(0.0f, 1.0f, 1.0f); // set the color of the quad
            gl.glVertex3f(-1.0f, 1.0f, 0.0f); // Top Left
            gl.glVertex3f(1.0f, 1.0f, 0.0f); // Top Right
            gl.glVertex3f(1.0f, -1.0f, 0.0f); // Bottom Right
            gl.glVertex3f(-1.0f, -1.0f, 0.0f); // Bottom Left
            // Done Drawing The Quad
            gl.glEnd();

            // increasing rotation for the next iteration
            rotateT += 0.2f;
        }

        @Override
        public void init(final GLAutoDrawable glDrawable)
        {
            final GL2 gl = glDrawable.getGL().getGL2();
            gl.glShadeModel(GLLightingFunc.GL_SMOOTH);
            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClearDepth(1.0f);
            gl.glEnable(GL.GL_DEPTH_TEST);
            gl.glDepthFunc(GL.GL_LEQUAL);
            gl.glHint(GL2ES1.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        }

        @Override
        public void reshape(final GLAutoDrawable glDrawable, final int x, final int y, final int width, final int height)
        {
            final GL2 gl = glDrawable.getGL().getGL2();
            exp_gl_viewport[0] = x;
            exp_gl_viewport[1] = y;
            exp_gl_viewport[2] = width;
            exp_gl_viewport[3] = height;
            gl.glGetIntegerv(GL.GL_VIEWPORT, has_gl_viewport, 0);
            System.err.println("GLEL reshape: Surface "+glDrawable.getSurfaceWidth()+"x"+glDrawable.getSurfaceWidth()+
                             ", reshape "+x+"/"+y+" "+width+"x"+height);
            System.err.println("GLEL reshape: Viewport "+has_gl_viewport[0]+"/"+has_gl_viewport[1]+", "+has_gl_viewport[2]+"x"+has_gl_viewport[3]);

            final float aspect = (float) width / (float) height;
            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glLoadIdentity();
            final float fh = 0.5f;
            final float fw = fh * aspect;
            gl.glFrustumf(-fw, fw, -fh, fh, 1.0f, 1000.0f);
            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();
        }

        @Override
        public void dispose(final GLAutoDrawable gLDrawable)
        {}
    }

    static long duration = 500; // ms

    static void setFrameSize(final Frame frame, final boolean frameLayout, final int newWith, final int newHeight) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final java.awt.Dimension d = new java.awt.Dimension(newWith, newHeight);
                    frame.setSize(d);
                    if( frameLayout ) {
                        frame.validate();
                    }
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        // System.setProperty("sun.awt.noerasebackground", "true");
        final GLProfile profile = GLProfile.getDefault();
        final GLCapabilities glCapabilities = new GLCapabilities(profile);
        final GLWindow glWindow = GLWindow.create(glCapabilities);
        final GLEventListener demo = new JOGLQuadNewt();
        // final GLEventListener demo = new RedSquareES2(1);
        glWindow.addGLEventListener( demo );

        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        snap.setPostSNDetail(getClass().getSimpleName());
        glWindow.addGLEventListener(snap);

        glWindow.addWindowListener(new com.jogamp.newt.event.WindowAdapter() {
            @Override
            public void windowResized(final com.jogamp.newt.event.WindowEvent e) {
                System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
            }
            @Override
            public void windowMoved(final com.jogamp.newt.event.WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
            }
        });

        final NewtCanvasAWT canvas = new NewtCanvasAWT(glWindow);
        final Frame frame = new Frame(getClass().getSimpleName());
        final Animator animator = new Animator(glWindow);
        final boolean[] isClosed = { false };

        frame.add(canvas);
        frame.setSize(640, 480);
        // frame.setResizable(false);
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(final java.awt.event.WindowEvent e)
            {
                isClosed[0] = true;
                animator.stop();
                frame.dispose();
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame.validate();
                frame.setVisible(true);
            }
        });
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true, null));
        Assert.assertEquals(true,  NewtTestUtil.waitForRealized(glWindow, true, null));

        animator.start();
        Assert.assertTrue(animator.isAnimating());
        Assert.assertTrue(animator.isStarted());

        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
        System.err.println("XXXX");

        canvas.requestFocus();
        snap.setMakeSnapshot();

        Thread.sleep(100);
        setFrameSize(frame, false /* frameLayout */, 800, 600);
        snap.setMakeSnapshot();
        Thread.sleep(10);
        snap.setMakeSnapshot();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!isClosed[0] && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        if( !isClosed[0] ) {
            animator.stop();
            Assert.assertFalse(animator.isAnimating());
            Assert.assertFalse(animator.isStarted());
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    frame.dispose();
                }
            });
            glWindow.destroy();
            Assert.assertEquals(true,  NewtTestUtil.waitForRealized(glWindow, false, null));
        }
    }

    public static void main(final String[] args)
    {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            }
        }
        System.err.println("duration "+duration);
        org.junit.runner.JUnitCore.main(TestBug1431NewtCanvasAWT.class.getName());
    }
}
