package com.jogamp.test.junit.newt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Robot;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.swing.JFrame;

import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.test.junit.jogl.demos.es1.RedSquare;

public class TestFocus01SwingAWT {

    static {
        GLProfile.initSingleton();
    }

    static int width, height;

    static long durationPerTest = 800;

    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        width = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void testNewtCanvasAWTRequestFocus() throws AWTException,
            InvocationTargetException, InterruptedException {
        // Create a window.
        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("testNewtChildFocus");
        GLEventListener demo1 = new RedSquare();
        TestListenerCom01AWT.setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        // Monitor NEWT focus and keyboard events.
        NewtKeyAdapter newtKeyAdapter = new NewtKeyAdapter();
        glWindow1.addKeyListener(newtKeyAdapter);
        NewtFocusAdapter newtFocusAdapter = new NewtFocusAdapter();
        glWindow1.addWindowListener(newtFocusAdapter);

        // Wrap the window in a canvas.
        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        // Monitor AWT focus and keyboard events.
        AWTKeyAdapter awtKeyAdapter = new AWTKeyAdapter();
        newtCanvasAWT.addKeyListener(awtKeyAdapter);
        AWTFocusAdapter awtFocusAdapter = new AWTFocusAdapter();
        newtCanvasAWT.addFocusListener(awtFocusAdapter);

        // Add the canvas to a frame, and make it all visible.
        JFrame frame1 = new JFrame("Swing AWT Parent Frame: "
                + glWindow1.getTitle());
        frame1.getContentPane().add(newtCanvasAWT, BorderLayout.CENTER);
        frame1.setSize(width, height);
        frame1.setVisible(true);

        // Request the focus, which should automatically provide the window
        // with focus.
        newtCanvasAWT.requestFocus();

        Animator animator = new Animator(glWindow1);
        animator.start();

        // Wait for the window to initialize and receive focus.
        // TODO Eliminate the need for this delay.
        while (glWindow1.getDuration() < durationPerTest) {
            Thread.sleep(100);
        }

        // Verify focus status.
        assertFalse("AWT parent canvas has focus", newtCanvasAWT.hasFocus());
        assertTrue(newtCanvasAWT.getNEWTChild().hasFocus());

        // Type two keys, which should be directed to the focused window.
        Robot robot = new Robot();
        robot.keyPress(java.awt.event.KeyEvent.VK_A);
        robot.keyRelease(java.awt.event.KeyEvent.VK_A);
        robot.keyPress(java.awt.event.KeyEvent.VK_B);
        robot.keyRelease(java.awt.event.KeyEvent.VK_B);

        // Wait for the events to be processed.
        // TODO Eliminate the need for this delay.
        while (glWindow1.getDuration() < 2 * durationPerTest) {
            Thread.sleep(100);
        }

        assertEquals(1, awtFocusAdapter.focusLost);
        assertEquals(1, newtFocusAdapter.focusGained);
        assertEquals("AWT parent canvas received keyboard events", 0,
                awtKeyAdapter.keyTyped);
        assertEquals(2, newtKeyAdapter.keyTyped);

        // Remove listeners to avoid logging during dispose/destroy.
        glWindow1.removeKeyListener(newtKeyAdapter);
        glWindow1.removeWindowListener(newtFocusAdapter);
        newtCanvasAWT.removeKeyListener(awtKeyAdapter);
        newtCanvasAWT.removeFocusListener(awtFocusAdapter);

        // Shutdown the test.
        animator.stop();
        frame1.dispose();
        glWindow1.destroy(true);
    }

    private static final class NewtFocusAdapter extends WindowAdapter {

        int focusGained = 0;

        int focusLost = 0;

        @Override
        public void windowGainedFocus(WindowEvent e) {
            System.out.println(e);
            ++focusGained;
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
            System.out.println(e);
            ++focusLost;
        }
    }

    private static final class AWTFocusAdapter implements FocusListener {

        int focusGained = 0;

        int focusLost = 0;

        @Override
        public void focusGained(FocusEvent e) {
            System.out.println(e);
            ++focusGained;
        }

        @Override
        public void focusLost(FocusEvent e) {
            System.out.println(e);
            ++focusLost;
        }
    }

    private static final class NewtKeyAdapter extends KeyAdapter {

        int keyTyped;

        @Override
        public void keyTyped(KeyEvent e) {
            System.out.println(e);
            ++keyTyped;
        }
    }

    private static final class AWTKeyAdapter extends java.awt.event.KeyAdapter {

        int keyTyped;

        @Override
        public void keyTyped(java.awt.event.KeyEvent e) {
            System.out.println(e);
            ++keyTyped;
        }
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
        String tstname = TestFocus01SwingAWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }


}
