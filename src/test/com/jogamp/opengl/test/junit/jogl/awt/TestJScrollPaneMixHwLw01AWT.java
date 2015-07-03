package com.jogamp.opengl.test.junit.jogl.awt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.ScrollPane;
import java.awt.Shape;
import java.io.IOException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Documenting Bug 586
 *
 * <p>
 * JScrollPane cannot mix hw/lw components, only if setting property '-Dsun.awt.disableMixing=true'.
 * </p>
 * <p>
 * You can use ScrollPane, or maybe a slider and fwd the panning to the GLCanvas,
 * which could change it's GL viewport accordingly.
 * </p>
 * See git commit '8df12ca151dfc577c90b485d4ebfe491b88e55aa'.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestJScrollPaneMixHwLw01AWT extends UITestCase {
    static long durationPerTest = 500;

    static {
        // too late: use at cmd-line '-Dsun.awt.disableMixing=true' works
        // System.setProperty("sun.awt.disableMixing", "true");
    }

    /**
     * Doesn't work either ..
     */
    @SuppressWarnings("serial")
    public static class TransparentJScrollPane extends JScrollPane {

        public TransparentJScrollPane(final Component view) {
            super(view);

            setOpaque(false);

            try {
                ReflectionUtil.callStaticMethod(
                                            "com.sun.awt.AWTUtilities", "setComponentMixingCutoutShape",
                                            new Class<?>[] { Component.class, Shape.class },
                                            new Object[] { this, new Rectangle() } ,
                                            GraphicsConfiguration.class.getClassLoader());
                System.err.println("com.sun.awt.AWTUtilities.setComponentMixingCutoutShape(..) passed");
            } catch (final RuntimeException re) {
                System.err.println("com.sun.awt.AWTUtilities.setComponentMixingCutoutShape(..) failed: "+re.getMessage());
            }
        }

        @Override
        public void setOpaque(final boolean isOpaque) {
        }
    }

    protected void runTestGL(final GLCapabilities caps, final boolean useJScroll) throws InterruptedException {
        final String typeS = useJScroll ? "LW" : "HW";
        final JFrame frame = new JFrame("Mix Hw/Lw Swing - ScrollPane "+typeS);
        Assert.assertNotNull(frame);

        final Dimension f_sz = new Dimension(600,400);
        final Dimension glc_sz = new Dimension(500,600);

        final GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        glCanvas.addGLEventListener(new GearsES2());
        glCanvas.setPreferredSize(glc_sz);

        final JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        if(useJScroll) {
            final JScrollPane scrollPane = new TransparentJScrollPane(glCanvas);
            panel.add(scrollPane, BorderLayout.CENTER);
        } else {
            final ScrollPane scrollPane = new ScrollPane();
            scrollPane.add(glCanvas);
            panel.add(scrollPane, BorderLayout.CENTER);
        }

        final JTextArea textArea = new JTextArea();
        textArea.setText("Test\nTest\nTest\nTest\n");

        panel.add(textArea, BorderLayout.NORTH);

        frame.add(panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setLocationRelativeTo(null);
                    frame.setTitle("GLCanvas in JScrollPane example");
                    frame.setSize(f_sz);
                    frame.setVisible(true);
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }

        final Animator animator = new Animator(glCanvas);
        animator.start();

        Thread.sleep(durationPerTest);

        animator.stop();

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.dispose();
                }});
        } catch (final Throwable t) {
            t.printStackTrace();
            Assume.assumeNoException(t);
        }
    }

    // @Test doesn't work
    public void test01JScrollPane() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, true);
    }

    @Test
    public void test01ScrollPane() throws InterruptedException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities(glp);
        runTestGL(caps, false);
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atol(args[++i], durationPerTest);
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine());
        */
        System.out.println("durationPerTest: "+durationPerTest);
        final String tstname = TestJScrollPaneMixHwLw01AWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
